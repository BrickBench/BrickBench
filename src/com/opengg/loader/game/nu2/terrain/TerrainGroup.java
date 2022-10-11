package com.opengg.loader.game.nu2.terrain;

import com.opengg.core.console.GGConsole;
import com.opengg.core.engine.Resource;
import com.opengg.core.math.Vector2f;
import com.opengg.core.math.Vector3f;
import com.opengg.loader.Util;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.scene.SpecialObject;
import com.opengg.loader.loading.MapWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record TerrainGroup(Vector3f position, int flag, int terrainPlatformIndex, int x28, List<TerrainMeshBlock> blocks,
                           Optional<SpecialObject> platformObject, int objectDefinitionAddress, int objectContentsAddress, int objectContentsSize, int index) implements TerrainObject{

    @Override
    public Vector3f pos() {
        return position;
    }

    @Override
    public String name() {
        return platformObject.map(s -> s.name() + "_platform").orElse("TerrainGroup_" + index);
    }

    @Override
    public String path() {
        return "Terrain/Meshes/" + name();
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new StringProperty("Name",name(), false, 128),
                new VectorProperty("Position",pos(), true, this.platformObject.isEmpty()),
                new EditorEntityProperty("Platform object", this.platformObject().orElse(null), true, false, "")
        );
    }

    @Override
    public void applyPropertyEdit(String propName, Property newValue) {
        switch (newValue) {
            case VectorProperty vp when propName.equals("Position") -> MapWriter.applyPatch(MapWriter.WritableObject.TERRAIN, objectDefinitionAddress + 8, vp.value().toLittleEndianByteBuffer());
            case null, default -> {
            }
        }
    }

    public void export() {
        var file = Resource.getUserDataPath().resolve(Path.of("export", "terrain",
                EditorState.getActiveMap().levelData().name() + "_" + this.name() + ".obj"));
        try {
            Files.createDirectories(file.getParent());
            TerrainExporter.writeObjFile(this, file);
        } catch (IOException e) {
             GGConsole.warn("Failed to export terrain " + file);
        }
    }


    @Override
    public Map<String, Runnable> getButtonActions() {
        return Util.createOrderedMapFrom(
                Map.entry("Remove", () -> TerrainSerializer.removeObject(this)),
                Map.entry("Export", this::export));
    }

    public static record TerrainMeshBlock(Vector2f min, Vector2f max, List<TerrainMeshFace> faces){}

    public static record TerrainMeshFace(Vector3f min, Vector3f max, Vector3f vec1, Vector3f vec2, Vector3f vec3, Vector3f vec4, Vector3f norm1, Vector3f norm2, int flag1, int flag2, int flag3, int flag4, int address){ }

    public boolean isTerrainPlatform(){
        return platformObject.isPresent();
    }

    public enum TerrainProperty{
        NONE(0, 0, new Vector3f(0)),
        UNKNOWN(10000, 10000, new Vector3f(1)),
        SLIP(16, 0, new Vector3f(1,1,0)),
        WATER(0, 1, new Vector3f(0,0,1)),
        INSTAKILL(1, 3, new Vector3f(1,0,0.3f)),
        FASTKILL(1, 0, new Vector3f(1,0,0)),
        SLOWKILL(0, 6, new Vector3f(0.7f,0.4f,0)),
        R2_SWAMP_WATER(0, 9, new Vector3f(0.44f,0.52f,0)),
        PUSHBLOCK_SURFACE(0, 8, new Vector3f(0.3f,0,0.8f)),
        EDGE(22, 0, new Vector3f(0.1f,0.2f,0.6f)),
        FORCE_MOVABLE(25, 0, new Vector3f(0.2f,0.8f,0.2f)),
        GAME_MOVABLE(24, 0, new Vector3f(0, 0.6f, 0.5f)),
        SPINNER_SIDE(31, 0, new Vector3f(0, 1, 0)),
        ICE(9, 0, new Vector3f(0.6f, 0.94f, 0.94f)),
        METAL_OBJECT(27, 0, new Vector3f(0.55f,0.55f,0.55f)),
        ENERGY_WALL(12, 0, new Vector3f(0.25f,0.87f,0.87f)),
        REFLECTIVE_FLOOR(2, 0, new Vector3f(0)), //probably not  useful
        MAP_CUSTOM_FLOOR(15, 0,  new Vector3f(0.9f, 0.3f, 0.9f)),
        BUTTON(6, 0, new Vector3f(0.298f, 0.733f, 0.09f)),
        STOP_HOVER(26, 0, new Vector3f(0.248f, 0.001f, 0.001f)),
        TEST(0, 0, new Vector3f(1,0,1));

        public final int flag1;
        public final int flag2;

        public final Vector3f color;

        TerrainProperty(int flag1, int flag2, Vector3f color){
            this.flag1 = flag1;
            this.flag2 = flag2;
            this.color = color;
        }
    }
}
