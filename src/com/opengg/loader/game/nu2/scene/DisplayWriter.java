package com.opengg.loader.game.nu2.scene;

import com.opengg.loader.Util;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.commands.DisplayCommand;
import com.opengg.loader.game.nu2.scene.commands.DisplayCommandResource;
import com.opengg.loader.loading.MapLoader;
import com.opengg.loader.loading.MapWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.opengg.loader.loading.MapWriter.WritableObject.SCENE;

public class DisplayWriter {
    public static int appendToEnd(ByteBuffer contents) throws IOException {
        contents.rewind();

        var display = EditorState.getActiveMap().levelData().<NU2MapData>as().scene().blocks().get("DISP");
        var displayEnd = display.address() + display.size();

        SceneFileWriter.addSpace(displayEnd, contents.limit());
        MapWriter.applyPatch(MapWriter.WritableObject.SCENE, displayEnd, contents);
        return displayEnd;
    }

    public static int createNewGameModel(List<GameModel.GameModelPart> parts) throws IOException {
        var modelListsBuffer = ByteBuffer.allocate(parts.size() * Integer.BYTES * 2).order(ByteOrder.LITTLE_ENDIAN);

        for(var part : parts){
            modelListsBuffer.putInt(part.material().getID());
        }

        for(var part : parts){
            modelListsBuffer.putInt(part.sourceCommandIndex());
        }

        var modelListsAddress = DisplayWriter.appendToEnd(modelListsBuffer);
        var materialPart = modelListsAddress + 12; //adjust for new size after move
        var meshPart = materialPart + parts.size() * Integer.BYTES;

        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));
        var gameModelList = EditorState.getActiveMap().levelData().<NU2MapData>as().scene().gameModels();
        var newPosition = gameModelList.get(gameModelList.size() - 1).modelAddress() + 12;

        var newModelBuffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        newModelBuffer.asIntBuffer().put(parts.size()).put(materialPart - (newPosition + 4)).put(meshPart - (newPosition + 8));

        SceneFileWriter.addSpace(newPosition, 12);
        MapWriter.applyPatch(SCENE, newPosition, newModelBuffer);
        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

        SceneFileWriter.addPointer(newPosition + 4);
        SceneFileWriter.addPointer(newPosition + 8);

        MapWriter.applyPatch(SCENE,
                EditorState.getActiveMap().levelData().<NU2MapData>as().scene().blocks().get("DISP").address() + 0x10 + 0x8,
                Util.littleEndian(gameModelList.size() + 1));

        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

        var sizeListStart = EditorState.getActiveMap().levelData().<NU2MapData>as().scene().modelSizeListStart().get();
        var newIndexPosition = sizeListStart + (EditorState.getActiveMap().levelData().<NU2MapData>as().scene().gameModels().size() - 1) * 2;

        SceneFileWriter.addSpace(newIndexPosition, 2);
        MapWriter.applyPatch(SCENE, newIndexPosition, Util.littleEndian((short) parts.size()));

        return gameModelList.size();
    }


    public static List<Integer> addGameMeshes(List<GSCMesh> renderables) throws IOException {
        var meshes = EditorState.getActiveMap().levelData().<NU2MapData>as().scene()
                .uniqueRenderCommands().values().stream()
                .filter(u -> u instanceof GSCMesh)
                .map(u -> (GSCMesh) u)
                .sorted(Comparator.comparingInt(GSCMesh::getAddress))
                .collect(Collectors.toList());

        var lastMeshEnd = meshes.get(meshes.size() - 1).getAddress() + 0x38;

        var newAddresses = new ArrayList<Integer>();
        int counter = lastMeshEnd;

        var newRenderables = ByteBuffer.allocate(0x38 * renderables.size());
        newRenderables.order(ByteOrder.LITTLE_ENDIAN);
        for(var renderable : renderables){
            newRenderables.putInt(6)
                    .putInt(renderable.triangleCount)
                    .putShort((short) renderable.vertexSize)
                    .put(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF})
                    .putShort((short) 0)
                    .putInt(renderable.vertexOffset)
                    .putInt(renderable.vertexCount)
                    .putInt(renderable.indexOffset)
                    .putInt(renderable.indexListID)
                    .putInt(renderable.vertexListID)
                    .put(new byte[0x10]);

            newAddresses.add(counter);
            counter += 0x38;
        }

        newRenderables.rewind();

        SceneFileWriter.addSpace(lastMeshEnd, newRenderables.limit());
        MapWriter.applyPatch(MapWriter.WritableObject.SCENE, lastMeshEnd, newRenderables);
        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

        var scene = EditorState.getActiveMap().levelData().<NU2MapData>as().scene();
        var gscListLenAddr = scene.gscRenderableListFromGSNH().get() + scene.blocks().get("GSNH").address() + 8 + 0x14;
        var gscListEndAddr = scene.gscRenderableEndFromGSNH().get() +scene.blocks().get("GSNH").address() + 8;
        var gscListLen = scene.gscRenderableListLen().get();

        SceneFileWriter.addSpace(gscListEndAddr, 4 * renderables.size());
        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

        var pointerBuf = ByteBuffer.allocate(4 * renderables.size()).order(ByteOrder.LITTLE_ENDIAN);

        for(int i = 0; i < renderables.size(); i++){
            int currentAddr = gscListEndAddr + (i * 4);
            pointerBuf.putInt(newAddresses.get(i) - currentAddr);
            SceneFileWriter.addPointer(currentAddr);
        }

        MapWriter.applyPatch(SCENE, gscListEndAddr, pointerBuf);
        MapWriter.applyPatch(SCENE, gscListLenAddr, Util.littleEndian(gscListLen + renderables.size()));

        return newAddresses;
    }

    public static int addDisplayCommands(List<DisplayCommandResource<?>> commands) {
        var lastCommand = EditorState.getActiveMap().levelData().<NU2MapData>as().scene().renderCommandList().get(
                EditorState.getActiveMap().levelData().<NU2MapData>as().scene().renderCommandList().size() - 1).index();
        return addDisplayCommands(commands, lastCommand);
    }

    public static int addDisplayCommands(List<DisplayCommandResource<?>> commands, int index){
        adjustModelCommandIndices(index, commands.size());

        var address = EditorState.getActiveMap().levelData().<NU2MapData>as().scene().renderCommandList().get(index).address();

        var size = commands.size() * 16;
        var buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);

        var addrToAdjust = new ArrayList<Integer>();

        for (var command : commands) {
            var flag = switch (command.getType()) {
                case DYNAMIC_GEOMETRY -> 1;
                case FACEON, MTL_CLIP, OTHER, NEXT -> 1;
                case END, TERMINATE -> 4;
                case MTL -> 3;
                case GEOMCALL, MTXLOAD, LIGHTMAP, DUMMY -> 3;
            };
            buf.put((byte) command.getType().id);
            buf.put((byte) flag);
            buf.put((byte) 0);
            buf.put((byte) 0);
            var relAddr = buf.position() + address;
            if (command.getAddress() != 0 && command.getType() != DisplayCommand.CommandType.MTL_CLIP) {
                var newAddress = command.getAddress() > relAddr ? command.getAddress() + size : command.getAddress();
                buf.putInt(newAddress - relAddr);
                addrToAdjust.add(relAddr);
            } else if (command.getType() == DisplayCommand.CommandType.MTL_CLIP){
                buf.putInt(command.getAddress());
                addrToAdjust.add(relAddr);
            } else {
                buf.putInt(0);
            }
            buf.put(new byte[8]);
        }

        var displayListLenAddress = EditorState.getActiveMap().levelData().<NU2MapData>as().scene().blocks().get("DISP").address() + 8 + 4;
        var existingLength = EditorState.getActiveMap().levelData().<NU2MapData>as().scene().renderCommandList().size();

        try {
            SceneFileWriter.addSpace(address, size);
            MapWriter.applyPatch(SCENE, address, buf);
            MapWriter.applyPatch(SCENE, displayListLenAddress, Util.littleEndian(existingLength + commands.size()));

            for (var addr : addrToAdjust) {
                SceneFileWriter.addPointer(addr);
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }


        return address;
    }

    public static void adjustModelCommandIndices(int index, int shift) {
        var scene = EditorState.getActiveMap().levelData().<NU2MapData>as().scene();
        for(var model : scene.gameModels()){
            for(int i = 0; i < model.meshCommandIndices().size(); i++){
                var commandIdx = model.meshCommandIndices().get(i);
                if(shift < 0 && commandIdx >= index && commandIdx < index + shift){
                    throw new IllegalArgumentException("Cannot remove commands, models would be interrupted");
                }

                if(commandIdx >= index){
                    var newIndex = commandIdx + shift;
                    var indexPosition = model.meshListAddress() + (i * 4);
                    MapWriter.applyPatch(SCENE, indexPosition, Util.littleEndian(newIndex));
                }
            }
        }
    }

    public static void removeDisplayCommands(DisplayCommand firstCommand, int amount){
        adjustModelCommandIndices(firstCommand.index(), -amount);

        var scene = EditorState.getActiveMap().levelData().<NU2MapData>as().scene();

        var displayListLenAddress = scene.blocks().get("DISP").address() + 8 + 4;
        var existingLength = scene.renderCommandList().size();

        MapWriter.applyPatch(SCENE, displayListLenAddress, Util.littleEndian(existingLength - amount));
        SceneFileWriter.removeSpace(firstCommand.address(), amount * 16);
    }

    public static int createDisplayList(int startCommand, int priority) throws IOException {
        var scene = EditorState.getActiveMap().levelData().<NU2MapData>as().scene();
        var dispStart = scene.blocks().get("DISP").address() + 8;
        var existingCount = scene.displayLists().size();

        var newList = scene.displayLists().get(
                scene.displayLists().size() - 1).fileAddress() + 0x24;

        SceneFileWriter.addSpace(newList, 0x24);
        MapWriter.applyPatch(SCENE, dispStart + 0x60, Util.littleEndian(existingCount + 1));

        var mesh = ByteBuffer.allocate(0x24)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(10)
                .putInt(startCommand - (newList + 0x4))
                .putInt(0)
                .putInt(0)
                .putInt(-16)
                .putInt(1)
                .putInt(0)
                .putInt(dispStart - (newList + 0x1c))
                .putShort((short) 0)
                .putShort((short) 1);

        MapWriter.applyPatch(SCENE, newList, mesh);
        SceneFileWriter.addPointer(newList + 0x4);
        SceneFileWriter.addPointer(newList + 0x10);
        SceneFileWriter.addPointer(newList + 0x1c);
        return newList;
    }

    public static void deleteDisplayList(DisplayList object) {
        var scene = EditorState.getActiveMap().levelData().<NU2MapData>as().scene();

        var dispStart = scene.blocks().get("DISP").address() + 8;
        var existingCount = scene.displayLists().size();
        MapWriter.applyPatch(SCENE, dispStart + 0x60, Util.littleEndian(existingCount - 1));
        var isFirst = scene.displayLists().indexOf(object) == 0;
        SceneFileWriter.removeSpace(object.fileAddress(), 0x24);
        if(isFirst){
            MapWriter.applyPatch(SCENE, dispStart + 0x64, Util.littleEndian(object.fileAddress() - (dispStart + 0x64)));
        }
    }
}
