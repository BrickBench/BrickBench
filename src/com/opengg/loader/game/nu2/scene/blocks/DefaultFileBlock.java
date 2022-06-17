package com.opengg.loader.game.nu2.scene.blocks;

import com.opengg.core.console.GGConsole;
import com.opengg.loader.BrickBench;
import com.opengg.loader.Util;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.SceneFileLoader;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A block in an NU2 scene file (GSC/GHG).
 *
 * Extend this class to create a loader.
 */
public class DefaultFileBlock {
    public ByteBuffer fileBuffer;
    public long blockLength;
    public int blockID;
    public int blockOffset;
    public NU2MapData mapData;

    /**
     * Read in a block.
     *
     * @param fileBuffer The entire file buffer for the scene file.
     * @param blockLength The length of the block.
     * @param blockID The four-byte identifier for this block.
     * @param blockOffset The offset of the block in the file buffer.
     * @param mapData The NU2 map data that is being loaded into.
     */
    public void readFromFile(ByteBuffer fileBuffer, long blockLength, int blockID, int blockOffset, NU2MapData mapData) throws IOException {
        this.blockID = blockID;
        this.blockLength = blockLength;
        this.blockOffset = blockOffset;
        this.fileBuffer = fileBuffer;
        this.mapData = mapData;
        if(BrickBench.DEVMODE){
            GGConsole.debug("Loaded " + Util.blockIDToString(Integer.reverseBytes(blockID)) + " of length " + blockLength + " at " + Integer.toHexString(blockOffset));
        }
    }

    /**
     * Read a pointer at the current location.
     *
     * @see DefaultFileBlock#readPointer(int)
     */
    public int readPointer() {
        return readPointer(fileBuffer.position());
    }

    /**
     * Read a pointer at the given offset.
     *
     * This applies the relative pointer if possible.
     */
    public int readPointer(int offset){
        fileBuffer.position(offset);
        int temp = fileBuffer.getInt();
        if(SceneFileLoader.PARSE_PNTR) return temp;

        if (temp == 0) {
            return 0;
        } else {
            return offset + temp;
        }
    }
}
