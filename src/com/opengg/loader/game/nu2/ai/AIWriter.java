package com.opengg.loader.game.nu2.ai;

import com.opengg.loader.BrickBench;
import com.opengg.loader.Util;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.ai.CreatureSpawn;
import com.opengg.loader.loading.MapWriter;

import javax.swing.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AIWriter {
    public static String addAICreature() {
        var aiData = EditorState.getActiveMap().levelData().<NU2MapData>as().ai();
        var version = aiData.version().get();
        if(version == 0){
            JOptionPane.showMessageDialog(null, "Cannot create AI creature, as this map has no AI mesh.");
            return null;
        }

        if(version < 14){
            JOptionPane.showMessageDialog(null, "Cannot create AI creature. Please report this to the developers with the map name.");
            return null;
        }

        var length = switch (version){
            case 14, 15 -> 144;
            case 16 -> 145;
            case 17, 18, 19, 20 -> 165;
            default -> throw new IllegalStateException("Unexpected version: " + version);
        };

        var name = "Creature_" + aiData.creatureSpawns().size();
        var newCreature = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN)
                .put(Util.getStringBytes(name, 16))
                .put(Util.getStringBytes("Default", 16))
                .put(Util.getStringBytes("BossNass", 32))
                .put(BrickBench.CURRENT.ingamePosition.toLittleEndianByteBuffer())
                .putShort((short) 0);

        if(version >= 16){
            newCreature.put((byte)0);
        }

        var creatureBytes = newCreature
                .put(new byte[]{1,2})
                .putInt(1)
                .putFloat(0.4f)
                .putFloat(0.4f);

        var newCount = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(aiData.creatureSpawns().size() + 1);

        var lastAIEnd = aiData.creatureEndAddress().get();

        MapWriter.addSpaceAtLocation(MapWriter.WritableObject.CREATURE_SPAWN, lastAIEnd, length);
        MapWriter.applyPatch(MapWriter.WritableObject.CREATURE_SPAWN, lastAIEnd, creatureBytes);
        MapWriter.applyPatch(MapWriter.WritableObject.CREATURE_SPAWN, aiData.creatureStartAddress().get(), newCount);

        return name;
    }

    public static void addAILocator() {
        var aiData = EditorState.getActiveMap().levelData().<NU2MapData>as().ai();
        var version = aiData.version().get();
    }

    public static String addLocatorSet() {
        var aiData = EditorState.getActiveMap().levelData().<NU2MapData>as().ai();
        var newAddress = aiData.locatorSetAddress().get() + 4;
        if (aiData.aiLocatorSets().size() != 0) newAddress = aiData.aiLocatorSets().get(aiData.aiLocatorSets().size() - 1).endAddress();

        var name = "SET_" + (aiData.aiLocatorSets().size() + 1);
        var newSet = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
                .put(Util.getStringBytes(name, 16))
                .putInt(0).flip();

        MapWriter.addSpaceAtLocation(MapWriter.WritableObject.AI_LOCATOR, newAddress, 20);
        MapWriter.applyPatch(MapWriter.WritableObject.AI_LOCATOR, newAddress, newSet);
        MapWriter.applyPatch(MapWriter.WritableObject.AI_LOCATOR, aiData.locatorSetAddress().get(), Util.littleEndian(aiData.aiLocatorSets().size() + 1));

        return name;
    }

    public static void removeLocatorSet(AILocatorSet set) {
        var aiData = EditorState.getActiveMap().levelData().<NU2MapData>as().ai();
        MapWriter.removeSpaceAtLocation(MapWriter.WritableObject.AI_LOCATOR, set.fileAddress(), set.endAddress() - set.fileAddress());
        MapWriter.applyPatch(MapWriter.WritableObject.AI_LOCATOR, aiData.locatorSetAddress().get(), Util.littleEndian(aiData.aiLocatorSets().size() - 1));
    }

    public static void deleteAICreature(CreatureSpawn creature){
        var aiData = EditorState.getActiveMap().levelData().<NU2MapData>as().ai();
        var newCount = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(aiData.creatureSpawns().size() - 1);
        MapWriter.removeSpaceAtLocation(MapWriter.WritableObject.CREATURE_SPAWN, creature.fileAddress(), creature.size());
        MapWriter.applyPatch(MapWriter.WritableObject.CREATURE_SPAWN, aiData.creatureStartAddress().get(), newCount);
    }
}
