package com.opengg.loader.game.nu2;

import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.world.components.WorldObject;
import com.opengg.loader.components.EditorEntityRenderComponent;
import com.opengg.loader.game.nu2.ai.LocatorComponent;
import com.opengg.loader.components.MapComponent;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.ai.CreatureSpawnComponent;
import com.opengg.loader.game.nu2.ai.WorldTriggerComponent;
import com.opengg.loader.game.nu2.gizmo.GizmoManagerComponent;
import com.opengg.loader.game.nu2.rtl.RTLLightComponent;
import com.opengg.loader.game.nu2.scene.SceneFileLoader;
import com.opengg.loader.game.nu2.scene.SpecialObjectComponent;
import com.opengg.loader.game.nu2.scene.SplineComponent;
import com.opengg.loader.game.nu2.terrain.InfiniteWallComponent;
import com.opengg.loader.game.nu2.terrain.TerrainGroupComponent;
import com.opengg.loader.loading.MapWriter;

import java.util.ArrayList;
import java.util.List;

public class NU2MapComponent extends MapComponent<NU2MapData> {
    private final WorldObject terrainGroups = new WorldObject("terrainGroups");
    private final WorldObject walls = new WorldObject("walls");
    private final WorldObject triggers = new WorldObject("triggers");
    private final WorldObject portalSet = new WorldObject("portalSet");
    private final WorldObject doors = new WorldObject("doors");
    private final WorldObject gizmos = new WorldObject("gizmos");
    private final WorldObject splines = new WorldObject("splines");
    private final WorldObject specialObjects = new WorldObject("specialObjects");
    private final WorldObject aiLocators = new WorldObject("aiLocators");
    private final WorldObject aiSpawn = new WorldObject("aiSpawn");
    private final WorldObject staticMesh = new WorldObject("staticMesh");
    private final WorldObject rtlLights = new WorldObject("rtlLights");

    private NU2MapData mapData;
    public static List<byte[]> connections = new ArrayList<>();

    public NU2MapComponent(NU2MapData mapData){
        this.setName("map");
        this.updateMapData(mapData);
        this.setUpdateEnabled(false);
        EditorState.CURRENT.currentMap = this;

        this.attach(terrainGroups).attach(walls).attach(triggers).attach(portalSet).attach(doors)
                .attach(gizmos).attach(splines).attach(specialObjects).attach(aiLocators)
                .attach(aiSpawn).attach(staticMesh).attach(rtlLights);

        updateSceneFile();
        updateTextData();
        updateRTLData();
    }

    @Override
    public void updateMapData(NU2MapData mapData){
        this.mapData = mapData;
    }

    public void updateTextData(){
        doors.removeAll();

        mapData.txt().doors().forEach(door -> doors.attach(new DoorComponent(door)));
    }

    public void updateGizmoData(){
        gizmos.removeAll();

        GizmoManagerComponent gizManager = new GizmoManagerComponent();
        mapData.gizmo().gizmos().forEach(gizManager::addGizmo);
        gizmos.attach(gizManager);
    }

    public void updateAIData(){
        aiSpawn.removeAll();
        triggers.removeAll();
        aiLocators.removeAll();

        mapData.ai().creatureSpawns().forEach(creature -> aiSpawn.attach(new CreatureSpawnComponent(creature)));
        mapData.ai().triggers().forEach(trigger -> triggers.attach(new WorldTriggerComponent(trigger)));
        mapData.ai().aiLocators().forEach(locator -> aiLocators.attach(new LocatorComponent(locator)));
    }
    public void updateRTLData(){
        rtlLights.removeAll();
        mapData.rtl().lights().forEach(rtlLight -> rtlLights.attach(new RTLLightComponent(rtlLight)));
    }

    public void updateSceneFile(){
        SceneFileLoader.initializeGraphicsData(mapData);
        updateSpecialObjects();
        updateSplinesPortals();
        updateStaticMesh();
    }

    public void updateSpecialObjects(){
        specialObjects.removeAll();
        mapData.scene().specialObjects().forEach(specialObject -> specialObjects.attach(new SpecialObjectComponent(specialObject, mapData)));

        updateTerrain();
        updateAIData();
        updateGizmoData();
    }

    public void updateStaticMesh(){
        staticMesh.removeAll();
        mapData.scene().displayLists().forEach(s ->
                staticMesh.attach(new EditorEntityRenderComponent(s, s,
                        new SceneRenderUnit.UnitProperties().shaderPipeline("ttNormal"))));

    }

    public void updateSplinesPortals(){
        splines.removeAll();
        //  portalSet.removeAll();

        mapData.scene().splines().forEach(spline -> splines.attach(new SplineComponent(spline)));
        // mapData.scene().portalList().forEach(portal -> portalSet.attach(new PortalComponent(portal)));
    }

    public void updateTerrain(){
        terrainGroups.removeAll();
        walls.removeAll();

        mapData.terrain().terrainGroups().forEach(terrainGroup -> terrainGroups.attach(new TerrainGroupComponent(terrainGroup)));
        mapData.terrain().infiniteWallPoints().forEach(wall -> walls.attach(new InfiniteWallComponent(wall)));
    }

    @Override
    public void updateItemType(MapWriter.WritableObject type) {

        switch (type) {
            case SCENE -> {
                updateSceneFile();
                updateTextData();
                updateTerrain();
            }
            case SPLINE -> {
                updateSceneFile();
                updateTextData();
            }
            case TERRAIN -> {
                updateTerrain();
                updateAIData();
            }
            case GIZMO -> {
                updateGizmoData();
            }
            case TRIGGER, AI_LOCATOR, CREATURE_SPAWN -> {
                updateAIData();
            }
            case DOOR -> {
                updateTextData();
            }
            case LIGHTS -> {
                updateRTLData();
            }
        }
    }

    public NU2MapData getMapData() {
        return mapData;
    }
}
