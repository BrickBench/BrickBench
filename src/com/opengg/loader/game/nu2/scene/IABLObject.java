package com.opengg.loader.game.nu2.scene;

import com.opengg.core.math.Matrix4f;
import com.opengg.core.math.Quaternionf;
import com.opengg.core.math.Vector3f;
import com.opengg.core.math.Vector4f;

public record IABLObject (Matrix4f transform, IABLBoundingBox bounds, int address){
    public record IABLBoundingBox(Vector3f position, Vector3f size, int address){}

    public Vector3f pos(){
        return transform.transform(new Vector4f(0,0,0,1)).truncate();
    }

    public Quaternionf rot(){
        return new Quaternionf();
    }

    public Vector3f scale(){
        return new Vector3f(transform.m00, transform.m11, transform.m22);
    }
}
