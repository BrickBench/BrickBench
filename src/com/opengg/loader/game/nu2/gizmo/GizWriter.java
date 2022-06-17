package com.opengg.loader.game.nu2.gizmo;

import com.opengg.core.console.GGConsole;
import com.opengg.core.math.Vector3f;
import com.opengg.loader.BrickBench;
import com.opengg.loader.SwingUtil;
import com.opengg.loader.Util;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.loading.MapLoader;
import com.opengg.loader.loading.MapWriter;
import com.opengg.loader.game.nu2.gizmo.Gizmo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GizWriter {
    private static void addGizmoSlot(String gizmoName){
        if (gizmoName.equals("GizmoPickup")) {
            
        } else {
            var buffer = ByteBuffer.allocate(4 + gizmoName.length() + 4 + 4 + 4).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(gizmoName.length())
                .put(Util.getStringBytes(gizmoName, gizmoName.length()))
                .putInt(8)
                .putInt(1000)
                .putInt(0).rewind();
            
            MapWriter.addSpaceAtLocation(MapWriter.WritableObject.GIZMO, 4, buffer.capacity());
            MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, 4, buffer);
        }
    }

    public static String addNewGizmo(String type){
        String gizmoName = "";
        var gizData = ((NU2MapData) EditorState.getActiveMap().levelData()).gizmo();
        GizmoTypeData gizmoData = gizData.gizmoVersions().get(type);

        if (gizmoData == null) {
            try {
                GGConsole.log("Gizmo type " + type + " does not exist, adding slot");
                addGizmoSlot(type);
                MapLoader.reloadIndividualFile("giz");
                gizmoData = gizData.gizmoVersions().get(type);

            } catch (IOException e) {
                SwingUtil.showErrorAlert("Failed while adding slot for " + type, e);
                return null;
            }
        }

        ByteBuffer newGizmo = null;
        int newGizLength = 0;
        int gizmoCountPosition = 0;
        if(type.equals("Tube")){
            gizmoCountPosition = gizmoData.start + 4;
            gizmoName = "Tube_" + gizmoData.gizCount;
            newGizLength = switch (gizmoData.version){
                case 0, 1 -> 0x10 + 12 + 4 + 4;
                case 2 -> 0x10 + 12 + 4 + 4 + 1;
                default -> 0x10 + 12 + 4 + 4 + 1 + 1;
            };
            newGizmo = ByteBuffer.allocate(newGizLength).order(ByteOrder.LITTLE_ENDIAN)
                .put(Util.getStringBytes(gizmoName, 0x10))
                .put(BrickBench.CURRENT.ingamePosition.toLittleEndianByteBuffer())
                .putFloat(1)
                .putFloat(0.5f).rewind();
        }else if(type.equals("GizmoPickup")){
            newGizLength = switch (gizmoData.version){
                case 0, 1 -> 8 + 12 + 1;
                case 2 -> 8 + 12 + 1 + 1;
                default -> 8 + 12 + 1 + 1 + 1;
            };

            gizmoName = "Pup_" + gizmoData.gizCount;
            newGizmo = ByteBuffer.allocate(newGizLength).order(ByteOrder.LITTLE_ENDIAN).put(Util.getStringBytes(gizmoName, 8))
                   .put(BrickBench.CURRENT.ingamePosition.toLittleEndianByteBuffer()).put((byte)115).rewind();

            gizmoCountPosition = gizmoData.start + 4;
        }else if(type.equals("ZipUp")){
            var max = 16+12+12+12+2+2+1+1+1+1+1+1;
            newGizLength = switch (gizmoData.version){
                case 0, 1 -> 16+12+12+12+2+2+1+1+1;
                case 2 -> 16+12+12+12+2+2+1+1+1+1;
                case 3 -> 16+12+12+12+2+2+1+1+1+1+1;
                default -> max;
            };
            gizmoName = "ZipUp_" + gizmoData.gizCount;
            newGizmo = ByteBuffer.allocate(max).order(ByteOrder.LITTLE_ENDIAN)
                    .put(Util.getStringBytes(gizmoName, 16))
                    .put(BrickBench.CURRENT.ingamePosition.toLittleEndianByteBuffer())
                    .put(BrickBench.CURRENT.ingamePosition.add(new Vector3f(0,1,0)).toLittleEndianByteBuffer())
                    .put(BrickBench.CURRENT.ingamePosition.add(new Vector3f(0,0,1)).toLittleEndianByteBuffer())
                    .putShort((short) 0).putShort((short) 0).put(new byte[]{0,1,0,1,0,1}).rewind()
                    .slice(0, newGizLength);;
            gizmoCountPosition = gizmoData.start + 4;
        }else if(type.equals("Lever")){
            var max = 16+12+2+1+1+4+1+16+1;
            newGizLength = switch (gizmoData.version){
                case 0, 1 -> 16+12+2+1;
                case 2 -> 16+12+2+1+1;
                case 3 -> 16+12+2+1+1+4;
                case 4 -> 16+12+2+1+1+4+1;
                case 5 -> 16+12+2+1+1+4+1+16;
                default -> max;
            };
            gizmoName = "Lever_" + gizmoData.gizCount;

            newGizmo = ByteBuffer.allocate(max).order(ByteOrder.LITTLE_ENDIAN)
                    .put(Util.getStringBytes(gizmoName, 16))
                    .put(BrickBench.CURRENT.ingamePosition.toLittleEndianByteBuffer())
                    .putShort((short)0)
                    .put((byte)0x79)
                    .put((byte)0)
                    .putFloat(0.79f)
                    .put((byte)0)
                    .put(new Vector3f(0, -0.2f, -0.3f).toLittleEndianByteBuffer())
                    .putFloat(1)
                    .put((byte)1).rewind().slice(0, newGizLength);

            gizmoCountPosition = gizmoData.start + 4;
        }else if(type.equals("HatMachine")){
            var max = 4+8+12+2+1+1+16+1;
            newGizLength = switch (gizmoData.version){
                case 0, 1 -> 4+8+12+2+1;
                case 3 -> 4+8+12+2+1+1;
                case 4 -> 4+8+12+2+1+1+16;
                default -> max;
            };
            gizmoName = "Hat_" + gizmoData.gizCount;
            newGizmo = ByteBuffer.allocate(max).order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(8)
                    .put(Util.getStringBytes(gizmoName, 8))
                    .put(BrickBench.CURRENT.ingamePosition.toLittleEndianByteBuffer())
                    .putShort((short)0)
                    .put((byte)5)
                    .put((byte)0)
                    .put(new Vector3f(0, -0.2f, -0.3f).toLittleEndianByteBuffer())
                    .putFloat(1)
                    .put((byte) 0).rewind().slice(0, newGizLength);
            gizmoCountPosition = gizmoData.start + 4;
        }else if(type.equals("Panel")){
            var max = 4+10+12+2+1+1+16+1+2+1+1;
            newGizLength = switch (gizmoData.version){
                case 0, 1 -> 4+10+12+2+1;
                case 3 -> 4+10+12+2+1+1;
                case 4 -> 4+10+12+2+1+1+16;
                case 5 -> 4+10+12+2+1+1+16+1;
                case 6 -> 4+10+12+2+1+1+16+1+2;
                case 7 -> 4+10+12+2+1+1+16+1+2+1;
                default -> max;
            };
            gizmoName = "Panel_" + gizmoData.gizCount;
            newGizmo = ByteBuffer.allocate(max).order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(10)
                    .put(Util.getStringBytes(gizmoName, 10))
                    .put(BrickBench.CURRENT.ingamePosition.toLittleEndianByteBuffer())
                    .putShort((short)0)
                    .put((byte)0)
                    .put((byte)0)
                    .put(new Vector3f(0, -0.2f, -0.4f).toLittleEndianByteBuffer())
                    .putFloat(1)
                    .put((byte)0)
                    .put((byte)0)
                    .put((byte)0).rewind().slice(0, newGizLength);;
            gizmoCountPosition = gizmoData.start + 4;
        }


        MapWriter.addSpaceAtLocation(MapWriter.WritableObject.GIZMO, gizmoData.end, newGizLength);
        MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, gizmoData.end, newGizmo);
        MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, gizmoCountPosition, Util.littleEndian(gizmoData.gizCount + 1));

        return gizmoName;
    }

    public static void removeGizmo(Gizmo gizmo){
        var name = switch (gizmo) {
            case Gizmo.GizPickup g -> "GizmoPickup";
            case Gizmo.ZipUp g -> "ZipUp";
            case Gizmo.Lever g -> "Lever";
            case Gizmo.HatMachine g -> "HatMachine";
            case Gizmo.GizPanel g -> "Panel";
            case Gizmo.Tube g -> "Tube";
            case null, default -> "";
        };

        var gizmoData = ((NU2MapData)EditorState.getActiveMap().levelData()).gizmo().gizmoVersions().get(name);
        MapWriter.removeSpaceAtLocation(MapWriter.WritableObject.GIZMO, gizmo.fileAddress(), gizmo.fileLength());
        MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, gizmoData.start + 4, Util.littleEndian(gizmoData.gizCount - 1));
  
    }

    public record GizmoTypeData(int version, int start, int end, int gizCount){}
}
