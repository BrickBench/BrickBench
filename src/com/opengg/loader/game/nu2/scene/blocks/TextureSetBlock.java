package com.opengg.loader.game.nu2.scene.blocks;

import com.opengg.core.engine.Resource;
import com.opengg.loader.ImageUtil;
import com.opengg.loader.Util;
import com.opengg.loader.game.nu2.NU2MapData;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;

public class TextureSetBlock extends DefaultFileBlock {

    @Override
    public void readFromFile(ByteBuffer fileBuffer, long blockLength, int blockID, int blockOffset, NU2MapData mapData) throws IOException {
        super.readFromFile(fileBuffer, blockLength, blockID, blockOffset, mapData);
        int pos = fileBuffer.position();
    }



    private void parseCSC() throws IOException {
        int num1 = fileBuffer.getInt();
        int fileInitOffset = 4;
        ByteBuffer buffer2 = ByteBuffer.allocate(num1 * 4);
        //System.out.println("Big sections: " + num1);
        HashSet<Integer> unique = new HashSet<>();
        int upward = 0;
        for (int i = 0; i < num1; i++) {
            int structPos = 0x3c * i + fileInitOffset;
            //System.out.println(Integer.toHexString(structPos+blockOffset));
            fileBuffer.position(structPos + 4);
            //System.out.println(i + ", " + Integer.toHexString(4+structPos+blockOffset));
            int unknown1 = Util.asUnsignedShort(fileBuffer.getShort());
            int unknown2 = Util.asUnsignedShort(fileBuffer.getShort());
            int unknown3 = Util.asUnsignedShort(fileBuffer.getShort());
            unique.add(unknown3);
            //System.out.println("TT: " + Integer.toHexString(unknown3) + "(" + unknown1 + "," + unknown2 + ")");
            int second = Util.asUnsignedShort(fileBuffer.getShort());
            //System.out.println("Loading section num " + second);
            int prevLoc = 0;
            ArrayList<byte[]> subBuffers = new ArrayList<>();
            int tempwidth = unknown1;
            int tempheight = unknown2;
            for (int i2 = 0; i2 < second; i2++) {
                int sectionLoc = readPointer(0xc + i2 * 4 + structPos);
                int bppFactor = switch(unknown3){
                    case 0x8:
                    case 0xe:
                        yield 2;
                    default:
                        yield 1;
                };
                int multadjust = 1;
                if(unknown3 == 0x6){
                    multadjust = 4;
                }
                byte[] array = new byte[tempwidth * tempheight / bppFactor * multadjust];
                fileBuffer.position(sectionLoc);
                fileBuffer.get(array);
                if(array.length != 0 && sectionLoc != 0)
                    subBuffers.add(array);
                tempwidth/=2;
                tempheight/=2;


                if (i2 != 0) {
                    double bpp = ((double) (sectionLoc - prevLoc) / (unknown1 * unknown2)) * 8;
                    //System.out.println("bits per pixel: " + ((double) (sectionLoc - prevLoc) / (unknown1 * unknown2)) * 8);
                }
                prevLoc = sectionLoc;
            }
            int finalSecLoc = readPointer(structPos + 0x38);
            //System.out.println("Sectionout: " + Integer.toHexString(finalSecLoc));
           // System.out.println(upward+","+second);
            tempwidth = unknown1;
            tempheight = unknown2;
            for (int j = 0; j < subBuffers.size(); j++) {
                String texname = upward + (j != 0?"_"+j:"")+".png";
                switch (unknown3) {
                    case 8 -> {
                        fileBuffer.position(blockOffset+finalSecLoc);
                        byte[] array = new byte[16 * 16];
                        fileBuffer.get(array);
                        ImageUtil.exportPallete(subBuffers.get(j), array, 16, true, tempwidth,tempheight, Resource.getTexturePath("map/" + texname));
                    }
                    case 0xe -> {
                        ImageUtil.exportRGB(subBuffers.get(j), tempwidth,tempheight, Resource.getTexturePath("map/" + texname));
                    }
                    case 0x9-> {
                        fileBuffer.position(blockOffset+finalSecLoc);
                        byte[] array1 = new byte[16 * 256];
                        fileBuffer.get(array1);
                        ImageUtil.exportPallete8(subBuffers.get(j), array1, 256, true, tempwidth, tempheight, Resource.getTexturePath("map/" + texname));
                    }
                    case 0x6->{
                        ImageUtil.exportRGBA8(subBuffers.get(j), tempwidth,tempheight, Resource.getTexturePath("map/" + texname));
                    }
                }
                tempwidth/=2;
                tempheight/=2;
            }
            upward++;

        }
    }

}
