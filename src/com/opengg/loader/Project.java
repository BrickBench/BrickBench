package com.opengg.loader;

import com.fasterxml.jackson.annotation.*;
import com.opengg.loader.loading.MapLoader;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static com.opengg.loader.Project.EngineVersion.*;

/**
 * Represents a BrickBench project.
 */
public record Project(boolean isProject,
                      GameVersion game,
                      String projectName,
                      Path projectXml,
                      Path projectSource,
                      Assets assets,
                      ProjectStructure structure
) {

    public List<ProjectResource> resources() {
        return structure.getNodesOfType(ProjectResource.class);
    }

    public List<MapXml> maps() {
        return structure.getNodesOfType(MapXml.class);
    }

    /**
     * An identifier for the game version. 
     * This may or may not be accurate depending on the loader used. For example, LIJ1 and LB1 files are indistinguishable.
     * Therefore, the exactness of the game used in the current project only works in the context of a non-readonly project.
     */
    public enum GameVersion {
        LIJ1(NU2, "lij1", "Lego Indiana Jones 1"),
        LB1(NU2, "lb1", "Lego Batman"),
        LSW_TCS(NU2, "lsw-tcs", "Lego Star Wars: The Complete Saga"),

        LHP1_4(NXG, "lhp1-4", "Lego Harry Potter: Years 1-4"),
        LSW3(NXG, "lsw3", "Lego Star Wars 3: The Clone Wars");

        /**
         * The engine this game uses.
         */
        public final EngineVersion ENGINE;
        /**
         * The short name for this game (eg. lsw-tcs).
         */
        public final String SHORT_NAME;
        /**
         * The user-readable name for this game.
         */
        public final String NAME;
        GameVersion(EngineVersion engine, String shortName, String name) { ENGINE = engine; SHORT_NAME = shortName; NAME = name; };
    }

    /**
     * An identifier for the engine version.
     */
    public enum EngineVersion {
        NU2("gsc", "txt", "ter", "ai2", "giz", "par", "ptl", "rtl", "bur", "gra", "git", "sfx", "anm", "scp"),
        NXG("nxg", "giz");

        /**
         * Represents the list of filetypes this engine version uses. This is used for the heuristic
         * that determines engine version when loading a map.
         */
        public final List<String> FILETYPES;
        EngineVersion(String... types) { 
            FILETYPES = List.of(types);
        }

        public GameVersion getDefaultGame() {
            return switch (this) {
                case NU2 -> GameVersion.LSW_TCS;
                case NXG -> GameVersion.LHP1_4;
            };
        }
    }

    /**
     * An instance of a loaded map. This contains both a list of the files that were used to create this map,
     * and the {@link MapData} that resulted from this map being loaded.
     */
    public record MapInstance(List<MapLoader.MapFile> loadedFiles, MapData levelData) {
        public MapLoader.MapFile getFileOfExtension(String ext) {
            return loadedFiles.stream().filter(f -> f.getExtension().equalsIgnoreCase(ext))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No file is loaded in the current map with the extension " + ext));
        }

        public boolean hasFileOfExtension(String ext) {
            return loadedFiles.stream().anyMatch(f -> f.getExtension().equalsIgnoreCase(ext));
        }

        /**
         * Close all open files for this instance.
         */
        public void dispose(){
            try {
                for(var file : loadedFiles){
                    file.channel().close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A container for the assets used for a map.
     */
    public record Assets(List<ModelDef> models, List<TextureDef> textures){

        public sealed interface AssetDef{}
        public record ModelDef(String name, String path) implements AssetDef{}
        public record TextureDef(String name, String path, @JsonIgnore Icon icon) implements AssetDef{
            public TextureDef(String name, String path) {
                this(name, path, null);
            }

            public TextureDef {}
        }
    }
}
