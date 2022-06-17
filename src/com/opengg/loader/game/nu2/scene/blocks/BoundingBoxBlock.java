package com.opengg.loader.game.nu2.scene.blocks;

import com.opengg.core.math.Vector3f;
import com.opengg.loader.game.nu2.scene.IABLObject;
import com.opengg.loader.game.nu2.NU2MapData;

import java.io.IOException;
import java.nio.ByteBuffer;

public class BoundingBoxBlock extends DefaultFileBlock{
    @Override
    public void readFromFile(ByteBuffer fileBuffer,long blockLength,int blockID, int blockOffset, NU2MapData mapData) throws IOException {
        super.readFromFile(fileBuffer,blockLength,blockID,blockOffset, mapData);
        int section2Size = fileBuffer.getInt();
        int bndsSize = fileBuffer.getInt();
        int bndsLocation = 0;
        boolean flagCheck = (section2Size & 0x80000000) == 0;
        if(flagCheck){
            fileBuffer.getInt();
            fileBuffer.getInt();
            //16
            byte[] array = new byte[bndsSize << 4];
            fileBuffer.get(array);

            bndsLocation = fileBuffer.position();
            //32
            byte[] array2 = new byte[bndsSize << 5];
            fileBuffer.get(array2);
        }else{
            bndsLocation = fileBuffer.position();
            while(fileBuffer.position() < bndsLocation + (bndsSize << 5)){
                var addr = fileBuffer.position();

                Vector3f pos = new Vector3f(fileBuffer.getFloat(),fileBuffer.getFloat(),fileBuffer.getFloat());
                fileBuffer.getFloat();
                Vector3f size = new Vector3f(fileBuffer.getFloat(),fileBuffer.getFloat(),fileBuffer.getFloat());
                fileBuffer.getFloat();

                mapData.scene().boundingBoxes().add(new IABLObject.IABLBoundingBox(pos, size, addr));
            }
        }
     //   System.out.println("Remaining:" + blockData.remaining());
    }
}
