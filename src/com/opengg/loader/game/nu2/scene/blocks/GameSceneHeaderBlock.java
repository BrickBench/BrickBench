package com.opengg.loader.game.nu2.scene.blocks;

import com.opengg.core.console.GGConsole;
import com.opengg.core.system.Allocator;
import com.opengg.loader.Project;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.FileTexture;
import com.opengg.loader.game.nu2.scene.GSCMesh;
import com.opengg.loader.game.nu2.scene.GameBuffer;
import com.opengg.loader.loading.MapLoader;
import com.opengg.loader.game.nu2.scene.SceneFileLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class GameSceneHeaderBlock extends DefaultFileBlock{
    public void readFromFile(ByteBuffer fileBuffer, long blockLength, int blockID, int blockOffset, NU2MapData mapData) throws IOException {
        super.readFromFile(fileBuffer, blockLength, blockID, blockOffset, mapData);

        int texIndexList = readPointer();

        int texCount = fileBuffer.getInt();
        int texMetaPtr = readPointer();
        int materialPtrList = readPointer();

        mapData.scene().materialListAddressFromGSNH().set(materialPtrList - blockOffset);
        mapData.scene().texMetaListAddressFromGSNH().set(texMetaPtr - blockOffset);
        mapData.scene().texIndexListAddressFromGSNH().set(texIndexList - blockOffset);

        mapData.scene().textureCount().set(texCount);

        // fileBuffer.position(0x28);
        // int meshMetaPtr = readFilePositionPlusOffset();

        fileBuffer.position(blockOffset + 0x1d0);
        fileBuffer.position(readPointer());

        int gscRenderableList = fileBuffer.position();
        fileBuffer.position(gscRenderableList + 0x10);

        int listStartAddr = readPointer();
        int gscRenderableAmount = fileBuffer.getInt();
        int gscListEndAddr = fileBuffer.getInt();
        int listEndAddr = listStartAddr + gscRenderableAmount*4;

        for(int i = 0; i < gscRenderableAmount; i++){
            fileBuffer.position(listStartAddr + i * 4);
            var meshAddr = readPointer();
            var mesh = readMesh(meshAddr);
            mapData.scene().meshes().put(meshAddr, mesh);
        }

        if(listEndAddr != gscListEndAddr){
            GGConsole.warning("Game scene header has unequal list ends for parts!");
        }

        mapData.scene().gscRenderableEndFromGSNH().set(listEndAddr - blockOffset);
        mapData.scene().gscRenderableListFromGSNH().set(gscRenderableList - blockOffset);
        mapData.scene().gscRenderableListLen().set(gscRenderableAmount);

        if(MapLoader.CURRENT_GAME_VERSION == Project.GameVersion.LIJ1 || MapLoader.CURRENT_GAME_VERSION == Project.GameVersion.LB1){
            loadLIJTextures(texCount, texMetaPtr);
        } else {
            loadTCSTextures(texCount, texMetaPtr);
        }

        int vertexBufferCount = fileBuffer.getShort();
        for(int i = 0; i < vertexBufferCount; i++){
            int bufAddr = fileBuffer.position();
            int bufSize = fileBuffer.getInt();
            byte[] data = new byte[bufSize];
            fileBuffer.get(data);
            mapData.scene().gscVertexBuffers().add(new GameBuffer(data, bufAddr, bufSize));
        }

        int indexBufferCount = fileBuffer.getShort();
        for(int i = 0; i < indexBufferCount; i++){
            int bufAddr = fileBuffer.position();
            int bufSize = fileBuffer.getInt();
            byte[] data = new byte[bufSize];
            fileBuffer.get(data);
            mapData.scene().gscIndexBuffers().add(new GameBuffer(data, bufAddr, bufSize));
        }
    }

    public void loadLIJTextures(int texCount, int texMetaPtr) {
        fileBuffer.position(SceneFileLoader.pntrLocation-4);
        int texStart = readPointer();

        List<FileTexture.Descriptor> realDescriptors = new ArrayList<>();
        for (int i = 0; i < texCount; i++) {
            fileBuffer.position(texMetaPtr + i * 4);

            int descriptorPos = readPointer();
            fileBuffer.position(descriptorPos);

            var pos = fileBuffer.position();
            int width = fileBuffer.getInt();
            int height = fileBuffer.getInt();

            fileBuffer.position(descriptorPos + 0x44);
            int size = fileBuffer.getInt();

            realDescriptors.add(new FileTexture.Descriptor(pos, width, height, size, i));
        }

        fileBuffer.position(texStart);
        for(var descriptor : realDescriptors){
            if(descriptor.size() > 0){
                readTextureContents(descriptor);
            }
        }
    }

    public void loadTCSTextures(int texCount, int texDescriptorPtr) {
        fileBuffer.position(texDescriptorPtr);

        List<FileTexture.Descriptor> realDescriptors = new ArrayList<>();
        for(int i = 0; i < texCount; i++){
            fileBuffer.position(texDescriptorPtr + i * 4);
            int targetPtr = readPointer();
            fileBuffer.position(targetPtr);

            var descriptor = readTextureDescriptor();
            if(descriptor.width() != 0 && descriptor.height() != 0){
                realDescriptors.add(new FileTexture.Descriptor(descriptor.address(), descriptor.width(), descriptor.height(), 0, i));
            }
        }

        fileBuffer.position(0x6);

        for (var descriptor : realDescriptors) {
            int width = fileBuffer.getInt();
            int height = fileBuffer.getInt();
            fileBuffer.getInt();
            fileBuffer.getInt();
            fileBuffer.getInt();
            int size = fileBuffer.getInt();

            readTextureContents(new FileTexture.Descriptor(descriptor.address(), width, height, size, descriptor.trueIndex()));
        }
    }

    private void readTextureContents( FileTexture.Descriptor descriptor){
        int textureStart = fileBuffer.position();
        int type = fileBuffer.getInt();
        fileBuffer.position(textureStart);

        String extension = switch(type){
            case 0x474e5089 -> ".png";
            default -> ".dxt";
        };

        byte[] content = new byte[descriptor.size()];
        fileBuffer.get(content);

        String name = mapData.xmlData().loadedTextures().getOrDefault("Texture_" + descriptor.trueIndex(), "Texture_" + descriptor.trueIndex());

        ByteBuffer image = Allocator.alloc(content.length).put(content).flip();
        var texture = new FileTexture(name, image, textureStart, fileBuffer.position(), descriptor);
        mapData.scene().textures().add(texture);
        mapData.scene().texturesByRealIndex().put(descriptor.trueIndex(), texture);
    }

    private FileTexture.Descriptor readTextureDescriptor(){
        int pos = fileBuffer.position();

        int width = fileBuffer.getInt();
        int height = fileBuffer.getInt();
/*
        System.out.println();
        System.out.println(Integer.toHexString(pos));
        System.out.println(Integer.toHexString(fileBuffer.getInt()));
        System.out.println(Integer.toHexString(fileBuffer.getInt()));
        System.out.println(Integer.toHexString(fileBuffer.getInt()));
        System.out.println(Integer.toHexString(fileBuffer.getInt()));
*/
        return new FileTexture.Descriptor(pos, width, height,0, 0);
    }

    private GSCMesh readMesh(int ptr) {
        fileBuffer.position(ptr);

        int type = fileBuffer.getInt();

        int triangleCount = fileBuffer.getInt();
        int vertexSize = fileBuffer.getShort();
        fileBuffer.position(ptr + 0x14);

        int vertexOffset = fileBuffer.getInt();
        int vertexCount = fileBuffer.getInt();

        int indexOffset = fileBuffer.getInt();
        int indexListID = fileBuffer.getInt();
        int vertexListID = fileBuffer.getInt();
        int useDynamicBuffer = fileBuffer.getInt();

        fileBuffer.position(ptr + 0x34);
        int dynamicBuffer = fileBuffer.getInt();

        if (type != 6) {
            GGConsole.warn("Attempted to load a non-triangle strip mesh at " + Integer.toHexString(ptr));
        }

        mapData.scene().vertexBuffersBySize().computeIfAbsent(vertexSize, k -> new ArrayList<>()).add(vertexListID);
        return new GSCMesh(ptr, vertexCount, vertexSize, vertexOffset, vertexListID, triangleCount, indexOffset, indexListID, useDynamicBuffer, dynamicBuffer);
    }
}
