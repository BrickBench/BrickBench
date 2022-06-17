package com.opengg.loader.game.nu2;

import com.opengg.loader.loading.TXTParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AreasTXTParser extends TXTParser{

    private Area.AreaGlobalProperties currentProps = null;

    private List<Area.AreaGlobalProperties> properties = new ArrayList<>();

    @Override
    protected void parseBlockStart(String[] tokens) {
        currentProps = new Area.AreaGlobalProperties();
    }

    @Override
    protected void parseBlockEnd(String[] tokens) {
        properties.add(currentProps);
    }

    @Override
    protected void parseBlockAttribute(String[] tokens) {
        switch (tokens[0]){
            case "dir" -> currentProps.dir = tokens[1];
            case "file" -> currentProps.file = tokens[1];
            case "level" -> {
                currentProps.levels.add(tokens[1]);
                if(tokens[1].toUpperCase(Locale.ROOT).endsWith("STATUS")) currentProps.generateStatusScreen = true;
            }
            case "minikit" -> currentProps.minikit = tokens[1];

            case "name_id" -> currentProps.nameId = Integer.parseInt(tokens[1]);
            case "text_id" -> {
                currentProps.textId = Integer.parseInt(tokens[1]);
                if(currentProps.textId != -1){
                    currentProps.textId2 = Integer.parseInt(tokens[2]);
                }
            }

            case "redbrick_cheat" -> currentProps.redbrickCheat = tokens[1];
            case "vehicle_area" -> currentProps.isVehicleArea = true;
            case "ending_area" -> currentProps.isEndingArea = true;
            case "no_freeplay" -> currentProps.hasFreeplay = false;
            case "no_gold_brick" -> currentProps.givesGoldBrick = false;
            case "no_completion_points" -> currentProps.givesCompletionPoints = false;

            case "single_buffer" -> currentProps.isSingleBuffer = true;

            case "nocharactercollision" -> currentProps.hasCharacterCollision = false;
            case "nopickupgravity" -> currentProps.hasPickupGravity = false;

            case "bonus_area" -> currentProps.isBonusArea = true;
            case "super_bonus_area" -> currentProps.isSuperBonusArea = true;
            case "timetrial_time" -> currentProps.bonusTimeTrialTime = Integer.parseInt(tokens[1]);
        }
    }

    public List<Area.AreaGlobalProperties> getParsedProperties() {
        return properties;
    }
}
