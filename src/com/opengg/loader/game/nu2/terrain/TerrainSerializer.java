package com.opengg.loader.game.nu2.terrain;

import com.opengg.core.engine.Resource;
import com.opengg.core.math.Matrix4f;
import com.opengg.core.math.Vector3f;
import com.opengg.core.math.geom.Triangle;
import com.opengg.core.math.util.Tuple;
import com.opengg.loader.Util;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.GameModel;
import com.opengg.loader.loading.MapWriter;
import com.opengg.loader.game.nu2.scene.blocks.SceneExporter;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.lwjgl.util.meshoptimizer.MeshOptimizer;

import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TerrainSerializer {
    public static void importTerrain(GameModel model, Matrix4f initialTransform) {
        record QuadIndices(int a, int b, int c, int d){}
        record TerrainStrip(boolean reverse, List<Integer> indices){}

        var newModel = SceneExporter.convertToEngineModel(model, false);
        var blocks = new ArrayList<TerrainGroup.TerrainMeshBlock>();
        try (var scope = ResourceScope.newConfinedScope()) {
            for (var mesh : newModel.getMeshes()) {
                var sections = new ArrayList<TerrainGroup.TerrainMeshFace>();
                var globalMaxMin = getMaxima(mesh.getVertices().stream()
                        .map(t -> t.position)
                        .collect(Collectors.toList()));

                var indexBuffer = mesh.getIndexBuffer();
                indexBuffer.rewind();

                var unstripMaxSize = MeshOptimizer.meshopt_unstripifyBound(indexBuffer.capacity());
                var unstripIndices = MemorySegment.allocateNative(unstripMaxSize * Integer.BYTES, scope).asByteBuffer().order(ByteOrder.nativeOrder()).asIntBuffer();
                var unstripSize = MeshOptimizer.meshopt_unstripify(unstripIndices, indexBuffer, 0);
                 unstripIndices = unstripIndices.slice(0, (int) unstripSize);

                var restripIndices = MemorySegment.allocateNative(indexBuffer.capacity() * Integer.BYTES, scope).asByteBuffer().order(ByteOrder.nativeOrder()).asIntBuffer();
                var restripSize = MeshOptimizer.meshopt_stripify(restripIndices, unstripIndices, mesh.getVertices().size(), 0xffffffff);

                restripIndices = restripIndices.slice(0, (int) restripSize);

                var stripList = new ArrayList<TerrainStrip>();
                var indices = new ArrayList<Integer>();

                var reverse = false;
                for (int i = 0; i < restripIndices.capacity(); i++) {
                    var index = restripIndices.get();

                    if (index == 0xffffffff) {
                        stripList.add(new TerrainStrip(false, indices));

                        indices = new ArrayList<>();
                    } else {
                        indices.add(index);
                    }
                }

                stripList.add(new TerrainStrip(false, indices));

                for (var terrainStrip : stripList) {
                    var strip = terrainStrip.indices;
                    int idx = 0;
                    int a = strip.get(idx++);
                    int b = strip.get(idx++);

                    var meshParts = new ArrayList<QuadIndices>();

                    while (idx < strip.size()) {
                        int c = strip.get(idx++);
                        int d;
                        if (idx < strip.size()) {
                            d = strip.get(idx++);
                        } else {
                            d = -1;
                        }

                        meshParts.add(new QuadIndices(a, c, b, d));
                        a = c;
                        b = d;
                    }

                    for (var part : meshParts) {
                        Vector3f aVec, bVec, cVec, dVec, n1, n2;

                        if (terrainStrip.reverse) {
                            aVec = initialTransform.transform(mesh.getVertices().get(part.a).position);
                            bVec = initialTransform.transform(mesh.getVertices().get(part.c).position);
                            cVec = initialTransform.transform(mesh.getVertices().get(part.b).position);
                            dVec = part.d == -1 ? cVec : initialTransform.transform(mesh.getVertices().get(part.d).position);

                            n1 = new Triangle(aVec, bVec, cVec).n();
                            n2 = part.d != -1 ? new Triangle(bVec, dVec, cVec).n()
                                    : new Vector3f(0, 65535f, 0);
                        } else {
                            aVec = initialTransform.transform(mesh.getVertices().get(part.a).position);
                            bVec = initialTransform.transform(mesh.getVertices().get(part.b).position);
                            cVec = initialTransform.transform(mesh.getVertices().get(part.c).position);
                            dVec = part.d == -1 ? cVec : initialTransform.transform(mesh.getVertices().get(part.d).position);

                            n1 = new Triangle(aVec, bVec, cVec).n().inverse();
                            n2 = part.d != -1 ? new Triangle(bVec, dVec, cVec).n().inverse()
                                    : new Vector3f(0, 65535f, 0);
                        }

                        var maxima = getMaxima(List.of(aVec, bVec, cVec, dVec));

                        var section = new TerrainGroup.TerrainMeshFace(
                                maxima.x(), maxima.y(),
                                aVec, bVec, cVec, dVec,
                                n1, n2, 0, 0, 3, 0, 0);
                        sections.add(section);
                    }
                }

                var block = new TerrainGroup.TerrainMeshBlock(globalMaxMin.x().xz(), globalMaxMin.y().xz(), sections);
                blocks.add(block);
            }
        }

        var terrainGroup = new TerrainGroup(new Vector3f(0,0,0), 0, 0, 0, blocks, Optional.empty(), 0, 0, 0, 0);
        TerrainSerializer.addNewTerrainGroup(terrainGroup);
    }

    private static Tuple<Vector3f, Vector3f> getMaxima(List<Vector3f> vectors){
        Vector3f max = new Vector3f(-100_000_000, -100_000_000, -100_000_000), min = new Vector3f(100_000_000, 100_000_000, 100_000_000);

        var epsilon = 0.001f;

        for(var vector : vectors){
            if(vector.x > max.x) max = max.setX(vector.x + epsilon);
            if(vector.y > max.y) max = max.setY(vector.y + epsilon);
            if(vector.z > max.z) max = max.setZ(vector.z + epsilon);

            if(vector.x < min.x) min = min.setX(vector.x - epsilon);
            if(vector.y < min.y) min = min.setY(vector.y - epsilon);
            if(vector.z < min.z) min = min.setZ(vector.z - epsilon);
        }

        return Tuple.of(min, max);
    }

    public static void editTerrainPieceProperty(TerrainGroup.TerrainMeshFace section, TerrainGroup.TerrainProperty property){
        var addr = section.address() + (4*6) + (4*3*6);
        var buf = ByteBuffer.allocate(2).put((byte) property.flag1).put((byte) property.flag2);

        MapWriter.applyPatch(MapWriter.WritableObject.TERRAIN, addr, buf);
    }

    private static int getMoreWorkingRegion(int amount, NU2MapData.TerrainData terData){
        var newSize = terData.workingAreaEnd().get() + amount;
        var newObjectEnd = terData.objectAreaEnd().get() + amount;


        MapWriter.applyPatch(MapWriter.WritableObject.TERRAIN, 0,
                Util.littleEndian(newSize/2));
        MapWriter.addSpaceAtLocation(MapWriter.WritableObject.TERRAIN, terData.workingAreaEnd().get(), amount);

        terData.workingAreaEnd().set(newSize);
        terData.objectAreaEnd().set(newObjectEnd);
        return newSize - amount;
    }

    private static void removeWorkingArea(int address, int amount, NU2MapData.TerrainData terData){
        MapWriter.applyPatch(MapWriter.WritableObject.TERRAIN, 0,
                Util.littleEndian((terData.workingAreaEnd().get() - amount)/2));
        MapWriter.removeSpaceAtLocation(MapWriter.WritableObject.TERRAIN, address, amount);
    }

    public static void addNewTerrainGroup(TerrainGroup terrainGroup){
        var terData = ((NU2MapData) EditorState.getActiveMap().levelData()).terrain();

        int size = 0;
        for(var block : terrainGroup.blocks()){
            size += block.faces().size() * 100 + 0x14;
        }
        size += 0x4;

        var workingBuffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);

        for(var block : terrainGroup.blocks()){
            var blockMaxMin = getMaxima(block.faces().stream()
                    .flatMap(f -> List.of(f.vec1(), f.vec2(), f.vec3(), f.vec4()).stream())
                    .collect(Collectors.toList()));

            workingBuffer.put((byte) 0).put((byte) 0)
                    .putShort((short) block.faces().size())
                    .putFloat(blockMaxMin.x().x).putFloat(blockMaxMin.y().x).putFloat(blockMaxMin.x().z).putFloat(blockMaxMin.y().z);
            for(var subObject : block.faces()){
                var maxMin = getMaxima(List.of(subObject.vec1(), subObject.vec2(), subObject.vec3(), subObject.vec4()));
                workingBuffer.putFloat(maxMin.x().x).putFloat(maxMin.y().x);
                workingBuffer.putFloat(maxMin.x().y).putFloat(maxMin.y().y);
                workingBuffer.putFloat(maxMin.x().z).putFloat(maxMin.y().z);

                workingBuffer.put(subObject.vec1().toLittleEndianByteBuffer())
                        .put(subObject.vec2().toLittleEndianByteBuffer())
                        .put(subObject.vec3().toLittleEndianByteBuffer())
                        .put(subObject.vec4().toLittleEndianByteBuffer())
                        .put(subObject.norm1().toLittleEndianByteBuffer())
                        .put(subObject.norm2().toLittleEndianByteBuffer())
                        .put((byte)subObject.flag1())
                        .put((byte)subObject.flag2()).put((byte) subObject.flag3()).put((byte) subObject.flag4());
            }
        }

        workingBuffer.putInt(-1);

        var objectBuffer = ByteBuffer.allocate(0x34).order(ByteOrder.LITTLE_ENDIAN);
        objectBuffer.putInt(0)
                .putInt(workingBuffer.capacity()/2)
                .put(terrainGroup.pos().toLittleEndianByteBuffer())
                .putShort((short) terrainGroup.flag())
                .putShort((short) terrainGroup.terrainPlatformIndex())
                .put(new byte[0x14])
                .putInt(0)
                .putShort((short) 0)
                .putShort((short) 0xFFFF);

        var targetSite = terData.objectAreaEnd().get();
        var countSite = terData.workingAreaEnd().get();
        var count = terData.terrainGroups().size() + terData.infiniteWallPoints().size();

        MapWriter.applyPatch(MapWriter.WritableObject.TERRAIN, countSite, Util.littleEndian((short) count + 1));
        MapWriter.addSpaceAtLocation(MapWriter.WritableObject.TERRAIN, targetSite, 0x34);
        MapWriter.applyPatch(MapWriter.WritableObject.TERRAIN, targetSite, objectBuffer);
        int targetWorking = getMoreWorkingRegion(workingBuffer.capacity(), terData);
        MapWriter.applyPatch(MapWriter.WritableObject.TERRAIN, targetWorking, workingBuffer);

        targetSite = terData.objectAreaEnd().get();
        terData.terrainGroups().add(terrainGroup);
        terData.objectAreaEnd().set(targetSite + 0x34);

    }

    public static void removeObject(TerrainObject object){
        var terData = ((NU2MapData) EditorState.getActiveMap().levelData()).terrain();


        var count = terData.terrainGroups().size() + terData.infiniteWallPoints().size();
        var countSite = terData.workingAreaEnd().get();
        MapWriter.removeSpaceAtLocation(MapWriter.WritableObject.TERRAIN, object.objectDefinitionAddress(), 0x34);
        MapWriter.applyPatch(MapWriter.WritableObject.TERRAIN, countSite, Util.littleEndian((short) count - 1));
        removeWorkingArea(object.objectContentsAddress(), object.objectContentsSize(), terData);
    }

    public static void testWriteFile(){
        var terData = ((NU2MapData)EditorState.getActiveMap().levelData()).terrain();

        var mainDataBuf = ByteBuffer.allocate(terData.terrainGroups().size() * 0x34 + terData.infiniteWallPoints().size() * 0x34).order(ByteOrder.LITTLE_ENDIAN);
        var workingBuffers = new ArrayList<ByteBuffer>();

        for(var terrainObject : terData.terrainGroups()){
            var workingBuffer = serializeTerrainGroupData(terrainObject);
            mainDataBuf.putInt(0)
                    .putInt(workingBuffer.capacity()/2)
                    .put(terrainObject.pos().toLittleEndianByteBuffer())
                    .putShort((short) terrainObject.flag())
                    .putShort((short) terrainObject.terrainPlatformIndex())
                    .put(new byte[0x18])
                    .putShort((short) 0)
                    .putShort((short) 0xFFFF);

            workingBuffers.add(workingBuffer);
        }

        for(var wall : terData.infiniteWallPoints()){
            var workingBuffer = serializeInfiniteWall(wall);
            mainDataBuf.putInt(0)
                    .putInt(workingBuffer.capacity()/2)
                    .put(new byte[0xc])
                    .putShort((short) 2)
                    .put(new byte[0x1C])
                    .putShort((short) 0xFFFF);
            workingBuffers.add(workingBuffer);
        }

        mainDataBuf.position(0);
        mainDataBuf.putShort((short) (terData.terrainGroups().size() + terData.infiniteWallPoints().size()));
        mainDataBuf.position(0);

        var bufferSum = workingBuffers.stream().mapToInt(Buffer::capacity).sum();
        if(bufferSum % 2 != 0) bufferSum++;
        var fileBuffer= ByteBuffer.allocate(mainDataBuf.capacity() + bufferSum + 4).order(ByteOrder.LITTLE_ENDIAN);
        fileBuffer.putInt(bufferSum / 2 + 2);
        for(var workingBuffer : workingBuffers){
            fileBuffer.put(workingBuffer);
        }

        fileBuffer.put(mainDataBuf);
        fileBuffer.flip();
        try (var channel = new FileOutputStream(Resource.getUserDataPath().resolve("MOSEISLEY_A.ter").toFile(), false).getChannel()) {
            channel.write(fileBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ByteBuffer serializeTerrainGroupData(TerrainGroup terrainGroup){

        int size = 0;
        for(var block : terrainGroup.blocks()){
            size += block.faces().size() * 100 + 0x14;
        }
        size += 0x4;

        var workingBuffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);

        for(var block : terrainGroup.blocks()){
            workingBuffer.put((byte) 0).put((byte) 0)
                    .putShort((short) block.faces().size())
                    .putFloat(block.min().x).putFloat(block.max().x).putFloat(block.min().y).putFloat(block.max().y);
            for(var face : block.faces()){
                workingBuffer.putFloat(face.min().x).putFloat(face.max().x);
                workingBuffer.putFloat(face.min().y).putFloat(face.max().y);
                workingBuffer.putFloat(face.min().z).putFloat(face.max().z);

                workingBuffer.put(face.vec1().toLittleEndianByteBuffer())
                        .put(face.vec2().toLittleEndianByteBuffer())
                        .put(face.vec3().toLittleEndianByteBuffer())
                        .put(face.vec4().toLittleEndianByteBuffer())
                        .put(face.norm1().toLittleEndianByteBuffer())
                        .put(face.norm2().toLittleEndianByteBuffer())
                        .put((byte)face.flag1())
                        .put((byte)face.flag2())
                        .put((byte)face.flag3())
                        .put((byte)face.flag4());
            }
        }

        workingBuffer.putInt(-1);
        workingBuffer.rewind();

        return workingBuffer;
    }

    private static ByteBuffer serializeInfiniteWall(InfiniteWall wall){
        var workingBuffer = ByteBuffer.allocate(wall.wall().size() * 12 + 4 + 4).order(ByteOrder.LITTLE_ENDIAN);

        workingBuffer.putInt(0x12345678);
        workingBuffer.putShort((short) wall.wall().size());
        workingBuffer.putShort((short) 0);
        for(var vec : wall.wall()){
            workingBuffer.put(vec.toLittleEndianByteBuffer());
        }

        workingBuffer.rewind();

        return workingBuffer;
    }
}
