package com.opengg.loader.components;

import com.opengg.core.math.Vector3f;
import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.world.components.RenderComponent;
import com.opengg.loader.BrickBench;

import java.util.Objects;

public class TextBillboardComponent extends RenderComponent {
    public static boolean SHOW = true;

    public TextBillboardComponent(String text, Vector3f pos){
        this(text, pos, true);
    }

    public TextBillboardComponent(String text, Vector3f pos, boolean invertPosition){
        super(new SceneRenderUnit.UnitProperties());
        var mesh = BrickBench.WORLD_OBJECT_FONT.createFromText(Objects.requireNonNullElse(text, "NO_TEXT"));

        this.setUpdateEnabled(false);
        this.setPositionOffset(pos.multiply(new Vector3f(invertPosition ? -1 : 1,1,1)).add(new Vector3f(0,0.56f,0)));
        this.setRenderable(() -> {
            if(SHOW) mesh.render();
        });
        this.setRenderDistance(5f);
    }
}

