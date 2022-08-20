package com.opengg.loader.game.nu2;

import com.opengg.loader.game.nu2.Door;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.Spline;

import java.nio.ByteBuffer;
import java.util.*;

public class TxtLoader {
    public static int maxTerrainGroups = 0x80;
    public static int maxTerrainPlatforms = 0x100;

    public static void load(ByteBuffer fileData, NU2MapData mapData){
        var string = new String(fileData.array());
        try(var scanner = new Scanner(string)) {
            while(scanner.hasNextLine()){
                var line = scanner.nextLine();
                line = line.replaceAll("(//.*)", "").trim();
                var tokens = line.split("[\s=]");
                switch (tokens[0]){
                    case "farclip","farclip_pc"-> {
                        mapData.txt().settingsMap().put("farclip",Float.valueOf(tokens[1]));
                    }
                    case "door_start" -> {
                        Spline doorSpline = null;
                        String targetMap = "Unknown";

                        while(!(line = scanner.nextLine().replaceAll("([/].*)", "").trim()).equals("door_end")) {
                            if(line.isBlank()) continue;
                            var contents = line.replace("\"", "").split("[=\s]");
                            switch (contents[0]){
                                case "spline" -> doorSpline =
                                        mapData.getSplineByName(contents[1].toLowerCase()).orElse(null);
                                case "level" -> targetMap = contents[1];
                            }
                        }
                        if(doorSpline != null) mapData.txt().doors().add(new Door(doorSpline, targetMap));
                    }
                    case "max_ter_groups" -> maxTerrainGroups = Integer.parseInt(line.split(" ")[1]);
                    case "max_ter_platforms" -> maxTerrainPlatforms = Integer.parseInt(line.split(" ")[1]);
                    case "settings_start" -> {
                        while(!(line = scanner.nextLine().replaceAll("([/].*)", "").trim()).equals("settings_end")) {
                            if(line.isBlank()) continue;
                            var contents = line.replace("\"", "").split("[=\s]");
                            switch (contents[0]){
                                case "farclip" , "farclip_pc"-> mapData.txt().settingsMap().put("farclip",Float.valueOf(contents[1]));
                            }
                        }
                    }
                }
            }
        }

    }
}
