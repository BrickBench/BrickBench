package com.opengg.loader.game.nu2.scene;

import com.opengg.core.math.Vector3f;
import com.opengg.core.render.SceneRenderUnit;
import com.opengg.loader.components.EditorEntityRenderComponent;
import com.opengg.loader.components.TextBillboardComponent;
import com.opengg.loader.game.nu2.NU2MapData;

public class SpecialObjectComponent extends EditorEntityRenderComponent {
    private SpecialObject specialObject;
    private IABLObject.IABLBoundingBox bounds;

    public SpecialObjectComponent(SpecialObject specialObject, NU2MapData mapData) {
        super(specialObject, specialObject, new SceneRenderUnit.UnitProperties().shaderPipeline("ttNormal"));
        this.specialObject = specialObject;
        this.bounds = mapData.scene().boundingBoxes().get(specialObject.boundingBoxIndex());
        this.setPositionOffset(specialObject.pos());
        this.setOverrideMatrix(specialObject.iablObj().transform());
        this.setUpdateEnabled(false);
        this.attach(new TextBillboardComponent(specialObject.name(), new Vector3f(specialObject.pos().multiply(new Vector3f(2,0,0)))));
        this.attach(new IABLComponent(bounds));
    }
}
