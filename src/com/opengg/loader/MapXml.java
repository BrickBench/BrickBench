package com.opengg.loader;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.opengg.core.console.GGConsole;
import com.opengg.core.engine.OpenGG;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.editor.MapEditPanel;
import com.opengg.loader.loading.MapIO;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Stores the files and resources used for an open map.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public final class MapXml implements ProjectStructure.Node<MapXml> {
    private String name;
    private MapType mapType;
    @JsonIgnore private Path mapFilesDirectory;
    @JsonIgnore private final List<Path> files;
    private final Map<String, String> loadedModels;
    private final Map<String, String> loadedTextures;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MapXml(@JsonProperty("mapName") String mapName,
                  @JsonProperty("mapType" )MapType mapType,
                  @JsonProperty("associatedFiles") List<String> associatedFiles,
                  @JsonProperty("loadedModels") Map<String, String> loadedModels,
                  @JsonProperty("loadedTextures") Map<String, String> loadedTextures) {
        this(mapName, null, mapType, associatedFiles.stream().map(Path::of).collect(Collectors.toList()), loadedModels, loadedTextures);
    }

    public MapXml(String name, Path mapFilesDirectory, MapType mapType,
                  List<Path> files, Map<String, String> loadedModels, Map<String, String> loadedTextures) {
        this.name = name;
        this.mapFilesDirectory = mapFilesDirectory;
        this.mapType = mapType;
        this.files = files;
        this.loadedModels = loadedModels;
        this.loadedTextures = loadedTextures;
    }

    public void setName(String mapName) {
        this.name = mapName;
    }

    public void setMapFilesDirectory(Path mapFilesDirectory) {
        this.mapFilesDirectory = mapFilesDirectory;
    }

    /**
     * The root directory where the map files are stored.
     */
    public Path mapFilesDirectory() {
        return mapFilesDirectory;
    }

    public void setMapType(MapType mapType) {
        this.mapType = mapType;
    }

    public MapType mapType() {
        return mapType;
    }

    /**
     * The list of relative paths that represent the files this map uses.
     */
    public List<Path> files() {
        return files;
    }

    @JsonProperty
    public List<String> associatedFiles() {
        return files.stream().map(Path::toString).collect(Collectors.toList());
    }

    public Map<String, String> loadedModels() {
        return loadedModels;
    }

    public Map<String, String> loadedTextures() {
        return loadedTextures;
    }


    @Override
    public String name() {
        return name;
    }

    @Override
    public String path() {
        return "Maps/" + name();
    }

    @Override
    public String namespace() {
        return "Project";
    }

    @Override
    public void applyPropertyEdit(String propName, Property newValue) {
        switch (newValue) {
            case StringProperty sp && propName.equals("Name") -> {
                try (var exit = SwingUtil.showLoadingAlert("Renaming...", "Renaming map files...", false)){
                    MapIO.renameMap(EditorState.getProject(), this, sp.value());
                    EditorState.updateProject(EditorState.getProject());
                    OpenGG.asyncExec(() -> {
                        EditorState.addAndActivateProjectMap(this);
                    });
                } catch (IOException e) {
                    GGConsole.error("Failed to rename map");
                    GGConsole.exception(e);
                }
            }
            default -> {}
        };
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new StringProperty("Name", name(), true, 512),
                new CustomUIProperty("Map", new MapEditPanel(this), false)
        );
    }

    public enum MapType {
        NORMAL, INTRO, OUTRO, MIDTRO, STATUS, NEW_GAME, LOAD_GAME, TEST, SINGLE_FILE
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MapXml) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.mapFilesDirectory, that.mapFilesDirectory) &&
                Objects.equals(this.mapType, that.mapType) &&
                Objects.equals(this.files, that.files) &&
                Objects.equals(this.loadedModels, that.loadedModels) &&
                Objects.equals(this.loadedTextures, that.loadedTextures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, mapFilesDirectory, mapType, files, loadedModels, loadedTextures);
    }

    @Override
    public String toString() {
        return "MapXml[" +
                "mapName=" + name + ", " +
                "mapFile=" + mapFilesDirectory + ", " +
                "mapType=" + mapType + ", " +
                "files=" + files + ", " +
                "loadedModels=" + loadedModels + ", " +
                "loadedTextures=" + loadedTextures + ']';
    }
}
