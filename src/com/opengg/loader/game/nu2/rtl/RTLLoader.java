package com.opengg.loader.game.nu2.rtl;

import com.opengg.core.math.Vector3f;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.rtl.RTLLight;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RTLLoader {
    public static void newLoad(ByteBuffer data){
        int version = data.getInt();
        data.position(0x5c);
        int flag = data.getInt();
        data.position(4);
        int position = data.position();
        while(flag != 0){
            data.position(position);
            data.getFloat();
            data.position(position+64);
            data.getFloat();
            data.position(position + 0x39);
            flag = data.getInt();
            position += 0x23;
        }
    }

    public static void load(ByteBuffer data, NU2MapData mapData) {
        data.order(ByteOrder.LITTLE_ENDIAN);
        int version = data.getInt();

        int lightOneCount = switch (version){
            case 0 -> 0x80;
            case 1 -> 0;
            case 2 -> 0x40;
            case 3 -> 0x40;
            default -> 0x80;
        };
        int firstPosition = data.position();
        for (int i = 0; i < lightOneCount; i++) {
            int address = data.position();
            Vector3f pos = new Vector3f(data.getFloat(),data.getFloat(),data.getFloat());
            Vector3f u2 = new Vector3f(data.getFloat(),data.getFloat(),data.getFloat());
            Vector3f color = new Vector3f(data.getFloat(),data.getFloat(),data.getFloat());
            Vector3f u4 = new Vector3f(data.getFloat(),data.getFloat(),data.getFloat());
            Vector3f u5 = new Vector3f(data.getFloat(),data.getFloat(),data.getFloat());
            mapData.rtl().lights().add(new RTLLight(pos, color, address));

           // System.out.println(u1 + " | " + u2 + " |\n " + u3 + " | " + u4 + " |\n " + u5);
            //MapViewer.pointsToV(u1);
            //MapViewer.textPoints.add(Tuple.of(u1.toString(),u1));
            //Large
            float unk1 = data.getFloat();
            float unk2 = data.getFloat();
            //System.out.println(unk1 + "," + unk2);
            float d1 = data.getFloat();
            float d2 = data.getFloat();
            float d3 = data.getFloat();
            float d4 = data.getFloat();
            //System.out.println(d1+","+d2+","+d3+","+d4);

            var unk = data.getFloat();//data.getInt();
          //  System.out.println(unk);

            data.getShort();
            data.getShort();
            data.getShort();
            data.getShort();

            data.getInt();

            data.getShort();
            data.getShort();

            data.getInt();
            data.getInt();
            data.getInt();
            data.position(firstPosition + 0x8c * i);
            //System.out.println("--end--");
        }
        //System.out.println("---------------------------------------------");
        int num1 = switch (version){
            case 0 -> 0x20;
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> 0x20;
            default -> 0x20;
        };
        int secondStart = data.position();
        for (int i = 0; i < num1; i++) {
            data.getInt();
            data.getInt();
            data.getInt();
            data.getInt();
            data.getInt();
            data.getInt();
            data.getInt();
            Vector3f u1 = new Vector3f(data.getFloat(),data.getFloat(),data.getFloat());
            //System.out.println(u1);
            data.getInt();
            data.getInt();
            data.getInt();
            data.getInt();
            data.position(secondStart+i*0x4c);
        }
       // System.out.println("Unread: " + (data.capacity() - data.position()));
    }
}
