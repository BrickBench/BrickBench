package com.opengg.loader.game.nu2.scene;

import com.opengg.loader.Util;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.loading.MapLoader;
import com.opengg.loader.loading.MapWriter;

import java.io.IOException;
import java.nio.*;
import java.util.*;

import static com.opengg.loader.loading.MapWriter.WritableObject.SCENE;

public class SceneFileWriter {
    public static List<Integer> appendByteBuffers(List<ByteBuffer> byteBuffers, boolean isIndexBuffer, int bufferIndex, int objectSize) throws IOException {
        ByteBuffer byteBuffer2 = ByteBuffer.allocate(byteBuffers.stream().mapToInt(Buffer::capacity).sum());
        for(var buffer: byteBuffers){
            byteBuffer2.put(buffer);
        }

        int current = appendMesh(byteBuffer2, isIndexBuffer, bufferIndex);
        var ints = new ArrayList<Integer>();
        for(var buffer : byteBuffers){
            ints.add(current/objectSize);
            current += buffer.capacity();
        }
        return ints;
    }

    private static int appendMesh(ByteBuffer sb, boolean isIndexBuffer, int bufferIndex) throws IOException {
        GameBuffer target;

        var sceneStruct = EditorState.getActiveMap().levelData().<NU2MapData>as().scene();
        if(isIndexBuffer){
            target = sceneStruct.gscIndexBuffers().get(bufferIndex);
        }else{
            target = sceneStruct.gscVertexBuffers().get(bufferIndex);
        }

        int resizeLoc = target.fileAddress() + target.length() + 4;

        MapWriter.applyPatch(SCENE, target.fileAddress(), Util.littleEndian(target.length() + sb.limit()));
        SceneFileWriter.addSpace(resizeLoc, sb.limit());
        MapWriter.applyPatch(SCENE, resizeLoc, sb);

        return resizeLoc - (target.fileAddress() + 4);
    }

    public static int createTextureEntries(int amount) throws IOException {
        var targetBlock = EditorState.getActiveMap().levelData().<NU2MapData>as().scene().blocks().get("VBIB");
        var targetAddress = targetBlock.address() - 4;

        SceneFileWriter.addSpace(targetAddress, 0x28 * amount);

        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

        var textureListEnd =
                EditorState.getActiveMap().levelData().<NU2MapData>as().scene().blocks().get("GSNH").address() + 8 +
                        EditorState.getActiveMap().levelData().<NU2MapData>as().scene().texMetaListAddressFromGSNH().get() +
                        EditorState.getActiveMap().levelData().<NU2MapData>as().scene().textureCount().get() * 4;

        var textureIndexList =
                EditorState.getActiveMap().levelData().<NU2MapData>as().scene().blocks().get("GSNH").address() + 8 +
                        EditorState.getActiveMap().levelData().<NU2MapData>as().scene().texIndexListAddressFromGSNH().get();

        var countAddress = EditorState.getActiveMap().levelData().<NU2MapData>as().scene().blocks().get("GSNH").address() + 8 + 4;
        var count = EditorState.getActiveMap().levelData().<NU2MapData>as().scene().textureCount().get();

        SceneFileWriter.addSpace(textureListEnd, 0x4 * amount);
        MapWriter.applyPatch(SCENE, countAddress, Util.littleEndian(count + amount));
        for(int i = 0; i < amount;  i++){
            int target = targetAddress + i*0x28;
            int ptr = textureListEnd + i*4;
            MapWriter.applyPatch(SCENE, ptr, Util.littleEndian(target - ptr));
            SceneFileWriter.addPointer(ptr);
        }

        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

        SceneFileWriter.addSpace(textureIndexList + 4, 0x4 * amount);

        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

        return targetAddress;
    }

    public static void appendTextures(List<TextureHeader> textures, int startDataAddress) throws IOException {
        int allLengths = textures.stream().mapToInt(t -> t.contents.length).sum() + textures.size()*24;
        var buffer = ByteBuffer.allocate(allLengths).order(ByteOrder.LITTLE_ENDIAN);
        for(var texture : textures){
            buffer.putInt(texture.width)
                    .putInt(texture.height)
                    .putInt(texture.minmaps)
                    .putInt(0)
                    .putInt(2)
                    .putInt(texture.contents.length)
                    .put(texture.contents);
        }

        var metaBuffers = ByteBuffer.allocate(textures.size()*0x28).order(ByteOrder.LITTLE_ENDIAN);
        for(var texture : textures){
            metaBuffers.putInt(texture.width);
            metaBuffers.putInt(texture.height);
            metaBuffers.putInt(new Random().nextInt());
            metaBuffers.putInt(new Random().nextInt());
            metaBuffers.putInt(new Random().nextInt());
            metaBuffers.putInt(new Random().nextInt());
            metaBuffers.put(new byte[0x10]);
        }
        MapWriter.applyPatch(SCENE, startDataAddress, metaBuffers);

        var existingTextures = EditorState.getActiveMap().levelData().<NU2MapData>as().scene().textures();
        var lastTexture = existingTextures.get(existingTextures.size()-1);

        SceneFileWriter.addSpace(lastTexture.textureEnd(), buffer.capacity());
        MapWriter.applyPatch(SCENE, lastTexture.textureEnd(), buffer);
        MapWriter.applyPatch(SCENE, 4, Util.littleEndian((short)
                EditorState.getActiveMap().levelData().<NU2MapData>as().scene().textureCount().get()));
    }

    public static void replaceTexture(FileTexture original, TextureHeader newTexture) throws IOException {
        int originalSize = original.textureEnd() - original.textureAddress();
        int newSize = newTexture.contents.length;

        MapWriter.applyPatch(MapWriter.WritableObject.SCENE, original.descriptor().address(), Util.littleEndian(newTexture.width));
        MapWriter.applyPatch(MapWriter.WritableObject.SCENE, original.descriptor().address() + 4, Util.littleEndian(newTexture.width));


        if(originalSize > newSize){
            SceneFileWriter.removeSpace(original.textureAddress(), originalSize - newSize);
        }else if(originalSize < newSize){
            SceneFileWriter.addSpace(original.textureAddress(), newSize - originalSize);
        }

        var buffer = ByteBuffer.allocate(newTexture.contents.length + 24).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(newTexture.width)
                .putInt(newTexture.height)
                .putInt(newTexture.minmaps)
                .putInt(0)
                .putInt(2)
                .putInt(newTexture.contents.length)
                .put(newTexture.contents);

        MapWriter.applyPatch(MapWriter.WritableObject.SCENE, original.textureAddress() - 24, buffer);
    }

    public static void addPointer(int target) throws IOException {
        var writer = EditorState.getActiveMap().getFileOfExtension("gsc").channel();
        var block = EditorState.getActiveMap().levelData().<NU2MapData>as().scene().blocks().get("PNTR");

        writer.position(block.address() + 4);
        int size = Util.readLittleEndianInt(writer);
        int numPntr = Util.readLittleEndianInt(writer);

        var targetAddr = block.address() + 8 + 4 + (numPntr * 4);
        MapWriter.addSpaceAtLocation(SCENE, targetAddr, 4);
        MapWriter.applyPatch(SCENE, targetAddr, Util.littleEndian(target - targetAddr));
        MapWriter.applyPatch(SCENE, block.address() + 8, Util.littleEndian(numPntr + 1));
        MapWriter.applyPatch(SCENE, block.address() + 4, Util.littleEndian(size + 4));
    }

    public static void addSpace(int address, int size) throws IOException {
        adjustPointers(address, size);
        readjustBlocksAfterResize(address, size);
        MapWriter.addSpaceAtLocation(SCENE, address, size);
    }

    public static void removeSpace(int address, int size){
        try{
            clearPointersIntoArea(address, size);
            adjustPointers(address, -size);
            MapWriter.removeSpaceAtLocation(SCENE, address, size);
            readjustBlocksAfterResize(address, -size);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    private static void readjustBlocksAfterResize(int address, int size){
        boolean hasFoundBlock = false;
        var replacementBlocks = new LinkedHashMap<String, NU2MapData.SceneData.Block>();
        for(var block : EditorState.getActiveMap().levelData().<NU2MapData>as().scene().blocks().entrySet()){
            if(block.getValue().address() > address && block.getKey().equals("NTBL")){
                return;
            }

            if(hasFoundBlock){
                replacementBlocks.put(block.getKey(), new NU2MapData.SceneData.Block(block.getValue().address() + size, block.getValue().size()));
            }

            if(block.getValue().address() < address && block.getValue().address() + block.getValue().size() >= address){
                MapWriter.applyPatch(SCENE, block.getValue().address() + 4, Util.littleEndian(block.getValue().size() + size));
                replacementBlocks.put(block.getKey(), new NU2MapData.SceneData.Block(block.getValue().address(), block.getValue().size() + size));

                hasFoundBlock = true;
            }
        }

        if(hasFoundBlock){
            EditorState.getActiveMap().levelData().<NU2MapData>as().scene().blocks().putAll(replacementBlocks);
        }else{
            var block = EditorState.getActiveMap().levelData().<NU2MapData>as().scene().blocks().get("PNTR");
            MapWriter.applyPatch(SCENE, block.address() + 4, Util.littleEndian(block.size() + size));
            replacementBlocks.put("PNTR", new NU2MapData.SceneData.Block(block.address(), block.size() + size));
        }
    }

    public static void clearPointersIntoArea(int address, int size) throws IOException {
        var writer = EditorState.getActiveMap().getFileOfExtension("gsc").channel();
        var block = EditorState.getActiveMap().levelData().<NU2MapData>as().scene().blocks().get("PNTR");

        writer.position(block.address() + 4);
        int blockSize = Util.readLittleEndianInt(writer);
        int numPntr = Util.readLittleEndianInt(writer);
        var ptrs = ByteBuffer.allocate(numPntr * 4).order(ByteOrder.LITTLE_ENDIAN);
        writer.read(ptrs); ptrs.rewind();

        var removalAddresses = new ArrayList<Integer>();
        for (int i = 0; i < numPntr; i++) {
            int ptrLocation = i * 4 + block.address() + 4 + 8;
            int ptrOffset = ptrs.getInt(i * 4);
            int ptrTarget = ptrOffset + ptrLocation;

            if(ptrTarget > address && ptrTarget <= address + size){
                removalAddresses.add(ptrLocation);
            }else if(!removalAddresses.isEmpty()){
                var shift = removalAddresses.size() * 4;
                ptrs.putInt(i * 4, ptrOffset + shift);
            }
        }

        ptrs.flip().limit(numPntr * 4);

        MapWriter.applyPatch(SCENE, block.address() + 4, Util.littleEndian(blockSize - removalAddresses.size() * 4));
        MapWriter.applyPatch(SCENE, block.address() + 8, Util.littleEndian(numPntr - removalAddresses.size()));
        MapWriter.applyPatch(SCENE, block.address() + 12, ptrs);

        removalAddresses.sort(Comparator.comparingInt(i -> i));

        int removalCount = 0;
        for(var removalAddress : removalAddresses){
            MapWriter.removeSpaceAtLocation(SCENE, removalAddress - removalCount, 4);
            removalCount += 4;
        }
    }

    private static void adjustPointers(int address, int space) throws IOException {
        var writer = EditorState.getActiveMap().getFileOfExtension("gsc").channel();
        writer.position(0);
        var NU20Start = Util.readLittleEndianInt(writer);
        if(NU20Start > address){
            MapWriter.applyPatch(SCENE, 0, Util.littleEndian(NU20Start + space));
            return;
        }

        var header = NU20Start + 0x18;
        writer.position(header + 4);
        var ptrSectionOffset = Util.readLittleEndianInt(writer);
        var ptrSection = header + 4 + ptrSectionOffset;

        if(ptrSection > address) {
            MapWriter.applyPatch(SCENE, header + 4, Util.littleEndian(ptrSectionOffset + space));
        }

        writer.position(ptrSection);
        int numPntr = Util.readLittleEndianInt(writer);
        var ptrs = ByteBuffer.allocate(numPntr * 4).order(ByteOrder.LITTLE_ENDIAN);
        writer.read(ptrs); ptrs.rewind();

        for (int i = 0; i < numPntr; i++) {
            int realPos = i * 4 + ptrSection + 4;
            int ptrOffset = ptrs.getInt();
            int ptr = ptrOffset + realPos;

            writer.position(ptr);
            int offset = Util.readLittleEndianInt(writer);
            if (offset != 0){
                if(ptr < address && ptr + offset >= address){
                    MapWriter.applyPatch(SCENE, ptr, Util.littleEndian(offset + space));
                }else if(ptr > address && ptr + offset <= address){
                    MapWriter.applyPatch(SCENE, ptr, Util.littleEndian(offset - space));
                }
            }

            if(realPos > address && realPos + ptrOffset < address){
                ptrs.position(i * 4);
                ptrs.putInt(ptrOffset - space);
            }
        }

        ptrs.flip().limit(numPntr * 4);

        MapWriter.applyPatch(SCENE, ptrSection + 4, ptrs);
    }

    public record TextureHeader(byte[] contents, int width, int height, int minmaps){}
}
