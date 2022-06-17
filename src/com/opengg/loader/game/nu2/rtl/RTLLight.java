package com.opengg.loader.game.nu2.rtl;

import com.opengg.core.math.Vector3f;
import com.opengg.loader.MapEntity;

import java.util.List;

public record RTLLight (Vector3f pos, Vector3f color, int address) implements MapEntity<RTLLight> {
    @Override
    public String name() {
        return "Light_" + Integer.toHexString(address);
    }

    @Override
    public String path() {
        return "Render/Lights/" + name();
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new VectorProperty("Position", pos, true, false),
                new ColorProperty("Color", color)
        );
    }
}
