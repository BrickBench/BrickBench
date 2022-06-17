package com.opengg.loader;

import com.opengg.core.math.Vector2f;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

/**
 * Math and other utilities.
 */
public class Util {
    /**
     * Convert a short to a float angle.
     * This maps the entire short range (-32768:32767) to -180:180 degrees
     */
    public static float shortAngleToFloat(short angle){
        return angle * 360f / 2 / Short.MAX_VALUE;
    }

    /**
     * Convert a float to a short angle.
     * This does the reverse of {@link shortAngleToFloat} 
     */
    public static short floatToShortAngle(float angle){
        return (short) (angle * Short.MAX_VALUE * 2 / 360);
    }

    /**
     * Rotate the given point a certain angle around the given origin.
     */
    public static Vector2f rotateAroundPoint(float angle, Vector2f origin, Vector2f point){
        double x = Math.cos(Math.toRadians(angle)) * (point.x - origin.x) - Math.sin(Math.toRadians(angle)) * (point.y - origin.y) + origin.x;
        double y = Math.sin(Math.toRadians(angle)) * (point.x - origin.x) - Math.cos(Math.toRadians(angle)) * (point.y - origin.y) + origin.y;
        return new Vector2f((float)x,(float)y);
    }

    /**
     * Convert a signed short to an unsigned int.
     */
    public static int asUnsignedShort(short a) {
        return a & 0x0000FFFF;
    }

    /**
     * Convert a signed byte to an unsigned short.
     */
    public static short asUnsignedByte(byte a) {
        return (short) (a & 0xFF);
    }

    /**
     * Convert a signed int to an unsigned long.
     */
    public static long asUnsignedLong(int a) {
        return a & (long) 0x00000000FFFFFFFF;
    }

    /**
     * Convert a four-byte block ID to its string representation.
     */
    public static String blockIDToString(int blockID) {
        String s = "";
        s = s + (char) (blockID >> 24) + (char) ((blockID >> 16) & 255) + (char) ((blockID >> 8) & 255) + (char) ((blockID) & 255);
        return s;
    }

    /**
     * Convert a byte-array fixed point number to a float, given the location of the point.
     */
    public static float fixedPointToFloat(byte[] arr, int pointLoc) {
        int a = 0;

        if (arr.length == 4) {
            a = a | (arr[3] << 24);
        }

        if (arr.length > 2) {
            a = a | (arr[2] << 16);
        }
        if (arr.length > 1) {
            a = a | (asUnsignedByte(arr[1]) << 8);
        }

        if (arr.length > 0) {
            a |= asUnsignedByte(arr[0]);
        }

        int decimal = a & ((1 << pointLoc) - 1);
        int integer = a >> (pointLoc);

        float result = 0;
        while (integer != 0) {
            result *= 2;
            result += (integer & 1);
            integer = integer >> 1;
        }

        float dResult = 0;
        int exp = -1;

        for (int i = pointLoc - 1; i >= 0; i--) {
            double temp = ((decimal >> (i)) & 1) * Math.pow(2, exp);
            dResult += temp;
            exp--;
        }
        return result + dResult;
    }

    /**
     * Scales the given image to a certain width.
     */
    public static BufferedImage getScaledImage(int targetWidth, BufferedImage src) {
        return getScaledImage(targetWidth, (int) (src.getHeight() * ((double)targetWidth/(double)src.getWidth())), src);
    }

    /**
     * Scales the given image to a given width and height.
     *
     * This compresses or stretches the image as needed.
     */
    public static BufferedImage getScaledImage(int targetWidth, int targetHeight, Image src){
        BufferedImage tThumbImage = new BufferedImage( targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB );
        Graphics2D tGraphics2D = tThumbImage.createGraphics(); //create a graphics object to paint to
        tGraphics2D.setBackground( new Color(0f,0f,0f,1f));
        tGraphics2D.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR );
        tGraphics2D.drawImage( src, 0, 0, targetWidth, targetHeight, null ); //draw the image scaled

        return tThumbImage;
    }

    /**
     * Cleanly extracts a null-terminated string from a byte buffer.
     */
    public static String getStringFromBuffer(ByteBuffer buf, int size){
        byte[] data = new byte[size];
        buf.get(data);
        return new String(data).trim();
    }

    /**
     * Create a byte buffer of a certain size to store the given string.
     *
     * This function writes as much of the given string as fits into the buffer, filling the rest with null.
     */
    public static ByteBuffer getStringBytes(String str, int size){
        return ByteBuffer.allocate(size).put(str.substring(0, Math.min(str.length(), size)).getBytes(StandardCharsets.UTF_8)).position(size).flip();
    }

    
    /**
     * Read and return a null-terminated string from the given bytebuffer at the current position.
     */
    public static String getNullTerminated(ByteBuffer bb) {
        StringBuilder sb = new StringBuilder(64);
        while (bb.remaining() > 0) // safer
        {
            char c = (char) bb.get();
            if (c == '\0') break;
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Returns a buffer with a little-endian representation of the given float.
     */
    public static ByteBuffer littleEndian(float f){
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(f).flip();
    }

    /**
     * Returns a buffer with a little-endian representation of the given int.
     */
    public static ByteBuffer littleEndian(int f){
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(f).flip();
    }

    /**
     * Returns a buffer with a little-endian representation of the given short.
     */
    public static ByteBuffer littleEndian(short f){
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(f).flip();
    }

    /**
     * Read a little-endian integer from the given FileChannel.
     */
    static final ByteBuffer endianRead = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    public static int readLittleEndianInt(FileChannel file) throws IOException {
        endianRead.position(0);
        file.read(endianRead);
        endianRead.position(0);

        return endianRead.getInt();
    }

    /**
     * Create an order-maintaining map given its entries.
     */
    public static <T,U>  Map<T,U> createOrderedMapFrom(List<Map.Entry<? extends T, ? extends U>> entries){
        var map = new LinkedHashMap<T, U>();
        for(var entry : entries){
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    /**
     * Returns a buffer with a little-endian representation of the given float.
     */
    @SafeVarargs
    public static <T,U>  Map<T,U> createOrderedMapFrom(Map.Entry<? extends T, ? extends U>... entries){
        var map = new LinkedHashMap<T, U>();
        for(var entry : entries){
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    /**
     * Append the given entries to a map if a condition is met.
     */
    public static <T,U>  Map<T,U> appendIf(Map<T, U> original, boolean condition, Map.Entry<? extends T, ? extends U>... entries){
        if(condition){
            for(var entry : entries){
                original.put(entry.getKey(), entry.getValue());
            }
        }
        return original;
    }

    /**
     * Create an iterable from an xml NodeList.
     */
    public static Iterable<Node> iterable(final NodeList nodeList) {
        return () -> new Iterator<>() {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < nodeList.getLength();
            }

            @Override
            public Node next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return nodeList.item(index++);
            }
        };
    }

    /**
     * Return the namespace of an EditorEntity reference.
     *
     * @see EditorState
     */
    public static String getNamespace(String path) {
        var split = path.split(":");
        if (split.length == 2) {
            return split[0];
        }
        return "";
    }
}
