package com.opengg.loader.game.nu2.scene;

import com.opengg.core.math.Matrix4f;
import com.opengg.core.math.Vector3f;
import com.opengg.core.render.RenderEngine;
import com.opengg.core.render.Renderable;
import com.opengg.core.render.shader.CommonUniforms;

public class ParallaxComponent implements Renderable {
    public static boolean ENABLE_PARALLAX = true;

    GameModel model;
    float farclip = 1;

    public ParallaxComponent(GameModel model,float farclip){
        this.model = model;
        this.farclip = farclip;    
    }

    @Override
    public void render() {
        float scale = farclip * 0.1f*0.5f;
        if(model != null && ENABLE_PARALLAX){
            CommonUniforms.setModel(Matrix4f.IDENTITY.translate(RenderEngine.getCurrentView().getPosition().multiply(new Vector3f(-1,1,1))).scale(scale,scale,scale));
            model.render();
        }
    }
}
