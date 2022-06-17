package com.opengg.loader.loading;

import com.opengg.core.engine.Resource;
import com.opengg.loader.*;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.Area;
import com.opengg.loader.game.nu2.LevelsTXTParser;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class MapIO {
    public static void importNewMap(Project project, Path originalAbsFile, Path desiredDirectory, String originalName, String newName) throws IOException {
        var projectPath = Resource.getUserDataPath() + "/project/";
        var newMapDir = Path.of(projectPath, newName);

        var gameRoot = ProjectIO.getGameRootPathFromDirectory(originalAbsFile);
        Optional<LevelsTXTParser.LevelTXTEntry> existingEntry;
        if(gameRoot != null && Files.exists(Path.of(gameRoot.toString(),"levels", "levels.txt"))){
            var parser = new LevelsTXTParser();
            parser.parseFile(Path.of(gameRoot.toString(),"levels", "levels.txt"));
            var existingMaps = parser.getEntries();
            existingEntry = existingMaps.stream()
                    .filter(f -> f.name().equalsIgnoreCase(originalAbsFile.getFileName().toString()))
                    .filter(f -> f.name().equalsIgnoreCase(originalName))
                    .findFirst();
        }else{
            existingEntry = Optional.empty();
        }

        var mapType = existingEntry.map(LevelsTXTParser.LevelTXTEntry::type).orElse(getTypeFromPath(originalAbsFile));
        if(originalName.equals(newName) && existingEntry.isPresent()){
            newName = existingEntry.get().name();
        }

        var files = new ArrayList<Path>();
        var applicableFiles = MapLoader.findApplicableFiles(
                Files.isDirectory(originalAbsFile) ? originalAbsFile : originalAbsFile.getParent(), originalName, project.game().ENGINE);

        for(var path : applicableFiles){
            var relativeFilePath = Files.isRegularFile(originalAbsFile) ? path.getFileName() : FileUtil.getRelativePath(originalAbsFile, path);

            var newRelativeFilePath = relativeFilePath.toString().replaceAll("(?i)" + Pattern.quote(originalName), newName);
            var newFileDir = newMapDir.resolve(newRelativeFilePath);
            Files.createDirectories(newFileDir.getParent());
            Files.copy(path, newFileDir, StandardCopyOption.REPLACE_EXISTING);
            files.add(Path.of(newRelativeFilePath));
        }

        var mapXml = new MapXml(
                newName,
                newMapDir,
                mapType,
                files,
                new HashMap<>(),
                new HashMap<>()
        );

        var newNode = ProjectIO.addNodeStructureFromPath(project.structure(), desiredDirectory);

        if(newNode instanceof ProjectStructure.FolderNode fn){
            fn.children().add(mapXml);
        }else if(newNode instanceof Area an){
            an.maps().add(mapXml);
        }
    }

    public static MapXml.MapType getTypeFromPath(Path path){
        var parentEnd = path.getFileName().toString().toLowerCase().split("_");
        if(parentEnd.length < 2) return MapXml.MapType.NORMAL;
        var mapType = switch (parentEnd[1]){
            case "intro" -> MapXml.MapType.INTRO;
            case "outro" -> MapXml.MapType.OUTRO;
            case "midtro" -> MapXml.MapType.MIDTRO;
            case "status" -> MapXml.MapType.STATUS;
            default -> MapXml.MapType.NORMAL;
        };

        if(Files.isRegularFile(path)){
            mapType = MapXml.MapType.SINGLE_FILE;
        }

        return mapType;
    }

    public static boolean isMap(Path map) throws IOException {
        if (Files.isRegularFile(map)) {
            return FilenameUtils.getExtension(map.toString()).equalsIgnoreCase("gsc");
        }

        var files = Files.list(map).toList();

        for (var file : files) {
            if (FilenameUtils.getExtension(file.toString()).equalsIgnoreCase("gsc")) {
                return true;
            }
        }

        return false;
    }

    public static Path generateRelativePathForFile(ProjectStructure structure, MapXml map, Path relativeFile){
        var mapLocalPathList = structure.getFolderFor(map);
        var mapLocalPath = Path.of(mapLocalPathList.get(0), mapLocalPathList.subList(1, mapLocalPathList.size()).toArray(new String[0]));

        return mapLocalPath.resolve(relativeFile);
    }

    public static List<Path> saveMapAndGetFiles(MapXml map, Project project, Path output){
        var filesToSave = new ArrayList<Path>();

        for(var file : map.files()){
            var filePath = map.mapFilesDirectory().resolve(file);
            var diffPath = map.mapFilesDirectory().resolve(file + ".diff");
            var relativePath = generateRelativePathForFile(project.structure(), map, file);
            var ogPath = GameBaseManager.getBaseDirectoryOrPromptForNew(project.game()).get().resolve(relativePath);

            if(Files.exists(filePath) && Files.exists(ogPath)){
                try {
                    FileUtil.diff(ogPath, filePath, diffPath);
                    filesToSave.add(diffPath);

                } catch (IOException e) {
                    SwingUtil.showErrorAlert("Failed to generate diff for " + filePath, e);
                    filesToSave.add(map.mapFilesDirectory().resolve(relativePath));

                }
            }else if (Files.exists(filePath)) {
                filesToSave.add(filePath);

            }else{
                filesToSave.add(diffPath);
            }
        }
        return filesToSave;
    }

    public static void applyAllDiffs(MapXml map, Project project) throws RuntimeException{
        for(var file : map.files()){
            try {
                MapLoader.applyDiffIfNeeded(project.game(), project.structure(), map, file);
            } catch (IOException e) {
                var ogPath = MapIO.generateRelativePathForFile(project.structure(), map, file);
                SwingUtil.showErrorAlert("Map " + map.name() + " relies on the file \"" + ogPath + "\" which does not exist in the clean game copy. " +
                        "<br/> If you are sure your game copy is clean, please report this to the developers");
                throw new RuntimeException(e);
            }
        }
    }

    public static void renameMap(Project project, MapXml map, String newName) throws IOException {
        if(EditorState.getActiveMap() != null && EditorState.getActiveMap().levelData().xmlData() == map){
            EditorState.closeProjectMap(map);
        }

        var newDir = map.mapFilesDirectory().resolveSibling(newName);

        Files.createDirectories(newDir);
        Files.createDirectories(newDir.resolve("ai"));

        var newFilesList = new ArrayList<Path>();

        for(var file : map.files()){
            try {
                MapLoader.applyDiffIfNeeded(project.game(), project.structure(), map, file);
            } catch (IOException e) {
                e.printStackTrace();
            }

            var newPath = file.toString().replaceAll("(?s)" + Pattern.quote(map.name()), newName);
            Files.move(map.mapFilesDirectory().resolve(file), newDir.resolve(newPath));

            newFilesList.add(Path.of(newPath));
        }

        map.setName(newName);
        map.setMapFilesDirectory(newDir);

        map.files().clear();
        map.files().addAll(newFilesList);

        if(EditorState.getActiveMap() != null && EditorState.getActiveMap().levelData().xmlData() == map){
            BrickBench.CURRENT.useMapFromCurrentProject(map);
        }
    }

    public static void exportMap(Project project, Path exportDirectory, MapXml map) throws IOException {
        for(var file : map.files()){
            var newFile = exportDirectory.resolve(file);
            var oldFile = map.mapFilesDirectory().resolve(file);
            var oldDiffFile = map.mapFilesDirectory().resolve(file + ".diff");

            if(Files.exists(oldFile)){
                Files.createDirectories(newFile.getParent());
                Files.copy(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            }else if(!Files.exists(oldFile) && Files.exists(oldDiffFile)){
                Files.createDirectories(newFile.getParent());
                MapLoader.applyDiffForFile(Path.of(file + ".diff"),
                        GameBaseManager.getBaseDirectoryOrPromptForNew(project.game()).get(),
                        exportDirectory, project.structure(), map);
            }
        }
    }

    public static LevelsTXTParser.LevelTXTEntry createLevelsTXTEntry(Project project, MapXml map){
        if(map.mapType() == MapXml.MapType.SINGLE_FILE) return null;

        var mapPathList = project.structure().getFolderFor(map);
        var mapPath = String.join("/", mapPathList.subList(1, mapPathList.size()));

        return new LevelsTXTParser.LevelTXTEntry(mapPath, map.name(), map.mapType());
    }
}
