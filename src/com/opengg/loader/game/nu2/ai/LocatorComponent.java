package com.opengg.loader.game.nu2.ai;

import com.opengg.core.math.Vector3f;
import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.render.objects.TextureRenderable;
import com.opengg.core.render.texture.Texture;
import com.opengg.loader.components.EditorEntityRenderComponent;
import com.opengg.loader.components.TextBillboardComponent;
import com.opengg.loader.components.NativeCache;

import java.awt.*;

public class LocatorComponent extends EditorEntityRenderComponent {

    public LocatorComponent(AILocator locator) {
        super(locator, new TextureRenderable(NativeCache.CUBE, Texture.ofColor(Color.BLUE, 1)),
                new SceneRenderUnit.UnitProperties().shaderPipeline("xFixOnly"));
        this.setUpdateEnabled(false);
        this.setPositionOffset(locator.pos());
        this.setScaleOffset(0.2f);
        this.setRotationOffset(new Vector3f(0, locator.startAngle(), 0));
        this.attach(new TextBillboardComponent(locator.name(), locator.pos().multiply(-2,0,0), false));
    }
}
