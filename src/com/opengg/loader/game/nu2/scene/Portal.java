package com.opengg.loader.game.nu2.scene;

import com.opengg.core.math.Vector3f;
import com.opengg.loader.MapEntity;

import java.util.List;

public record Portal(int index, Rectangle rect) implements MapEntity<Portal> {
    @Override
    public Vector3f pos() {
        return rect.p1;
    }

    @Override
    public String name() {
        return "Portal_" + index;
    }

    @Override
    public String path() {
        return "Render/Portals/" + name();
    }

    @Override
    public List<Property> properties() {
        return List.of();
    }

    public record Rectangle(Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4){}
}
