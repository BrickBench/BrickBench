package com.opengg.loader.game.nu2.rtl;

import com.opengg.core.math.Vector3f;
import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.render.objects.ObjectCreator;
import com.opengg.core.render.objects.TextureRenderable;
import com.opengg.core.render.texture.Texture;
import com.opengg.core.world.components.RenderComponent;

import java.awt.*;

public class RTLLightComponent extends RenderComponent{
    private Vector3f color;
    public static boolean enableDebugModel = false;

    public RTLLightComponent(RTLLight light) {
        super(new SceneRenderUnit.UnitProperties().shaderPipeline("xFixOnly"));
        this.setPositionOffset(light.pos());
        setColor(light.color());
    }

    public void setColor(Vector3f color){
        this.color = color;
        this.setRenderable(new TextureRenderable(ObjectCreator.createCube(0.1f), Texture.ofColor(new Color(color.x,color.y,color.z))));
    }

    public void render(){
        if(enableDebugModel){
            super.render();
        }
    }
}
