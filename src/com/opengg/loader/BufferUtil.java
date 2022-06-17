package com.opengg.loader;

import com.opengg.core.math.Matrix4f;

import java.nio.ByteBuffer;

public class BufferUtil {
    public static Matrix4f readMatrix4f(ByteBuffer buffer) {
        return new Matrix4f(
                buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat(),
                buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat(),
                buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat(),
                buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
    }
}
