package com.opengg.loader.game.nu2;

import com.opengg.core.math.Vector3f;
import com.opengg.loader.MapEntity;
import com.opengg.loader.game.nu2.scene.Spline;

import java.util.List;

public record Door(Spline doorSpline, String map) implements MapEntity<Door> {

    @Override
    public Vector3f pos() {
        return doorSpline.points().get(0);
    }

    @Override
    public String name() {
        return doorSpline.name();
    }

    @Override
    public String path() {
        return "Doors/" + name();
    }

    @Override
    public List<Property> properties(){
        return List.of(
                new StringProperty("Name",name(), false, 128),
                new StringProperty("Target Map",map, false, 128),
                new VectorProperty("Position",pos(), true,false),
                new IntegerProperty("Players",(doorSpline.points().size()-4)/2, false),
                new EditorEntityProperty("Spline", doorSpline, true, false, "")
        );
    }
}
