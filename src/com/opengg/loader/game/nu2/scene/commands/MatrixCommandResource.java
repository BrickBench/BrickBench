package com.opengg.loader.game.nu2.scene.commands;

import com.opengg.core.math.Matrix4f;
import com.opengg.core.math.Quaternionf;
import com.opengg.core.math.Vector3f;
import com.opengg.core.render.shader.CommonUniforms;
import com.opengg.loader.loading.MapWriter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public record MatrixCommandResource(int address, Matrix4f matrix) implements DisplayCommandResource<MatrixCommandResource> {
    @Override
    public void run() {
        CommonUniforms.setModel(matrix);
    }

    @Override
    public int getAddress() {
        return address;
    }

    @Override
    public DisplayCommand.CommandType getType() {
        return DisplayCommand.CommandType.MTXLOAD;
    }

    @Override
    public Vector3f pos() {
        return matrix.getTranslation();
    }

    @Override
    public String name() {
        return "Matrix " + Integer.toHexString(address);
    }

    @Override
    public String path() {
        return "Render/Transforms/" + name();
    }

    @Override
    public void applyPropertyEdit(String propName, Property newValue) {
        var newMatrix = Matrix4f.IDENTITY;
        if(propName.equals("Position") && newValue instanceof VectorProperty vProp){
            newMatrix = newMatrix.translate(vProp.value()).rotate(matrix.getRotationNormalized()).scale(matrix.getScale());
        } else if(propName.equals("Scale") && newValue instanceof VectorProperty vProp){
            newMatrix = newMatrix.translate(matrix.getTranslation()).rotate(matrix.getRotationNormalized()).scale(vProp.value());
        } else if(propName.equals("Rotation") && newValue instanceof VectorProperty vProp){
            newMatrix = newMatrix.translate(matrix.getTranslation()).rotate(Quaternionf.createXYZ(vProp.value())).scale(matrix.getScale());
        }else{
            return;
        }

        var buf = ByteBuffer.allocate(16*4).order(ByteOrder.LITTLE_ENDIAN);
        buf.asFloatBuffer().put(newMatrix.getLinearArray());
        MapWriter.applyPatch(MapWriter.WritableObject.SCENE, address, buf);
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new VectorProperty("Position",pos(), true, true),
                new VectorProperty("Scale",matrix.getScale(), false, true),
                new VectorProperty("Rotation",matrix.getRotationNormalized().toEuler(), false, true)

        );
    }
}
