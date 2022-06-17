package com.opengg.loader.game.nu2.terrain;

import com.opengg.core.engine.OpenGG;
import com.opengg.core.math.Vector3f;

import java.util.List;
import java.util.Map;

public record InfiniteWall(List<Vector3f> wall, int objectDefinitionAddress, int objectContentsAddress, int objectContentsSize) implements TerrainObject{
    @Override
    public Vector3f pos() {
        return wall.get(0);
    }

    @Override
    public String name() {
        return "Infinite Wall " + objectContentsAddress;
    }

    @Override
    public String path() {
        return "Terrain/Walls/" + name();
    }

    @Override
    public Map<String, Runnable> getButtonActions() {
        return Map.of("Remove", () -> OpenGG.asyncExec(() -> TerrainSerializer.removeObject(this)));
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new StringProperty("Name",name(), false, 10)
        );
    }
}
