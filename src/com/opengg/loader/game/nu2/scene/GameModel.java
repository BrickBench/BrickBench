package com.opengg.loader.game.nu2.scene;

import com.opengg.core.engine.Resource;
import com.opengg.core.math.Vector3f;
import com.opengg.core.model.io.ModelExporter;
import com.opengg.core.render.Renderable;
import com.opengg.core.render.internal.opengl.OpenGLRenderer;
import com.opengg.core.render.shader.ShaderController;
import com.opengg.core.render.shader.UniformContainer;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.MapEntity;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.rtl.RTLLight;
import com.opengg.loader.game.nu2.rtl.RTLLight.LightType;
import com.opengg.loader.game.nu2.scene.blocks.SceneExporter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record GameModel(String name,
                        List<GameModelPart> modelParts,
                        int modelAddress,
                        int materialListAddress,
                        int meshListAddress,
                        List<Integer> meshCommandIndices,
                        List<ComputedLight> activeLights,
                        List<ComputedLight> ambientLights,
                        boolean usesLights)
        implements Renderable, MapEntity<GameModel> {

    public GameModel(String name,
                    List<GameModelPart> modelParts,
                    int modelAddress,
                    int materialListAddress,
                    int meshListAddress,
                    List<Integer> meshCommandIndices) {
        this(name, modelParts, modelAddress, materialListAddress, meshListAddress, meshCommandIndices,
            new ArrayList<>(), new ArrayList<>(), modelParts.stream().anyMatch(m -> m.material().getDefines().get("LIGHTING_STAGE") != 0));
    }

    @Override
    public void render(){
        ShaderController.setUniform("useLights", usesLights ? 1 : 0);
        OpenGLRenderer.getOpenGLRenderer().setBackfaceCulling(false);
        
        if(usesLights && activeLights().isEmpty()){
            var lights = updateLights();
            activeLights().addAll(lights.stream().filter(l -> l.light.type() != LightType.AMBIENT).toList());
            ambientLights().addAll(lights.stream().filter(l -> l.light.type() == LightType.AMBIENT).toList());
        }

        if (usesLights) {
            useLights(activeLights, ambientLights);
        }

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

    private List<ComputedLight> updateLights(){
        var allLights = ((NU2MapData) EditorState.getActiveMap().levelData()).rtl().lights();

        var ambientLights = new ArrayList<ComputedLight>();
        var lights = new ArrayList<ComputedLight>();
        var pos = ((UniformContainer.Matrix4fContainer) ShaderController.getUniform("model")).contents().transform(new Vector3f());
        for(var light : allLights){
            var distance = light.pos().distanceTo(pos);
            if(light.falloff() > distance || light.type() == RTLLight.LightType.CAMDIR){
                var influence = light.type() == RTLLight.LightType.CAMDIR ? 2.0f : Math.max((distance - light.distance())/(light.falloff() - light.distance()), 1);
                if (light.type() == RTLLight.LightType.AMBIENT) {
                    ambientLights.add(new ComputedLight(light, influence));
                } else {
                    lights.add(new ComputedLight(light, influence));
                }
            }
        }

        ambientLights.sort(Comparator.comparingDouble(c1 -> c1.score));
        lights.sort(Comparator.comparingDouble(c1 -> c1.score));
        Collections.reverse(lights);
        Collections.reverse(ambientLights);
       
        ambientLights.addAll(lights);
        return ambientLights;
    }

    private void useLights(List<ComputedLight> lights, List<ComputedLight> ambientLights) {
        ShaderController.setUniform("LIGHTING_LIGHTS_COUNT", lights.size());
        ShaderController.setUniform("ambientColor", ambientLights.stream().map(a -> a.light().color().multiply(a.score() * a.light().multiplier())).reduce(new Vector3f(0), (a,b) -> a.add(b)));

        var pos = ((UniformContainer.Matrix4fContainer) ShaderController.getUniform("model")).contents().transform(new Vector3f());
        for(int i = 0; i < lights.size() && i < 3; i++){
            var light = lights.get(i);
            if (light.light().type() == RTLLight.LightType.CAMDIR || light.light().type() == RTLLight.LightType.DIRECTIONAL) {
                ShaderController.setUniform("light" + i + ".pos", light.light().rot());
            } else {
                ShaderController.setUniform("light" + i + ".pos", light.light().pos().subtract(pos));
            }

            ShaderController.setUniform("light" + i + ".color", light.light().color().multiply(light.score() * light.light().multiplier()));
        }

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

    private record ComputedLight(RTLLight light, float score){}
    public record GameModelPart(FileMaterial material, GSCMesh renderable, int sourceCommandIndex){}
}
