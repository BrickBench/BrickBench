package com.opengg.loader.game.nu2.scene.blocks;

import com.opengg.core.math.Vector3f;
import com.opengg.loader.Util;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.Portal;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PortalSetFileBlock extends DefaultFileBlock{

    @Override
    public void readFromFile(ByteBuffer fileBuffer, long blockLength, int blockID, int blockOffset, NU2MapData mapData) throws IOException {
        super.readFromFile(fileBuffer,blockLength,blockID, blockOffset, mapData);
        mapData.scene().portalList().clear();
        //Dummy Integer
        fileBuffer.getInt();

        int sectionALen = Util.asUnsignedShort(fileBuffer.getShort());
        int sectionBLen = Util.asUnsignedShort(fileBuffer.getShort());
        int sectionCLen = Util.asUnsignedShort(fileBuffer.getShort());
        int sectionDLen = Util.asUnsignedShort(fileBuffer.getShort());
        int sectionELen = Util.asUnsignedShort(fileBuffer.getShort());
        int sectionFLen = Util.asUnsignedShort(fileBuffer.getShort());

        int[] sectionCData = new int[sectionCLen];
        for (int i = 0; i < sectionCLen; i++) {
            sectionCData[i] = Util.asUnsignedShort(fileBuffer.getShort());
        }
    //    System.out.println("Dumbo: " + Integer.toHexString(fileBuffer.position()));
        /*16 byte region*/
        float[] sectionDData = new float[sectionDLen*4];
        for (int i = 0; i < sectionDLen; i++) {
            sectionDData[i*4] = fileBuffer.getFloat();
            sectionDData[i*4+1] = fileBuffer.getFloat();
            sectionDData[i*4+2] = fileBuffer.getFloat();
            sectionDData[i*4+3] = fileBuffer.getFloat();
        }

        int[] sectionFData = new int[sectionFLen];
        for (int i = 0; i < sectionFLen; i++) {
            sectionFData[i] = Util.asUnsignedShort(fileBuffer.getShort());
        }
        //com.opengg.loader.game.nu2.render.blocks.PortalSetFileBlock.com.opengg.loader.game.nu2.scene.Portal Rectangle Coordinates
        float[] sectionEData = new float[sectionELen*3];
        for (int i = 0; i < sectionELen; i++) {
            sectionEData[i*3] = fileBuffer.getFloat();
            sectionEData[i*3+1] = fileBuffer.getFloat();
            sectionEData[i*3+2] = fileBuffer.getFloat();
        }

        //Room Section
        int[] sectionBData = new int[sectionBLen*6];
       // System.out.println("Room pos: " +fileBuffer.position());
        for (int i = 0; i < sectionBLen; i++) {
            //Index into Section C
            sectionBData[i*6] = fileBuffer.getInt();
            sectionBData[i*6+1] = fileBuffer.getInt();
          //  System.out.println(sectionDData[sectionBData[i*6+1]] +","+sectionDData[sectionBData[i*6+1]+1] +","+sectionDData[sectionBData[i*6+1]+2]+","+sectionDData[sectionBData[i*6+1]+3]);
            //0x08
            //Index into Section F
            sectionBData[i*6+2] = fileBuffer.getInt();
            //0x0F is num portals
           // System.out.println(com.opengg.loader.Util.asUnsignedByte(fileBuffer.get())+","+com.opengg.loader.Util.asUnsignedByte(fileBuffer.get())+","+
           //         com.opengg.loader.Util.asUnsignedByte(fileBuffer.get())+","+com.opengg.loader.Util.asUnsignedByte(fileBuffer.get()));
            //sectionBData[i*6+3] = fileBuffer.getInt();
            //0x10
            sectionBData[i*6+4] = fileBuffer.getInt();
            //System.out.println(sectionBData[i*6+4]);
            //0x14
            sectionBData[i*6+5] = fileBuffer.getInt();
            //System.out.println(sectionBData[i*6+5]);
          //  System.out.println("------");
        }

        //com.opengg.loader.game.nu2.render.blocks.PortalSetFileBlock.com.opengg.loader.game.nu2.scene.Portal Data
        int[] sectionAData = new int[sectionALen*8];
        for (int i = 0; i < sectionALen; i++) {
            fileBuffer.getFloat();
            fileBuffer.getInt();
            fileBuffer.getFloat();
            fileBuffer.getFloat();

            fileBuffer.getInt();
            Util.asUnsignedShort(fileBuffer.getShort());
            int room1ID = Util.asUnsignedShort(fileBuffer.getShort());
            int room2ID = Util.asUnsignedShort(fileBuffer.getShort());
            Util.asUnsignedShort(fileBuffer.getShort());
            fileBuffer.getInt();
        }


        for(int i = 0; i < sectionELen/4; i++){
            var p1 = new Vector3f(sectionEData[i*4*3],  sectionEData[i*4*3 + 1], sectionEData[i*4*3 + 2]);
            var p2 = new Vector3f(sectionEData[i*4*3 + 3],  sectionEData[i*4*3 + 4], sectionEData[i*4*3 + 5]);
            var p3 = new Vector3f(sectionEData[i*4*3 + 6],  sectionEData[i*4*3 + 7], sectionEData[i*4*3 + 8]);
            var p4 = new Vector3f(sectionEData[i*4*3 + 9],  sectionEData[i*4*3 + 10], sectionEData[i*4*3 + 11]);
            mapData.scene().portalList().add(new Portal(i, new Portal.Rectangle(p1, p2, p3, p4)));
        }
    }

}
