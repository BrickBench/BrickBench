package com.opengg.loader.game.nu2.ai;

import com.opengg.core.math.Matrix4f;
import com.opengg.core.math.Quaternionf;
import com.opengg.core.math.Vector3f;
import com.opengg.core.physics.collision.colliders.BoundingBox;
import com.opengg.loader.MapEntity;
import com.opengg.loader.loading.MapWriter;
import com.opengg.loader.Util;
import com.opengg.loader.components.Selectable;

import java.util.List;

public record WorldTrigger (Vector3f pos, Vector3f size, float angle, String name, int fileAddress) implements MapEntity<WorldTrigger>, Selectable {
    @Override
    public void applyPropertyEdit(String propName, Property newValue) {
        switch (newValue) {
            case VectorProperty vProp -> {
                switch (propName) {
                    case "Position" -> MapWriter.applyPatch(MapWriter.WritableObject.TRIGGER, fileAddress + 16, vProp.value().toLittleEndianByteBuffer());
                    case "Size" -> MapWriter.applyPatch(MapWriter.WritableObject.TRIGGER, fileAddress + 16 + 12, vProp.value().toLittleEndianByteBuffer());
                }
            }
            case StringProperty sProp when propName.equals("Name") ->  MapWriter.applyPatch(MapWriter.WritableObject.TRIGGER, fileAddress, Util.getStringBytes(sProp.value(), 16));
            case FloatProperty iProp when propName.equals("Angle") -> MapWriter.applyPatch(MapWriter.WritableObject.TRIGGER, fileAddress + 16 + 12 + 12, Util.littleEndian(Util.floatToShortAngle(iProp.value())));

            case null, default -> {}
        }
    }

    @Override
    public String path() {
        return "AI/Triggers/" + name;
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new StringProperty("Name", name(), true, 16),
                new VectorProperty("Position", pos(), true,true),
                new VectorProperty("Size", size, false,true),
                new FloatProperty("Angle", angle, true)
        );
    }

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(
                new Matrix4f().translate(this.pos()).rotate(Quaternionf.createXYZ(new Vector3f(0, angle(), 0))),
                size().inverse(), size()
        );
    }

    @Override
    public int getSelectionOrder() {
        return 2;
    }
}
