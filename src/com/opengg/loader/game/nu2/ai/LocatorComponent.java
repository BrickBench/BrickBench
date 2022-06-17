package com.opengg.loader.game.nu2.ai;

import com.opengg.core.math.Vector3f;
import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.render.objects.ObjectCreator;
import com.opengg.core.render.objects.TextureRenderable;
import com.opengg.core.render.texture.Texture;
import com.opengg.loader.components.EditorEntityRenderComponent;
import com.opengg.loader.components.TextBillboardComponent;

import java.awt.*;

public class LocatorComponent extends EditorEntityRenderComponent {
    public static Vector3f BOX_SIZE = new Vector3f(0.2f, 0.2f, 0.2f);

    public LocatorComponent(AILocator locator) {
        super(locator, new TextureRenderable(ObjectCreator.createQuadPrism(
                        BOX_SIZE.multiply(-0.5f,-0.5f,-0.5f),
                        BOX_SIZE.multiply(0.5f,0.5f,0.5f)),
                        Texture.ofColor(Color.BLUE, 1)),
                new SceneRenderUnit.UnitProperties().shaderPipeline("xFixOnly"));
        this.setUpdateEnabled(false);
        this.setPositionOffset(locator.pos());
        this.setRotationOffset(new Vector3f(0, locator.startAngle(), 0));
        this.attach(new TextBillboardComponent(locator.name(), locator.pos().multiply(-2,0,0), false));
    }
}
