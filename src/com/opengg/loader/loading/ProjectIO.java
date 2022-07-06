package com.opengg.loader.loading;

import com.opengg.core.console.GGConsole;
import com.opengg.core.engine.Resource;
import com.opengg.core.model.io.AssimpModelLoader;
import com.opengg.core.util.SystemUtil;
import com.opengg.loader.*;
import com.opengg.loader.Project.EngineVersion;
import com.opengg.loader.ProjectStructure.Node;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.editor.windows.ExportDialog;
import com.opengg.loader.game.nu2.Area;
import com.opengg.loader.game.nu2.AreaIO;
import com.opengg.loader.game.nu2.AreasTXTParser;
import com.opengg.loader.game.nu2.AreasTXTWriter;
import com.opengg.loader.game.nu2.LevelsTXTParser;
import com.opengg.loader.game.nu2.LevelsTXTWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ProjectIO {
    /**
     * Save the project archive for the given project to the given output file.
     */
    public static boolean saveProject(Project project, Path output) {
        var filesToSave = new ArrayList<Path>();
        
        GGConsole.log("Saving " + project.projectName());

        try (var exit = SwingUtil.showLoadingAlert("Saving...", "Saving project...", false)){
            for (var map : project.maps()) {
                var otherSaveFiles = MapIO.saveMapAndGetFiles(map, project, project.projectXml().resolveSibling(Path.of(map.name())));
                filesToSave.addAll(otherSaveFiles);
            }

            for (var texture : project.assets().textures()) {
                filesToSave.add(project.projectXml().resolveSibling(texture.path()));
            }

            for (var model : project.assets().models()) {
                var allFiles = Files.walk(project.projectXml().resolveSibling(Path.of(model.path()).getParent()))
                        .filter(Files::isRegularFile).toList();
                filesToSave.addAll(allFiles);
            }

            for (var resource : project.resources()) {
                var allFiles = Files.walk(project.projectXml().resolveSibling(Path.of(resource.path()).getParent()))
                        .filter(Files::isRegularFile).toList();

                filesToSave.addAll(allFiles);
            }

            ProjectXmlIO.writeProjectFile(project, project.projectXml());
            filesToSave.add(project.projectXml());

            FileUtil.compressFolder(project.projectXml().getParent(), output.toAbsolutePath(), filesToSave);

            return true;
        } catch (IOException e) {
            SwingUtil.showErrorAlert("Failed to save project", e);
            return false;
        }
    }

    /**
     * Load a project from the given project archive.
     */
    public static Project loadProject(Path projectFile) throws IOException {
        var proj = Resource.getUserDataPath().resolve("project");

        GGConsole.log("Opening project archive " + projectFile);
        FileUtils.deleteDirectory(proj.toFile());
        FileUtil.unzip(projectFile.toString(), proj.toString());
        var project = ProjectXmlIO.readProjectFile(Resource.getUserDataPath().resolve(Path.of("project", "project.xml")), projectFile);
        var gameFile = GameBaseManager.getBaseDirectoryOrPromptForNew(project.game());
        if (gameFile.isEmpty()) return null;

        GGConsole.log("Loaded project " + project.projectName());

        return project;
    }

    /**
     * Export the given project to the given export directory.
     *
     * This creates a copy of the project that is ready for use ingame. Depending on the export type selected,
     * merging may be necessary for the global files.
     */
    public static void exportProject(Project project, Path exportDirectory, ExportDialog.GlobalTextExportType exportType) throws IOException {
        if(exportType != ExportDialog.GlobalTextExportType.NONE){
            if (project.game().ENGINE == Project.EngineVersion.NU2) {
                var levelsDir = exportDirectory.resolve("LEVELS");
                Files.createDirectories(levelsDir);

                var areasEntries = new ArrayList<Area.AreaGlobalProperties>();
                var levelsEntries = new ArrayList<LevelsTXTParser.LevelTXTEntry>();

                if(exportType == ExportDialog.GlobalTextExportType.FULL){
                    var areasEntriesParser = new AreasTXTParser();
                    areasEntriesParser.parseFile(Path.of(GameBaseManager.getBaseDirectoryOrPromptForNew(project.game()).get().toString(), "LEVELS", "AREAS.TXT"));
                    areasEntries.addAll(areasEntriesParser.getParsedProperties());

                    var levelsEntriesParser = new LevelsTXTParser();
                    levelsEntriesParser.parseFile(Path.of(GameBaseManager.getBaseDirectoryOrPromptForNew(project.game()).get().toString(), "LEVELS", "LEVELS.TXT"));
                    levelsEntries.addAll(levelsEntriesParser.getEntries());
                }

                for(var map : project.maps()){
                    if(levelsEntries.stream().noneMatch(m -> m.name().equalsIgnoreCase(map.name()))){
                        if(map.mapType() != MapXml.MapType.SINGLE_FILE) levelsEntries.add(MapIO.createLevelsTXTEntry(project, map));
                    }
                }

                for(var area : project.structure().getNodesOfType(Area.class)){
                    var newEntry = AreaIO.repairAndGetGlobalProperties(project, area);
                    if(areasEntries.stream().noneMatch(m -> m.file.equalsIgnoreCase(area.name()))){
                        areasEntries.add(newEntry);
                    }else{
                        var old = areasEntries.stream().filter(m -> m.file.equalsIgnoreCase(area.name())).findFirst().get();
                        areasEntries.set(areasEntries.indexOf(old), newEntry);
                    }
                }

                var levelsTxtName = exportType == ExportDialog.GlobalTextExportType.STUB ?
                        levelsDir.resolve("LEVELS_" + project.projectName() + ".TXT") :
                        levelsDir.resolve("LEVELS.TXT");

                var areasTxtName = exportType == ExportDialog.GlobalTextExportType.STUB ?
                        levelsDir.resolve("AREAS_" + project.projectName() + ".TXT") :
                        levelsDir.resolve("AREAS.TXT");

                LevelsTXTWriter.write(levelsTxtName, levelsEntries);
                AreasTXTWriter.write(areasTxtName, areasEntries);
            }
        }
        walkStructureForExport(project, project.structure().root(), exportDirectory);
    }

    private static void walkStructureForExport(Project project, ProjectStructure.Node<?> node, Path currentDirectory) throws IOException {
        if (node.name().equals("root") && node instanceof ProjectStructure.FolderNode fn) {
            for (var child : fn.children()) {
                walkStructureForExport(project, child, currentDirectory);
            }
            return;
        }

        switch (node) {
            case ProjectStructure.FolderNode fn -> {
                var newDirectory = currentDirectory.resolve(fn.name());
                Files.createDirectories(newDirectory);
                for (var child : fn.children()) {
                    walkStructureForExport(project, child, newDirectory);
                }
            }
            case Area an -> {
                var levelTxt = currentDirectory.resolve(Path.of(an.name(), an.name() + ".txt"));
                if (Files.exists(levelTxt))
                    Files.delete(levelTxt);
                Files.createDirectories(currentDirectory.resolve(an.name()));
                Files.writeString(levelTxt, AreaIO.generateLevelTXTText(an));
                for (var map : an.maps()) {
                    walkStructureForExport(project, map, currentDirectory.resolve(Path.of(an.name(), AreaIO.getFolderFor(an, map))));
                }
            }
            case MapXml map -> {
                MapIO.exportMap(project, currentDirectory, map);
            }
            case ProjectResource rn -> {
                var file = rn.path();
                var name = rn.name();

                var absFile = project.projectXml().resolveSibling(file);

                var newFile = currentDirectory.resolve(name);
                if (Files.isDirectory(Path.of(file))) {
                    FileUtils.copyDirectory(absFile.toFile(), newFile.toFile());
                } else {
                    Files.copy(absFile, newFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * Create and run a test instance of the given project.
     */
    public static void testProject(Project project) {
        if (SystemUtil.IS_LINUX) {
            SwingUtil.showErrorAlert("This feature is currently not available on Linux");
            return;
        }

        if (project.game() != Project.GameVersion.LSW_TCS) {
            SwingUtil.showErrorAlert("This feature is currently only available for Lego Star Wars: TCS");
            return;
        }

        try (var exit = SwingUtil.showLoadingAlert("Generating test...", "Generating test copy of TCS...", false)) {
            var cleanFolder = GameBaseManager.getBaseDirectoryOrPromptForNew(project.game()).get();
            var testFolder = cleanFolder.resolveSibling("lswtcs_test");

            FileUtils.deleteDirectory(testFolder.toFile());
            Files.createDirectories(testFolder);

            FileUtil.generateLinkTree(cleanFolder, testFolder, false, "pak");
            exportProject(project, testFolder, ExportDialog.GlobalTextExportType.FULL);

            exit.close();

            var testProcess = new ProcessBuilder().command(Path.of(testFolder.toString(), "LEGOStarWarsSaga.exe").toString()).directory(testFolder.toFile()).start();

            var shutdownHook = new Thread(() -> {
                try {
                    testProcess.destroy();
                    if (Files.exists(testFolder))
                        FileUtils.deleteDirectory(testFolder.toFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (IOException e) {
            SwingUtil.showErrorAlert("Failed to create test project instance", e);
        }
    }

    /**
     * Open a map/area as a readonly project.
     */
    public static Project createReadOnlyProject(Path projectFile) throws IOException {
        EngineVersion engineVersion = EngineVersion.NU2;
        List<Node<?>> nodes = new ArrayList<>();

        if (Files.isRegularFile(projectFile)) {
            var mapName = FilenameUtils.removeExtension(projectFile.getFileName().toString()).toUpperCase(Locale.ROOT).replace("_PC", "");
            var mapFiles = MapLoader.findApplicableFiles(projectFile.getParent(), mapName, MapLoader.determineEngineVersion(projectFile.getParent(), mapName));
            engineVersion = MapLoader.determineEngineVersion(projectFile, mapName);
            nodes.add(new MapXml(mapName, projectFile, MapIO.getTypeFromPath(projectFile), mapFiles, Map.of(), Map.of()));
        } else {
            if(!AreaIO.findMapsInDirectory(projectFile).isEmpty()){
                var maps = new ArrayList<MapXml>();

                for (var map : AreaIO.findMapsInDirectory(projectFile)) {
                    var mapDir = projectFile.resolve(map.directory());
                    var mapName = map.directory().toString();
                    var mapFiles = MapLoader.findApplicableFiles(mapDir, mapDir.getFileName().toString(), MapLoader.determineEngineVersion(mapDir.getParent(), mapName));
                    engineVersion = MapLoader.determineEngineVersion(mapDir.getParent(), mapName);
                    maps.add(new MapXml(mapName, mapDir, MapIO.getTypeFromPath(mapDir), mapFiles, Map.of(), Map.of()));
                }
                
                nodes.add(new Area(projectFile.toString(), maps, new Area.AreaProperties()));
            } else {
                var mapName = projectFile.getFileName().toString();
                var mapFiles = MapLoader.findApplicableFiles(projectFile, projectFile.getFileName().toString(), MapLoader.determineEngineVersion(projectFile.getParent(), mapName));
                engineVersion = MapLoader.determineEngineVersion(projectFile.getParent(), mapName);
                nodes.add(new MapXml(mapName, projectFile, MapIO.getTypeFromPath(projectFile), mapFiles, Map.of(), Map.of()));
            }
        }
        
        var structure = new ProjectStructure.FolderNode("root", nodes);
        return new Project(false, engineVersion.getDefaultGame(), projectFile.getFileName().toString(),
                projectFile, projectFile, new Project.Assets(List.of(), List.of()), new ProjectStructure(structure));
    }

    /**
     * Inport the given asset into the given project.
     */
    public static void importAsset(Project project, Path asset) {
        var ext = FilenameUtils.getExtension(asset.toString());

        if (ext.equalsIgnoreCase("dds")) {
            var assetPath = Path.of("textures", RandomStringUtils.random(10, false, true), asset.getFileName().toString());
            var realPath = project.projectXml().resolveSibling(assetPath);

            try {
                project.assets().textures().removeIf(m -> m.name().equalsIgnoreCase(asset.getFileName().toString())); //delete existing entries

                Files.createDirectories(realPath.getParent());
                Files.copy(asset, realPath);

                var icon = new ImageIcon(Util.getScaledImage(96, 96, ImageIO.read(realPath.toFile())));

                project.assets().textures().add(new Project.Assets.TextureDef(asset.getFileName().toString(), assetPath.toString(), icon));
            } catch (IOException e) {
                SwingUtil.showErrorAlert("Failed to import asset", e);
            }
        } else {
            var possibleMaterialPath = Path.of(asset.toString().replaceAll("(?i).obj", ".mtl"));

            var newModelName = asset.getFileName().toString();
            if(project.assets().models().stream().anyMatch(m -> m.name().equalsIgnoreCase(asset.getFileName().toString()))) { //already exists
                newModelName =  FilenameUtils.removeExtension(newModelName) + "_" + project.assets().models().stream().filter(m -> m.name().startsWith(asset.getFileName().toString())).count()
                                + "." + FilenameUtils.getExtension(newModelName);
            }

            var localAssetDir = Path.of("models", RandomStringUtils.random(10, false, true), FilenameUtils.removeExtension(newModelName));
            var localAssetPath = localAssetDir.resolveSibling(newModelName);
            var localMaterialPath = localAssetDir.resolveSibling(newModelName.replaceAll("(?i).obj", ".mtl"));

            var absAssetPath = project.projectXml().resolveSibling(localAssetPath);
            var absMaterialPath = project.projectXml().resolveSibling(localMaterialPath);

            try {
                Files.createDirectories(absAssetPath.getParent());
                Files.copy(asset, absAssetPath);

                if (ext.equalsIgnoreCase("obj") && Files.exists(possibleMaterialPath)) {
                    Files.copy(possibleMaterialPath, absMaterialPath);
                }

                var loadedModel = AssimpModelLoader.loadModel(asset.toString());

                for (var material : loadedModel.getMaterials()) {
                    if (!material.mapKdFilename.isEmpty()) {
                        if (!Files.exists(absAssetPath.resolveSibling(material.mapKdFilename))) {
                            Files.copy(asset.resolveSibling(material.mapKdFilename), absAssetPath.resolveSibling(material.mapKdFilename));
                        }
                    }

                    if (!material.bumpFilename.isEmpty()) {
                        if (!Files.exists(absAssetPath.resolveSibling(material.bumpFilename))) {
                            Files.copy(asset.resolveSibling(material.bumpFilename), absAssetPath.resolveSibling(material.bumpFilename));
                        }
                    }
                }

                project.assets().models().add(new Project.Assets.ModelDef(newModelName, localAssetPath.toString()));
            } catch (IOException e) {
                SwingUtil.showErrorAlert("Failed while importing the asset", e);
            }
        }
    }

    /**
     * Import a project resource into the given project.
     *
     * @see com.opengg.loader.ProjectResource
     */
    public static void importResource(Project project, Path resourceFile, String name, Path desiredDirectory) throws IOException {
        var parent = ProjectIO.addNodeStructureFromPath(project.structure(), desiredDirectory);
        if (!(parent instanceof ProjectStructure.FolderNode fn)) {
            JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "Cannot add resource to non-folder node");
        } else {
            var uniqueOutputFile = Path.of("resources", RandomStringUtils.random(10, false, true), resourceFile.getFileName().toString());
            var absUniqueOutputFile = project.projectXml().resolveSibling(uniqueOutputFile);
            var resource = new ProjectResource(name, uniqueOutputFile.toString());
            Files.createDirectories(absUniqueOutputFile.getParent());

            if (Files.isDirectory(resourceFile)) {
                FileUtils.copyDirectory(resourceFile.toFile(), absUniqueOutputFile.toFile());
            } else {
                Files.copy(resourceFile, absUniqueOutputFile);
            }

            fn.children().add(resource);
        }
    }

    /**
     * Given a possibly in a TCS game directory, return the root of the TCS directory.
     */
    public static Path getGameRootPathFromDirectory(Path mapDirectory){
        if (!mapDirectory.toString().toLowerCase().contains("levels") &&
                !mapDirectory.toString().toLowerCase().contains("stuff")) {
            return null;
        }

        var rootDir = mapDirectory;

        if (mapDirectory.toString().toLowerCase().contains("levels")) {
            while (!rootDir.getFileName().toString().equalsIgnoreCase("levels")) {
                rootDir = rootDir.getParent();
            }
            rootDir = rootDir.getParent();

        } else if (mapDirectory.toString().toLowerCase().contains("stuff")) {
            while (!rootDir.getFileName().toString().equalsIgnoreCase("stuff")) {
                rootDir = rootDir.getParent();
            }
            rootDir = rootDir.getParent();
        }

        return rootDir;
    }

    /**
     * Return the portion of the given directory that is relative to the TCS game directory.
     *
     * For example, if "C:/Games/TCS/LEVELS/AREAS.TXT" is passed in, this returns "LEVELS/AREAS.TXT"
     */
    public static Path getLocalPathFromGameRoot(Path mapDirectory) {
        if (mapDirectory.toString().toLowerCase().contains("levels") ||
                mapDirectory.toString().toLowerCase().contains("stuff")) {
            return FileUtil.getRelativePath(getGameRootPathFromDirectory(mapDirectory), mapDirectory);
        } else {
            return mapDirectory.getFileName();
        }
    }

    public static String[] parseDesiredDirectory(Path mapDirectory) {
        var actualDirectory = mapDirectory;
        if (Files.isRegularFile(mapDirectory)) {
            actualDirectory = actualDirectory.getParent();
        }

        return actualDirectory.toString().split("[\\\\/]");
    }

    public static ProjectStructure.Node<?> addNodeStructureFromPath(ProjectStructure structure, Path desiredDirectory) {
        var localPath = parseDesiredDirectory(desiredDirectory);

        var goodNode = structure.root();

        int lastGoodIndex;
        for (lastGoodIndex = 0; lastGoodIndex < localPath.length; lastGoodIndex++) {
            int finalI = lastGoodIndex;
            var nextGoodNode = goodNode.children().stream()
                    .filter(g -> g.name().equalsIgnoreCase(localPath[finalI]))
                    .findFirst();

            if (nextGoodNode.isPresent()) {
                switch (nextGoodNode.get()) {
                     case ProjectStructure.FolderNode fn -> goodNode = fn;
                     case Area an -> { return an; }
                     default -> throw new UnsupportedOperationException("Cannot add a map to another map");
                }
            } else {
                break;
            }
        }

        for (int i = lastGoodIndex; i < localPath.length; i++) {
            var newFolder = new ProjectStructure.FolderNode(localPath[i], new ArrayList<>());
            goodNode.children().add(newFolder);

            goodNode = newFolder;
        }

        return goodNode;
    }

    public static void unpackAllMapsForRoot(ProjectStructure.Node<?> node) {
        switch (node) {
            case ProjectStructure.FolderNode fn -> fn.children().forEach(ProjectIO::unpackAllMapsForRoot);
            case MapXml mn -> MapIO.applyAllDiffs(mn, EditorState.getProject());
            case Area an -> an.maps().forEach(mn -> MapIO.applyAllDiffs(mn, EditorState.getProject()));
            default -> {}
        }
    }
}
