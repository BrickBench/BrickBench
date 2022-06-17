package com.opengg.loader.game.nu2.scene;

import com.opengg.core.math.Vector3f;
import com.opengg.loader.MapEntity;
import com.opengg.loader.loading.MapWriter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record Spline(String name, List<Vector3f> points, int address) implements MapEntity<Spline> {
    @Override
    public Vector3f pos() {
        return points.get(0);
    }

    @Override
    public String path() {
        return "Splines/" + name;
    }

    @Override
    public void applyPropertyEdit(String propName, Property newValue) {
        switch (newValue) {
            case VectorProperty vProp -> {
                var index = Integer.parseInt(propName);
                var offset = address + 8 + (12 * index);
                MapWriter.applyPatch(MapWriter.WritableObject.SPLINE, offset, vProp.value().toLittleEndianByteBuffer());
            }
            case null, default -> {
            }
        }
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new StringProperty("Name",name(), false, 128),
                new IntegerProperty("Vertex count", points.size(), false),
                new ListProperty("Vertices",
                        IntStream.range(0, points.size())
                                .mapToObj(p -> new VectorProperty(Integer.toString(p), points.get(p), true, true))
                                .collect(Collectors.toList()), false)
        );
    }
}
