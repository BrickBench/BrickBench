package com.opengg.loader.game.nu2.scene;

import com.opengg.core.math.Matrix4f;
import com.opengg.core.math.Quaternionf;
import com.opengg.core.math.Vector3f;
import com.opengg.core.physics.collision.colliders.BoundingBox;
import com.opengg.core.render.Renderable;
import com.opengg.core.render.shader.ShaderController;
import com.opengg.loader.Util;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.MapEntity;
import com.opengg.loader.loading.MapWriter;
import com.opengg.loader.components.Selectable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.stream.Collectors;

public record SpecialObject(GameModel model,
                            Matrix4f initialTransform, IABLObject iablObj, int remoteIABLIndex, String name,
                            int boundingBoxIndex, List<Float> lodCutoffs, float windSpeedFactor, float windShearFactor, int fileAddress) implements Renderable, MapEntity<SpecialObject>, Selectable {

    @Override
    public Vector3f pos(){
        return iablObj.pos();
    }

    @Override
    public String path() {
        return "Render/SpecialObjects/" + name;
    }

    @Override
    public void render() {
        if(EditorState.CURRENT.shouldHighlight && EditorState.getSelectedObject().get() instanceof SpecialObject obj && obj != this) {
            ShaderController.setUniform("muteColors", 1);
        }else{
            ShaderController.setUniform("muteColors", 0);
        }
        model.render();
    }

    @Override
    public int getSelectionOrder() {
        return 1;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(
                ((NU2MapData) EditorState.getActiveMap().levelData()).scene().boundingBoxes().get(boundingBoxIndex()).position(),
                iablObj.bounds().size().inverse(),
                iablObj.bounds().size()
        );
    }

    @Override
    public void applyPropertyEdit(String propName, Property newValue) {
        Matrix4f newMatrix = null;
        switch (newValue) {
            case VectorProperty vProp && propName.equals("Position") -> {
                newMatrix = Matrix4f.IDENTITY.translate(vProp.value())
                        .rotate(iablObj.transform().getRotationNormalized())
                        .scale(iablObj.transform().getScale());
                var boundsDiff1 = iablObj.bounds().position().subtract(pos());
                var boundsDiff2 = ((NU2MapData) EditorState.getActiveMap().levelData()).scene().boundingBoxes().get(boundingBoxIndex()).position().subtract(pos());
                MapWriter.applyPatch(MapWriter.WritableObject.SCENE,
                        fileAddress + 16 * 4 + 16 * 4, vProp.value().add(boundsDiff1).toLittleEndianByteBuffer());
                MapWriter.applyPatch(MapWriter.WritableObject.SCENE,
                        ((NU2MapData) EditorState.getActiveMap().levelData()).scene().boundingBoxes().get(boundingBoxIndex()).address(),
                        vProp.value().add(boundsDiff2).toLittleEndianByteBuffer());
            }
            case VectorProperty vProp && propName.equals("Rotation") ->
                    newMatrix = Matrix4f.IDENTITY.translate(iablObj.transform().getTranslation())
                                .rotate(Quaternionf.createYXZ(vProp.value()))
                                .scale(iablObj.transform().getScale());
            case VectorProperty vProp && propName.equals("Scale") ->
                    newMatrix = Matrix4f.IDENTITY.translate(iablObj.transform().getTranslation())
                                .rotate(iablObj.transform().getRotationNormalized())
                                .scale(vProp.value());
            case EditorEntityProperty mop && propName.equals("Model") -> {
                var object = (GameModel) mop.value();
                var propAddr = this.fileAddress + 0xb0;
                var offset = object.modelAddress() - propAddr;
                MapWriter.applyPatch(MapWriter.WritableObject.SCENE, propAddr, Util.littleEndian(offset));
            }
            case FloatProperty fProp && propName.equals("Wind speed factor") -> MapWriter.applyPatch(MapWriter.WritableObject.SCENE, fileAddress + 0xca, Util.littleEndian((char) ((int) (fProp.value() * 65535))));
            case FloatProperty fProp && propName.equals("Wind shear factor") -> MapWriter.applyPatch(MapWriter.WritableObject.SCENE, fileAddress + 0xc8, Util.littleEndian((char) ((int) (fProp.value() * 65535))));
            case null, default -> {
            }
        }

        if(newMatrix != null){
            var buf = ByteBuffer.allocate(16*4).order(ByteOrder.LITTLE_ENDIAN);
            buf.asFloatBuffer().put(newMatrix.getLinearArray());

            MapWriter.applyPatch(MapWriter.WritableObject.SCENE, fileAddress, buf);
            MapWriter.applyPatch(MapWriter.WritableObject.SCENE, fileAddress + 16*4, buf);
        }
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new StringProperty("Name", name(), false, 128),
                new VectorProperty("Position", pos(), true, true),
                new VectorProperty("Scale", iablObj.transform().getScale(), false, true),
                new VectorProperty("Rotation", iablObj.transform().getRotationNormalized().toEuler(), false, true),
                new EditorEntityProperty("Model", model, true, true, "Render/Models"),
                new FloatProperty("Wind speed factor", windSpeedFactor, true),
                new FloatProperty("Wind shear factor", windShearFactor, true),
                new StringProperty("LOD cutoffs", lodCutoffs.stream().map(String::valueOf).collect(Collectors.joining(", ")), false, 0)
        );
    }
}
