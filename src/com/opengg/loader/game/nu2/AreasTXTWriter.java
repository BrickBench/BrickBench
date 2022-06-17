package com.opengg.loader.game.nu2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AreasTXTWriter {
    public static void write(Path output, List<Area.AreaGlobalProperties> entries) throws IOException {
        StringBuilder str = new StringBuilder();
        for(var entry : entries){
            str.append("area_start\n");
            str.append("\tdir \"").append(entry.dir).append("\"\n");
            str.append("\tfile \"").append(entry.file).append("\"\n");

            for(var level : entry.levels){
                str.append("\tlevel \"").append(level).append("\"\n");
            }

            if(!entry.minikit.isEmpty()) str.append("\tminikit \"").append(entry.minikit).append("\"\n");
            if(!entry.redbrickCheat.isEmpty()) str.append("\tredbrick_cheat \"").append(entry.redbrickCheat).append("\"\n");

            str.append("\tname_id ").append(entry.nameId).append("\n");
            str.append("\ttext_id ").append(entry.textId).append(" ").append(entry.textId2).append("\n");

            if(entry.isVehicleArea) str.append("\tvehicle_area\n");
            if(entry.isBonusArea){
                str.append("\tbonus_area\n");
                str.append("\ttimetrial_time ").append(entry.bonusTimeTrialTime).append("\n");
            }
            if(entry.isSuperBonusArea) str.append("\tsuper_bonus_area\n");
            if(entry.isEndingArea) str.append("\tending_area\n");
            if(entry.isHubArea) str.append("\thub_area\n");

            if(!entry.hasCharacterCollision) str.append("\tnocharactercollision\n");
            if(!entry.hasPickupGravity) str.append("\tnopickupgravity\n");
            if(entry.isSingleBuffer) str.append("\tsingle_buffer\n");

            if(!entry.givesGoldBrick) str.append("\tno_gold_brick\n");
            if(!entry.givesCompletionPoints) str.append("\tno_completion_points\n");

            str.append("area_end\n\n");
        }

        if(Files.exists(output))
            Files.delete(output);

        Files.writeString(output, str.toString());
    }
}
