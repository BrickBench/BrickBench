package com.opengg.loader.game.nu2;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.opengg.loader.MapXml;
import com.opengg.loader.ProjectStructure;
import com.opengg.loader.editor.AreaEditPanel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public final class Area implements ProjectStructure.Node<Area> {
    private String name;
    private final List<MapXml> maps;
    private final AreaProperties areaProperties;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Area(@JsonProperty("name") String name, @JsonProperty("maps") List<MapXml> maps, @JsonProperty("areaProperties") AreaProperties areaProperties) {
        this.name = name;
        this.maps = maps;
        this.areaProperties = areaProperties;
    }

    public String name() {
        return name;
    }

    @Override
    public String path() {
        return "Areas/";
    }

    @Override
    public String namespace() {
        return "Project";
    }

    @Override
    @JsonIgnore
    public List<Property> properties() {
        return List.of(
                new CustomUIProperty("Edit area", new AreaEditPanel(this), false)
        );
    }

    public void setName(String name){
        for(var map : maps){
            map.setName(map.name().replaceAll("(?i)" + Pattern.quote(this.name), name));
        }
        this.name = name;
    }

    public List<MapXml> maps() {
        return maps;
    }

    public AreaProperties areaProperties() {
        return areaProperties;
    }

    public static class AreaCreature {
        public String name = "default";
        public Type type = Type.RESIDENT;
        public boolean extraToggle = false;

        public enum Type {
            RESIDENT, CUTSCENE, PLAYER
        }

        public static Type typeFromString(String token) {
            return switch (token) {
                case "resident":
                    yield Type.RESIDENT;
                case "cutscene":
                    yield Type.CUTSCENE;
                case "player":
                    yield Type.PLAYER;
                default:
                    throw new IllegalStateException("Unexpected value: " + token);
            };
        }
    }

    public static class AreaStreaming {
        public String levelName = "default level";
        public String level1 = "";
        public String level2 = "";
    }

    public static class AreaAIMessage {
        public String messageName = "default name";
        public String output0 = "";
        public String output1 = "";
        public String output2 = "";
        public String output3 = "";
    }

    public static class AreaSuperCounter {
        public Color color = Color.BLACK;
        public static int counter = 0;
        public String name = "SuperCounter ";
        public List<AreaSuperCounterPickup> pickups = new ArrayList<>();

        public AreaSuperCounter() {
            name += counter;
            counter++;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class AreaGlobalProperties {
        @JsonIgnore public String dir;
        @JsonIgnore public String file;
        @JsonIgnore public List<String> levels = new ArrayList<>();
        public String minikit = "";
        public int nameId = 0;
        public int textId = -1;
        public int textId2 = -1;
        public String redbrickCheat = "";

        public boolean hasFreeplay = true;
        public boolean isVehicleArea = false;
        public boolean isEndingArea = false;
        public boolean isBonusArea = false;
        public boolean isSuperBonusArea = false;

        public int bonusTimeTrialTime = 300;

        public boolean hasPickupGravity = true;
        public boolean hasCharacterCollision = true;
        public boolean isSingleBuffer = false;

        public boolean givesGoldBrick = true;
        public boolean givesCompletionPoints = true;

        public boolean isHubArea = false;

        public boolean generateStatusScreen = true;
    }

    public static class AreaSuperCounterPickup {
        public String pickupName;
        public String levelName;
        public Type type;
        public String target;

        public enum Type {
            GIZMO, OBJECT
        }

        public static Type typeFromString(String token) {
            return switch (token) {
                case "draw_at_obj":
                    yield Type.OBJECT;
                case "draw_at_gizmo":
                    yield Type.GIZMO;
                default:
                    throw new IllegalStateException("Unexpected value: " + token);
            };
        }
    }

    public static class AreaProperties {
        public int freeplayCoins = 0, storyCoins = 0;
        public String music = "";
        public AreaGlobalProperties globalProperties = new AreaGlobalProperties();
        public List<AreaCreature> creatures = new ArrayList<>();
        public List<AreaStreaming> streaming = new ArrayList<>();
        public List<AreaAIMessage> aiMessages = new ArrayList<>();
        public List<AreaSuperCounter> superCounters = new ArrayList<>();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Area) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.maps, that.maps) &&
                Objects.equals(this.areaProperties, that.areaProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, maps, areaProperties);
    }

    @Override
    public String toString() {
        return "Area[" +
                "name=" + name + ", " +
                "maps=" + maps + ", " +
                "properties=" + areaProperties + ']';
    }
}
