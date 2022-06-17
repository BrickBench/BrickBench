package com.opengg.loader.game.nu2;

import com.opengg.loader.MapXml;
import com.opengg.loader.loading.TXTParser;

import java.util.ArrayList;
import java.util.List;

public class LevelsTXTParser extends TXTParser{
    private String path;
    private String name;
    private MapXml.MapType type;

    private List<LevelTXTEntry> entries = new ArrayList<>();

    @Override
    protected void parseBlockStart(String[] tokens) {
        type = MapXml.MapType.NORMAL;
    }

    @Override
    protected void parseBlockEnd(String[] tokens) {
        entries.add(new LevelTXTEntry(path, name, type));
    }

    @Override
    protected void parseBlockAttribute(String[] tokens) {
        switch (tokens[0]){
            case "dir" -> path = tokens[1];
            case "file" -> name = tokens[1];
            case "intro_level" -> type = MapXml.MapType.INTRO;
            case "outro_level" -> type = MapXml.MapType.OUTRO;
            case "midtro_level" -> type = MapXml.MapType.MIDTRO;
            case "status_level" -> type = MapXml.MapType.STATUS;
            case "newgame_level" -> type = MapXml.MapType.NEW_GAME;
            case "loadgame_level" -> type = MapXml.MapType.LOAD_GAME;
            case "test_level" -> type = MapXml.MapType.TEST;
        }
    }

    public List<LevelTXTEntry> getEntries(){
        return entries;
    }

    public record LevelTXTEntry(String path, String name, MapXml.MapType type){}
}
