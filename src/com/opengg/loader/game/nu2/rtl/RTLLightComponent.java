package com.opengg.loader.game.nu2.rtl;

import com.opengg.core.math.Vector3f;
import com.opengg.core.math.Quaternionf;
import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.render.objects.ObjectCreator;
import com.opengg.core.render.objects.TextureRenderable;
import com.opengg.core.render.objects.RenderableGroup;
import com.opengg.core.render.texture.Texture;
import com.opengg.loader.components.EditorEntityRenderComponent;
import com.opengg.loader.components.NativeCache;

import java.awt.*;

public class RTLLightComponent extends EditorEntityRenderComponent {
    public RTLLightComponent(RTLLight light) {
        super(light, new SceneRenderUnit.UnitProperties().shaderPipeline("xFixOnly"));
        this.setPositionOffset(light.pos());
        this.setScaleOffset(0.1f);
        this.setRotationOffset(Quaternionf.createYXZ(light.rot().normalize()));

        if (light.type() == RTLLight.LightType.DIRECTIONAL) {
            this.setRenderable(new TextureRenderable(RenderableGroup.of(NativeCache.CUBE, ObjectCreator.createQuadPrism(new Vector3f(0.01f, -0.01f, -0.01f), new Vector3f(0.2f, 0.01f, 0.01f))),
                Texture.ofColor(new Color(light.color().x, light.color().y, light.color().z))));
        } else {
            this.setRenderable(new TextureRenderable(NativeCache.CUBE, Texture.ofColor(new Color(light.color().x, light.color().y, light.color().z))));
        }
    }
}
