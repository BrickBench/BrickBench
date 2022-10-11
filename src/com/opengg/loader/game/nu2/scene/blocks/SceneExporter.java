package com.opengg.loader.game.nu2.scene.blocks;

import com.opengg.core.math.Matrix4f;
import com.opengg.core.math.Vector2f;
import com.opengg.core.math.Vector3f;
import com.opengg.core.math.Vector4f;
import com.opengg.core.model.GGVertex;
import com.opengg.core.model.Material;
import com.opengg.core.model.Mesh;
import com.opengg.core.model.Model;
import com.opengg.core.render.shader.VertexArrayBinding;
import com.opengg.loader.SwingUtil;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.FileMaterial;
import com.opengg.loader.game.nu2.scene.FileTexture;
import com.opengg.loader.game.nu2.scene.GSCMesh;
import com.opengg.loader.game.nu2.scene.GameModel;
import org.lwjgl.util.meshoptimizer.MeshOptimizer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class SceneExporter {
    public record TransformableModelPart(GameModel.GameModelPart part, Matrix4f transform){}

    public static Model convertToEngineModel(List<TransformableModelPart> modelParts, String name, boolean writeMaterial, boolean generateAsTriStrip){

        var meshes = new ArrayList<Mesh>();
        var writtenMaterials = new HashMap<FileMaterial, Material>();
        for(var part : modelParts){
            var mesh = convertToEngineMesh((GSCMesh) part.part().renderable(), part.transform(), generateAsTriStrip);
            mesh.setTriStrip(generateAsTriStrip);

            if(!writtenMaterials.containsKey(part.part().material())){
                var material = getEngineMaterial(part.part().material());
                mesh.setMaterial(material);

                writtenMaterials.put(part.part().material(), material);
            } else {
                mesh.setMaterial(writtenMaterials.get(part.part().material()));

            }


            meshes.add(mesh);
        }

        return new Model(meshes, name);
    }

    public static Model convertToEngineModel(GameModel model, boolean writeMaterial, boolean generateAsTriStrip){
        return convertToEngineModel(model.modelParts().stream().map(m -> new TransformableModelPart(m, Matrix4f.IDENTITY)).collect(Collectors.toList()), model.name(), writeMaterial, generateAsTriStrip);
    }

    public static Model convertToEngineModel(GameModel model, boolean writeMaterial){
        return convertToEngineModel(model, writeMaterial, true);
    }

    private static Mesh convertToEngineMesh(GSCMesh mesh, Matrix4f transform, boolean generateAsTriStrip) {
        record Face(int v1, int v2, int v3) {}

        List<GGVertex> vertices = new ArrayList<>();
        List<Face> faces = new ArrayList<>();

        var indexBuffers = ((NU2MapData)EditorState.getActiveMap().levelData()).scene().gscIndexBuffers().get(mesh.indexListID);
        var indexBuffer = ByteBuffer.wrap(indexBuffers.contents()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();

        var vertexBuffers = ((NU2MapData)EditorState.getActiveMap().levelData()).scene().gscVertexBuffers().get(mesh.vertexListID);
        var vertexBuffer = ByteBuffer.wrap(vertexBuffers.contents()).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < mesh.vertexCount; i++) {

            int vertexIndex = (mesh.vertexOffset + i) * mesh.vertexSize;
            vertexBuffer.position(vertexIndex);

            var vec = new Vector3f(vertexBuffer.getFloat(), vertexBuffer.getFloat(), vertexBuffer.getFloat());
            var pos = transform.transform(vec);

            var normalProperty = mesh.material.getArrayBindings().stream().filter(s -> s.name().equals("vs_normal")).findFirst().orElse(
                    new VertexArrayBinding.VertexArrayAttribute("none", 8, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT2, 0)
            );

            var colorProperty = mesh.material.getArrayBindings().stream().filter(s -> s.name().equals("color")).findFirst().orElse(
                    new VertexArrayBinding.VertexArrayAttribute("none", 8, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT2, 0)
            );

            var bitangentProperty = mesh.material.getArrayBindings().stream().filter(s -> s.name().equals("bitangent")).findFirst().orElse(
                    new VertexArrayBinding.VertexArrayAttribute("none", 8, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT2, 0)
            );

            var texCoordProperty = mesh.material.getArrayBindings().stream().filter(s -> s.name().equals("vs_uv0")).findFirst().orElse(
                    new VertexArrayBinding.VertexArrayAttribute("none", 8, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT2, 0)
            );

            vertexBuffer.position(vertexIndex + normalProperty.offset());
            var normal = new Vector3f(
                    Byte.toUnsignedInt(vertexBuffer.get()) / 256f,
                    Byte.toUnsignedInt(vertexBuffer.get()) / 256f,
                    Byte.toUnsignedInt(vertexBuffer.get()) / 256f).multiply(2).subtract(1);

            vertexBuffer.position(vertexIndex + bitangentProperty.offset());
            var bitangent = new Vector3f(
                    Byte.toUnsignedInt(vertexBuffer.get()) / 256f,
                    Byte.toUnsignedInt(vertexBuffer.get()) / 256f,
                    Byte.toUnsignedInt(vertexBuffer.get()) / 256f).multiply(2).subtract(1);

            vertexBuffer.position(vertexIndex + colorProperty.offset());
            var color = new Vector4f(
                    Byte.toUnsignedInt(vertexBuffer.get()) / 128f,
                    Byte.toUnsignedInt(vertexBuffer.get()) / 128f,
                    Byte.toUnsignedInt(vertexBuffer.get()) / 128f,
                    Byte.toUnsignedInt(vertexBuffer.get())).multiply(2);

            vertexBuffer.position(vertexIndex + texCoordProperty.offset());
            var texCoord = new Vector2f(vertexBuffer.getFloat(), -vertexBuffer.getFloat());

            vertices.add(new GGVertex(pos, normal, texCoord).setColor(color).setBiTangent(bitangent));
        }

        var indices = new ArrayList<Integer>();
        if(generateAsTriStrip){
            for (int i = 0; i < mesh.triangleCount + 2; i++) {
                int indexIdx = i + mesh.indexOffset;
                indexBuffer.position(indexIdx);
                indices.add((int) indexBuffer.get());
            }
        }else{
            try (var scope = MemorySession.openConfined()) {
                var intIndices = MemorySegment.allocateNative((mesh.triangleCount + 2) * Integer.BYTES, scope).asByteBuffer().order(ByteOrder.nativeOrder()).asIntBuffer();

                for (int i = 0; i < mesh.triangleCount + 2; i++) {
                    int indexIdx = i + mesh.indexOffset;
                    indexBuffer.position(indexIdx);
                    intIndices.put(indexBuffer.get());
                }

                intIndices.flip();

                var unstripMaxSize = MeshOptimizer.meshopt_unstripifyBound(intIndices.capacity());
                var unstripIndices = MemorySegment.allocateNative(unstripMaxSize * Integer.BYTES, scope).asByteBuffer().order(ByteOrder.nativeOrder()).asIntBuffer();
                var unstripSize = MeshOptimizer.meshopt_unstripify(unstripIndices, intIndices, 0);
                unstripIndices = unstripIndices.slice(0, (int) unstripSize);

                for (int i = 0; i < unstripSize; i++) {
                    unstripIndices.position(i);
                    indices.add(unstripIndices.get());
                }
            }
        }

        var intIndices = indices.stream().mapToInt(i -> i).toArray();
        return new Mesh(vertices, intIndices, false);
    }

    public static void exportTexture(FileTexture texture, Path path) {
        try {
            Files.createDirectories(path.getParent());
            FileOutputStream fs = new FileOutputStream(path.toFile());
            fs.getChannel().write(texture.contents());
            fs.close();
        } catch (IOException e) {
            SwingUtil.showErrorAlert("Failed to export texture " + path, e);
        }

        texture.contents().rewind();
    }

    private static Material getEngineMaterial(FileMaterial material){
        var newMat = new Material(material.name());

        if(material.getDiffuseFileTexture() != null){
            var kdTex = EditorState.getActiveMap().levelData().name() + "_" + String.format("%03d", material.getDiffuseFileTexture().descriptor().trueIndex()) + ".dds";

            newMat.mapKdFilename = kdTex;
            newMat.mapKd = material.getTexture();
            System.out.println(kdTex);
            System.out.println(material.getTexture().getData().size());
            newMat.hascolmap = true;
        }

        if(material.getNormalFileTexture() != null){
            var bumpTex = EditorState.getActiveMap().levelData().name() + "_" + String.format("%03d", material.getDiffuseFileTexture().descriptor().trueIndex()) + ".dds";

            newMat.bumpFilename = bumpTex;
            newMat.bumpMap = material.getNormalTexture();
            newMat.hasnormmap = true;
        }

        newMat.kd = material.getColor().truncate();

        return newMat;
    }
}

