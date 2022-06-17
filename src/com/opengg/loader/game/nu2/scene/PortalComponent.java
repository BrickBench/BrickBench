package com.opengg.loader.game.nu2.scene;

import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.render.objects.ObjectCreator;
import com.opengg.core.render.objects.TextureRenderable;
import com.opengg.core.render.texture.Texture;
import com.opengg.loader.components.EditorEntityRenderComponent;
import com.opengg.loader.game.nu2.scene.Portal;

import java.awt.*;

public class PortalComponent extends EditorEntityRenderComponent {

    public PortalComponent(Portal portal) {
        super(portal, new TextureRenderable(ObjectCreator.create3DRectangle(portal.rect().p1(), portal.rect().p2(), portal.rect().p3(), portal.rect().p4()),
                Texture.ofColor(Color.RED, 0.3f)), new SceneRenderUnit.UnitProperties().shaderPipeline("xFixOnly").transparency(true));
    }
}
