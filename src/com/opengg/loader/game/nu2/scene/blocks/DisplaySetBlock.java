package com.opengg.loader.game.nu2.scene.blocks;

import com.opengg.core.console.GGConsole;
import com.opengg.core.math.Vector3f;
import com.opengg.core.math.Vector4f;
import com.opengg.loader.BufferUtil;
import com.opengg.loader.game.nu2.scene.*;
import com.opengg.loader.game.nu2.scene.IABLObject;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.commands.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DisplaySetBlock extends DefaultFileBlock {

    @Override
    public void readFromFile(ByteBuffer fileBuffer, long blockLength, int blockID, int blockOffset, NU2MapData mapData) throws IOException {
        super.readFromFile(fileBuffer, blockLength, blockID, blockOffset, mapData);

        fileBuffer.position(blockOffset + 0x4);
        int displayListLength = fileBuffer.getInt();
        int displayListPos = readPointer();
        parseDisplayCommands(displayListPos);

        fileBuffer.position(blockOffset + 0x60);
        int staticDisplayObjectCount = fileBuffer.getInt();
        int staticDisplayObjectPos = readPointer();

        parseDisplayLists(staticDisplayObjectPos, staticDisplayObjectCount);

        fileBuffer.position(blockOffset + 0x10);
        int gameModelCount = fileBuffer.getInt();
        int gameModelPointer = readPointer();
        parseGameModels(gameModelPointer, gameModelCount);

        fileBuffer.position(blockOffset + 0x18);
        var modelSizeList = readPointer();
        mapData.scene().modelSizeListStart().set(modelSizeList);

        for (int i = 0; i < mapData.scene().gameModels().size(); i++) {
            fileBuffer.position(modelSizeList + i * 2);

            var sizeListEntry = fileBuffer.getShort();
            var modelSize = mapData.scene().gameModels().get(i).modelParts().size();

            if (modelSize != sizeListEntry) {
                GGConsole.warning("Non-matching size entries for model " + mapData.scene().gameModels().get(i).name() + ": " + sizeListEntry + " vs " + modelSize);
            }
        }

        fileBuffer.position(blockOffset + 0x4c);
        int dispObject4Size = fileBuffer.getInt();

        fileBuffer.position(blockOffset + 0x54);
        int dispObject4Pos = readPointer();
        parseDisplayObject4s(dispObject4Pos, dispObject4Size);

        fileBuffer.position(blockOffset + 0x6c);
        int specialObjectSize = fileBuffer.getInt();
        int specialObjectLocation = readPointer();
        mapData.scene().specialObjects().addAll(parseSpecialObjects(specialObjectLocation, specialObjectSize));
        mapData.scene().modelInstances().addAll(analyzeCustomListForInstances());
    }

    private void parseDisplayCommands(int commandListStart) {
        fileBuffer.position(commandListStart);
        int commandSize = 16; //size

        int commandPtr = commandListStart;
        int commandIndex = 0;
        while (true) {
            fileBuffer.position(commandPtr);
            var commandType = DisplayCommand.CommandType.getByID(Byte.toUnsignedInt(fileBuffer.get()));
            var flags = Byte.toUnsignedInt(fileBuffer.get());

            fileBuffer.position(commandPtr + 4);
            int resourcePtr = readPointer();

            if (commandType == DisplayCommand.CommandType.MTL_CLIP) flags = 0;
            if (commandType == DisplayCommand.CommandType.DYNAMIC_GEOMETRY) flags = 0;
            if (commandType == DisplayCommand.CommandType.END) {
                mapData.scene().renderCommandList().add(new DisplayCommand(commandIndex, commandPtr, flags, DisplayCommand.CommandType.END, new UntypedCommandResource(resourcePtr, DisplayCommand.CommandType.END)));
                break;
            }


            if (!mapData.scene().uniqueRenderCommands().containsKey(resourcePtr)) {
                DisplayCommandResource<?> nextRenderCommand = switch (commandType) {
                    case MTXLOAD -> {
                        fileBuffer.position(resourcePtr);
                        var matrix = BufferUtil.readMatrix4f(fileBuffer);
                        yield new MatrixCommandResource(resourcePtr, matrix);
                    }
                    case GEOMCALL -> mapData.scene().meshes().get(resourcePtr);
                    case MTL -> mapData.scene().materials().get(resourcePtr);
                    case LIGHTMAP -> {
                        fileBuffer.position(resourcePtr);
                        int type = fileBuffer.getInt();
                        int lm = fileBuffer.getInt();
                        int lm2 = fileBuffer.getInt();
                        int lm3 = fileBuffer.getInt();
                        int lm4 = fileBuffer.getInt();
                        float xOffset = fileBuffer.getFloat();
                        float yOffset = fileBuffer.getFloat();
                        float zOffset = fileBuffer.getFloat();
                        float wOffset = fileBuffer.getFloat();

                        yield new LightmapCommandResource(resourcePtr, type, lm, lm2, lm3, lm4,xOffset,yOffset,zOffset,wOffset, mapData);

                    }
                    default -> {
                        yield new UntypedCommandResource(resourcePtr, commandType);
                    }
                };
                mapData.scene().uniqueRenderCommands().put(resourcePtr, nextRenderCommand);
            }

            mapData.scene().renderCommandList().add(new DisplayCommand(commandIndex, commandPtr, flags, commandType, mapData.scene().uniqueRenderCommands().get(resourcePtr)));

            commandPtr += commandSize;
            commandIndex++;
        }
    }

    private void parseDisplayLists(int staticObjectStart, int staticObjectCount) {
        int blockLocation = staticObjectStart;

        for (int i = 0; i < staticObjectCount; i++) {
            fileBuffer.position(blockLocation);
            int loadOrder = fileBuffer.getInt();
            int commandStart = readPointer();
            var firstCommand = mapData.scene().renderCommandList().stream().filter(f -> f.address() == commandStart).findFirst().get();
            var commands = generateDisplayCommandList(firstCommand.index());

            boolean isCustomList = commands.get(0).type() == DisplayCommand.CommandType.DUMMY;

            fileBuffer.position(blockLocation + 0x20);
            var size = fileBuffer.getShort();
            var firstbound = fileBuffer.getShort();

            mapData.scene().displayLists().add(new DisplayList("Static mesh " + i, i, loadOrder, commands, firstbound, size, isCustomList, blockLocation, mapData.scene()));

            blockLocation += 0x24;
        }
    }

    private List<DisplayCommand> generateDisplayCommandList(int firstCommand){
        int index = firstCommand;
        var commands = new ArrayList<DisplayCommand>();
        try {
            while (true) {
                var command = mapData.scene().renderCommandList().get(index);
                commands.add(command);

                if(command.flags() == 4){
                    return commands;
                }

                index++;
            }
        } catch (Exception e) {
            GGConsole.warning("Failed to generate static display list");
        }

        return null;
    }

    private void parseDisplayObject4s(int displayObject4Location, int displayObject4Count) {
        for (int counter = 0; counter < displayObject4Count; counter++) {
            int object4Location = readPointer(displayObject4Location + counter * 4);
            int realsubSection1Location = readPointer(object4Location);
            int subSection2Location = readPointer(object4Location + 8);
            int startCommand = readPointer(object4Location + 0xc);
            int endCommand = readPointer(object4Location + 0x18);
            int subSection5Location = readPointer(object4Location + 0x10);
            int adfasdf = readPointer(object4Location + 0x28);
            int subSection7Location = readPointer(object4Location + 0x2c);
            int subSection8Location = readPointer(object4Location + 0x30);

            //com.opengg.loader.CSCViewer.commandsToRun.add(start);

        }
    }


    private List<SpecialObject> parseSpecialObjects(int specialObjectPos, int specialObjectSize) {
        var objs = new ArrayList<SpecialObject>();
        for (int i = 0; i < specialObjectSize; i++) {
            var thisSpecObj = specialObjectPos + i * 0xd0;
            fileBuffer.position(thisSpecObj);
            var initialMatrix =   BufferUtil.readMatrix4f(fileBuffer);
            var localIABL = getIABLObject(fileBuffer);

            var unknownVec43 = new Vector4f(fileBuffer.getFloat(), fileBuffer.getFloat(), fileBuffer.getFloat(), fileBuffer.getFloat());

            int modelAddress = readPointer();
            int stringAddr = readPointer();
            int visibilityFn = fileBuffer.getInt();
            int lodPtr = readPointer();
            int boundingBoxIndex = fileBuffer.getInt();
            int remoteIablAddress = fileBuffer.getInt();
            short windShearFactor = fileBuffer.getShort();
            short windSpeedFactor = fileBuffer.getShort();

            //int index = get3ALA(fileBuffer,remoteIablAddress);

            var displayObject = mapData.scene().gameModels().stream().filter(m -> m.modelAddress() == modelAddress).findFirst().get();
            var name = NameTableFileBlock.CURRENT.getByOffsetFromStart(stringAddr);
            var lods = getLODLevels(lodPtr);

            var specObj = new SpecialObject(displayObject, initialMatrix, localIABL, remoteIablAddress,
                    name, boundingBoxIndex, lods, windSpeedFactor / 65535.0f, windShearFactor / 65535.0f, thisSpecObj);
            objs.add(specObj);
        }
        return objs;
    }

    public List<Float> getLODLevels(int ptr) {
        var oldPos = fileBuffer.position();
        fileBuffer.position(ptr);

        var lods = new ArrayList<Float>();
        while(true) {
            var nextLod = fileBuffer.getFloat();
            if(nextLod == 0) {
                return lods;
            }

            lods.add(nextLod);
        }
    }

    public IABLObject getIABLObject(ByteBuffer source) {
        int addr = source.position();
        var localIablMatrix =  BufferUtil.readMatrix4f(fileBuffer);
        var boundsPos = new Vector3f(source.getFloat(), source.getFloat(), source.getFloat());
        source.getFloat();
        var boundsSize = new Vector3f(source.getFloat(), source.getFloat(), source.getFloat());
        source.getFloat();

        return new IABLObject(localIablMatrix, new IABLObject.IABLBoundingBox(boundsPos, boundsSize, addr + 16 * 4), addr);
    }

    public int get3ALA(ByteBuffer source, int position) {
        int oldPos = source.position();
        source.position(position);
        var localIablMatrix =  BufferUtil.readMatrix4f(source);
        var boundsPos = new Vector3f(source.getFloat(), source.getFloat(), source.getFloat());
        source.getFloat();
        var boundsSize = new Vector3f(source.getFloat(), source.getFloat(), source.getFloat());
        int ret = source.getShort();
        source.position(oldPos);
        return ret;
    }

    private void parseGameModels(int gameModelPos, int gameModelCount) {
        var allCommands = mapData.scene().renderCommandList();
        var materialsCopy = List.copyOf(mapData.scene().materials().values());

        for (int i = 0; i < gameModelCount; i++) {
            var address = gameModelPos + i * 0xc;

            var name = mapData.xmlData().loadedModels().getOrDefault("GameModel_" + i, "GameModel_" + i);

            fileBuffer.position(address);
            int commandCount = fileBuffer.getInt();

            if (mapData.name().toLowerCase(Locale.ROOT).contains("gungan") && commandCount == 0) commandCount = 3;

            int materialOffset = readPointer();
            int meshOffset = readPointer();

            List<FileMaterial> materials = new ArrayList<>();
            List<GSCMesh> renderables = new ArrayList<>();
            List<Integer> renderableIndices = new ArrayList<>();

            fileBuffer.position(materialOffset);
            for (int j = 0; j < commandCount; j++) {
                materials.add(materialsCopy.get(fileBuffer.getInt()));
            }

            fileBuffer.position(meshOffset);
            for (int j = 0; j < commandCount; j++) {
                var renderableIndex = fileBuffer.getInt();
                var commandResource = allCommands.get(renderableIndex).command();

                renderableIndices.add(renderableIndex);
                if (commandResource instanceof GSCMesh rc) {
                    renderables.add(rc);
                } else {
                    renderables.add(null); // is FACEON, investigate
                }
            }

            var commands = IntStream.range(0, commandCount)
                    .mapToObj(idx -> new GameModel.GameModelPart(materials.get(idx), renderables.get(idx), renderableIndices.get(idx)))
                    .collect(Collectors.toList());


            mapData.scene().gameModels().add(new GameModel(name, commands, address, materialOffset, meshOffset, renderableIndices));
        }
    }

    private List<ModelInstance> analyzeCustomListForInstances(){
        //check for duplicate materials
        var existingMeshes = new HashMap<DisplayCommandResource<?>, GameModel>();

        for (var model : mapData.scene().gameModels()) {
            for (var part : model.modelParts()) {
                existingMeshes.put(mapData.scene().renderCommandList().get(part.sourceCommandIndex()).command(), model);
            }
        }

        var lastDisplayList = mapData.scene().displayLists().get(mapData.scene().displayLists().size() - 1);

        if (lastDisplayList.isCustomList()) {
            record InstanceFinder (MatrixCommandResource matrix,
                                   List<DisplayCommand> foundMeshes,
                                   GameModel model) {};

            var completeInstances = new ArrayList<ModelInstance>();
            var matrixToInstanceMap = new HashMap<Integer, InstanceFinder>();
            MatrixCommandResource lastResource = null;

            for (var command : lastDisplayList.commands()) {
                if (command.command() instanceof MatrixCommandResource mcr) {
                    lastResource = mcr;
                    continue;
                }

                var associatedModel = existingMeshes.get(command.command());
                if (associatedModel != null) {
                    MatrixCommandResource finalLastResource = lastResource;
                    var existingInstance = matrixToInstanceMap.computeIfAbsent(lastResource.address(),
                            l -> new InstanceFinder(finalLastResource, new ArrayList<>(), associatedModel));
                    existingInstance.foundMeshes.add(command);
                }
            }

            for (var instanceFinder : matrixToInstanceMap.values()) {
                if (instanceFinder.foundMeshes().size() == instanceFinder.model().modelParts().size()) {
                    var previousInstancesOfModel = completeInstances.stream().filter(c -> c.model() == instanceFinder.model()).count();
                    completeInstances.add(new ModelInstance(instanceFinder.model(), instanceFinder.matrix, instanceFinder.foundMeshes(), (int) previousInstancesOfModel));
                }
            }

            return completeInstances;
        } else {
            return List.of();
        }
    }
}
