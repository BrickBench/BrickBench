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
            int idx = Util.asUnsignedShort(fileBuffer.getShort());
        }
    //    System.out.println("Dumbo: " + Integer.toHexString(fileBuffer.position()));
        /*16 byte region*/
        for (int i = 0; i < sectionDLen; i++) {
            float f1 = fileBuffer.getFloat();
            float f2 = fileBuffer.getFloat();
            float f3 = fileBuffer.getFloat();
            float f4 = fileBuffer.getFloat();
        }

        for (int i = 0; i < sectionFLen; i++) {
            int shorty = Util.asUnsignedShort(fileBuffer.getShort());
        }
        //com.opengg.loader.game.nu2.render.blocks.PortalSetFileBlock.com.opengg.loader.game.nu2.scene.Portal Rectangle Coordinates
        for (int i = 0; i < sectionELen/4; i++) {
            var p1 = new Vector3f(fileBuffer.getFloat(), fileBuffer.getFloat(), fileBuffer.getFloat());
            var p2 = new Vector3f(fileBuffer.getFloat(), fileBuffer.getFloat(), fileBuffer.getFloat());
            var p3 = new Vector3f(fileBuffer.getFloat(), fileBuffer.getFloat(), fileBuffer.getFloat());
            var p4 = new Vector3f(fileBuffer.getFloat(), fileBuffer.getFloat(), fileBuffer.getFloat());
            mapData.scene().portalList().add(new Portal(i, new Portal.Rectangle(p1, p2, p3, p4)));
        }

        //Room Section
        for (int i = 0; i < sectionBLen; i++) {
            //Index into Section C
            fileBuffer.getInt();
            //Index into Section D
            fileBuffer.getInt();
            //0x08
            //Index into Section F
            fileBuffer.getInt();
            //0x0F is num portals
            //sectionBData[i*6+3] = fileBuffer.getInt();
            //0x10
            fileBuffer.getInt();
            //System.out.println(sectionBData[i*6+4]);
            //0x14
            fileBuffer.getInt();
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
            var p1 = new Vector3f(fileBuffer.getFloat(), fileBuffer.getFloat(), fileBuffer.getFloat());
            var p2 = new Vector3f(fileBuffer.getFloat(), fileBuffer.getFloat(), fileBuffer.getFloat());
            var p3 = new Vector3f(fileBuffer.getFloat(), fileBuffer.getFloat(), fileBuffer.getFloat());
            var p4 = new Vector3f(fileBuffer.getFloat(), fileBuffer.getFloat(), fileBuffer.getFloat());
            mapData.scene().portalList().add(new Portal(i, new Portal.Rectangle(p1, p2, p3, p4)));
        }
    }

}
