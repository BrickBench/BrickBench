package com.opengg.loader.components;

import com.opengg.core.render.Renderable;
import com.opengg.core.render.objects.ObjectCreator;

public class NativeCache {
    public static Renderable CUBE;

    public static void initialize() {
        CUBE = ObjectCreator.createCube(1);
    }
}
