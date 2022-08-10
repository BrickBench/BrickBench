package com.opengg.loader.game.nu2.rtl;

import com.opengg.core.math.Vector3f;
import com.opengg.core.physics.collision.colliders.BoundingBox;
import com.opengg.loader.MapEntity;
import com.opengg.loader.Util;
import com.opengg.loader.components.Selectable;
import com.opengg.loader.loading.MapWriter;

import java.util.List;

public record RTLLight (Vector3f pos, Vector3f rot, Vector3f color, Vector3f flickerColor, LightType type, float distance, float falloff, float multiplier, int address, int idx) implements MapEntity<RTLLight>, Selectable {
    @Override
    public String name() {
        return "Light_" + idx;
    }

    @Override
    public String path() {
        return "Render/Lights/" + name();
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new VectorProperty("Position", pos, true, true),
                new VectorProperty("Angle", rot, false, true),
                new ColorProperty("Color", color),
                new ColorProperty("Flicker Color", flickerColor),
                new EnumProperty("Light Type", type, true),
                new FloatProperty("Distance", distance, true),
                new FloatProperty("Falloff", falloff, true),
                new FloatProperty("Multiplier", multiplier, true)
        );
    }

    @Override
    public void applyPropertyEdit(String propName, Property newValue) {
        switch (newValue) {
            case VectorProperty vp && propName.equals("Position") -> MapWriter.applyPatch(MapWriter.WritableObject.LIGHTS, address, vp.value().toLittleEndianByteBuffer());
            case VectorProperty vp && propName.equals("Angle") -> MapWriter.applyPatch(MapWriter.WritableObject.LIGHTS, address + 12, vp.value().toLittleEndianByteBuffer());
            case ColorProperty cp && propName.equals("Color") -> MapWriter.applyPatch(MapWriter.WritableObject.LIGHTS, address + 12 + 12 + 12, cp.value().toLittleEndianByteBuffer());
            case ColorProperty cp && propName.equals("Flicker Color") -> MapWriter.applyPatch(MapWriter.WritableObject.LIGHTS, address + 12 + 12 + 12 + 12, cp.value().toLittleEndianByteBuffer());
            case EnumProperty ep && propName.equals("Light Type") -> MapWriter.applyPatch(MapWriter.WritableObject.LIGHTS, address + 0x58, new byte[]{(byte) ((LightType)ep.value()).BYTE_VALUE});
            case FloatProperty fp && propName.equals("Distance") -> MapWriter.applyPatch(MapWriter.WritableObject.LIGHTS, address + 0x3C, Util.littleEndian(fp.value()));
            case FloatProperty fp && propName.equals("Falloff") -> MapWriter.applyPatch(MapWriter.WritableObject.LIGHTS, address + 0x40, Util.littleEndian(fp.value()));
            case FloatProperty fp && propName.equals("Multiplier") -> MapWriter.applyPatch(MapWriter.WritableObject.LIGHTS, address + 0x6C, Util.littleEndian(fp.value()));
            default -> {}
        }
    }

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(pos.subtract(0.15f), pos.add(0.15f));
    }

    public enum LightType {
        INVALID((byte)0),
        AMBIENT((byte)1),
        POINT((byte)2),
        POINTFLICKER((byte)3),
        DIRECTIONAL((byte)4),
        CAMDIR((byte)5),
        POINTBLEND((byte)6),
        ANTILIGHT((byte)7),
        JONFLICKER((byte)8);

        public final int BYTE_VALUE;

        LightType(byte val) {
            BYTE_VALUE = val;
        }

        public static LightType getLightTypeFromId(byte id) {
            return switch (id) {
                case 0 -> INVALID;
                case 1 -> AMBIENT;
                case 2 -> POINT;
                case 3 -> POINTFLICKER;
                case 4 -> DIRECTIONAL;
                case 5 -> CAMDIR;
                case 6 -> POINTBLEND;
                case 7 -> ANTILIGHT;
                case 8 -> JONFLICKER;
                default -> throw new IllegalArgumentException("Unknown.LIGHTS light " + id);
            };
        }
    }
}
