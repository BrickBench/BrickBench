package com.opengg.loader.game.nu2.scene;

import com.opengg.core.math.Vector3f;
import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.render.objects.DrawnObject;
import com.opengg.core.render.objects.TextureRenderable;
import com.opengg.core.render.texture.Texture;
import com.opengg.core.system.Allocator;
import com.opengg.loader.components.EditorEntityRenderComponent;
import com.opengg.loader.components.TextBillboardComponent;
import com.opengg.loader.game.nu2.scene.Spline;

import java.awt.*;

public class SplineComponent  extends EditorEntityRenderComponent {
    public SplineComponent(Spline spline){
        super(spline, new SceneRenderUnit.UnitProperties().shaderPipeline("xFixOnly"));
        this.setUpdateEnabled(false);
        if(spline.points().isEmpty()) return;

        var buf = Allocator.allocFloat(spline.points().size()*8);
        for(var point : spline.points()){
            buf.put(point.x).put(point.y).put(point.z).put(1).put(1).put(1).put(1).put(1);
        }
        buf.flip();

        var drawnObject = DrawnObject.create(buf);
        drawnObject.setRenderType(DrawnObject.DrawType.LINE_STRIP);
        this.setRenderable(new TextureRenderable(drawnObject, Texture.ofColor(Color.PINK)));
        this.attach(new TextBillboardComponent(spline.name(), new Vector3f(spline.points().get(0))));
    }
}
