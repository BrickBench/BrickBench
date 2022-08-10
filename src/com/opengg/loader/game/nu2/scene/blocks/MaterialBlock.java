package com.opengg.loader.game.nu2.scene.blocks;

import com.opengg.core.math.Vector4f;
import com.opengg.loader.Util;
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
            material.setColor(new Vector4f(
                    fileBuffer.getFloat(),
                    fileBuffer.getFloat(),
                    fileBuffer.getFloat(),
                    fileBuffer.getFloat()));
            
            fileBuffer.position(ptr + 0x74);
            material.setDiffuseFileTexture(mapData.scene().texturesByRealIndex().get((int) fileBuffer.getShort()));

            fileBuffer.position(ptr + 0xB4);
            material.setTextureFlags(fileBuffer.getInt());

            fileBuffer.position(ptr + 0xB4 + 0x60);
            float exp = fileBuffer.getFloat();

            fileBuffer.position(ptr + 0xB4 + 0x78);
            float reflPower = fileBuffer.getFloat();

            fileBuffer.position(ptr + 0xB4 + 0x90);
            float fresnelMul = fileBuffer.getFloat();
            float fresnelCoeff = fileBuffer.getFloat();
            material.setReflectivityColor(Util.packedIntToVector4f(0x1f1f1f1f));
            material.setSpecular(new Vector4f(exp, reflPower, fresnelMul, fresnelCoeff));
            
            fileBuffer.position(ptr + 0xB4 + 0x48);
            material.setSpecularFileTexture(mapData.scene().texturesByRealIndex().get(fileBuffer.getInt()));
            material.setNormalIndex(mapData.scene().texturesByRealIndex().get(fileBuffer.getInt()));
            
            fileBuffer.position(ptr + 0xB4 + 0x13C);
            int vertexFormatBits = fileBuffer.getInt();
            int formatBits2 = fileBuffer.getInt();

            fileBuffer.position(ptr + 0xB4 + 0xA8);
            byte lightmapIdx = fileBuffer.get();
            byte surfaceIdx = fileBuffer.get();
            byte specularIdx = fileBuffer.get();
            byte normalIdx = fileBuffer.get();

            fileBuffer.position(ptr + 0xB4 + 0x1B4);
            int inputDefines = fileBuffer.getInt();
            int shaderDefines = fileBuffer.getInt();
            int uvsetCoords = fileBuffer.getInt();

            material.setAlphaType(alphaBlend);
            material.setFormatBits(vertexFormatBits);
            material.setInputDefinesBits(inputDefines);
            material.setShaderDefinesBits(shaderDefines);
            material.setUVSetCoords(uvsetCoords);
            material.setLightmapSetIndex(lightmapIdx);
            material.setSpecularIndex(specularIdx);
            material.setSurfaceUVIndex(surfaceIdx);
            material.generateShaderSettings();

            fileBuffer.position(ptr + 0x2C4);

        }
    }
}
