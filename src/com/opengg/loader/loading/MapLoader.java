package com.opengg.loader.loading;

import com.opengg.core.console.GGConsole;
import com.opengg.core.engine.Resource;
import com.opengg.loader.*;
import com.opengg.loader.editor.BottomRow;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.SceneFileLoader;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class MapLoader {
    public static Project.GameVersion CURRENT_GAME_VERSION = Project.GameVersion.LSW_TCS;

    /**
     * Load an instance of the given map.
     *
     * This will load the files in the map, autoidentifying the relevant engine by identifying which engine best matches the set of extensions
     * that show up in the map. This uses the extensions listed in {@link Project#EngineVersion}.
     */
    public static Project.MapInstance loadMapFromXml(Project project, MapXml mapXml) throws IOException {
        GGConsole.log("Parsing map " + mapXml.name() + " at " + mapXml.mapFilesDirectory() + " as " + project.game().NAME);

        try (var alert = SwingUtil.showLoadingAlert("Loading...", "Unpacking and loading map " + mapXml.name(), false)) {
            var files = new ArrayList<MapFile>();

            alert.setState("Patching files");

            MapIO.applyAllDiffs(mapXml, project);

            for(var mapFile : mapXml.files()){
                var filePath = mapXml.mapFilesDirectory().resolve(mapFile);

                var workingCopy = FileChannel.open(filePath,
                        project.isProject() ? new OpenOption[] { StandardOpenOption.READ, StandardOpenOption.WRITE } : new OpenOption[] { StandardOpenOption.READ } );
                files.add(new MapFile(filePath, workingCopy));
            }

            var mapData = switch (project.game().ENGINE) {
                case NU2 -> new NU2MapData(mapXml.name(),
                        mapXml,
                        new NU2MapData.SceneData(),
                        new NU2MapData.GizmoData(),
                        new NU2MapData.TerrainData(),
                        new NU2MapData.AIData(),
                        new NU2MapData.TxtData(),
                        new NU2MapData.GitData(),
                        new NU2MapData.RTLData());
                default -> null;
            };

            BottomRow.setLoadProgressMax(files.size());

            int progress = 0;
            for (var file : files) {
                BottomRow.setLoadState("Loading file: " + file.fileName.getFileName());
                BottomRow.setLoadProgress(progress++);
                GGConsole.log("Parsing file " + file.fileName.getFileName());
                alert.setState("Parsing " + file.fileName.getFileName());
                try {
                    mapData = mapData.loadFile(file);
                } catch (FileLoadException exception) {
                    GGConsole.error("Failed to load " + file.fileName + ": " + exception.getMessage());
                }
            }

            BottomRow.setLoadProgress(files.size());
            BottomRow.setLoadState("Loaded map " + mapXml.name());
            GGConsole.log("Done loading " + mapXml.name());

            return new Project.MapInstance(files, mapData);
        }
    }

    /**
     * Given a project and map, apply the diff for the file in the map.
     *
     * This unpacks the .diff file containing the compressed delta changes for the given file (if it exists), resulting
     * in the original uncompressed file. If the original uncompressed file already exists, do nothing. The new file
     * is placed in the same directory as the delta file.
     */
    public static void applyDiffIfNeeded(Project.GameVersion game, ProjectStructure structure, MapXml map, Path file) throws IOException {
        var filePath = map.mapFilesDirectory().resolve(file);
        var diffPath = map.mapFilesDirectory().resolve(file + ".diff");

        if (Files.exists(diffPath)) {
            var relativePath = MapIO.generateRelativePathForFile(structure, map, file);
            var ogPath = GameBaseManager.getBaseDirectoryOrPromptForNew(game).get().resolve(relativePath);

            if (Files.exists(ogPath) && !Files.exists(filePath)) {
                FileUtil.patch(ogPath, diffPath, filePath);
            }
        }
    }

    /**
     * 
     */
    public static void applyDiffForFile(Path diffPath, Path sourceGamePath, ProjectStructure structure, MapXml map) throws IOException {
        applyDiffForFile(diffPath, sourceGamePath, map.mapFilesDirectory(), structure, map);
    }

    /**
     * 
     */
    public static void applyDiffForFile(Path diffPath, Path sourceGamePath, Path outputPath, ProjectStructure structure, MapXml map) throws IOException {
        var nonPatchName = Path.of(diffPath.toString().replace(".diff", ""));
        var relativePath = MapIO.generateRelativePathForFile(structure, map, nonPatchName);
        var originalPath = sourceGamePath.resolve(relativePath);
        var newFile = outputPath.resolve(nonPatchName);

        if(Files.exists(newFile)) Files.delete(newFile);
        FileUtil.patch(originalPath,  map.mapFilesDirectory().resolve(diffPath), newFile);
    }

    /**
     * Recreate the map instance by triggering a reload of a single file extension type.
     */
    public static Project.MapInstance reloadIndividualFile(String ext) throws IOException {
        var currentInstance = EditorState.getActiveMap();
        var targetFile = currentInstance.getFileOfExtension(ext);

        return new Project.MapInstance(currentInstance.loadedFiles(), currentInstance.levelData().loadFile(targetFile));
    }

    /**
     * Determine the engine version of the given map. 
     *
     * This uses the extensions found in {@code Project#EngineVersion}, identifying which engine version has the most matching file extensions,
     * and returns that.
     *
     * Note, this does not identify the individual game version. This is done on a per-engine mechanism.
     */
    public static Project.EngineVersion determineEngineVersion(Path mapPath, String mapName) {
        return Arrays.stream(Project.EngineVersion.values()).max(Comparator.comparingInt(e -> findApplicableFiles(mapPath, mapName, e).size())).get();
    }

    /**
     * Returns all of the files in the given directory that are relevant to the given map name.
     */
    public static List<Path> findApplicableFiles(Path searchDirectory, String mapName, Project.EngineVersion engine){
        List<Path> filesToSearch;
        try {
            if (Files.isDirectory(searchDirectory)) {
                filesToSearch = new ArrayList<>();
                for (var file : Files.list(searchDirectory).toList()) {
                    if (Files.isRegularFile(file)) {
                        var name = FilenameUtils.removeExtension(file.getFileName().toString());
                        var ext = FilenameUtils.getExtension(file.getFileName().toString());
                        if (name.equalsIgnoreCase(mapName) ||
                                (name.equalsIgnoreCase(mapName + "_PC") && ext.equalsIgnoreCase("gsc"))) {
                            filesToSearch.add(file);
                        }
                    }
                }

                var aiDir = searchDirectory.resolve("AI");
                if (Files.exists(aiDir)) {
                    for (var file : Files.list(aiDir).toList()) {
                        if (Files.isRegularFile(file)) {
                            var name = FilenameUtils.removeExtension(file.getFileName().toString());
                            var ext = FilenameUtils.getExtension(file.getFileName().toString());
                            if (name.equalsIgnoreCase(mapName) || ext.equalsIgnoreCase("scp") || file.getFileName().toString().equalsIgnoreCase("script.txt")) {
                                filesToSearch.add(file);
                            }
                        }
                    }
                }
            } else {
                filesToSearch = List.of(searchDirectory);
            }
        } catch (IOException e) {
            GGConsole.exception(e);
            throw new RuntimeException(e);
        }

        filesToSearch = filesToSearch.stream()
                .filter(file -> engine.FILETYPES.contains(FilenameUtils.getExtension(file.toString()).toLowerCase()))
                .sorted(Comparator.comparingInt(f -> engine.FILETYPES.indexOf(FilenameUtils.getExtension(f.toString()).toLowerCase())))
                .collect(Collectors.toList());
        return filesToSearch;
    }

    /**
     * Load the global NU2 THINGS_PC.GSC file.
     */
    public static NU2MapData loadThings() throws IOException {
        GGConsole.log("Loading THINGS_PC.GSC");
        var fileName = Resource.getUserDataPath().resolve("THINGS_PC.GSC");
        var mapData = new NU2MapData("THINGS_PC.GSC", 
                new MapXml("THINGS_PC.GSC", fileName, MapXml.MapType.SINGLE_FILE,
                        List.of(fileName), Map.of(), Map.of()),
                new NU2MapData.SceneData(),
                new NU2MapData.GizmoData(),
                new NU2MapData.TerrainData(),
                new NU2MapData.AIData(),
                new NU2MapData.TxtData(),
                new NU2MapData.GitData(),
                new NU2MapData.RTLData());

        var file = FileChannel.open(fileName, StandardOpenOption.READ);

        mapData = mapData.loadFile(new MapFile(fileName, file));
        SceneFileLoader.initializeGraphicsData(mapData);

        return mapData;
    }

    public record MapFile(Path fileName, FileChannel channel){
        public String getExtension(){
            return FilenameUtils.getExtension(fileName().toString());
        }
    }
}
