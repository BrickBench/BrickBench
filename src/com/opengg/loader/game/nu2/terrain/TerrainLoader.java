package com.opengg.loader.game.nu2.terrain;

import com.opengg.core.console.GGConsole;
import com.opengg.core.math.Vector2f;
import com.opengg.core.math.Vector3f;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.SpecialObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TerrainLoader {
    public static void load(ByteBuffer fileData, NU2MapData mapData) {
        int contentAreaSize = fileData.getInt() * 2;
        fileData.position(contentAreaSize);

        mapData.terrain().workingAreaEnd().set(contentAreaSize);

        parseTerrainGroups(fileData, mapData);
    }

    static List<TerrainGroup.TerrainMeshBlock> loadTerrainGroupSubData(ByteBuffer data, int dataOffset, short someToggle) {
        var blocks = new ArrayList<TerrainGroup.TerrainMeshBlock>();

        data.position(dataOffset);
        var ptr = dataOffset;
        if (someToggle < 2) {
            while (-1 < data.get()) {
                var faces = new ArrayList<TerrainGroup.TerrainMeshFace>();

                data.get();
                int subCount = data.getShort();
                float blockMinX = data.getFloat();
                float blockMaxX = data.getFloat();
                float blockMinZ = data.getFloat();
                float blockMaxZ = data.getFloat();

                int subPtr = data.position();
                for (int j = 0; j < subCount; j++) {
                    int addr = data.position();

                    var minX = data.getFloat();
                    var maxX = data.getFloat();
                    var minY = data.getFloat();
                    var maxY = data.getFloat();
                    var minZ = data.getFloat();
                    var maxZ = data.getFloat();

                    var p1 = new Vector3f(data.getFloat(), data.getFloat(), data.getFloat());
                    var p2 = new Vector3f(data.getFloat(), data.getFloat(), data.getFloat());
                    var p3 = new Vector3f(data.getFloat(), data.getFloat(), data.getFloat());
                    var p4 = new Vector3f(data.getFloat(), data.getFloat(), data.getFloat());
                    var norm1 = new Vector3f(data.getFloat(), data.getFloat(), data.getFloat());
                    var norm2 = new Vector3f(data.getFloat(), data.getFloat(), data.getFloat());

                    int property1 = Byte.toUnsignedInt(data.get());
                    int property2 = Byte.toUnsignedInt(data.get());

                    int flag1 = Byte.toUnsignedInt(data.get());
                    int flag2 = Byte.toUnsignedInt(data.get());

                    var obj = new TerrainGroup.TerrainMeshFace(
                            new Vector3f(minX, minY, minZ), new Vector3f(maxX, maxY, maxZ),
                            p1, p2, p3, p4,
                            norm1, norm2,
                            property1, property2, flag1, flag2, addr);

                    faces.add(obj);
                    subPtr += 100;
                }
                ptr = subPtr;
                data.position(ptr);

                blocks.add(new TerrainGroup.TerrainMeshBlock(new Vector2f(blockMinX, blockMinZ), new Vector2f(blockMaxX, blockMaxZ), faces));
            }
        }

        return blocks;
    }

    static void parseTerrainGroups(ByteBuffer data, NU2MapData mapData) {
        int objectCount = Short.toUnsignedInt(data.getShort());
        int identifiedPlatforms = 0;
        data.position(data.position() - 2);

        int dataOffset = 4;
        for (int i = 0; i < objectCount; i++) {
            int ptr = data.position();

            data.position(ptr + 0x4);

            var dataOffsetChange = data.getInt() * 2;

            data.position(ptr + 0x14);
            short flag = data.getShort();

            if (flag == 2) {
                data.position(dataOffset);
                if (data.getInt() == 0x12345678) {
                    data.position(dataOffset);
                    data.putInt(0);
                    int sublistDataPosition = data.position();
                    var wall = getInfiniteWall(data);
                    mapData.terrain().infiniteWallPoints().add(new InfiniteWall(wall, ptr, dataOffset, dataOffsetChange));

                    data.position(sublistDataPosition);
                    short shortVal = (short) (data.getShort() - 1);
                    data.position(sublistDataPosition);
                    data.putShort(shortVal);
                }
            } else if (flag == 0 || flag == 1) {
                data.position(ptr + 0x8);
                var position = new Vector3f(data.getFloat(), data.getFloat(), data.getFloat());
                var platformFlag = data.getShort();
                var terrainPlatformOffset = data.getShort();
                data.position(ptr + 0x2c);
                var x28 = data.getInt();
                var x32 = data.getShort();

                SpecialObject platformObject = null;
                if (platformFlag == 1) {
                    platformObject = parseTerrainPlatforms(mapData, terrainPlatformOffset, identifiedPlatforms);
                    if (platformObject != null) identifiedPlatforms++;
                }

                mapData.terrain().terrainGroups().add(new TerrainGroup(position, platformFlag, terrainPlatformOffset,
                        x28, loadTerrainGroupSubData(data, dataOffset, platformFlag), Optional.ofNullable(platformObject), ptr, dataOffset, dataOffsetChange, i));

            }else{
                throw new UnsupportedOperationException("BrickBench does not support terrain object " + flag);
            }

            dataOffset = dataOffset + dataOffsetChange;
            data.position(ptr + 0x34);
        }

        mapData.terrain().objectAreaEnd().set(data.position());
    }

    static SpecialObject parseTerrainPlatforms(NU2MapData mapData, int terrainPlatformIndex, int objectsFound) {
        int specialObjectIndex = 0;

        if(mapData.scene().DINIData().isEmpty()){
            specialObjectIndex = objectsFound;
        }else{
            if(mapData.scene().DINIData().size() > terrainPlatformIndex){
                specialObjectIndex = mapData.scene().DINIData().get(terrainPlatformIndex);
            }else{
                GGConsole.warning("Failed to find DINI entry for terrain index " + terrainPlatformIndex);
                return null;
            }
        }

        if (!mapData.scene().uniqueRenderCommands().isEmpty()) {
            for (var specialObject : mapData.scene().specialObjects()) {
                if (specialObject.boundingBoxIndex() == specialObjectIndex) {
                    return specialObject;
                }
            }

            GGConsole.warning("Failed to find terrain platform for terrain group.");
        }

        return null;
    }

    static List<Vector3f> getInfiniteWall(ByteBuffer data) {
        int listSize = Short.toUnsignedInt(data.getShort());
        data.getShort();
        List<Vector3f> line2 = new ArrayList<>();
        for (int i = 0; i < listSize; i++) {
            float x = data.getFloat();
            float y = data.getFloat();
            float z = data.getFloat();
            line2.add(new Vector3f(x, 0, z));
        }

        return line2;
    }
}
