package com.opengg.loader.game.nu2.scene;

import com.opengg.core.Configuration;
import com.opengg.core.console.GGConsole;
import com.opengg.core.render.GraphicsBuffer;
import com.opengg.core.render.texture.Texture;
import com.opengg.loader.Project;
import com.opengg.loader.Util;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.blocks.*;
import com.opengg.loader.loading.MapLoader;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class SceneFileLoader {
    public static final int NUS0_HEX_ID = 0x4E55533A;

    public static int pntrLocation = -1;
    public static boolean PARSE_PNTR = true;

    public static void load(ByteBuffer fileBuffer, NU2MapData mapData) throws IOException {
        fileBuffer.order(ByteOrder.LITTLE_ENDIAN);

        int headerLocation;
        int nu20Start;

        FileTexture.FileTextureCache.restartIconLoader();
        GSCMesh.lightCache.clear();

        int firstValue = fileBuffer.getInt();
        if ((firstValue == 0x3032554e
                && (Configuration.get("gsc-format").equalsIgnoreCase("autodetect") || Configuration.get("gsc-format").isEmpty()))
                || Configuration.get("gsc-format").equalsIgnoreCase("LIJ/Batman")) { //lij,batman
            MapLoader.CURRENT_GAME_VERSION = Project.GameVersion.LIJ1;
            nu20Start = 0;
        } else { //tcs
            MapLoader.CURRENT_GAME_VERSION = Project.GameVersion.LSW_TCS;
            fileBuffer.position(0);
            nu20Start = fileBuffer.getInt() + 4;
        }

        fileBuffer.position(nu20Start + 0x18);
        pntrLocation = fileBuffer.position() + fileBuffer.getInt();
        headerLocation = fileBuffer.position() + fileBuffer.getInt();

        if(MapLoader.CURRENT_GAME_VERSION == Project.GameVersion.LSW_TCS){
            fileBuffer.position(pntrLocation);
            PARSE_PNTR = true;
            parsePNTRValues(fileBuffer);
        }else{
            PARSE_PNTR = false;
        }

        fileBuffer.position(headerLocation - 8);
        loadBlockAtPosition(fileBuffer, mapData, true);
        fileBuffer.position(nu20Start + 0x20);

        while (fileBuffer.remaining() > 0) {
            if(!loadBlockAtPosition(fileBuffer, mapData, false)) break;
        }

        //Re-sort because of out of order block loading
        LinkedHashMap<String, NU2MapData.SceneData.Block> temp = new LinkedHashMap<>(mapData.scene().blocks());
        mapData.scene().blocks().clear();
        temp.entrySet()
                .stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().address()))
                .forEach((e1)->{mapData.scene().blocks().put(e1.getKey(),e1.getValue());});
    }

    private static boolean loadBlockAtPosition(ByteBuffer fileBuffer, NU2MapData mapData, boolean loadGameScene) throws IOException {
        int savePtr = fileBuffer.position();
        int blockId = fileBuffer.getInt();
        int blockSize = fileBuffer.getInt();

        String blockName = Util.blockIDToString(Integer.reverseBytes(blockId));

        if (blockSize > 1e8) return false;
        if (blockSize == 0) throw new RuntimeException("Attempted to read zero-size block");
        if (blockId == NUS0_HEX_ID) return false;
        DefaultFileBlock block = switch (blockName) {
            case "PORT" -> new PortalSetFileBlock();
            case "DISP" -> new DisplaySetBlock();
            case "TST0" -> new TextureSetBlock();
            case "NTBL" -> new NameTableFileBlock();
            case "BNDS" -> new BoundingBoxBlock();
            case "SST0" -> new SplineBlock();
            case "BINH" -> new BINHBlock();
            case "GSNH" -> {
                if(loadGameScene) yield new GameSceneHeaderBlock();
                else yield new DefaultFileBlock();
            }
            case "INID" -> new DINIBlock();
            case "MS00" -> new MaterialBlock();
            //case IABL_HEX_ID -> new IABLBlock();
            //case ANIMATED_TEX_HEX_ID -> new AnimatedTextureBlock();
            default -> new DefaultFileBlock();
        };
        block.readFromFile(fileBuffer, blockSize, blockId, fileBuffer.position(), mapData);
        block.fileBuffer = null;
        fileBuffer.position(savePtr + blockSize);

        mapData.scene().blocks().put(blockName, new NU2MapData.SceneData.Block(savePtr, blockSize));
        return true;
    }

    private static void parsePNTRValues(ByteBuffer fileBuffer) {
        int numPntr = fileBuffer.getInt();
        for (int i = 0; i < numPntr; i++) {
            int pos = fileBuffer.position();
            int ptrOffset = fileBuffer.getInt();
            
            int ptr = ptrOffset + pos;
            fileBuffer.position(ptr);

            int offset = fileBuffer.getInt();
            if (offset != 0)
                fileBuffer.putInt(ptr, ptr + offset);

            fileBuffer.position(pos + 4);
        }
    }

    public static void initializeGraphicsData(NU2MapData mapData) {
        GGConsole.log("Loading scene graphics data");

        var vertexBuffers = mapData.scene().gscVertexBuffers().stream()
                .map(gameBuf -> {
                    try (ResourceScope scope = ResourceScope.newConfinedScope()) {
                        var segment = MemorySegment.allocateNative(gameBuf.length(), scope);
                        segment.asByteBuffer().put(gameBuf.contents());
                        return GraphicsBuffer.allocate(GraphicsBuffer.BufferType.VERTEX_ARRAY_BUFFER, segment.asByteBuffer(), GraphicsBuffer.UsageType.NONE);
                    }
                }).collect(Collectors.toList());

        var indexBuffers = mapData.scene().gscIndexBuffers().stream()
                .map(gameBuf -> {
                    try (ResourceScope scope = ResourceScope.newConfinedScope()) {
                        var segment = MemorySegment.allocateNative(gameBuf.length(), scope);
                        segment.asByteBuffer().put(gameBuf.contents());
                        return GraphicsBuffer.allocate(GraphicsBuffer.BufferType.ELEMENT_ARRAY_BUFFER, segment.asByteBuffer(), GraphicsBuffer.UsageType.NONE);
                    }
                }).collect(Collectors.toList());

        for (var command : mapData.scene().uniqueRenderCommands().values()) {
            if (command instanceof GSCMesh rc) {
                rc.generateObject(vertexBuffers, indexBuffers);
            }
        }

        for (var texture : mapData.scene().textures()) {
            texture.nativeTexture().complete(Texture.create(Texture.config().wrapType(Texture.WrapType.REPEAT).maxFilter(Texture.FilterType.LINEAR).minimumFilter(Texture.FilterType.LINEAR), texture.ggTexture()));
        }

        for (var material : mapData.scene().materials().values()) {
            material.loadTextures();
        }

        GGConsole.log("Done loading graphics data");
    }
}
