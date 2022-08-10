package com.opengg.loader.components;

import com.opengg.core.math.Matrix4f;
import com.opengg.core.math.Vector2f;
import com.opengg.core.math.Vector3f;
import com.opengg.core.render.Renderable;
import com.opengg.core.render.shader.CommonUniforms;
import com.opengg.core.render.shader.ShaderController;

public class BillBoardRenderable implements Renderable {
    private Matrix4f matrix;
    private Renderable object;
    private Vector3f center;
    private Vector2f size;

    public BillBoardRenderable(Renderable renderable, Vector3f center){
        object = renderable;
        this.matrix = new Matrix4f();
        this.center = center;
        this.size = new Vector2f(1);
    }

    @Override
    public void render() {
        ShaderController.setUniform("billboardCenter", center);
        ShaderController.setUniform("billboardSize", size);
        CommonUniforms.setModel(matrix);
        object.render();
        ShaderController.setUniform("billboardSize", new Vector2f(0));
    }
}
