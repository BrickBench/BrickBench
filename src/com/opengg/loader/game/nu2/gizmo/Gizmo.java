package com.opengg.loader.game.nu2.gizmo;

import com.opengg.core.math.Quaternionf;
import com.opengg.core.math.Vector3f;
import com.opengg.core.physics.collision.colliders.BoundingBox;
import com.opengg.loader.game.nu2.scene.SpecialObject;
import com.opengg.loader.MapEntity;
import com.opengg.loader.loading.MapWriter;
import com.opengg.loader.Util;
import com.opengg.loader.components.Selectable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public sealed interface Gizmo extends MapEntity<Gizmo>, Selectable  {
    int fileAddress();
    int fileLength();

    @Override
    default Map<String, Runnable> getButtonActions() {
        return Util.createOrderedMapFrom(Map.entry("Remove", () -> GizWriter.removeGizmo(this)));
    }

    record Lever(String name, Vector3f pos, float angle, Gizmo.Visibility visibility, Gizmo.StudColor studColor, PullBehavior behavior, float pullTime, Visibility floorCircleVisiblity, Vector3f activationPos, float activationRange, int fileAddress, int fileLength) implements Gizmo{
        @Override
        public BoundingBox getBoundingBox() {
            return new BoundingBox(pos.subtract(0.2f), pos.add(0.2f));
        }

        @Override
        public String path() {
            return "Gizmo/Levers/" + name;
        }

        @Override
        public List<Property> properties() {
            return List.of(
                    new StringProperty("Name",name(), true, 16),
                    new VectorProperty("Position",pos(), true,true),
                    new FloatProperty("Angle", angle, true),

                    new VectorProperty("Activation position",activationPos, true,activationRange != 0),
                    new FloatProperty("Activation range",activationRange, activationRange != 0),

                    new FloatProperty("Reset time",pullTime, true),
                    new EnumProperty("Reset behavior",behavior, true),
                    new EnumProperty("Stud color",studColor, true),
                    new EnumProperty("Body visibility",visibility, true),
                    new EnumProperty("Floor cross visibility",floorCircleVisiblity, true)
            );
        }

        @Override
        public void applyPropertyEdit(String propName, Property newValue) {
            switch (newValue) {
                case VectorProperty nVec && propName.equals("Position") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 16, nVec.value().toLittleEndianByteBuffer());
                case StringProperty sProp && propName.equals("Name") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress, Util.getStringBytes(sProp.stringValue(), 16));
                case FloatProperty fProp && propName.equals("Reset time") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 16 + 12 + 2 + 1 + 1,
                        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(fProp.value()));
                case EnumProperty enumProperty && propName.equals("Reset behavior") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 16 + 12 + 2 + 1,
                        new byte[]{(byte) (enumProperty.value() == PullBehavior.MULTIPLE_PULLS ? 1 : 0)});
                case EnumProperty enumProperty && propName.equals("Body visibility") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 16 + 12 + 2 + 1 + 1 + 4,
                        new byte[]{(byte) (enumProperty.value() == Visibility.INVISIBLE ? 1 : 0)});
                case EnumProperty enumProperty && propName.equals("Floor cross visibility") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 16 + 12 + 2 + 1 + 1 + 4 + 1 + 12 + 4,
                        new byte[]{(byte) (enumProperty.value() == Visibility.VISIBLE ? 0 : 1)});
                case EnumProperty enumProperty && propName.equals("Stud color") -> {
                    var colorChar = switch ((Gizmo.StudColor) enumProperty.value()) {
                        case RED -> 'r';
                        case BLUE -> 'b';
                        case ORANGE -> 'o';
                        case YELLOW -> 'y';
                        case GREEN -> 'g';
                        case LIGHT_BLUE -> 'u';
                        case PURPLE -> 'p';
                        case NONE -> '\0';
                    };
                    MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 16 + 12 + 2, new byte[]{(byte) colorChar});
                }
                case FloatProperty iProp && propName.equals("Angle") -> {
                    short backAngle = Util.floatToShortAngle(iProp.value());
                    MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 16 + 12,
                            ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(backAngle).array());
                }
                case VectorProperty nVec && propName.equals("Activation position") -> {
                    var newPosition = Quaternionf.createYXZ(new Vector3f(0, -angle, 0)).transform(nVec.value().subtract(pos));
                    MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 16 + 12 + 2 + 1 + 1 + 4 + 1, newPosition.toLittleEndianByteBuffer());
                }
                case FloatProperty nF && propName.equals("Activation range") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 16 + 12 + 2 + 1 + 1 + 4 + 1 + 12,
                     Util.littleEndian(nF.value()));
                case null, default -> {
                }
            }
        }

        public enum PullBehavior{
            STAY_DOWN, MULTIPLE_PULLS
        }

    }

    record GizPickup(String name, Vector3f pos, PickupType type, int fileAddress, int fileLength, int index) implements Gizmo{
        @Override
        public BoundingBox getBoundingBox() {
            return new BoundingBox(pos.subtract(0.1f), pos.add(0.1f));
        }

        public enum PickupType{
            SILVER_STUD("silvercoin.png", "silver_pan_coin1"),
            GOLD_STUD("goldcoin.png", "gold_pan_coin1"),
            BLUE_STUD("bluecoin.png", "blue_pan_coin1"),
            PURPLE_STUD("purplecoin.png", "purple_pan_coin1"),
            RED_BRICK("redsquares.png", "Red_brick"),
            MINIKIT("greypill.png", "mini_kit_pickup"),
            CHALLENGE_MINIKIT("bluepill.png", "Char_pickup"),
            POWERUP("swirl.png", "plop"),
            HEART("heart.png", "heart"),
            TORPEDO("torp.png", ""),
            UNKNOWN("silvercoin.png", "");

            public final String path;
            public final String specialObject;

            PickupType(String file, String specialObject){
                this.path = file;
                this.specialObject = specialObject;
            }

            char getCode() {
                return switch (this) {
                    case SILVER_STUD -> 's';
                    case GOLD_STUD -> 'g';
                    case BLUE_STUD -> 'b';
                    case PURPLE_STUD -> 'p';
                    case RED_BRICK -> 'r';
                    case MINIKIT -> 'm';
                    case CHALLENGE_MINIKIT -> 'c';
                    case POWERUP -> 'u';
                    case HEART -> 'h';
                    case TORPEDO -> 't';
                    case UNKNOWN -> '\s';
                };
            }

            static PickupType getType(char code) {
                return switch (code){
                    case 's' -> Gizmo.GizPickup.PickupType.SILVER_STUD;
                    case 'g' -> Gizmo.GizPickup.PickupType.GOLD_STUD;
                    case 'b' -> Gizmo.GizPickup.PickupType.BLUE_STUD;
                    case 'p' -> Gizmo.GizPickup.PickupType.PURPLE_STUD;
                    case 'r' -> Gizmo.GizPickup.PickupType.RED_BRICK;
                    case 'm' -> Gizmo.GizPickup.PickupType.MINIKIT;
                    case 'c' -> Gizmo.GizPickup.PickupType.CHALLENGE_MINIKIT;
                    case 'u' -> Gizmo.GizPickup.PickupType.POWERUP;
                    case 'h' -> Gizmo.GizPickup.PickupType.HEART;
                    case 't' -> Gizmo.GizPickup.PickupType.TORPEDO;
                    default ->  Gizmo.GizPickup.PickupType.UNKNOWN;
                };
            }
        }

        @Override
        public String path() {
            return "Gizmo/Pickups/" + name + UNIQUE_CHAR + index();
        }

        @Override
        public List<Property> properties(){
            return List.of(
                    new StringProperty("Name", name(), true, 8),
                    new VectorProperty("Position", pos(), true,true),
                    new EnumProperty("Pickup type", type, true)
            );
        }

        @Override
        public void applyPropertyEdit(String propName, Property newValue) {
            switch (newValue) {
                case VectorProperty nVec && propName.equals("Position") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 8, nVec.value().toLittleEndianByteBuffer());
                case StringProperty sProp && propName.equals("Name") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress, Util.getStringBytes(sProp.stringValue(), 8));
                case EnumProperty eProp && propName.equals("Pickup type") -> {
                    var colorChar = ((PickupType) eProp.value()).getCode();
                    MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 12 + 8, new byte[]{(byte) colorChar});
                }
                case null, default -> {
                }
            }
        }
    }

    record ZipUp(String name, Vector3f start, Vector3f axis, Vector3f end, ZipType zipType, Direction direction, Visibility hookVisibility, Visibility crossVisibility, int fileAddress, int fileLength) implements Gizmo{
        @Override
        public Vector3f pos() {
            return axis;
        }

        @Override
        public String path() {
            return "Gizmo/ZipUps/" + name;
        }

        @Override
        public void applyPropertyEdit(String propName, Property newValue) {
            switch (newValue) {
                case StringProperty sProp && propName.equals("Name") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress, Util.getStringBytes(sProp.value(), 16));
                case VectorProperty nVec && propName.equals("Start") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 16, nVec.value().toLittleEndianByteBuffer());
                case VectorProperty nVec && propName.equals("Swing axis") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 16 + 12, nVec.value().toLittleEndianByteBuffer());
                case VectorProperty nVec && propName.equals("End") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 16 + 12 + 12, nVec.value().toLittleEndianByteBuffer());
                case EnumProperty nVec && propName.equals("Zip type") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 16 + 12 + 12 + 12 + 2 + 2, new byte[]{(byte) (nVec.value() == ZipType.SWING ? 1 : 0)});
                case EnumProperty nVec && propName.equals("Zip direction") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 16 + 12 + 12 + 12 + 2 + 2 + 1 + 1, new byte[]{(byte) (nVec.value() == Direction.TWO_WAY ? 1 : 0)});
                case EnumProperty nVec && propName.equals("Hook visibility") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 16 + 12 + 12 + 12 + 2 + 2 + 1 + 1 + 1, new byte[]{(byte) (nVec.value() == Option.YES ? 1 : 0)});
                case EnumProperty nVec && propName.equals("Floor cross visibility") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 16 + 12 + 12 + 12 + 2 + 2 + 1 + 1 + 1 + 1 + 1, new byte[]{(byte) (nVec.value() == Option.YES ? 1 : 0)});
                case null, default -> {
                }
            }
        }

        @Override
        public List<Property> properties() {
            return List.of(
                    new StringProperty("Name",name(), true, 16),
                    new VectorProperty("Start",start(), true,true),
                    new VectorProperty("Swing axis",pos(), true, true),
                    new VectorProperty("End",end(), true, true),

                    new EnumProperty("Zip type",zipType, true),
                    new EnumProperty("Zip direction",direction, true),
                    new EnumProperty("Hook visibility",hookVisibility, true),
                    new EnumProperty("Floor cross visibility",crossVisibility, true)
            );
        }

        @Override
        public BoundingBox getBoundingBox() {
            return new BoundingBox(axis.subtract(0.15f), axis.add(0.15f));
        }

        public enum ZipType{SWING, ZIP}
        public enum Direction{ONE_WAY, TWO_WAY}
    }

    record GizPanel(int nameLength, String name, Vector3f position, float angle, Vector3f activationPos, float activationRange, PanelType panelType, Visibility frontVisibility,
                    Visibility floorCrossVisibility, Option alternativeFace, Option alternativeColor, int fileAddress, int fileLength) implements Gizmo{

        @Override
        public Vector3f pos() {
            return position;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String path() {
            return "Gizmo/Panels/" + name;
        }

        @Override
        public List<Property> properties() {
            List<Property> source = List.of(
                    new StringProperty("Name",name(), true, 32),
                    new VectorProperty("Position",pos(),true, true),
                    new FloatProperty("Angle",angle, true),

                    new VectorProperty("Activation position",activationPos, true,activationRange != 0),
                    new FloatProperty("Activation range",activationRange, activationRange != 0),

                    new EnumProperty("Panel type",panelType, true),
                    new EnumProperty("Face visibility",frontVisibility, true),
                    new EnumProperty("Floor cross visibility",floorCrossVisibility, true));

            if(panelType == PanelType.ASTROMECH || panelType == PanelType.PROTOCOL_DROID){
                source = new ArrayList<>(source);
                source.addAll(List.of(
                        new EnumProperty("Use alternative droid color",alternativeFace, true),
                        new EnumProperty("Use alternative body color",alternativeColor, true))
                );
            }
            return source;
        }

        @Override
        public void applyPropertyEdit(String propName, Property newValue) {
            switch (newValue) {
                case VectorProperty vProp && propName.equals("Position") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4 + nameLength,
                        vProp.value().toLittleEndianByteBuffer());
                case FloatProperty iProp && propName.equals("Angle") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4 + nameLength + 12, Util.littleEndian(Util.floatToShortAngle(iProp.value())));
                case EnumProperty eProp && propName.equals("Panel type") -> {
                    byte byteType = (byte) switch ((PanelType) eProp.value()) {
                        case ASTROMECH -> 0;
                        case PROTOCOL_DROID -> 1;
                        case BOUNTY_HUNTER -> 2;
                        case STORMTROOPER -> 3;
                        case UNDEFINED -> 4;
                    };
                    MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4 + nameLength + 12 + 2, new byte[]{byteType});
                }
                case VectorProperty nVec && propName.equals("Activation position") ->
                        MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4 + nameLength + 12 + 2 + 2, Quaternionf.createYXZ(new Vector3f(0, -angle, 0)).transform(nVec.value().subtract(position)).toLittleEndianByteBuffer());
                case FloatProperty nF && propName.equals("Activation range") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4 + nameLength + 12 + 2 + 2 + 12, Util.littleEndian(nF.value()));
                case EnumProperty eProp && propName.equals("Face visibility") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4 + nameLength + 12 + 2 + 1, new byte[]{(byte) (eProp.value() == Visibility.VISIBLE ? 0 : 1)});
                case EnumProperty eProp && propName.equals("Floor cross visibility") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4 + nameLength + 12 + 2 + 1 + 1 + 16, new byte[]{(byte) (eProp.value() == Visibility.VISIBLE ? 0 : 1)});
                case EnumProperty eProp && propName.equals("Use alternative droid color") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4 + nameLength + 12 + 2 + 1 + 1 + 16 + 1, new byte[]{(byte) (eProp.value() == Option.YES ? 1 : 0)});
                case EnumProperty eProp && propName.equals("Use alternative body color") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4 + nameLength + 12 + 2 + 1 + 1 + 16 + 1 + 1, new byte[]{(byte) (eProp.value() == Option.YES ? 1 : 0)});
                case StringProperty sp && propName.equals("Name") -> {
                    var nameBuf = sp.stringValue().getBytes(StandardCharsets.UTF_8);
                    MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress, Util.littleEndian(nameBuf.length));
                    MapWriter.removeSpaceAtLocation(MapWriter.WritableObject.GIZMO, fileAddress + 4, nameLength);
                    MapWriter.addSpaceAtLocation(MapWriter.WritableObject.GIZMO, fileAddress + 4, nameBuf.length);
                    MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4, nameBuf);
                }
                case null, default -> {
                }
            }
        }

        @Override
        public BoundingBox getBoundingBox() {
            return new BoundingBox(position.subtract(0.2f), position.add(0.2f));
        }

        public enum PanelType{
            PROTOCOL_DROID, ASTROMECH, BOUNTY_HUNTER, STORMTROOPER, UNDEFINED
        }
    }

    record PushBlock(SpecialObject specialObject, List<SpecialObject> linkedObjects, int fileAddress, int fileLength) implements Gizmo{
        @Override
        public Vector3f pos() {
            return linkedObjects.stream().map(SpecialObject::pos).findFirst().orElse(new Vector3f());
        }

        @Override
        public String name() {
            return specialObject.name();
        }

        @Override
        public String path() {
            return "Gizmo/PushBlocks/" + name();
        }

        @Override
        public List<Property> properties() {
            return List.of(
                    new EditorEntityProperty("Linked special object", specialObject, false, false, "Render/SpecialObjects/"),
                    new VectorProperty("Position",pos(), false, false),
                    new ListProperty("Secondary special objects",
                                    IntStream.range(0, this.linkedObjects().size()).mapToObj(p ->
                                           new EditorEntityProperty("Object " + p, linkedObjects.get(p), false, false,
                                                   "Render/SpecialObjects/"))
                                           .collect(Collectors.toList()), true)
            );
        }

        @Override
        public BoundingBox getBoundingBox() {
            return null;
        }
    }
    record Tube(String name,Vector3f pos, float radius, float height, String specialObject, int fileAddress,int fileLength) implements Gizmo{

        @Override
        public String path() {
            return "Gizmo/Tubes/" + name();
        }

        @Override
        public List<Property> properties() {
            return List.of(
                    new StringProperty("Name", name(), true, 16),
                    new VectorProperty("Position", pos(), true, true),
                    new FloatProperty("Height", height(), true),
                    new FloatProperty("Radius", radius(), true));
        }

        @Override
        public void applyPropertyEdit(String propName, Property newValue) {
            switch (newValue) {
                case StringProperty sProp && propName.equals("Name") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress, Util.getStringBytes(sProp.value(), 0x10));
                case VectorProperty nVec && propName.equals("Position") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 0x10, nVec.value().toLittleEndianByteBuffer());
                case FloatProperty vProp && propName.equals("Height") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 0x10 + 12, Util.littleEndian(vProp.value()));
                case FloatProperty vProp && propName.equals("Radius") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 0x10 + 12 + 4, Util.littleEndian(vProp.value()));
                case null, default -> {}
            }
        }

        @Override
        public BoundingBox getBoundingBox() {
            return new BoundingBox(pos.subtract(new Vector3f(radius/2, 0, radius/2)), pos.add(new Vector3f(radius/2, height, radius/2)));
        }
    }
    record HatMachine(int nameLength, String name, Vector3f pos, float angle, HatType type, StudColor studColor, Visibility floorVisibility, Vector3f activationPos, float activationRange, int fileAddress, int fileLength) implements Gizmo{

        @Override
        public BoundingBox getBoundingBox() {
            return new BoundingBox(pos.subtract(0.2f), pos.add(0.2f));
        }

        @Override
        public void applyPropertyEdit(String propName, Property newValue) {
            switch (newValue) {
                case VectorProperty nVec && propName.equals("Position") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4 + nameLength, nVec.value().toLittleEndianByteBuffer());
                case StringProperty sProp && propName.equals("Name") -> {
                    var nameBuf = sProp.stringValue().getBytes(StandardCharsets.UTF_8);
                    MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress, Util.littleEndian(nameBuf.length));
                    MapWriter.removeSpaceAtLocation(MapWriter.WritableObject.GIZMO, fileAddress + 4, nameLength);
                    MapWriter.addSpaceAtLocation(MapWriter.WritableObject.GIZMO, fileAddress + 4, nameBuf.length);
                    MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4, nameBuf);
                }
                case FloatProperty iProp && propName.equals("Angle") -> {
                    short backAngle = Util.floatToShortAngle(iProp.value());
                    MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4 + nameLength + 12, Util.littleEndian(backAngle));
                }
                case VectorProperty nVec && propName.equals("Activation position") -> {
                    var newPosition = Quaternionf.createYXZ(new Vector3f(0, -angle, 0)).transform(nVec.value().subtract(pos));
                    MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4 + nameLength + 12 + 2 + 1 + 1, newPosition.toLittleEndianByteBuffer());
                }
                case FloatProperty nF && propName.equals("Activation range") -> MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4 + nameLength + 12 + 2 + 1 + 1 + 12, Util.littleEndian(nF.value()));
                case EnumProperty enumProperty && propName.equals("Floor cross visibility") ->
                        MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4 + nameLength + 12 + 2 + 1 + 1 + 1 + 12 + 4, new byte[]{(byte) (enumProperty.value() == Visibility.VISIBLE ? 0 : 1)});
                case EnumProperty eProp && propName.equals("Hat type") -> {
                    byte byteType = (byte) switch ((HatType) eProp.value()) {
                        case LEIA -> 1;
                        case FEDORA -> 2;
                        case TOP_HAT -> 3;
                        case BASEBALL_CAP -> 4;
                        case RANDOM -> 0;
                        case STORMTROOPER -> 5;
                        case BOUNTY_HUNTER -> 6;
                        case DROID_PANEL -> 7;
                    };
                    MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4 + nameLength + 12 + 2, new byte[]{byteType});
                }
                case EnumProperty enumProperty && propName.equals("Stud color") -> {
                    var colorChar = ((StudColor) enumProperty.value()).getCode();
                    MapWriter.applyPatch(MapWriter.WritableObject.GIZMO, fileAddress + 4 + nameLength + 12 + 2 + 1, new byte[]{(byte) colorChar});
                }
                case null, default -> {
                }
            }
        }

        @Override
        public String path() {
            return "Gizmo/HatMachines/" + name;
        }

        @Override
        public List<Property> properties() {
            return List.of(
                    new StringProperty("Name", name(), true, 16),
                    new VectorProperty("Position", pos(), true,true),
                    new FloatProperty("Angle", angle, true),
                    new VectorProperty("Activation position", activationPos, true,activationRange != 0),
                    new FloatProperty("Activation range", activationRange, activationRange != 0),
                    new EnumProperty("Hat type", type, true),
                    new EnumProperty("Stud color", studColor, true),
                    new EnumProperty("Floor cross visibility", floorVisibility, true)
            );
        }

        public enum HatType{
            STORMTROOPER, BOUNTY_HUNTER, LEIA, FEDORA, TOP_HAT, BASEBALL_CAP, DROID_PANEL, RANDOM
        }
    }

    enum StudColor {
        RED, YELLOW, BLUE, ORANGE, GREEN, PURPLE, LIGHT_BLUE, NONE;

        char getCode() {
            return switch (this) {
                case RED -> 'r';
                case BLUE -> 'b';
                case ORANGE -> 'o';
                case YELLOW -> 'y';
                case GREEN -> 'g';
                case LIGHT_BLUE -> 'u';
                case PURPLE -> 'p';
                case NONE -> '\0';
            };
        }

        static StudColor getColor(char code) {
            return switch (code){
                case 'r' -> Gizmo.Lever.StudColor.RED;
                case 'o' -> Gizmo.Lever.StudColor.ORANGE;
                case 'y' -> Gizmo.Lever.StudColor.YELLOW;
                case 'b' -> Gizmo.Lever.StudColor.BLUE;
                case 'g' -> Gizmo.Lever.StudColor.GREEN;
                case 'u' -> Gizmo.Lever.StudColor.LIGHT_BLUE;
                case 'p' -> Gizmo.Lever.StudColor.PURPLE;
                default -> Gizmo.Lever.StudColor.NONE;
            };
        }
    }

    enum Option{
        YES, NO
    }

    enum Visibility {
        VISIBLE, INVISIBLE
    }
}
