package com.opengg.loader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImageUtil {
    public static final boolean exportTex = true;

    public static BufferedImage fromABGR8888DDS(ByteBuffer buf){
        buf.position(0xc);
        int height = buf.getInt();
        int width = buf.getInt();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        buf.position(0x80);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float a = buf.getFloat();
                float b = buf.getFloat();
                float g = buf.getFloat();
                float r = buf.getFloat();
                Color realColor = new Color(r,g,b,a);
                image.setRGB(y,x,((realColor.getAlpha() << 24) | 0x00ffffff) & realColor.getRGB());
            }
        }
        buf.rewind();
        return image;
    }

    public static Color colorFromRGB565(int rgb565) {
        int b = (((rgb565) & 0x001F) << 3);
        int g = (((rgb565) & 0x07E0) >> 3);
        int r = (((rgb565) & 0xF800) >> 8);
        return new Color(r, g, b);
    }

    public static Color colorFromRGB5A3(int rgb5a3) {
        boolean useA = ((rgb5a3 >> 15) & 1) == 1;
        int r;
        int g;
        int b;
        int a = 255;
        if (!useA) {
            r = ((rgb5a3 >> 8) & 15) * 0x11;
            g = ((rgb5a3 >> 4) & 15) * 0x11;
            b = ((rgb5a3) & 15) * 0x11;
            a = ((rgb5a3 >> 12) & 7) * 0x20;
        } else {
            r = ((rgb5a3 >> 10) & 31) * 0x8;
            g = ((rgb5a3 >> 5) & 31) * 0x8;
            b = ((rgb5a3) & 31) * 0x8;
        }
        return new Color(r, g, b, a);
    }

    public static void exportRGB565(byte[] array, int width, int height, String location) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        BitInputStream bit = new BitInputStream(new ByteArrayInputStream(array));
        int x = 0;
        int y = 0;
        for (int i = 0; i < (width * height / 16); i++) {
            for (int yRel = 0; yRel < 4; yRel++) {
                for (int xRel = 0; xRel < 4; xRel++) {

                    image.setRGB(x + xRel, y + yRel, colorFromRGB5A3(bit.readBits(16)).getRGB());
                }
            }
            x += 4;
            if (x == width) {
                y += 4;
                x = 0;
            }
        }
        File outputfile = new File(location);
        ImageIO.write(image, "png", outputfile);
    }

    public static void export1Bit(byte[] array, int width, int height, String location) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        BitInputStream bit = new BitInputStream(new ByteArrayInputStream(array));
        int x = 0;
        int y = 0;
        for (int i = 0; i < (width * height / 64); i++) {
            for (int yRel = 0; yRel < 8; yRel++) {
                for (int xRel = 0; xRel < 8; xRel++) {
                    image.setRGB(x + xRel, y + yRel, bit.readBits(1) == 1 ? 100000 : 0);
                }
            }
            x += 8;
            if (x == width) {
                y += 8;
                x = 0;
            }
        }
        File outputfile = new File(location);
        ImageIO.write(image, "png", outputfile);
    }

    public static void exportPallete(byte[] array, byte[] pallete, int numColors, boolean isRGBA, int width, int height, String location) throws IOException {
        BitInputStream bit = new BitInputStream(new ByteArrayInputStream(array));
        BufferedImage image = new BufferedImage(width, height, isRGBA ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        BitInputStream bit2 = new BitInputStream(new ByteArrayInputStream(pallete));
        Color[] colors = new Color[numColors];
        for (int i = 0; i < numColors; i++) {
            int color1 = bit2.readBits(16);
            if (!isRGBA) {
                colors[i] = colorFromRGB565(color1);
            } else {
                colors[i] = colorFromRGB5A3(color1);
            }

        }
        int x = 0;
        int y = 0;
        for (int i = 0; i < (width * height / 64); i++) {
            for (int yRel = 0; yRel < 8; yRel++) {
                for (int xRel = 0; xRel < 8; xRel++) {
                    int index = bit.readBits(4);
                    if (!isRGBA) {
                        image.setRGB(x + xRel, y + yRel, colors[index].getRGB());
                    } else {
                        image.setRGB(x + xRel, y + yRel, ((colors[index].getAlpha() << 24) | 0x00ffffff) & colors[index].getRGB());
                    }
                }
            }
            x += 8;
            if (x == width) {
                y += 8;
                x = 0;
            }
        }
        File outputfile = new File(location);
        ImageIO.write(image, "png", outputfile);
    }

    public static void exportPallete8(byte[] array, byte[] pallete, int numColors, boolean isRGBA, int width, int height, String location) throws IOException {
        BitInputStream bit = new BitInputStream(new ByteArrayInputStream(array));
        BufferedImage image = new BufferedImage(width, height, isRGBA ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        BitInputStream bit2 = new BitInputStream(new ByteArrayInputStream(pallete));
        Color[] colors = new Color[numColors];
        for (int i = 0; i < numColors; i++) {
            int color1 = bit2.readBits(16);
            if (!isRGBA) {
                colors[i] = colorFromRGB565(color1);
            } else {
                colors[i] = colorFromRGB5A3(color1);
            }

        }
        int x = 0;
        int y = 0;
        for (int i = 0; i < (width * height / 32); i++) {
            for (int yRel = 0; yRel < 4; yRel++) {
                for (int xRel = 0; xRel < 8; xRel++) {
                    int index = bit.readBits(8);
                    if (!isRGBA) {
                        image.setRGB(x + xRel, y + yRel, colors[index].getRGB());
                    } else {
                        image.setRGB(x + xRel, y + yRel, ((colors[index].getAlpha() << 24) | 0x00ffffff) & colors[index].getRGB());
                    }
                }
            }
            x += 8;
            if (x == width) {
                y += 4;
                x = 0;
            }
        }
        File outputfile = new File(location);
        ImageIO.write(image, "png", outputfile);
    }

    public static void exportPallete16(byte[] array, byte[] pallete, int numColors, boolean isRGBA, int width, int height, String location) throws IOException {
        BitInputStream bit = new BitInputStream(new ByteArrayInputStream(array));
        BufferedImage image = new BufferedImage(width, height, isRGBA ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        BitInputStream bit2 = new BitInputStream(new ByteArrayInputStream(pallete));
        Color[] colors = new Color[numColors];
        for (int i = 0; i < numColors; i++) {
            int color1 = bit2.readBits(16);
            if (!isRGBA) {
                colors[i] = colorFromRGB565(color1);
            } else {
                colors[i] = colorFromRGB5A3(color1);
            }

        }
        int x = 0;
        int y = 0;
        for (int i = 0; i < (width * height / 16); i++) {
            for (int yRel = 0; yRel < 4; yRel++) {
                for (int xRel = 0; xRel < 4; xRel++) {
                    int index = bit.readBits(16);
                    if (!isRGBA) {
                        image.setRGB(x + xRel, y + yRel, colors[index].getRGB());
                    } else {
                        image.setRGB(x + xRel, y + yRel, ((colors[index].getAlpha() << 24) | 0x00ffffff) & colors[index].getRGB());
                    }
                }
            }
            x += 4;
            if (x == width) {
                y += 4;
                x = 0;
            }
        }
        File outputfile = new File(location);
        ImageIO.write(image, "png", outputfile);
    }

    public static void exportRGB(byte[] array, int width, int height, String location) throws IOException {
        BitInputStream bit = new BitInputStream(new ByteArrayInputStream(array));
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int x = 0, y = 0;
        int counter = 0;
        for (int i = 0; i < (width * height / 16); i++) {
            int color1 = bit.readBits(16);
            Color colorc1 = colorFromRGB565(color1);
            int color2 = bit.readBits(16);
            boolean alpha = color1 >= color2;
            Color colorc2 = colorFromRGB565(color2);
            Color colorc3 = new Color((int) ((2.0 / 3) * colorc1.getRed() + (1.0 / 3) * colorc2.getRed()),
                    (int) ((2.0 / 3) * colorc1.getGreen() + (1.0 / 3) * colorc2.getGreen()),
                    (int) ((2.0 / 3) * colorc1.getBlue() + (1.0 / 3) * colorc2.getBlue()));
            Color colorc4 = new Color((int) ((2.0 / 3) * colorc2.getRed() + (1.0 / 3) * colorc1.getRed()),
                    (int) ((2.0 / 3) * colorc2.getGreen() + (1.0 / 3) * colorc1.getGreen()),
                    (int) ((2.0 / 3) * colorc2.getBlue() + (1.0 / 3) * colorc1.getBlue()));
            int xRel = 0;
            int yRel = 0;
            switch (counter) {
                case 0 -> {
                    xRel = 0;
                    yRel = 0;
                }
                case 1 -> {
                    xRel = 4;
                    yRel = 0;
                }
                case 2 -> {
                    xRel = 0;
                    yRel = 4;
                }
                case 3 -> {
                    xRel = 4;
                    yRel = 4;
                }
            }
            for (int y2 = 0; y2 < 4; y2++) {
                for (int x2 = 0; x2 < 4; x2++) {
                    int currBit = bit.readBits(2);
                    switch (currBit) {
                        case 0:
                            image.setRGB(x + xRel + x2, y + yRel + y2, colorc1.getRGB());
                            break;
                        case 1:
                            image.setRGB(x + xRel + x2, y + yRel + y2, colorc2.getRGB());
                            break;
                        case 2:
                            image.setRGB(x + xRel + x2, y + yRel + y2, colorc3.getRGB());
                            break;
                        case 3:
                            if (alpha) {
                                image.setRGB(x + xRel + x2, y + yRel + y2, colorc4.getRGB());
                            }
                            break;
                    }
                }
            }
            counter++;
            if (counter == 4) {
                x += 8;
                if (x == width) {
                    x = 0;
                    y += 8;
                }
                counter = 0;
            }

        }
        if(exportTex)
            ImageIO.write(image, "png", new File(location));
    }

    public static void exportRGBA8(byte[] array, int width, int height, String location) throws IOException {
        BitInputStream bit = new BitInputStream(new ByteArrayInputStream(array));
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int x = 0, y = 0;
        int xRel = 0;
        int yRel = 0;
        for (int i = 0; i < (width * height / 16); i++) {
            byte[] c1 = new byte[32];
            byte[] c2 = new byte[32];
            bit.read(c1);
            bit.read(c2);
            for (int y2 = 0; y2 < 4; y2++) {
                for (int x2 = 0; x2 < 4; x2++) {

                    int index = y2*8 + x2 * 2;
                    Color color = new Color(Byte.toUnsignedInt(c1[index + 1]),Byte.toUnsignedInt(c2[index]),Byte.toUnsignedInt(c2[index + 1]));
                    image.setRGB(x + xRel + x2, y + yRel + y2, ((Byte.toUnsignedInt(c1[index]) << 24) | 0x00ffffff) & color.getRGB());
                }
            }
            xRel += 4;
            if(xRel == width){
                xRel =0;
                yRel +=4;
            }

        }
        File outputfile = new File(location);
        ImageIO.write(image, "png", outputfile);
    }

    public static void exportDDS(byte[] array, int width, int height, String location) throws IOException {
        FileOutputStream stream = new FileOutputStream(location);
        ByteBuffer buf = ByteBuffer.allocate(array.length + 148);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.putInt(0x20534444);
        buf.putInt(124);
        //flags
        buf.putInt(0xA1007);
        buf.putInt(height);
        buf.putInt(width);
        buf.putInt(Math.max(1, ((width + 3) / 4)) * 8);
        //Not used volume texture
        buf.putInt(0);
        //Not used mipmap
        buf.putInt(0);
        //Not used reserverd;
        buf.put(new byte[11 * 4]);
        //Pixel format
        buf.putInt(32);
        buf.putInt(0x4);
        //buf.putInt(827611204);
        buf.putInt(0x30315844);
        //Unused
        buf.putInt(0);
        //rgbamask
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0);
        //pixelformatend
        buf.putInt(0x1000);
        buf.putInt(0);
        //unused,unused.unused
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0);
 //       System.out.println("Export header size: " + buf.position());

        //Additional header
        //more
        buf.putInt(71);
        //2d texture

        buf.putInt(3);
        //unused
        buf.putInt(0);
        //num elements array
        buf.putInt(1);
        //alpha mode
        buf.putInt(0x3);


        for (int i = 0; i < array.length / 4; i++) {
            byte b1 = array[i * 4];
            byte b2 = array[i * 4 + 1];
            byte b3 = array[i * 4 + 2];
            byte b4 = array[i * 4 + 3];
            buf.put(b4);
            buf.put(b3);
            buf.put(b2);
            buf.put(b1);
        }
        //buf.put(array);
        buf.flip();
        stream.getChannel().write(buf);


        stream.close();

    }
}

