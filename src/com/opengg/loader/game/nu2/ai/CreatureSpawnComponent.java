package com.opengg.loader.game.nu2.ai;

import com.opengg.core.math.Vector3f;
import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.render.objects.TextureRenderable;
import com.opengg.core.render.texture.Texture;
import com.opengg.loader.components.EditorEntityRenderComponent;
import com.opengg.loader.components.TextBillboardComponent;
import com.opengg.loader.components.NativeCache;

import java.awt.*;

public class CreatureSpawnComponent extends EditorEntityRenderComponent {
    public static Vector3f BOX_SIZE = new Vector3f(0.2f, 0.4f, 0.2f);

    public CreatureSpawnComponent(CreatureSpawn creature) {
        super(creature, new TextureRenderable(NativeCache.CUBE, Texture.ofColor(Color.RED, 1)),
                new SceneRenderUnit.UnitProperties().shaderPipeline("xFixOnly"));
        this.setUpdateEnabled(false);
        this.setPositionOffset(creature.spawnPos().add(new Vector3f(0, BOX_SIZE.y()/2, 0)));
        this.setScaleOffset(BOX_SIZE);
        this.setRotationOffset(new Vector3f(0, creature.startAngle(), 0));
        this.attach(new TextBillboardComponent(creature.name(), creature.pos().multiply(-2,0,0), false));
    }

}
