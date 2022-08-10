package com.opengg.loader.game.nu2.scene;

import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.render.objects.TextureRenderable;
import com.opengg.core.render.texture.Texture;
import com.opengg.core.world.components.RenderComponent;
import com.opengg.loader.components.NativeCache;

import java.awt.*;

public class IABLComponent extends RenderComponent {
    public static boolean SHOW = true;

    public IABLComponent(IABLObject bounds) {
       this(bounds.bounds()); 
    }

    public IABLComponent(IABLObject.IABLBoundingBox box) {
        super(new SceneRenderUnit.UnitProperties().shaderPipeline("xFixOnly").transparency(true));

        this.setUpdateEnabled(false);
        this.setScaleOffset(box.size().multiply(2));
        this.setPositionOffset(box.position());
        this.setPositionType(PositionType.ABSOLUTE);

        var renderable = new TextureRenderable(NativeCache.CUBE, Texture.ofColor(Color.PINK, 0.4f));
        this.setRenderable(() -> {
            if(SHOW)
                renderable.render();
        });
    }
}
