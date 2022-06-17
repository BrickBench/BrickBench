package com.opengg.loader.game.nu2;

import com.opengg.loader.loading.TXTParser;

import java.awt.*;
import java.util.Locale;

public class LevelTXTParser extends TXTParser {
    protected Area.AreaProperties area;
    private Area.AreaSuperCounter tempCounter;

    public LevelTXTParser(Area.AreaProperties area) {
        this.area = area;
    }

    @Override
    protected void parseBlockStart(String[] tokens) {
        if (currentBlock.toLowerCase(Locale.ROOT).equals("supercounter")) {
            tempCounter = new Area.AreaSuperCounter();
        }
    }

    @Override
    protected void parseBlockEnd(String[] tokens) {
        if (currentBlock.toLowerCase(Locale.ROOT).equals("supercounter")) {
            area.superCounters.add(tempCounter);
        }
    }

    @Override
    protected void parseBlockAttribute(String[] tokens) {
        switch (currentBlock.toLowerCase(Locale.ROOT)) {
            case "supercounter" -> {
                switch (currentAttribute.toLowerCase(Locale.ROOT).trim()) {
                    case "pickup" -> {
                        Area.AreaSuperCounterPickup pickup = new Area.AreaSuperCounterPickup();
                        pickup.pickupName = tokens[1];
                        pickup.levelName = tokens[3];
                        pickup.type = Area.AreaSuperCounterPickup.typeFromString(tokens[4]);
                        pickup.target = tokens[5];
                        tempCounter.pickups.add(pickup);
                    }
                    case "colour" -> tempCounter.color = new Color(Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]), Integer.parseInt(tokens[2]));
                }
            }
            default -> {
                switch (currentAttribute.toLowerCase(Locale.ROOT)) {
                    case "streaming" -> {
                        Area.AreaStreaming streaming = new Area.AreaStreaming();
                        streaming.levelName = tokens[1];
                        streaming.level1 = tokens[2];
                        if (tokens.length > 3) streaming.level2 = tokens[3];
                        area.streaming.add(streaming);
                    }
                    case "aimessage" -> {
                        Area.AreaAIMessage areaAIMessage = new Area.AreaAIMessage();
                        areaAIMessage.messageName = tokens[1];
                        if (tokens.length >= 4) areaAIMessage.output0 = tokens[3];
                        if (tokens.length >= 6) areaAIMessage.output1 = tokens[5];
                        if (tokens.length >= 8) areaAIMessage.output2 = tokens[7];
                        if (tokens.length >= 10) areaAIMessage.output3 = tokens[9];
                        area.aiMessages.add(areaAIMessage);
                    }
                    case "character" -> {
                        Area.AreaCreature creature = new Area.AreaCreature();
                        creature.name = tokens[1];
                        creature.type = Area.AreaCreature.typeFromString(tokens[2]);
                        if (tokens.length > 3) creature.extraToggle = tokens[3].contains("extra_toggle");
                        area.creatures.add(creature);
                    }
                    case "story_coins" -> area.storyCoins = Integer.parseInt(tokens[1]);
                    case "freeplay_coins" -> area.freeplayCoins = Integer.parseInt(tokens[1]);
                    case "music" -> area.music = tokens[1];
                }
            }
        }
    }
}
