package com.opengg.loader.game.nu2.scene;

import com.opengg.core.math.Matrix4f;
import com.opengg.core.math.Quaternionf;
import com.opengg.core.math.Vector3f;
import com.opengg.loader.FileUtil;
import com.opengg.loader.SwingUtil;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.commands.DisplayCommand;
import com.opengg.loader.game.nu2.scene.commands.MatrixCommandResource;
import com.opengg.loader.MapEntity;
import com.opengg.loader.loading.MapLoader;
import com.opengg.loader.loading.MapWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record ModelInstance (GameModel model, MatrixCommandResource matrix, List<DisplayCommand> associatedCommands, int modelInstanceNumber) implements MapEntity<ModelInstance> {
    @Override
    public Vector3f pos() {
        return matrix.matrix().getTranslation();
    }

    @Override
    public String name() {
        return model.name() + "_Instance_" + modelInstanceNumber;
    }

    @Override
    public String path() {
        return "Render/StaticObjects/" + name();
    }

    @Override
    public void applyPropertyEdit(String propName, Property newValue) {
        Matrix4f newMatrix = switch (newValue) {
            case VectorProperty vProp && propName.equals("Position") ->
                    Matrix4f.IDENTITY.translate(vProp.value())
                        .rotate(matrix.matrix().getRotationNormalized())
                        .scale(matrix.matrix().getScale());
            case VectorProperty vProp && propName.equals("Rotation") ->
                    Matrix4f.IDENTITY.translate(matrix.matrix().getTranslation())
                        .rotate(Quaternionf.createYXZ(vProp.value()))
                        .scale(matrix.matrix().getScale());
            case VectorProperty vProp && propName.equals("Scale") ->
                    Matrix4f.IDENTITY.translate(matrix.matrix().getTranslation())
                        .rotate(matrix.matrix().getRotationNormalized())
                        .scale(vProp.value());
            case null, default -> null;
        };

        if(newMatrix != null){
            var buf = ByteBuffer.allocate(16*4).order(ByteOrder.LITTLE_ENDIAN);
            buf.asFloatBuffer().put(newMatrix.getLinearArray());
            MapWriter.applyPatch(MapWriter.WritableObject.SCENE, matrix.address(), buf);
        }
    }

    private void delete() {
        var backup = FileUtil.createBackup(EditorState.getActiveMap(), EditorState.getActiveMap().getFileOfExtension("gsc"));

        try {
            Collections.reverse(associatedCommands);

            for (var command : associatedCommands) {
                var oneBeforeCommand = ((NU2MapData)EditorState.getActiveMap().levelData()).scene().renderCommandList().get(command.index() - 1);
                DisplayWriter.removeDisplayCommands(oneBeforeCommand, 2);
                EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));
            }

            FileUtil.clearBackup(backup);
        } catch(IOException e){
            SwingUtil.showErrorAlert("Failed while deleting static model", e);
            FileUtil.applyBackup(backup);
        }
    }

    @Override
    public Map<String, Runnable> getButtonActions() {
        return Map.of("Delete", this::delete);
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new EditorEntityProperty("Model", model, true, false, ""),
                new VectorProperty("Position", pos(), true, true),
                new VectorProperty("Scale", matrix.matrix().getScale(), false, true),
                new VectorProperty("Rotation", matrix.matrix().getRotationNormalized().toEuler(), false, true)
        );
    }
}
