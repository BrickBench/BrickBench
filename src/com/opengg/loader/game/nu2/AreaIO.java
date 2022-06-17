package com.opengg.loader.game.nu2;

import com.opengg.loader.*;
import com.opengg.loader.loading.ProjectIO;
import com.opengg.loader.loading.MapIO;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AreaIO {
    public static Area createEmptyArea(String name){
        return new Area(name, new ArrayList<>(), new Area.AreaProperties());
    }

    public static Area createAreaFromDirectory(Project project, Path originalAreaDirectory, Path targetAreaDirectory, List<MapLocation> maps) throws IOException {
        var originalName = originalAreaDirectory.getFileName().toString();
        var targetName = targetAreaDirectory.getFileName().toString();

        var properties = new Area.AreaProperties();

        var areaTxt = originalAreaDirectory.resolve(originalAreaDirectory.getFileName().toString() + ".TXT");
        if (!Files.exists(areaTxt)) {
            if (originalAreaDirectory.getFileName().toString().toUpperCase().contains("ENDING")) {
                areaTxt = Files.list(originalAreaDirectory).filter(o -> o.getFileName().toString().toUpperCase(Locale.ROOT).endsWith("ENDING.TXT"))
                        .findFirst().get();
            }
        }
        new LevelTXTParser(properties).parseFile(areaTxt);

        var rootDir = ProjectIO.getGameRootPathFromDirectory(originalAreaDirectory);
        if(rootDir != null && Files.exists(Path.of(rootDir.toString(),"levels", "areas.txt"))){
            var areasParser = new AreasTXTParser();
            areasParser.parseFile(Path.of(rootDir.toString(),"levels", "areas.txt"));
            properties.globalProperties = areasParser.getParsedProperties().stream()
                    .filter(p -> p.file.equalsIgnoreCase(originalName))
                    .findFirst().orElse(new Area.AreaGlobalProperties());
        }

        String desiredName = originalName;
        if(properties.globalProperties.file != null && targetName.equalsIgnoreCase(originalName)){
            desiredName = properties.globalProperties.file;
        }else if(!targetName.equalsIgnoreCase(originalName)){
            desiredName = targetName;
        }

        var area = new Area(desiredName, new ArrayList<>(), properties);

        var newAreaNode = ProjectIO.addNodeStructureFromPath(project.structure(), targetAreaDirectory.getParent());
        ((ProjectStructure.FolderNode) newAreaNode).children().add(area);

        for(var map : maps){
            var mapDirectory = originalAreaDirectory.resolve(map.directory);
            var mapDesiredPath = targetAreaDirectory.resolve(map.directory);

            var newName =  map.name.replaceAll("(?i)" + Pattern.quote(originalName), targetName);
            if (project.maps().stream().anyMatch(m -> m.name().equalsIgnoreCase(newName))) {
                var newMap = project.maps().stream().filter(m -> m.name().equalsIgnoreCase(newName)).findFirst().get();
                var oldParent = project.structure().getParent(newMap);

                if (oldParent instanceof ProjectStructure.FolderNode fn) {
                    fn.children().removeIf(n -> n.name().equalsIgnoreCase(newName));
                }

                ProjectIO.addNodeStructureFromPath(project.structure(), mapDesiredPath);
            } else {
                MapIO.importNewMap(project, mapDirectory, mapDesiredPath, map.name, map.name.replaceAll("(?i)" + Pattern.quote(originalName), targetName));
            }
        }

        return area;
    }

    public static void renameArea(Project project, Area area, String newName){
        var oldName = area.name();
        for(var map : area.maps()){
            try {
                MapIO.renameMap(project, map, map.name().replaceAll("(?i)" + Pattern.quote(oldName), newName));
            } catch (IOException e) {
                SwingUtil.showErrorAlert("Failed to move map " + map.name(), e);
            }
        }
        area.setName(newName);
    }

    public static Area.AreaGlobalProperties repairAndGetGlobalProperties(Project project, Area area){
        var props = area.areaProperties().globalProperties;

        props.file = area.name();
        props.dir = project.structure().getFolderFor(area).stream().skip(1).collect(Collectors.joining("/"));

        props.levels.clear();
        for(var map : area.maps()){
            props.levels.add(map.name());
        }

        return props;
    }

    public static List<MapLocation> findMapsInDirectory(Path area){
        if(Files.isRegularFile(area)) return List.of();

        try {
            return Files.list(area)
                    .filter(Files::isDirectory)
                    .flatMap(dir -> {
                        try {
                            return Files.list(dir)
                                    .filter(file -> file.toString().toUpperCase().endsWith("_PC.GSC"));
                        } catch (IOException e) {
                            SwingUtil.showErrorAlert("Failed to search directory " + area, e);
                            return Stream.empty();
                        }})
                    .map(f -> f.resolveSibling(f.getFileName().toString().toUpperCase(Locale.ROOT).replace("_PC.GSC", "")))
                    .map(f -> new MapLocation(FileUtil.getRelativePath(area, f.getParent()), FilenameUtils.getBaseName(f.toString())))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            SwingUtil.showErrorAlert("Failed to search directory " + area, e);
            return List.of();
        }
    }

    public static String generateLevelTXTText(Area area){
        var buf =  new StringBuilder();
        buf.append("//Generated by BrickBench\n\n");

        buf.append("story_coins ").append(area.areaProperties().storyCoins).append("\n");
        buf.append("freeplay_coins ").append(area.areaProperties().freeplayCoins).append("\n\n");

        for(var character : area.areaProperties().creatures){
            buf.append("character \"").append(character.name).append("\" ").append(character.type.name().toLowerCase());
            if(character.extraToggle){
                buf.append(" ;extra_toggle");
            }
            buf.append("\n");
        }
        buf.append("\n");

        for(var message : area.areaProperties().aiMessages){
            buf.append("AIMessage \"").append(message.messageName).append("\"");
            if(!message.output0.isEmpty()){
                buf.append(" output0 ").append(message.output0);
            }
            if(!message.output1.isEmpty()){
                buf.append(" output1 ").append(message.output1);
            }
            if(!message.output2.isEmpty()){
                buf.append(" output2 ").append(message.output2);
            }
            if(!message.output3.isEmpty()){
                buf.append(" output3 ").append(message.output3);
            }
            buf.append("\n");
        }
        buf.append("\n");

        for(var streaming : area.areaProperties().streaming){
            buf.append("streaming \"").append(streaming.levelName).append("\" \"").append(streaming.level1).append("\" \"").append(streaming.level2).append("\"\n");
        }
        buf.append("\n");

        for(var counter : area.areaProperties().superCounters){
            buf.append("supercounter_start\n");

            for(var pickup : counter.pickups){
                buf.append("\tpickup \"").append(pickup.pickupName).append("\" in_level \"").append(pickup.levelName).append("\" ");
                if(pickup.type == Area.AreaSuperCounterPickup.Type.GIZMO){
                    buf.append("draw_at_gizmo");
                }else {
                    buf.append("draw_at_obj");
                }
                buf.append(" \"").append(pickup.target).append("\"\n");
            }

            buf.append("\tcolour ").append(counter.color.getRed()).append(" ").append(counter.color.getGreen()).append(" ").append(counter.color.getBlue()).append("\n");
            buf.append("supercounter_end\n");
        }

        return buf.toString();
    }

    public static String getFolderFor(Area area, MapXml map) {
        return switch (map.mapType()) {
            case INTRO -> area.name() + "_INTRO";
            case OUTRO -> area.name() + "_OUTRO";
            case MIDTRO -> area.name() + "_MIDTRO";
            case STATUS -> area.name() + "_STATUS";
            case null, default -> map.name();
        };
    }

    public record MapLocation(Path directory, String name){}
}
