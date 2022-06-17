package com.opengg.loader.game.nu2.ai;

import com.opengg.core.math.Matrix4f;
import com.opengg.core.math.Vector3f;
import com.opengg.core.physics.collision.colliders.BoundingBox;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.MapEntity;
import com.opengg.loader.loading.MapWriter;
import com.opengg.loader.Util;
import com.opengg.loader.components.Selectable;

import java.util.List;
import java.util.Map;

public record CreatureSpawn(String name, String initialScript, String type, Vector3f spawnPos, float startAngle, String typeText,
                            AILocator locator1, AILocator locator2, WorldTrigger trigger1, WorldTrigger trigger2, int fileAddress, int size) implements MapEntity<CreatureSpawn>, Selectable {
    @Override
    public Vector3f pos() {
        return spawnPos;
    }

    @Override
    public String path() {
        return "AI/Creatures/" + name;
    }

    @Override
    public Map<String, Runnable> getButtonActions() {
        return Util.createOrderedMapFrom(Map.entry("Remove", () -> AIWriter.deleteAICreature(this)));
    }

    @Override
    public void applyPropertyEdit(String propName, Property newValue) {
        var typeSize = ((NU2MapData) EditorState.getActiveMap().levelData()).ai().version().get() < 14 ? 16 : 32;

        switch (newValue) {
            case VectorProperty nVec && "Spawn Location".equals(propName) -> MapWriter.applyPatch(MapWriter.WritableObject.CREATURE_SPAWN, fileAddress + 16 + 16 + typeSize, nVec.value().toLittleEndianByteBuffer());
            case StringProperty sProp -> {
                switch (propName) {
                    case "Name" -> MapWriter.applyPatch(MapWriter.WritableObject.CREATURE_SPAWN, fileAddress, Util.getStringBytes(sProp.stringValue(), 16));
                    case "Initial Script Behavior" -> MapWriter.applyPatch(MapWriter.WritableObject.CREATURE_SPAWN, fileAddress + 16, Util.getStringBytes(sProp.stringValue(), 16));
                    case "Type" -> MapWriter.applyPatch(MapWriter.WritableObject.CREATURE_SPAWN, fileAddress + 16 + 16, Util.getStringBytes(sProp.stringValue(), typeSize));
                }
            }
            case FloatProperty iProp && "Spawn Angle".equals(propName) -> MapWriter.applyPatch(MapWriter.WritableObject.CREATURE_SPAWN, fileAddress + 16 + 16 + typeSize + 12, Util.littleEndian(Util.floatToShortAngle(iProp.value())));

            case null, default -> {}
        }
    }

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(new Matrix4f().translate(this.pos()),
                CreatureSpawnComponent.BOX_SIZE.multiply(-0.5f, 0, -0.5f),
                CreatureSpawnComponent.BOX_SIZE.multiply(0.5f, 1, 0.5f));
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new StringProperty("Name",name, true, 16),
                new StringProperty("Type",type, true, ((NU2MapData) EditorState.getActiveMap().levelData()).ai().version().get() < 0xe ? 16 : 32),
                new StringProperty("Initial Script Behavior",initialScript, true, 16),
                new VectorProperty("Spawn Location",spawnPos, true,true),
                new FloatProperty("Spawn Angle", startAngle, true)
        );
    }
}
