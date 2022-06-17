package com.opengg.loader.components;

import com.opengg.core.math.Matrix4f;
import com.opengg.core.math.Vector2f;
import com.opengg.core.math.Vector3f;
import com.opengg.core.render.Renderable;
import com.opengg.core.render.shader.CommonUniforms;
import com.opengg.core.render.shader.ShaderController;
import com.opengg.loader.components.EditorEntityRenderComponent;

public class BillBoardRenderable implements Renderable {
    private Matrix4f matrix;
    private Renderable object;
    private Vector3f center;
    private Vector2f size;
    private EditorEntityRenderComponent wow;
    public BillBoardRenderable(Renderable renderable, Vector3f center){
        object = renderable;
        this.matrix = new Matrix4f();
        this.center = center;
        this.size = new Vector2f(1);
    }
    public void setWow(EditorEntityRenderComponent w){
        wow = w;
    }

    @Override
    public void render() {
        //System.out.println(center + "    |    "+ RenderEngine.getCurrentView().getPosition() + " |  " + wow.getPosition());
        ShaderController.setUniform("billboardCenter",center);
        ShaderController.setUniform("billboardSize",size);
        CommonUniforms.setModel(matrix);
        object.render();
        ShaderController.setUniform("billboardSize",new Vector2f(0));
    }
}
