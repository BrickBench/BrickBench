package com.opengg.loader.components;

import com.opengg.core.engine.OpenGG;
import com.opengg.core.math.Matrix4f;
import com.opengg.core.math.Vector3f;
import com.opengg.core.render.SceneRenderUnit;
import com.opengg.loader.EditorEntity;
import com.opengg.loader.FileUtil;
import com.opengg.loader.SwingUtil;
import com.opengg.loader.Util;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.scene.GameModel;
import com.opengg.loader.game.nu2.scene.DisplayImporter;
import com.opengg.loader.game.nu2.terrain.TerrainSerializer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TemporaryMeshComponent extends EditorEntityRenderComponent implements EditorEntity<TemporaryMeshComponent> {
    private GameModel currentModel;
    private boolean makeMesh;
    private boolean makeTerrain;

    public TemporaryMeshComponent(GameModel model) {
        super(model, new SceneRenderUnit.UnitProperties().shaderPipeline("ttNormal"));

        currentModel = model;

        this.setName("TemporaryModel_" + UUID.randomUUID());
    }

    private void cancel() {
        EditorState.selectObject(null);
    }

    private void apply() {
        if(!makeMesh && !makeTerrain){
            SwingUtil.showLoadingAlert("Mesh Import","You must specify whether to apply this as a scene model, terrain, or both",true);
            return;
        }
        OpenGG.asyncExec(() -> {
            var matrix = Matrix4f.IDENTITY.translate(getPosition()).rotate(getRotation()).scale(getScale());

            var hasGsc = EditorState.getActiveMap().hasFileOfExtension("gsc");
            var hasTer = EditorState.getActiveMap().hasFileOfExtension("ter");

            FileUtil.Backup backupGsc = null;
            FileUtil.Backup backupTer  = null;

            if (hasGsc && makeMesh) backupGsc = FileUtil.createBackup(EditorState.getActiveMap(), EditorState.getActiveMap().getFileOfExtension("gsc"));
            if (hasTer && makeTerrain) backupTer = FileUtil.createBackup(EditorState.getActiveMap(), EditorState.getActiveMap().getFileOfExtension("ter"));


            try (var exit = SwingUtil.showLoadingAlert("Generating...", "Generating meshes...", false)) {
                if(makeMesh){
                    if (!hasGsc) {
                        SwingUtil.showErrorAlert("This map does not have a scene file, cannot add model as a scene model");
                    } else {
                        DisplayImporter.addModelToCustomList(currentModel, matrix);
                    }
                }
                if(makeTerrain){
                    if (!hasTer) {
                        SwingUtil.showErrorAlert("This map does not have a terrain file, cannot add model as terrain");
                } else {
                        TerrainSerializer.importTerrain(currentModel, matrix);
                    }
                }

                if (hasGsc && makeMesh) FileUtil.clearBackup(backupGsc);
                if (hasTer && makeTerrain) FileUtil.clearBackup(backupTer);

            } catch (Exception e) {
                SwingUtil.showErrorAlert("Failed to apply model", e);
                if (hasGsc && makeMesh) FileUtil.applyBackup(backupGsc);
                if (hasTer && makeTerrain) FileUtil.applyBackup(backupTer);
            }
        });


        EditorState.selectObject(null);
    }

    @Override
    public Vector3f pos() {
        return getPosition();
    }

    @Override
    public String name() {
        return getName();
    }

    @Override
    public String path() {
        return "MeshPlacingObjects/" + name();
    }

    @Override
    public void applyPropertyEdit(String propName, Property newValue) {
        switch (newValue) {
            case EditorEntityProperty mop when propName.equals("Model") -> {
                var newModel = (GameModel) mop.value();
                this.currentModel = newModel;
                this.setRenderable(newModel);
            }
            case VectorProperty vp when propName.equals("Position") -> this.setPositionOffset(vp.value());
            case VectorProperty vp when propName.equals("Rotation") -> this.setRotationOffset(vp.value());
            case VectorProperty vp when propName.equals("Scale") -> this.setScaleOffset(vp.value());
            case BooleanProperty bp when propName.equals("Terrain") -> makeTerrain = bp.value();
            case BooleanProperty bp when propName.equals("Scene model") -> makeMesh = bp.value();
            case null, default -> {
            }
        }
    }

    @Override
    public Map<String, Runnable> getButtonActions() {
        return Util.createOrderedMapFrom(
                Map.entry("Apply", this::apply),
                Map.entry("Cancel", this::cancel));
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new EditorEntityProperty("Model", currentModel, true, true, "Render/Models"),
                new VectorProperty("Position", pos(), true, true),
                new VectorProperty("Rotation", getRotation().toEuler(), false, true),
                new VectorProperty("Scale", getScale(), false, true),
                new GroupProperty("Apply as", List.of(
                        new BooleanProperty("Terrain", makeTerrain, true),
                        new BooleanProperty("Scene model", makeMesh, true)),true));
    }
}
