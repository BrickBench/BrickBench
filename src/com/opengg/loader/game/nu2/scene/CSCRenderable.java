package com.opengg.loader.game.nu2.scene;

import com.opengg.core.render.objects.DrawnObject;
import com.opengg.core.render.shader.VertexArrayBinding;
import com.opengg.core.render.shader.VertexArrayFormat;

import java.nio.FloatBuffer;
import java.util.List;

public abstract class CSCRenderable implements GameRenderable<CSCRenderable>{
    boolean generatedObject = false;

    FloatBuffer bufferContents;
    DrawnObject renderedObject;
    public static final VertexArrayFormat superCSCFormat;

    static{
        superCSCFormat = new VertexArrayFormat(List.of(
                new VertexArrayBinding(0, 10 * 4, 0, List.of(
                        new VertexArrayBinding.VertexArrayAttribute("position", 3 * 4, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT3, 0),
                        new VertexArrayBinding.VertexArrayAttribute("normal", 3 * 4, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT3, 3 * 4),
                        new VertexArrayBinding.VertexArrayAttribute("texcoord", 2 * 4, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT2, 6 * 4),
                        new VertexArrayBinding.VertexArrayAttribute("lightmapcoord", 2 * 4, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT2, 8 * 4)
                ))
        ));

    }

    public CSCRenderable(FloatBuffer bufferContents) {
        this.bufferContents = bufferContents;
    }

    public void generateObject(){
        if(!generatedObject){
            this.renderedObject = DrawnObject.create(superCSCFormat,bufferContents);
            this.renderedObject.setRenderType(DrawnObject.DrawType.TRIANGLE_STRIP);
            generatedObject = true;
        }
    }

    @Override
    public void render() {
        if(!generatedObject) generateObject();

        renderedObject.render();
    }
}
