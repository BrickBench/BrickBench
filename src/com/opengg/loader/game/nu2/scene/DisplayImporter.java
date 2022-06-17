package com.opengg.loader.game.nu2.scene;

import com.opengg.core.console.GGConsole;
import com.opengg.core.math.Matrix4f;
import com.opengg.core.model.Material;
import com.opengg.core.model.Model;
import com.opengg.core.model.io.AssimpModelLoader;
import com.opengg.loader.BrickBench;
import com.opengg.loader.EditorEntity;
import com.opengg.loader.Util;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.commands.DisplayCommand;
import com.opengg.loader.game.nu2.scene.commands.DisplayCommandResource;
import com.opengg.loader.game.nu2.scene.commands.MatrixCommandResource;
import com.opengg.loader.game.nu2.scene.commands.UntypedCommandResource;
import com.opengg.loader.loading.MapLoader;
import com.opengg.loader.loading.MapWriter;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.opengg.loader.loading.MapWriter.WritableObject.SCENE;

public class DisplayImporter {
    public static String importModel(Path path, boolean reverseWindingOrder, boolean reverseX, boolean useShading) throws IOException {

        VertexFormat format = VertexFormat.POS_NORM_BI_COLOR_UV;

        if (!useShading) {
            format = VertexFormat.POS_BI_COLOR_UV;
        }

        if (!((NU2MapData)((NU2MapData)EditorState.getActiveMap().levelData())).scene().vertexBuffersBySize().containsKey(format.size)) {
            GGConsole.warning("Could not find vertex buffer of the right size when importing " + path + ": This map does not contain a buffer for " + format);
            JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "Could not import model, this map does not yet support custom meshes. \nPlease report this to the developers.");
            return null;
        }

        var model = AssimpModelLoader.loadModelAsTriStrip(path.toString(), Matrix4f.IDENTITY, reverseWindingOrder);

        for (var mesh : model.getMeshes()) {
            if (mesh.getVertices().size() > 65000) { //a bit under max value of uint16
                GGConsole.warning("Model " + path.getFileName() + " has a mesh with " + mesh.getVertices().size() + ", ending import");
                JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "This model is too large, a mesh with " + mesh.getVertices().size() + " vertices was found (maximum is 65,000). Please simplify or subdivide the model and try again.");
                return null;
            }
        }

        var textures = model.getMaterials().stream()
                .map(m -> path.getParent().resolve(m.mapKdFilename))
                .collect(Collectors.toList());

        if (!textures.stream()
                .filter(Files::isRegularFile)
                .map(t -> FilenameUtils.removeExtension(t.toString()) + ".dds")
                .allMatch(t -> Files.exists(Path.of(t)))) {
            GGConsole.warning("Imported model contained non-DDS images");
            JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "This model contains non-DDS images, and cannot be loaded");
            return null;
        }

        var lastDisplayList = getCustomListOrGenerate();

        var startingIndex = importTextures(textures);

        GGConsole.log("Imported textures at " + startingIndex);

        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

        var materialTypes = textures.stream()
                .map(t -> useShading ? MaterialWriter.MaterialType.COLOR_PHONG : MaterialWriter.MaterialType.TEXTURE_FLAT)
                .collect(Collectors.toList());

        int currentMaterialIndex = ((NU2MapData)EditorState.getActiveMap().levelData()).scene().materials().size();
        var materials = MaterialWriter.createMaterials(materialTypes);

        int textureCounter = 0;
        var ggMaterialToGSCMaterial = new HashMap<Material, Integer>();
        for (int i = 0; i < model.getMaterials().size(); i++) {
            var materialAddr = materials.get(i);
            var fileMaterial = model.getMaterials().get(i);

            if (Files.isRegularFile(textures.get(i))) {
                MapWriter.applyPatch(SCENE, materialAddr + 0x74, Util.littleEndian((short) (startingIndex + textureCounter)));
                MapWriter.applyPatch(SCENE, materialAddr + 0xB4 + 0x4, Util.littleEndian((short) (startingIndex + textureCounter)));

                textureCounter++;
            }

            MapWriter.applyPatch(SCENE, materialAddr + 0x54, fileMaterial.kd.toLittleEndianByteBuffer());
            ggMaterialToGSCMaterial.put(fileMaterial, currentMaterialIndex + i);
        }

        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

        var vBufs = getVertexBuffersFrom(model, reverseX, format);
        var iBufs = getIndexBuffersFrom(model);

        var iBufOffsets = SceneFileWriter.appendByteBuffers(iBufs, true, 0, 2);
        var vBufOffsets = SceneFileWriter.appendByteBuffers(vBufs, false,
                ((NU2MapData)EditorState.getActiveMap().levelData()).scene().vertexBuffersBySize().get(format.size).get(0), format.size);
        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

        var renderables = generateRenderablesFor(model, vBufOffsets, iBufOffsets,
                ((NU2MapData)EditorState.getActiveMap().levelData()).scene().vertexBuffersBySize().get(format.size).get(0), format.size);
        var renderableAddresses = DisplayWriter.addGameMeshes(renderables);

        var matrixBuffer = ByteBuffer.allocate(16 * Float.BYTES * renderables.size());
        IntStream.range(0, renderables.size()).forEach(i -> matrixBuffer.asFloatBuffer().put(i * 16, Matrix4f.IDENTITY.getLinearArray()));
        var matricesStart = DisplayWriter.appendToEnd(matrixBuffer);

        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

        var commands = new ArrayList<DisplayCommandResource<?>>();
        for (int i = 0; i < renderables.size(); i++) {
            commands.add(new UntypedCommandResource(matricesStart + (i * 16 * Float.BYTES), DisplayCommand.CommandType.MTXLOAD));
            commands.add(new UntypedCommandResource(renderableAddresses.get(i), DisplayCommand.CommandType.GEOMCALL));
        }

        int commandAddress = DisplayWriter.addDisplayCommands(commands);

        var first =  ((NU2MapData)EditorState.getActiveMap().levelData()).scene().renderCommandList().get(0);
        var newCommandIdx = (commandAddress - first.address()) / 16;

        var parts = new ArrayList<GameModel.GameModelPart>();
        var materialList = List.copyOf(((NU2MapData)EditorState.getActiveMap().levelData()).scene().materials().values());
        for (int i = 0; i < renderables.size(); i++) {
            var material = materialList.get(ggMaterialToGSCMaterial.get(model.getMeshes().get(i).getMaterial()));
            parts.add(new GameModel.GameModelPart(material, null, newCommandIdx + (i * 2) + 1)); //add GEOMCALL
        }

        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

        int newModelIndex = DisplayWriter.createNewGameModel(parts);
        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

        var materialsDynamicList = new ArrayList<DisplayCommandResource<?>>();
        for (var part : parts) {
            materialsDynamicList.add(part.material());
        }

        var lastCommand = lastDisplayList.get().commands().get(lastDisplayList.get().commands().size() - 1);

        DisplayWriter.addDisplayCommands(materialsDynamicList, lastCommand.index());

        var newModelName = ((NU2MapData)EditorState.getActiveMap().levelData()).scene().gameModels().get(newModelIndex).name();
        GGConsole.log("Model " + newModelName + " was imported");
        return newModelName;
    }

    public static EditorEntity.Ref<DisplayList> getCustomListOrGenerate() throws IOException {
        var lastDisplayList = ((NU2MapData)EditorState.getActiveMap().levelData()).scene().displayLists().get(((NU2MapData)EditorState.getActiveMap().levelData()).scene().displayLists().size() - 1);
        if (!lastDisplayList.isCustomList()) {
            GGConsole.log("Custom model display list not found, generating...");

            var commandList = new ArrayList<DisplayCommandResource<?>>();

            commandList.add(new UntypedCommandResource(0x99999999, DisplayCommand.CommandType.DUMMY));
            commandList.add(new UntypedCommandResource(0, DisplayCommand.CommandType.TERMINATE));

            var lastCommandIndex = lastDisplayList.commands().get(lastDisplayList.commands().size()-1).index() + 1;
            int commandAddress = DisplayWriter.addDisplayCommands(commandList, lastCommandIndex);

            EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));
            DisplayWriter.createDisplayList(commandAddress, 0);
            EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

            lastDisplayList = ((NU2MapData)EditorState.getActiveMap().levelData()).scene().displayLists().get(((NU2MapData)EditorState.getActiveMap().levelData()).scene().displayLists().size() - 1);
        }

        return lastDisplayList.ref();
    }

    public static void addModelToCustomList(GameModel model, Matrix4f matrix) throws IOException {
        var lastDisplayList = getCustomListOrGenerate();

        for (var part : model.modelParts()) {
            boolean foundMaterial = false;

            for (var command : lastDisplayList.get().commands()) {
                if (command.command() instanceof FileMaterial material && material.getID() == part.material().getID()) {
                    foundMaterial = true;
                    break;
                }
            }

            if (!foundMaterial) {
                List<DisplayCommandResource<?>> materialsDynamicList = List.of(part.material());

                var lastCommand = lastDisplayList.get().commands().get(lastDisplayList.get().commands().size() - 1);
                DisplayWriter.addDisplayCommands(materialsDynamicList, lastCommand.index());
                EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));
            }
        }

        ByteBuffer buf = ByteBuffer.allocate(16 * 4).order(ByteOrder.LITTLE_ENDIAN);
        buf.asFloatBuffer().put(matrix.getLinearArray()).flip();

        int matrixEnd = DisplayWriter.appendToEnd(buf);
        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

        var modelPath = model.path();
        for (int i = 0; i < model.modelParts().size(); i++) {
            int materialCommandIndex = -1;
            var currentModel = new EditorEntity.Ref<GameModel>(modelPath);
            var currentPart = currentModel.get().modelParts().get(i);

            for (var command : lastDisplayList.get().commands()) {
                if (command.command() instanceof FileMaterial material && material.getID() == currentPart.material().getID()) {
                    materialCommandIndex = command.index();
                    break;
                }
            }

            var newMeshCommands = new ArrayList<DisplayCommandResource<?>>();
            newMeshCommands.add(new MatrixCommandResource(matrixEnd, matrix));
            newMeshCommands.add(new UntypedCommandResource(currentPart.renderable().getAddress(), DisplayCommand.CommandType.GEOMCALL));

            DisplayWriter.addDisplayCommands(newMeshCommands, materialCommandIndex + 1);
            EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

            matrixEnd += 32;
        }
    }

    private static List<GSCMesh> generateRenderablesFor(Model model, List<Integer> vertexBufs, List<Integer> indexBufs, int vertIndex, int vertSize) {
        var renderables = new ArrayList<GSCMesh>();

        for (int i = 0; i < model.getMeshes().size(); i++) {
            var mesh = model.getMeshes().get(i);
            var vBufOffset = vertexBufs.get(i);
            var iBufOffset = indexBufs.get(i);

            var gscRenderable = new GSCMesh(0, mesh.getVertices().size(), vertSize,
                    vBufOffset, vertIndex,
                    mesh.getIndexBuffer().limit() - 2,
                    iBufOffset, 0, 0, 0);

            renderables.add(gscRenderable);
        }

        return renderables;
    }

    private static List<ByteBuffer> getVertexBuffersFrom(Model model, boolean reverseX, VertexFormat format) {
        var buffers = new ArrayList<ByteBuffer>();

        for (var mesh : model.getMeshes()) {
            var vertices = mesh.getVertices();
            var buffer = ByteBuffer.allocate(vertices.size() * format.size).order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < vertices.size(); i++) {
                var vertex = vertices.get(i);

                int offset = i * format.size;
                buffer.position(offset);

                var adjustedNormal = vertex.normal.add(1).divide(2).multiply(256);
                int adjustedX = (int) adjustedNormal.x, adjustedY = (int) adjustedNormal.y, adjustedZ = (int) adjustedNormal.z;

                var adjustedBitangent = vertex.biTangent.add(1).divide(2).multiply(256);
                int adjustedBiX = (int) adjustedBitangent.x, adjustedBiY = (int) adjustedBitangent.y, adjustedBiZ = (int) adjustedBitangent.z;

                buffer.putFloat(vertex.position.x * (reverseX ? -1 : 1)).putFloat(vertex.position.y).putFloat(vertex.position.z);
                if (format  == VertexFormat.POS_NORM_BI_COLOR_UV) {
                    buffer.put((byte) (adjustedX & 0xff)).put((byte) (adjustedY & 0xff)).put((byte) (adjustedZ & 0xff)).put((byte) 255);
                }
                buffer.put((byte) (adjustedBiX & 0xff)).put((byte) (adjustedBiY & 0xff)).put((byte) (adjustedBiZ & 0xff)).put((byte) 255);
                buffer.put((byte) (vertex.color.x() * 127)).put((byte) (vertex.color.y() * 127)).put((byte) (vertex.color.z() * 127)).put((byte) 255);

                buffer.putFloat(vertex.uvs.x).putFloat(vertex.uvs.y);
            }


            buffer.rewind();
            buffers.add(buffer);
        }

        return buffers;
    }

    private static List<ByteBuffer> getIndexBuffersFrom(Model model) {
        var buffers = new ArrayList<ByteBuffer>();
        for (var mesh : model.getMeshes()) {
            var shortbuf = ByteBuffer.allocate(2 * mesh.getIndexBuffer().limit()).order(ByteOrder.LITTLE_ENDIAN);
            while (mesh.getIndexBuffer().hasRemaining()) {
                shortbuf.putShort((short) mesh.getIndexBuffer().get());
            }
            shortbuf.rewind();
            mesh.getIndexBuffer().rewind();
            buffers.add(shortbuf);

        }

        return buffers;
    }

    public static int importTextures(List<Path> textures) throws IOException {
        var texList = new ArrayList<SceneFileWriter.TextureHeader>();

        int startingTexture = ((NU2MapData)EditorState.getActiveMap().levelData()).scene().textureCount().get();
        int currentTexture = startingTexture;
        for (var texture : textures) {
            if (!Files.exists(texture) || Files.isDirectory(texture)) continue;
            byte[] data = Files.readAllBytes(texture);

            var buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            buffer.position(7 * 4);
            int mipCount = buffer.getInt();

            var image = ImageIO.read(new ByteArrayInputStream(data));

            texList.add(new SceneFileWriter.TextureHeader(data, image.getWidth(), image.getHeight(), mipCount));
            ((NU2MapData)EditorState.getActiveMap().levelData()).xmlData().loadedTextures().put("Texture_" + currentTexture, texture.getFileName().toString());
            currentTexture++;
        }

        int targetAddress = SceneFileWriter.createTextureEntries(texList.size());
        SceneFileWriter.appendTextures(texList, targetAddress);
        return startingTexture;
    }

    public static void replaceTexture(FileTexture original, Path replacement) throws IOException {
        byte[] data = Files.readAllBytes(replacement);

        var buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(7 * 4);
        int mipCount = buffer.getInt();
        var image = ImageIO.read(new ByteArrayInputStream(data));
        var tex = new SceneFileWriter.TextureHeader(data, image.getWidth(), image.getHeight(), mipCount);
        SceneFileWriter.replaceTexture(original, tex);
        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

    }

    enum VertexFormat {
        POS_NORM_BI_COLOR_UV (32),
        POS_BI_COLOR_UV (28);

        public final int size;

        VertexFormat(int size) {
            this.size = size;
        }
    }
}
