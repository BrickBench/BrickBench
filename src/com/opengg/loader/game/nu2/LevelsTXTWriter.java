package com.opengg.loader.game.nu2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LevelsTXTWriter {
    public static void write(Path output, List<LevelsTXTParser.LevelTXTEntry> entries) throws IOException {
        StringBuilder str = new StringBuilder();
        for(var entry : entries){

            var mapType = switch (entry.type()){
                case NORMAL -> "";
                case INTRO -> "intro_level";
                case OUTRO -> "outro_level";
                case MIDTRO -> "midtro_level";
                case STATUS -> "status_level";
                case NEW_GAME -> "newgame_level";
                case LOAD_GAME -> "loadgame_level";
                case TEST -> "test_level";
                case SINGLE_FILE -> throw new IllegalStateException();
            };

             str.append("""
                     level_start
                     \tdir "%s"
                     \tfile "%s"
                     \t%s
                     level_end
                     """.formatted(entry.path(), entry.name(), mapType));
        }

        if(Files.exists(output))
            Files.delete(output);

        Files.writeString(output, str.toString());
    }
}


