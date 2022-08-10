package com.opengg.loader.game.nu2.scene;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.opengg.core.engine.Resource;
import com.opengg.core.model.io.ModelExporter;
import com.opengg.core.render.Renderable;
import com.opengg.core.render.internal.opengl.OpenGLRenderer;
import com.opengg.core.render.shader.ShaderController;
import com.opengg.loader.MapEntity;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.scene.blocks.SceneExporter;

public record GameModel(String name,
                        List<GameModelPart> modelParts,
                        int modelAddress,
                        int materialListAddress,
                        int meshListAddress,
                        List<Integer> meshCommandIndices)
        implements Renderable, MapEntity<GameModel> {

    @Override
    public void render(){
        OpenGLRenderer.getOpenGLRenderer().setBackfaceCulling(false);

        for(var command : modelParts){
            if(command.renderable != null) {
                command.material.apply();
                command.renderable.render();
            }
        }

        OpenGLRenderer.getOpenGLRenderer().setDepthTest(true);
        OpenGLRenderer.getOpenGLRenderer().setDepthWrite(true);
        OpenGLRenderer.getOpenGLRenderer().setDepthFunc(OpenGLRenderer.DepthTestFunction.LEQUAL);
        OpenGLRenderer.getOpenGLRenderer().setAlphaBlendEnable(true);
        OpenGLRenderer.getOpenGLRenderer().setAlphaBlendFunction(OpenGLRenderer.AlphaBlendFunction.ADD);
        OpenGLRenderer.getOpenGLRenderer().setAlphaBlendSource(OpenGLRenderer.AlphaBlendSource.SRC_ALPHA,
                OpenGLRenderer.AlphaBlendSource.ONE_MINUS_SRC_ALPHA);

        ShaderController.setUniform("alphaCutoff", 0.1f);
    }

    
    public void export(){
        var name = EditorState.getActiveMap().levelData().name() + "_" + this.name();
        var dir = Resource.getUserDataPath().resolve(Path.of("export", "models", name));

        var model = SceneExporter.convertToEngineModel(this, true);
        ModelExporter.exportModel(model, dir, name, true);
    }

    @Override
    public Map<String, Runnable> getButtonActions() {
        return Map.of("Export", this::export);
    }

    @Override
    public String path() {
        return "Render/Models/" + name();
    }

    @Override
    public List<Property> properties() {
        var meshList = modelParts().stream()
                .filter(m -> m.renderable() != null)
                .map(m -> (MapEntity.Property)new TupleProperty("E",
                        List.of(new EditorEntityProperty(m.material().name(), m.material(), false, true, "Render/Materials/"),
                                new EditorEntityProperty(m.renderable().name(), m.renderable(), false, true, "Render/Meshes/"))))
                .collect(Collectors.toList());

        return List.of(
                new ListProperty("Meshes", meshList, true)
        );
    }

    public record GameModelPart(FileMaterial material, GSCMesh renderable, int sourceCommandIndex){}
}
