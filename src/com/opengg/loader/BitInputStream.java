package com.opengg.loader;

import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream that can read individual bits.
 */
public class BitInputStream extends InputStream {
    private final InputStream inStream;
    private int bitCount;
    private int buffer;

    public BitInputStream(InputStream in) {
        this.inStream = in;
    }

    public void close() throws IOException {
        if (inStream != null) {
            inStream.close();
        }
    }


    public int readBits(int numBits) throws IOException {
        int result = 0;
        if (inStream == null) {
            return -1;
        }

        while (numBits > bitCount) {
            result |= (buffer << (numBits - bitCount));
            numBits -= bitCount;
            if ((buffer = inStream.read()) == -1) {
                return -1;
            }
            bitCount = Byte.SIZE;
        }

        if (numBits > 0) {
            result |= buffer >> (bitCount - numBits);
            buffer &= ((1 << (bitCount-numBits))-1);
            bitCount -= numBits;
        }
        return result;
    }

    public int read() throws IOException {
        return readBits(8);
    }
}

