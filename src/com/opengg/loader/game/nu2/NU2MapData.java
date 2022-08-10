package com.opengg.loader.game.nu2;

import com.opengg.core.Configuration;
import com.opengg.core.console.GGConsole;
import com.opengg.core.math.FastInt;
import com.opengg.loader.EditorEntity;
import com.opengg.loader.MapData;
import com.opengg.loader.MapXml;
import com.opengg.loader.Project;
import com.opengg.loader.components.MapComponent;
import com.opengg.loader.game.nu2.gizmo.GitNode;
import com.opengg.loader.game.nu2.gizmo.Gizmo;
import com.opengg.loader.game.nu2.rtl.RTLLight;
import com.opengg.loader.game.nu2.scene.*;
import com.opengg.loader.game.nu2.ai.CreatureSpawn;
import com.opengg.loader.game.nu2.ai.AILocator;
import com.opengg.loader.game.nu2.ai.WorldTrigger;
import com.opengg.loader.game.nu2.terrain.InfiniteWall;
import com.opengg.loader.game.nu2.terrain.TerrainGroup;
import com.opengg.loader.game.nu2.ai.AILocatorSet;
import com.opengg.loader.loading.FileLoadException;
import com.opengg.loader.loading.MapLoader;
import com.opengg.loader.game.nu2.ai.AI2Loader;
import com.opengg.loader.game.nu2.gizmo.GitLoader;
import com.opengg.loader.game.nu2.gizmo.GizLoader;
import com.opengg.loader.game.nu2.gizmo.GizWriter;
import com.opengg.loader.game.nu2.scene.commands.DisplayCommandResource;
import com.opengg.loader.game.nu2.scene.commands.DisplayCommand;
import com.opengg.loader.game.nu2.rtl.RTLLoader;
import com.opengg.loader.game.nu2.scene.SceneFileLoader;
import com.opengg.loader.game.nu2.terrain.TerrainLoader;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.Collectors;

public record NU2MapData(String name,
                         MapXml xmlData,
                         SceneData scene,
                         GizmoData gizmo,
                         TerrainData terrain,
                         AIData ai,
                         TxtData txt,
                         GitData git,
                         RTLData rtl) implements MapData {

    public Optional<SpecialObject> getSpecialObjectByName(String name){
        return scene.specialObjects.stream().filter(s -> s.name().equals(name)).findFirst();
    }

    public Optional<Spline> getSplineByName(String name){
        return scene.splines.stream().filter(s -> s.name().equals(name)).findFirst();
    }

    public Optional<Gizmo> getGizmoByName(String name){
        return gizmo.gizmos.stream().filter(s -> s.name().equals(name)).findFirst();
    }

    public Optional<AILocator> getLocatorByName(String name){
        return ai.aiLocators.stream().filter(s -> s.name().equals(name)).findFirst();
    }

    public Optional<WorldTrigger> getTriggerByName(String name){
        return ai.triggers.stream().filter(s -> s.name().equals(name)).findFirst();
    }

    public Optional<Door> getDoorByName(String name){
        return txt.doors.stream().filter(s -> s.name().equals(name)).findFirst();
    }

    public record SceneData(
                             List<SpecialObject> specialObjects,
                             List<Portal> portalList,
                             List<Spline> splines,
                             List<IABLObject.IABLBoundingBox> boundingBoxes,
                             Map<Integer, FileMaterial> materials,
                             Map<Integer, GSCMesh> meshes,
                             List<DisplayCommand> renderCommandList,
                             Map<Integer, DisplayCommandResource<?>> uniqueRenderCommands,
                             Map<String, Block> blocks,
                             Map<Integer,FileTexture> texturesByRealIndex,
                             List<DisplayList> displayLists,
                             List<GameModel> gameModels,
                             List<ModelInstance> modelInstances,
                             List<FileTexture> textures,
                             List<GameBuffer> gscVertexBuffers,
                             List<GameBuffer> gscIndexBuffers,
                             List<Short> DINIData,
                             Map<Integer, List<Integer>> vertexBuffersBySize,
                             FastInt gscRenderableListFromGSNH,
                             FastInt gscRenderableEndFromGSNH,
                             FastInt texIndexListAddressFromGSNH,
                             FastInt texMetaListAddressFromGSNH,
                             FastInt materialListAddressFromGSNH,
                             FastInt gscRenderableListLen,
                             FastInt modelSizeListStart,
                             FastInt textureCount){
        public SceneData(){
            this(
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new LinkedHashMap<>(),
                    new LinkedHashMap<>(),
                    new ArrayList<>(),
                    new LinkedHashMap<>(),
                    new LinkedHashMap<>(),
                    new LinkedHashMap<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new LinkedHashMap<>(),
                    new FastInt(),
                    new FastInt(),
                    new FastInt(),
                    new FastInt(),
                    new FastInt(),
                    new FastInt(),
                    new FastInt(),
                    new FastInt()
            );
        }

        public record Block(int address, int size){}
    }

    public record GizmoData(Map<String, GizWriter.GizmoTypeData> gizmoVersions,
                            List<Gizmo> gizmos){
        public GizmoData(){
            this(new LinkedHashMap<>(), new ArrayList<>());
        }

        public List<Gizmo.GizPanel> getPanels() {
            return gizmos.stream().filter(s -> s instanceof Gizmo.GizPanel).map(g -> (Gizmo.GizPanel)g).collect(Collectors.toList());
        }

        public List<Gizmo.GizPickup> getPickups() {
            return gizmos.stream().filter(s -> s instanceof Gizmo.GizPickup).map(g -> (Gizmo.GizPickup)g).collect(Collectors.toList());
        }

        public List<Gizmo.Lever> getLevers() {
            return gizmos.stream().filter(s -> s instanceof Gizmo.Lever).map(g -> (Gizmo.Lever)g).collect(Collectors.toList());
        }

        public List<Gizmo.HatMachine> getHatMachines() {
            return gizmos.stream().filter(s -> s instanceof Gizmo.HatMachine).map(g -> (Gizmo.HatMachine)g).collect(Collectors.toList());
        }

        public List<Gizmo.PushBlock> getPushBlocks() {
            return gizmos.stream().filter(s -> s instanceof Gizmo.PushBlock).map(g -> (Gizmo.PushBlock)g).collect(Collectors.toList());
        }

        public List<Gizmo.ZipUp> getZipUps() {
            return gizmos.stream().filter(s -> s instanceof Gizmo.ZipUp).map(g -> (Gizmo.ZipUp)g).collect(Collectors.toList());
        }
    }

    public record TerrainData(FastInt workingAreaEnd,
                              FastInt objectAreaEnd,
                              List<InfiniteWall> infiniteWallPoints,
                              List<TerrainGroup> terrainGroups){
        public TerrainData(){
            this(new FastInt(), new FastInt(), new ArrayList<>(), new ArrayList<>());
        }
    }

    public record TxtData(List<Door> doors,Map<String,Float> settingsMap){
        public TxtData(){
            this(new ArrayList<>(),new HashMap<>());
        }
    }

    public record AIData(List<WorldTrigger> triggers,
                         List<AILocator> aiLocators,
                         List<AILocatorSet> aiLocatorSets,
                         List<CreatureSpawn> creatureSpawns,
                         FastInt version,
                         FastInt locatorAddress,
                         FastInt locatorSetAddress,
                         FastInt creatureStartAddress,
                         FastInt creatureEndAddress){
        public AIData(){
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),new ArrayList<>(), new FastInt(), new FastInt(), new FastInt(), new FastInt(), new FastInt());
        }
    }

    public record GitData(Map<Integer, GitNode> gitNodes){
        public GitData(){
            this(new LinkedHashMap<>());
        }
    }
    
    public record RTLData(List<RTLLight> lights){
        public RTLData(){
            this(new ArrayList<>());
        }
    }

    @Override
    public MapComponent<?> createEngineComponent() {
        return new NU2MapComponent(this);
    }

    @Override
    public NU2MapData loadFile(MapLoader.MapFile file) throws IOException {
        file.channel().position(0);
        var loadedFile = ByteBuffer.allocate((int) file.channel().size());
        file.channel().read(loadedFile);
        loadedFile.rewind();

        var newMapData = switch (file.getExtension().toLowerCase()) {
            case "txt" -> {
                if (FilenameUtils.removeExtension(file.fileName().getFileName().toString()).equalsIgnoreCase(name)) {
                    yield new NU2MapData(this.name(), this.xmlData(), this.scene(), this.gizmo(), this.terrain(), this.ai(), new NU2MapData.TxtData(), this.git(), this.rtl());
                } else {
                    yield this;
                }
            }
            case "giz" -> new NU2MapData(this.name(), this.xmlData(), this.scene(), new NU2MapData.GizmoData(), this.terrain(), this.ai(), this.txt(), this.git(), this.rtl());
            case "git" -> new NU2MapData(this.name(), this.xmlData(), this.scene(), this.gizmo(), this.terrain(), this.ai(), this.txt(), new NU2MapData.GitData(), this.rtl());
            case "ai2" -> new NU2MapData(this.name(), this.xmlData(), this.scene(), this.gizmo(), this.terrain(), new NU2MapData.AIData(), this.txt(), this.git(), this.rtl());
            case "ter", "ctr" -> new NU2MapData(this.name(), this.xmlData(), this.scene(), this.gizmo(), new NU2MapData.TerrainData(), this.ai(), this.txt(), this.git(), this.rtl());
            case "gsc" -> new NU2MapData(this.name(), this.xmlData(), new NU2MapData.SceneData(), this.gizmo(), this.terrain(), this.ai(), this.txt(), this.git(), this.rtl());
            case "rtl" -> new NU2MapData(this.name(), this.xmlData(), this.scene(), this.gizmo(), this.terrain(), this.ai(), this.txt(), this.git(), new NU2MapData.RTLData());

            default -> this;
        };

        try {
            switch (file.getExtension().toLowerCase()) {
                case "ctr" -> TerrainLoader.load(loadedFile, newMapData);
                case "ter" -> TerrainLoader.load(loadedFile.order(ByteOrder.LITTLE_ENDIAN), newMapData);
                case "gsc" -> SceneFileLoader.load(loadedFile, newMapData);
                case "ai2" -> AI2Loader.load(loadedFile, newMapData);
                case "giz" -> {
                    if (MapLoader.CURRENT_GAME_VERSION == Project.GameVersion.LSW_TCS && !Configuration.get("gscFormat").equalsIgnoreCase("LIJ/Batman"))
                        GizLoader.load(loadedFile, newMapData);
                }
                case "git" -> GitLoader.loadGit(loadedFile, newMapData);
                case "txt" -> {
                    if (FilenameUtils.removeExtension(file.fileName().getFileName().toString()).equalsIgnoreCase(name)) {
                        TxtLoader.load(loadedFile, newMapData);
                    }
                }
                case "rtl" -> RTLLoader.load(loadedFile, newMapData);
                default -> GGConsole.log("Ignoring file extension " + file.getExtension());
            }
        } catch (RuntimeException e) {
            throw new FileLoadException(e.getMessage(), e);
        }

        return newMapData;
    }

    @Override
    public Map<String, EditorEntity<?>> getNamespace() {
        var namespace = new LinkedHashMap<String, EditorEntity<?>>();
        var allLists = List.of(
                this.ai().aiLocators(),
                this.ai().creatureSpawns(),
                this.ai().triggers(),
                this.ai().aiLocatorSets(),
                this.git().gitNodes().values(),
                this.txt().doors(),
                this.gizmo().gizmos(),
                this.scene().materials().values(),
                this.scene().specialObjects(),
                this.scene().modelInstances(),
                this.scene().gameModels(),
                this.scene().displayLists(),
                this.scene().textures(),
                this.scene().portalList(),
                this.scene().uniqueRenderCommands().values(),
                this.scene().splines(),
                this.rtl().lights(),
                this.terrain().terrainGroups(),
                this.terrain().infiniteWallPoints()
        );

        for(var list : allLists){
            for(var obj : list){
                namespace.put(obj.path(), obj);
            }
        }

        return namespace;
    }
}
