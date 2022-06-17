package com.opengg.loader.game.nu2.scene.blocks;

import com.opengg.loader.Util;
import com.opengg.loader.game.nu2.NU2MapData;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

public class NameTableFileBlock extends DefaultFileBlock{
    public static NameTableFileBlock CURRENT;

    static final Map<Long, String> nameTable = new LinkedHashMap<>();
    @Override
    public void readFromFile(ByteBuffer fileBuffer, long blockLength, int blockID, int blockOffset, NU2MapData mapData) throws IOException {
        super.readFromFile(fileBuffer, blockLength, blockID, blockOffset, mapData);

        CURRENT = this;

        nameTable.clear();

        int size = fileBuffer.getInt();
        while (fileBuffer.position() < blockOffset + blockLength){
            long offset = fileBuffer.position();
            String value = Util.getNullTerminated(fileBuffer);
            nameTable.put(offset, value);
        }
    }

    public String getByOffsetFromNameTable(int offset){
        return nameTable.get((long)offset + blockOffset);
    }

    public String getByOffsetFromStart(int offset){
        return nameTable.get((long)offset);
    }
}
