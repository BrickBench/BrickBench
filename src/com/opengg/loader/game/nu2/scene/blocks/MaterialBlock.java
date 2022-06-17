package com.opengg.loader.game.nu2.scene.blocks;

import com.opengg.core.math.Vector4f;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.FileMaterial;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MaterialBlock extends DefaultFileBlock {
    @Override
    public void readFromFile(ByteBuffer fileBuffer, long blockLength, int blockID, int blockOffset, NU2MapData mapData) throws IOException {
        super.readFromFile(fileBuffer, blockLength, blockID, blockOffset, mapData);
        int materialCount = fileBuffer.getInt();
        fileBuffer.getInt();

        for (int i = 0; i < materialCount; i++) {
            var ptr = fileBuffer.position();
            var material = new FileMaterial(ptr);
            //     System.out.println("New Material " + Integer.toHexString(ptr));
            mapData.scene().materials().put(ptr, material);

            fileBuffer.position(ptr);
            var data = new byte[0x2C4];
            fileBuffer.get(data);

            fileBuffer.position(ptr + 0x38);
            int materialID = fileBuffer.getInt();
            material.mysteryPointer = readPointer();
            material.setID(materialID);
            int alphaBlend = fileBuffer.getInt();

            fileBuffer.position(ptr + 0x54);
            Vector4f color = new Vector4f(
                    fileBuffer.getFloat(),
                    fileBuffer.getFloat(),
                    fileBuffer.getFloat(),
                    fileBuffer.getFloat());

            fileBuffer.position(ptr + 0x74);
            material.setDiffuseFileTexture(mapData.scene().texturesByRealIndex().get((int) fileBuffer.getShort()));

            fileBuffer.position(ptr + 0xB4);
            material.setTextureFlags(fileBuffer.getInt());

            fileBuffer.position(ptr + 0xB4 + 0x4c);
            material.setNormalIndex(mapData.scene().texturesByRealIndex().get(fileBuffer.getInt()));

            fileBuffer.position(ptr + 0xB4 + 0x13C);
            int vertexFormatBits = fileBuffer.getInt();
            int formatBits2 = fileBuffer.getInt();

            fileBuffer.position(ptr + 0xB4 + 0xA8);
            byte lightmapIdx = fileBuffer.get();
            byte surfaceIdx = fileBuffer.get();
            fileBuffer.get();
            byte normalIdx = fileBuffer.get();

            fileBuffer.position(ptr + 0xB4 + 0x1B4);
            int inputDefines = fileBuffer.getInt();
            int shaderDefines = fileBuffer.getInt();
            int uvsetCoords = fileBuffer.getInt();

            material.setColor(color);
            material.setAlphaType(alphaBlend);
            material.setFormatBits(vertexFormatBits);
            material.setInputDefinesBits(inputDefines);
            material.setShaderDefinesBits(shaderDefines);
            material.setUVSetCoords(uvsetCoords);
            material.setLightmapSetIndex(lightmapIdx);
            material.setSurfaceUVIndex(surfaceIdx);
            material.generateShaderSettings();

            fileBuffer.position(ptr + 0x2C4);

        }
    }
}