package com.opengg.loader.game.nu2.ai;

import com.opengg.core.math.Vector3f;
import com.opengg.core.physics.collision.colliders.BoundingBox;
import com.opengg.loader.MapEntity;
import com.opengg.loader.components.Selectable;
import com.opengg.loader.loading.MapWriter;
import com.opengg.loader.Util;

import java.util.List;

public record AILocator(Vector3f pos, String name, int id, float startAngle, int fileAddress) implements MapEntity<AILocator>, Selectable {

    @Override
    public void applyPropertyEdit(String propName, Property newValue) {
        switch (newValue) {
            case StringProperty sProp when sProp.name().equals("Name") -> MapWriter.applyPatch(MapWriter.WritableObject.AI_LOCATOR, fileAddress, Util.getStringBytes(sProp.stringValue(), 16));
            case VectorProperty vProp when vProp.name().equals("Position") -> MapWriter.applyPatch(MapWriter.WritableObject.AI_LOCATOR, fileAddress + 16, vProp.value().toLittleEndianByteBuffer());
            case FloatProperty fProp when propName.equals("Angle") -> MapWriter.applyPatch(MapWriter.WritableObject.CREATURE_SPAWN, fileAddress + 16 + 12, Util.littleEndian(Util.floatToShortAngle(fProp.value())));
            default -> {}
        }
    }

    @Override
    public String path() {
        return "AI/Locators/" + name;
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new StringProperty("Name", name(), true, 16),
                new VectorProperty("Position", pos(), true,true),
                new FloatProperty("Angle", startAngle, true)
        );
    }

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(pos.subtract(0.1f), pos.add(0.1f));
    }
}
