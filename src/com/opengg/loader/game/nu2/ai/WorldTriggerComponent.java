package com.opengg.loader.game.nu2.ai;

import com.opengg.core.math.Quaternionf;
import com.opengg.core.math.Vector3f;
import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.render.objects.ObjectCreator;
import com.opengg.core.render.objects.TextureRenderable;
import com.opengg.core.render.texture.Texture;
import com.opengg.loader.components.EditorEntityRenderComponent;
import com.opengg.loader.components.TextBillboardComponent;

import java.awt.*;

public class WorldTriggerComponent extends EditorEntityRenderComponent {
    private WorldTrigger trigger;

    public WorldTriggerComponent(WorldTrigger trigger) {
        super(trigger, new TextureRenderable(ObjectCreator.createQuadPrism(trigger.size(), trigger.size().inverse()), Texture.ofColor(Color.ORANGE, 0.3f)),
                new SceneRenderUnit.UnitProperties().transparency(true).shaderPipeline("xFixOnly"));

        this.trigger = trigger;
        this.setUpdateEnabled(false);
        this.setPositionOffset(trigger.pos());
        this.setRotationOffset(Quaternionf.createXYZ(new Vector3f(0, trigger.angle(), 0)));

        this.attach(new TextBillboardComponent(trigger.name(), new Vector3f(0, trigger.size().y, 0), false));
    }
}
