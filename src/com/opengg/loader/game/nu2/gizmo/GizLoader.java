package com.opengg.loader.game.nu2.gizmo;

import com.opengg.core.math.Quaternionf;
import com.opengg.core.math.Vector3f;
import com.opengg.loader.BrickBench;
import com.opengg.loader.Util;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.SpecialObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class GizLoader {

    public static void load(ByteBuffer fileData, NU2MapData mapData) {
        fileData.order(ByteOrder.LITTLE_ENDIAN);
        fileData.getInt();
        while(fileData.remaining() > 3){
            int typeNameLength = fileData.getInt();
            if(typeNameLength <= 0) break;

            String typeName = Util.getStringFromBuffer(fileData,typeNameLength);
            int sectionLength = fileData.getInt();
            int version = 0;
            int gizCount = 0;

            var sectionStart = fileData.position();
            switch (typeName) {
                case "GizForce" ->{
                    version = Byte.toUnsignedInt(fileData.get());
                    gizCount = Short.toUnsignedInt(fileData.getShort());
                    if(gizCount != 0){
                        for (int i = 0; i < gizCount; i++) {
                            String name = Util.getStringFromBuffer(fileData,16);
                            Vector3f position = new Vector3f(fileData.getFloat(),fileData.getFloat(),fileData.getFloat());
                            BrickBench.pointsToView.add(position);
                            if(version == 1){
                                Vector3f mystery1 = new Vector3f(fileData.getFloat(),fileData.getFloat(),fileData.getFloat());
                            }
                            float x = fileData.getFloat();
                            if(version >= 8){
                                float x1 = fileData.getFloat();
                            }
                            float x2 = fileData.getFloat();
                            if(version == 1){
                                Vector3f mystery2 = new Vector3f(fileData.getFloat(),fileData.getFloat(),fileData.getFloat());
                                fileData.getShort();
                            }
                            int x3 = fileData.getInt();
                            int x4 = Byte.toUnsignedInt(fileData.get());
                            if(10 < version){
                                int x5 = Byte.toUnsignedInt(fileData.get());

                            }
                            int x6 = Byte.toUnsignedInt(fileData.get());

                            if(version == 1){
                                fileData.get();
                            }
                            //Inner Func
                            int inner1 = Byte.toUnsignedInt(fileData.get());
                            int inner2 = Byte.toUnsignedInt(fileData.get());

                            for (int j = 0; j < inner2; j++) {
                                int len = Byte.toUnsignedInt(fileData.get());
                                if(len != 0){
                                    String specialObject = Util.getStringFromBuffer(fileData,len);
                                }
                                float inner3 = fileData.getFloat();
                                float inner4 = fileData.getFloat();
                                if(inner1  < 2){

                                }else{
                                    int wow = fileData.getInt();
                                }
                                if(len != 0){
                                    if(8 < version){
                                        fileData.getShort();
                                    }
                                }
                            }
                            //End Inner Func
                            float x7 = fileData.getFloat();
                            float x8 = fileData.getFloat();

                            if(version > 5){
                                float x9 = fileData.getFloat();
                            }

                            float x10;
                            if (version >= 7) {
                                x10 = fileData.getFloat();
                            }

                            if(2 < version){
                                float x11 = fileData.getFloat();
                            }

                            if(3 < version){
                                if(version < 5){
                                    int xx = Short.toUnsignedInt(fileData.getShort());
                                }else{
                                    int len = Byte.toUnsignedInt(fileData.get());
                                    if(len != 0){
                                        Util.getStringFromBuffer(fileData,len);
                                    }
                                }
                                int studNumber = Short.toUnsignedInt(fileData.getShort());
                                int xx2 = Short.toUnsignedInt(fileData.getShort());
                                int xx3 = Short.toUnsignedInt(fileData.getShort());

                                new Vector3f(fileData.getFloat(),fileData.getFloat(),fileData.getFloat());
                            }

                            if (version >= 10) {
                                fileData.getFloat();
                            }

                            if(version > 0xd){
                                int len = Byte.toUnsignedInt(fileData.get());
                                if(len != 0){
                                    Util.getStringFromBuffer(fileData,len);
                                }
                                len = Byte.toUnsignedInt(fileData.get());
                                if(len != 0){
                                    Util.getStringFromBuffer(fileData,len);
                                }
                                len = Byte.toUnsignedInt(fileData.get());
                                if(len != 0){
                                   Util.getStringFromBuffer(fileData,len);
                                }
                            }


                        }
                    }
                }
                case "GizObstacle" -> {
                    version = Byte.toUnsignedInt(fileData.get());
                    gizCount = Short.toUnsignedInt(fileData.getShort());
                    for (int i = 0; i < gizCount; i++) {
                        String name = Util.getStringFromBuffer(fileData,16);
                        Vector3f vec1 = new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());
                        if (version >= 2) {
                            new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());
                        }

                        fileData.getFloat();
                        fileData.getFloat();
                        if (version >= 3) {
                            new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());
                            fileData.getShort();
                        }
                        fileData.getInt();
                        if (version > 0xb) {
                            fileData.getInt();
                        }
                        if (version == 6) {
                            int unknown = Short.toUnsignedInt(fileData.getShort());
                            fileData.get();
                        }
                        fileData.get();
                        fileData.get();
                        if (version >= 7) {
                            fileData.get();
                        }
                        //Wonderful Inner Func
                        int v2 = Byte.toUnsignedInt(fileData.get());
                        int big = Byte.toUnsignedInt(fileData.get());
                        for (int j = 0; j < big; j++) {
                            int bytestreamlength = Byte.toUnsignedInt(fileData.get());
                            byte[] stream = new byte[bytestreamlength];
                            fileData.get(stream);
                            float x = fileData.getFloat();
                            float y = fileData.getFloat();
                            //try{
                               // throw new Exception();
                            //}catch(Exception e){
                                //e.printStackTrace();
                            //}
                            if(v2 < 2){

                            }else{
                                fileData.getInt();
                            }
                            if(bytestreamlength != 0){
                                if (version >= 8) {
                                    fileData.getShort();
                                }
                            }
                        }
                        //end

                        if (version >= 4) {
                            fileData.getFloat();
                        }
                        if (version >= 5) {
                            fileData.getFloat();
                        }
                        if(version > 7){
                            fileData.getFloat();
                        }
                        if(version > 8){
                            if(version < 10){
                                fileData.getShort();
                            }else{
                                int bytestreamlength = Byte.toUnsignedInt(fileData.get());
                                if(bytestreamlength != 0) {
                                    byte[] stream = new byte[bytestreamlength];
                                    fileData.get(stream);
                                }

                            }
                            fileData.getShort();
                            fileData.getShort();
                            fileData.getShort();
                            new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());
                        }
                        if(version < 0xb){

                        }else{
                            fileData.getFloat();
                        }
                        if(version < 0xd){

                        }else{
                            int bytestreamlength = Byte.toUnsignedInt(fileData.get());
                            byte[] stream = new byte[bytestreamlength];
                            fileData.get(stream);
                        }
                        if(0xd < version){
                            int bytestreamlength = Byte.toUnsignedInt(fileData.get());
                            byte[] stream = new byte[bytestreamlength];
                            fileData.get(stream);
                        }
                    }

                }
                case "MiniCut" -> {
                    version = fileData.getInt();
                    gizCount = fileData.getInt();
                    for (int i = 0; i < gizCount; i++) {
                        int nameLen = Byte.toUnsignedInt(fileData.get());
                        byte[] nameDat = new byte[nameLen];
                        fileData.get(nameDat);
                        String name = new String(nameDat);
                        var str = fileData.getFloat() + "," + fileData.getFloat() + "," +
                                fileData.getFloat() + "," + fileData.getFloat() + "," + fileData.getFloat();
                        int subBuffer = Byte.toUnsignedInt(fileData.get());
                        for (int j = 0; j < subBuffer; j++) {
                            int nameLen2 = Byte.toUnsignedInt(fileData.get());
                            byte[] nameDat2 = new byte[nameLen2];
                            fileData.get(nameDat2);
                            String name2 = new String(nameDat2);
                            var str2 = fileData.getFloat() + "," + fileData.getFloat() + "," + fileData.getFloat() + "," + fileData.getFloat();
                            fileData.getShort();
                            fileData.getShort();
                            fileData.getShort();
                            var str3 = fileData.getFloat() + "," + fileData.getFloat();
                        }
                    }
                }
                case "blowup" ->{
                    version = fileData.getInt();
                    gizCount = 0;
                    if(version > 1){
                        gizCount = fileData.getInt();
                    }
                    int iVar9 =fileData.getInt();
                    if(version > 1){
                        for (int i = 0; i < gizCount; i++) {
                            int len = Byte.toUnsignedInt(fileData.get());
                            String name = Util.getStringFromBuffer(fileData,len);
                            len = Byte.toUnsignedInt(fileData.get());
                            //special
                            Util.getStringFromBuffer(fileData,len);
                            if(version < 0x11){
                                if(version < 5){
                                    fileData.getShort();
                                }else{
                                    len = Byte.toUnsignedInt(fileData.get());
                                    Util.getStringFromBuffer(fileData,len);
                                }
                            }else{
                                for (int j = 0; j < 2; j++) {
                                    len = Byte.toUnsignedInt(fileData.get());
                                    if(len > 0) {
                                        //par reference
                                        Util.getStringFromBuffer(fileData, len);
                                    }
                                }
                            }
                            if(version > 3){
                                for (int j = 0; j < 3; j++) {
                                    len = Byte.toUnsignedInt(fileData.get());
                                    if(len > 0) {
                                        //ptl reference
                                        Util.getStringFromBuffer(fileData, len);
                                    }
                                }
                            }
                            if(version > 0x19){
                                for (int j = 0; j < 2; j++) {
                                    len = Byte.toUnsignedInt(fileData.get());
                                    if(len > 0) {
                                        Util.getStringFromBuffer(fileData, len);
                                    }
                                }
                            }
                            if(version > 0x1a){
                                for (int j = 0; j < 2; j++) {
                                    len = Byte.toUnsignedInt(fileData.get());
                                    if(len > 0) {
                                        Util.getStringFromBuffer(fileData, len);
                                    }
                                }
                            }
                            int u514 = fileData.getInt();
                            if(version < 7){

                            }else{
                                int studCount = fileData.getInt();
                                int u495 = fileData.get();
                            }
                            if(version < 7){

                            }else{
                                fileData.getFloat();
                            }
                            if(version> 8){
                                len = Byte.toUnsignedInt(fileData.get());
                                if(len > 0) {
                                    //Blowup decal
                                    Util.getStringFromBuffer(fileData, len);
                                }
                            }
                            if(version < 0xe){
                                //get from aibl
                            }else{
                                fileData.getFloat();
                                fileData.getFloat();
                            }
                            if (version >= 0xf) {
                                fileData.get();
                                fileData.get();
                            } else {
                                //default 0
                            }
                            if (version >= 0x10) {
                                int unk = Byte.toUnsignedInt(fileData.get());
                                if(unk != 0){
                                    //no padding here
                                    Vector3f vec1 = new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());
                                    fileData.getFloat();
                                    fileData.getFloat();
                                    fileData.getFloat();
                                    fileData.getFloat();
                                    fileData.getFloat();
                                    fileData.getShort();
                                    fileData.get();
                                    fileData.get();
                                }
                            }
                            if(version < 0x16){
                                if (version >= 0x12) {
                                    len = Byte.toUnsignedInt(fileData.get());
                                    if(len > 0) {
                                        //Blowup emitobj
                                        Util.getStringFromBuffer(fileData, len);
                                    }
                                    fileData.get();
                                    fileData.getFloat();
                                    fileData.getFloat();
                                }
                            }else{
                                for (int j = 0; j < 4; j++) {
                                    len = Byte.toUnsignedInt(fileData.get());
                                    if(len > 0) {
                                        //Blowup emitobj
                                        Util.getStringFromBuffer(fileData, len);
                                    }
                                }
                                fileData.get();
                                fileData.getFloat();
                                fileData.getFloat();
                            }
                            if(version > 0x12){
                                len = Byte.toUnsignedInt(fileData.get());
                                if(len > 0) {
                                    //Blowup shadow
                                    Util.getStringFromBuffer(fileData, len);
                                }
                            }
                            if(version > 0x13){
                                len = Byte.toUnsignedInt(fileData.get());
                                if(len > 0) {
                                    //Blowup swap
                                    Util.getStringFromBuffer(fileData, len);
                                }
                            }
                            if(version > 0x16){
                                fileData.getFloat();
                            }
                            if(version > 0x17){
                                fileData.getFloat();
                            }

                        }
                        for (int i = 0; i < iVar9; i++) {
                            int len2 = Byte.toUnsignedInt(fileData.get());
                            //gizobstacle?
                            Util.getStringFromBuffer(fileData, len2);
                            if(version >= 2){
                                len2 = Byte.toUnsignedInt(fileData.get());
                                Util.getStringFromBuffer(fileData, len2);
                            }
                            new Vector3f(fileData.getFloat(),fileData.getFloat(),fileData.getFloat());
                            fileData.getShort();
                            fileData.getShort();
                            fileData.getShort();
                            if(version >= 2){
                                if (version >= 0x14) {
                                    if(version<0x1c){
                                        fileData.getInt();
                                    }else{
                                        if(version < 0x1d){
                                            fileData.getInt();
                                            //padding
                                            fileData.getInt();
                                        }else{
                                            fileData.getInt();
                                        }
                                    }
                                } else {
                                    fileData.getShort();
                                }
                                if(version > 0x1d){
                                    fileData.getInt();
                                }
                                fileData.getInt();
                                fileData.get();
                                fileData.get();
                            }
                            if(version > 3){
                                fileData.get();
                            }
                            if(version > 5){
                                fileData.getFloat();
                            }
                            if (version >= 8) {
                                fileData.getFloat();
                                fileData.getFloat();
                            }
                            if(version > 8){
                                fileData.getShort();
                                fileData.getShort();
                                fileData.getShort();
                                fileData.getFloat();
                                fileData.getFloat();
                                fileData.getFloat();
                            }
                            if (version >= 10) {
                                fileData.getFloat();
                            }
                            if (version >= 0xb) {
                                fileData.getFloat();
                                fileData.getFloat();
                                fileData.getFloat();
                            }
                            if (version >= 0xc) {
                                fileData.get();
                            }
                            if (version >= 0xd) {
                                fileData.getShort();
                                fileData.getShort();
                            }
                            if(version > 0x12){
                                fileData.getShort();
                                fileData.getShort();
                                fileData.getShort();
                                fileData.getFloat();
                                fileData.getFloat();
                                fileData.getFloat();
                                fileData.getFloat();
                            }
                            if(version> 0x14){
                                fileData.getFloat();
                            }
                            if(version > 0x16){
                                fileData.getFloat();
                            }
                            if(version > 0x1e){
                                fileData.getFloat();
                            }
                        }
                    }
                }
                case "Panel" -> {
                    version = fileData.getInt();
                    gizCount = fileData.getInt();
                    for (int i = 0; i < gizCount; i++) {
                        int filePosition = fileData.position();
                        int nameLen = fileData.getInt();
                        String name = Util.getStringFromBuffer(fileData,nameLen);
                        Vector3f pos = new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());
                        float angle = Util.shortAngleToFloat(fileData.getShort());
                        var type = fileData.get();
                        var panelType = switch (type){
                            case 0 -> Gizmo.GizPanel.PanelType.ASTROMECH;
                            case 1 -> Gizmo.GizPanel.PanelType.PROTOCOL_DROID;
                            case 2 -> Gizmo.GizPanel.PanelType.BOUNTY_HUNTER;
                            case 3 -> Gizmo.GizPanel.PanelType.STORMTROOPER;
                            default -> Gizmo.GizPanel.PanelType.UNDEFINED;
                        };

                        Vector3f activationPos = pos;
                        float activationRange = 0;
                        Gizmo.Visibility frontVisibility = Gizmo.Visibility.VISIBLE;
                        Gizmo.Visibility floorCross = Gizmo.Visibility.VISIBLE;

                        Gizmo.Option alternativeFaceColor = Gizmo.Option.NO;
                        Gizmo.Option alternativeBody = Gizmo.Option.NO;

                        if (version >= 3) {
                            frontVisibility = fileData.get() == 1 ? Gizmo.Visibility.INVISIBLE : Gizmo.Visibility.VISIBLE;
                        }
                        if (version >= 4) {
                            var activationOffset  = new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());
                            activationPos = Quaternionf.createYXZ(new Vector3f(0, angle, 0)).transform(activationOffset).add(pos);
                            activationRange = fileData.getFloat();
                        }
                        if (version >= 5) {
                            floorCross = fileData.get() == 1 ? Gizmo.Visibility.INVISIBLE : Gizmo.Visibility.VISIBLE;
                        }
                        if (version >= 6) {
                            alternativeFaceColor = fileData.get() == 1 ? Gizmo.Option.YES : Gizmo.Option.NO;
                            alternativeBody = fileData.get() == 1 ? Gizmo.Option.YES : Gizmo.Option.NO;
                        }
                        if (version >= 7) {
                            fileData.get();
                        }
                        if (version >= 8) {
                            fileData.get();
                        }

                        mapData.gizmo().gizmos().add(new Gizmo.GizPanel(nameLen, name, pos, angle, activationPos, activationRange, panelType, frontVisibility, floorCross, alternativeFaceColor, alternativeBody, filePosition, fileData.position() - filePosition));
                    }
                }
                case "ZipUp" -> {
                    version = fileData.getInt();
                    gizCount = fileData.getInt();
                    for (int i = 0; i < gizCount; i++) {
                        int address = fileData.position();
                        String name = Util.getStringFromBuffer(fileData,16);
                        Vector3f start = new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());
                        Vector3f axis = new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());
                        Vector3f end = new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());

                        fileData.getShort();
                        fileData.getShort();
                        Gizmo.ZipUp.ZipType type = fileData.get() == 1 ? Gizmo.ZipUp.ZipType.SWING : Gizmo.ZipUp.ZipType.ZIP;
                        fileData.get();
                        Gizmo.ZipUp.Direction direction = fileData.get() == 1 ? Gizmo.ZipUp.Direction.TWO_WAY : Gizmo.ZipUp.Direction.ONE_WAY;

                        Gizmo.Visibility floorCross = Gizmo.Visibility.INVISIBLE;
                        Gizmo.Visibility hook = Gizmo.Visibility.INVISIBLE;
                        if (version >= 2) {
                            hook = fileData.get() == 1 ? Gizmo.Visibility.VISIBLE : Gizmo.Visibility.INVISIBLE;
                        }
                        if (version >= 3) {
                            fileData.get();
                        }
                        if (version >= 4) {
                            floorCross = fileData.get() == 1 ? Gizmo.Visibility.VISIBLE : Gizmo.Visibility.INVISIBLE;
                        }

                        mapData.gizmo().gizmos().add(new Gizmo.ZipUp(name, start, axis,end,  type, direction, hook, floorCross, address, fileData.position() - address));
                    }
                }
                case "GizmoPickup" -> {
                    version = fileData.getInt();
                    gizCount = fileData.getInt();
                    if (version >= 3) {
                        fileData.getInt();
                    }
                    if (version >= 5) {
                        fileData.getFloat();
                        fileData.getFloat();
                    }
                    for (int i = 0; i < gizCount; i++) {
                        int address = fileData.position();

                        String pickupName = Util.getStringFromBuffer(fileData,8).trim();
                        Vector3f pos = new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());
                        byte type = fileData.get();

                        if(pickupName.isEmpty()) {
                            pickupName = "UNNAMED_PICKUP_" + i;
                        }

                        var realType = switch (type){
                            case 's' -> Gizmo.GizPickup.PickupType.SILVER_STUD;
                            case 'g' -> Gizmo.GizPickup.PickupType.GOLD_STUD;
                            case 'b' -> Gizmo.GizPickup.PickupType.BLUE_STUD;
                            case 'p' -> Gizmo.GizPickup.PickupType.PURPLE_STUD;
                            case 'u' -> Gizmo.GizPickup.PickupType.POWERUP;
                            case 'm' -> Gizmo.GizPickup.PickupType.MINIKIT;
                            case 'c' -> Gizmo.GizPickup.PickupType.CHALLENGE_MINIKIT;
                            case 'r' -> Gizmo.GizPickup.PickupType.RED_BRICK;
                            case 'h' -> Gizmo.GizPickup.PickupType.HEART;
                            case 't' -> Gizmo.GizPickup.PickupType.TORPEDO;
                            default -> Gizmo.GizPickup.PickupType.UNKNOWN;
                        };

                        //Both likely padding
                        if (version >= 2) {
                            fileData.get();
                        }
                        if (version >= 3) {
                            fileData.get();
                        }

                        mapData.gizmo().gizmos().add(new Gizmo.GizPickup(pickupName, pos, realType, address, fileData.position() - address));
                    }
                }
                case "Lever" -> {
                    version = fileData.getInt();
                    gizCount = fileData.getInt();
                    for (int i = 0; i < gizCount; i++) {
                        var start = fileData.position();
                        byte[] leverNameDat = new byte[16];
                        fileData.get(leverNameDat);
                        String leverName = new String(leverNameDat).trim();
                        Vector3f pos = new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());
                        float angle = Util.shortAngleToFloat(fileData.getShort());
                        byte colorByte = fileData.get();

                        var color = Gizmo.StudColor.getColor((char) colorByte);

                        Vector3f activationPos = new Vector3f();
                        float activationRange = 1;
                        float pullTime = 1;
                        Gizmo.Lever.PullBehavior behavior = Gizmo.Lever.PullBehavior.STAY_DOWN;
                        Gizmo.Lever.Visibility visibility = Gizmo.Lever.Visibility.VISIBLE;
                        Gizmo.Lever.Visibility floorVisibility = Gizmo.Lever.Visibility.VISIBLE;

                        if (version >= 2) {
                            int val = fileData.get();
                            if(val == 1) behavior = Gizmo.Lever.PullBehavior.MULTIPLE_PULLS;
                        }
                        if (version >= 3) {
                            pullTime = fileData.getFloat();
                        }
                        if (version >= 4) {
                            byte val = fileData.get();
                            if(val == 1) visibility = Gizmo.Lever.Visibility.INVISIBLE;
                        }
                        if (version >= 5) {
                            var activationOffset  = new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());
                            activationPos = Quaternionf.createYXZ(new Vector3f(0, angle, 0)).transform(activationOffset).add(pos);
                            activationRange = fileData.getFloat();
                        }
                        if (version >= 6) {
                            floorVisibility = fileData.get() == 0 ? Gizmo.Visibility.VISIBLE : Gizmo.Visibility.INVISIBLE;
                        }

                        mapData.gizmo().gizmos().add(new Gizmo.Lever(leverName, pos, angle, visibility, color, behavior, pullTime, floorVisibility, activationPos, activationRange, start, fileData.position() - start));
                    }
                }
                case "GizTimer" ->{
                    version = fileData.getInt();
                    gizCount = fileData.getInt();
                    for (int i = 0; i < gizCount; i++) {
                        int timeNameLength = fileData.getInt();
                        byte[] timeNameData = new byte[timeNameLength];
                        fileData.get(timeNameData);
                        fileData.getFloat();
                        fileData.getShort();
                        Vector3f pos = new Vector3f(fileData.getFloat(),fileData.getFloat(),fileData.getFloat());
                    }
                }
                case "PushBlocks" ->{
                    version = fileData.getInt();
                    gizCount = fileData.getInt();
                    for (int i = 0; i < gizCount; i++) {
                        int address = fileData.position();

                        int nameLen = Byte.toUnsignedInt(fileData.get());
                        String name = Util.getStringFromBuffer(fileData,nameLen);
                        fileData.getFloat();
                        fileData.get();
                        fileData.get();
                        fileData.get();
                        fileData.get();
                        if(version >= 3){
                            fileData.get();
                            fileData.get();
                        }
                        if(version >= 5){
                            fileData.get();
                            fileData.get();
                        }
                        //Push Block Links
                        var connectedSpecialObjects = new ArrayList<SpecialObject>();
                        if(version > 2){
                            int numLinkObjects = Byte.toUnsignedInt(fileData.get());
                            for (int j = 0; j < numLinkObjects; j++) {
                                int linkNameLen = Byte.toUnsignedInt(fileData.get());
                                mapData.getSpecialObjectByName(
                                        Util.getStringFromBuffer(fileData,linkNameLen))
                                        .ifPresent(connectedSpecialObjects::add);
                            }
                        }
                        mapData.gizmo().gizmos().add(
                                new Gizmo.PushBlock(mapData.getSpecialObjectByName(name).get(), connectedSpecialObjects, address, fileData.position() - address));
                    }
                }
                case "GizBuildit"->{
                    version = Byte.toUnsignedInt(fileData.get());
                    int numBuildIt = Short.toUnsignedInt(fileData.getShort());
                    for (int i = 0; i < numBuildIt; i++) {
                        String name = Util.getStringFromBuffer(fileData, 16);
                        Vector3f pos = new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());


                        int subVersion = Byte.toUnsignedInt(fileData.get());
                        if (subVersion != 0) {
                            int c = Byte.toUnsignedInt(fileData.get());
                            for (int j = 0; j < c; j++) {
                                int uVar1 = Byte.toUnsignedInt(fileData.get());
                                if (uVar1 != 0) {
                                    String unk = Util.getStringFromBuffer(fileData, uVar1);
                                }
                                fileData.getFloat();
                                fileData.getFloat();
                                if (subVersion < 2) {

                                } else {
                                    fileData.getInt();
                                }
                            }
                        }


                        fileData.getFloat();
                        if (version < 7) {
                            //padding
                            fileData.getFloat();
                        }
                        short numStuds = fileData.getShort();
                        fileData.getShort();
                        fileData.get();
                        fileData.get();
                        if (5 < version) {
                            fileData.getFloat();
                        }
                        if (6 < version) {
                            if (version < 8) {
                                fileData.getShort();
                            } else {
                                int len3 = Byte.toUnsignedInt(fileData.get());
                                if (len3 != 0) {
                                    Util.getStringFromBuffer(fileData, len3);
                                }
                            }
                            fileData.getShort();
                            fileData.getShort();
                            new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());
                        }
                        if (version < 9) {

                        } else {
                            fileData.getFloat();
                        }
                        if (version < 4) {
                            fileData.get();
                            if (1 < version) {
                                fileData.get();
                                if (2 < version) {
                                    int what = Byte.toUnsignedInt(fileData.get());
                                }
                            }
                        } else {
                            fileData.getShort();
                        }
                        if (4 < version) {
                            fileData.getShort();
                            int len3 = Byte.toUnsignedInt(fileData.get());
                            if (len3 != 0) {
                               Util.getStringFromBuffer(fileData, len3);
                            }
                        }
                    }

                }
                case "HatMachine"->{
                    version = fileData.getInt();
                    gizCount = fileData.getInt();
                    for (int i = 0; i < gizCount; i++) {
                        int address = fileData.position();

                        int nameLength = fileData.getInt();
                        String name = Util.getStringFromBuffer(fileData, nameLength).trim();
                        var pos = new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());
                        float angle = Util.shortAngleToFloat(fileData.getShort());
                        var hB = fileData.get();
                        var hat = switch (hB){
                            case 0 -> Gizmo.HatMachine.HatType.RANDOM;
                            case 1 -> Gizmo.HatMachine.HatType.LEIA;
                            case 2 -> Gizmo.HatMachine.HatType.FEDORA;
                            case 3 -> Gizmo.HatMachine.HatType.TOP_HAT;
                            case 4 -> Gizmo.HatMachine.HatType.BASEBALL_CAP;
                            case 5 -> Gizmo.HatMachine.HatType.STORMTROOPER;
                            case 6 -> Gizmo.HatMachine.HatType.BOUNTY_HUNTER;
                            case 7 -> Gizmo.HatMachine.HatType.DROID_PANEL;
                            default -> Gizmo.HatMachine.HatType.STORMTROOPER;
                        };

                        Vector3f activationPos = new Vector3f();
                        float activationRange = 0;
                        Gizmo.Visibility floorVisibility = Gizmo.Visibility.VISIBLE;
                        var color = Gizmo.Lever.StudColor.NONE;
                        if (version >= 3) {
                            byte colorByte = fileData.get();
                            color = Gizmo.StudColor.getColor((char) colorByte);
                        }
                        if(version >= 4){
                            var activationOffset  = new Vector3f(fileData.getFloat(), fileData.getFloat(), fileData.getFloat());
                            activationPos = Quaternionf.createYXZ(new Vector3f(0, angle, 0)).transform(activationOffset).add(pos);
                            activationRange = fileData.getFloat();
                        }
                        if(version >= 5){
                            floorVisibility = fileData.get() == 0 ? Gizmo.Visibility.VISIBLE : Gizmo.Visibility.INVISIBLE;
                        }

                        mapData.gizmo().gizmos().add(
                                new Gizmo.HatMachine(nameLength, name, pos, angle, hat,color, floorVisibility, activationPos, activationRange, address, fileData.position() - address));
                    }
                }
                case "Torp Machine" ->{
                    version = fileData.getInt();
                    gizCount = fileData.getInt();
                    if (version >= 3) {
                        fileData.getFloat();
                    }

                    for (int i = 0; i < gizCount; i++) {
                        int nameLen = fileData.getInt();

                        var name = Util.getStringFromBuffer(fileData,nameLen);
                        var pos = new Vector3f(fileData.getFloat(),fileData.getFloat(),fileData.getFloat());
                        var angle = fileData.getShort();
                        if (version >= 2) {
                            fileData.get();
                        }
                    }
                }
                case "Tube"->{
                    version = fileData.getInt();
                    gizCount = fileData.getInt();
                    for (int i = 0; i < gizCount; i++) {
                        int address = fileData.position();
                        var name = Util.getStringFromBuffer(fileData,0x10);
                        var pos = new Vector3f(fileData.getFloat(),fileData.getFloat(),fileData.getFloat());
                        var height = fileData.getFloat();
                        var radius = fileData.getFloat();
                        int s = 0;
                        if(version >= 2){
                            s = fileData.get();
                        }

                        String specialObject="";
                        if(version >= 3){
                            var specObjName = Byte.toUnsignedInt(fileData.get());
                            if(specObjName != 0){
                                specialObject = Util.getStringFromBuffer(fileData, specObjName);
                            }
                        }
                        mapData.gizmo().gizmos().add(new Gizmo.Tube(name,pos,radius,height,specialObject,address, fileData.position() - address));
                    }
                }
                default -> fileData.get(new byte[sectionLength]);
            }

            var sectionEnd = fileData.position();

            mapData.gizmo().gizmoVersions().put(typeName, new GizWriter.GizmoTypeData(version, sectionStart, sectionEnd, gizCount));

            if(typeName.equals("Techno")){
                break;
            }
        }
    }
}
