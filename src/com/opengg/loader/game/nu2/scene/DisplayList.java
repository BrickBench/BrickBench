package com.opengg.loader.game.nu2.scene;

import com.opengg.core.Configuration;
import com.opengg.core.engine.Resource;
import com.opengg.core.math.Matrix4f;
import com.opengg.core.model.io.ModelExporter;
import com.opengg.core.render.Renderable;
import com.opengg.core.render.internal.opengl.OpenGLRenderer;
import com.opengg.core.render.shader.ShaderController;
import com.opengg.loader.SwingUtil;
import com.opengg.loader.Util;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.commands.DisplayCommand;
import com.opengg.loader.game.nu2.scene.commands.MatrixCommandResource;
import com.opengg.loader.MapEntity;
import com.opengg.loader.game.nu2.scene.blocks.SceneExporter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record DisplayList(String name, int index, int order, List<DisplayCommand> commands, int firstBoundingBox, int boundingBoxCount, boolean isCustomList, int fileAddress, NU2MapData.SceneData sceneData) implements Renderable, MapEntity<DisplayList> {
    private List<SceneExporter.TransformableModelPart> getExportParts(){
        FileMaterial lastMaterial = null;
        Matrix4f lastMatrix = null;

        var meshes = new ArrayList<SceneExporter.TransformableModelPart>();
        for(var command : commands){
            if(command.command() instanceof MatrixCommandResource mc){
                lastMatrix = mc.matrix();
            }
            if(command.command() instanceof FileMaterial mat){
                lastMaterial = mat;
            }
            if(command.command() instanceof GSCMesh model){
                meshes.add(new SceneExporter.TransformableModelPart(new GameModel.GameModelPart(lastMaterial, model, 0), lastMatrix));
            }
        }

        return meshes;
    }

    private void export(){
        var name = EditorState.getActiveMap().levelData().name() + "_" + this.name();
        var dir = Resource.getUserDataPath().resolve(Path.of("export", "models", name));
        try (var exit = SwingUtil.showLoadingAlert("Exporting...", "Exporting model to " + dir.resolve(name) + ".obj", false)){
            var model = SceneExporter.convertToEngineModel(getExportParts(), name, true, false);
            ModelExporter.exportModel(model, dir, name, true);
        }
    }

    @Override
    public void render(){
        if(EditorState.CURRENT.shouldHighlight && EditorState.getSelectedObject().get() instanceof DisplayList obj && obj != this) {
            ShaderController.setUniform("muteColors", 1);
            return;
        }else{
            ShaderController.setUniform("muteColors", 0);
        }

        if(Boolean.parseBoolean(Configuration.get("wireframe"))) OpenGLRenderer.getOpenGLRenderer().setWireframe(true);

        ShaderController.setUniform("invertY", 0);
        ShaderController.setUniform("useLights", 0);
        OpenGLRenderer.getOpenGLRenderer().setDepthTest(true);
        OpenGLRenderer.getOpenGLRenderer().setDepthWrite(true);
        OpenGLRenderer.getOpenGLRenderer().setDepthFunc(OpenGLRenderer.DepthTestFunction.LEQUAL);
        OpenGLRenderer.getOpenGLRenderer().setWireframe(false);
        OpenGLRenderer.getOpenGLRenderer().setAlphaBlendEnable(true);
        OpenGLRenderer.getOpenGLRenderer().setBackfaceCulling(false);
        OpenGLRenderer.getOpenGLRenderer().setAlphaBlendFunction(OpenGLRenderer.AlphaBlendFunction.ADD);
        OpenGLRenderer.getOpenGLRenderer().setAlphaBlendSource(OpenGLRenderer.AlphaBlendSource.SRC_ALPHA,
                OpenGLRenderer.AlphaBlendSource.ONE_MINUS_SRC_ALPHA);
        ShaderController.setUniform("alphaCutoff", 0.1f);

        for(var command : commands){
            if(command.flags() == 0 || command.flags() == 3){
                command.run();
            }
        }

        OpenGLRenderer.getOpenGLRenderer().setDepthTest(true);
        OpenGLRenderer.getOpenGLRenderer().setDepthWrite(true);
        OpenGLRenderer.getOpenGLRenderer().setDepthFunc(OpenGLRenderer.DepthTestFunction.LEQUAL);
        OpenGLRenderer.getOpenGLRenderer().setWireframe(false);
        OpenGLRenderer.getOpenGLRenderer().setAlphaBlendEnable(true);
        OpenGLRenderer.getOpenGLRenderer().setAlphaBlendFunction(OpenGLRenderer.AlphaBlendFunction.ADD);
        OpenGLRenderer.getOpenGLRenderer().setAlphaBlendSource(OpenGLRenderer.AlphaBlendSource.SRC_ALPHA,
                OpenGLRenderer.AlphaBlendSource.ONE_MINUS_SRC_ALPHA);
        ShaderController.setUniform("alphaCutoff", 0.1f);
    }

    @Override
    public String path() {
        return "Render/DisplayLists/" + name;
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new StringProperty("Name", name(), false, 128),
                new IntegerProperty("Address", fileAddress, false),
                new IntegerProperty("Starting command address", commands.get(0).address(), false),
                new IntegerProperty("Load order", order, false),
                new IntegerProperty("Starting bounding box", firstBoundingBox, false),
                new IntegerProperty("Bounding box count", boundingBoxCount, false)
        );
    }

    @Override
    public Map<String, Runnable> getButtonActions() {
        return Util.createOrderedMapFrom(
                Map.entry("Delete", () -> DisplayWriter.deleteDisplayList(this)),
                Map.entry("Export", this::export));
    }
}
