package com.opengg.loader.game.nu2;

import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.render.objects.ObjectCreator;
import com.opengg.core.render.objects.TextureRenderable;
import com.opengg.core.render.texture.Texture;
import com.opengg.loader.components.EditorEntityRenderComponent;
import com.opengg.loader.components.TextBillboardComponent;
import com.opengg.loader.game.nu2.Door;

import java.awt.*;

public class DoorComponent extends EditorEntityRenderComponent {

    public DoorComponent(Door door) {
        super(door, new TextureRenderable(ObjectCreator.create3DRectangle(door.doorSpline().points().get(0),
                door.doorSpline().points().get(1),
                door.doorSpline().points().get(2),
                door.doorSpline().points().get(3)), Texture.ofColor(Color.GREEN, 0.5f)),
                new SceneRenderUnit.UnitProperties().shaderPipeline("xFixOnly").transparency(true));
        this.setUpdateEnabled(false);

        this.attach(new TextBillboardComponent(door.doorSpline().name(), door.doorSpline().points().get(1).add(door.doorSpline().points().get(2)).divide(2)));
    }
}
