package com.opengg.loader.game.nxg;

import com.opengg.loader.EditorEntity;
import com.opengg.loader.MapData;
import com.opengg.loader.MapXml;
import com.opengg.loader.components.MapComponent;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.loading.MapLoader;

import java.io.IOException;
import java.util.Map;

public record NXGMapData(String name, String path, MapXml xmlData) implements MapData {
    @Override
    public MapComponent<?> createEngineComponent() {
        return null;
    }

    @Override
    public NU2MapData loadFile(MapLoader.MapFile file) throws IOException {
        return null;
    }

    @Override
    public Map<String, EditorEntity<?>> getNamespace() {
        return null;
    }
}
