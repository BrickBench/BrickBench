package com.opengg.loader.game.nu2.scene.blocks;

import com.opengg.loader.game.nu2.NU2MapData;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DINIBlock  extends DefaultFileBlock{

    @Override
    public void readFromFile(ByteBuffer fileBuffer, long blockLength, int blockID, int blockOffset, NU2MapData mapData) throws IOException {
        super.readFromFile(fileBuffer, blockLength, blockID, blockOffset, mapData);
        int size = fileBuffer.getInt();
        fileBuffer.getInt();
        for(var i = 0; i < size; i++){
            mapData.scene().DINIData().add(fileBuffer.getShort());
        }
    }
}
