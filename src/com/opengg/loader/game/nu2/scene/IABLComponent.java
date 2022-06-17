package com.opengg.loader.game.nu2.scene;

import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.render.objects.ObjectCreator;
import com.opengg.core.render.objects.TextureRenderable;
import com.opengg.core.render.texture.Texture;
import com.opengg.core.world.components.RenderComponent;
import com.opengg.loader.game.nu2.scene.IABLObject;

import java.awt.*;

public class IABLComponent extends RenderComponent {
    public static boolean SHOW = true;

    public IABLComponent(IABLObject bounds) {
        super(new SceneRenderUnit.UnitProperties().shaderPipeline("xFixOnly").transparency(true));

        var bottomLeft = bounds.transform().transform(bounds.bounds().size().subtract(bounds.bounds().position()).multiply(0.5f));
        var topRight = bounds.transform().transform(bounds.bounds().size().add(bounds.bounds().position()).multiply(0.5f));

        this.setUpdateEnabled(false);
        this.setPositionType(PositionType.ABSOLUTE);

        this.setRenderable(new TextureRenderable(ObjectCreator.createQuadPrism(bottomLeft, topRight), Texture.ofColor(Color.PINK, 0.4f)));
    }

    public IABLComponent(IABLObject.IABLBoundingBox box) {
        super(new SceneRenderUnit.UnitProperties().shaderPipeline("xFixOnly").transparency(true));

        this.setUpdateEnabled(false);
        this.setPositionType(PositionType.ABSOLUTE);

        var renderable = new TextureRenderable(ObjectCreator.createQuadPrism(box.position().subtract(box.size()), box.position().add(box.size())), Texture.ofColor(Color.PINK, 0.4f));
        this.setRenderable(() -> {
            if(SHOW)
                renderable.render();
        });
    }
}
