package com.opengg.loader;

import com.opengg.loader.components.MapComponent;
import com.opengg.loader.loading.MapLoader;

import java.io.IOException;
import java.util.Map;

/**
 * A generic interface for any class that contains the current state of a map.
 *
 * This interface is used to hold map information for loaded map files.
 */
public interface MapData {
    /**
     * Returns the name of this map.
     */
    default String name() {
        return xmlData().name();
    }

    /**
     * Returns a {@link MapXml} containing a list of the files used for this map.
     */
    MapXml xmlData();

    /**
     * Create an OpenGG component to render this map.
     */
    MapComponent<?> createEngineComponent();

    /**
     * Reload the given file for this map.
     */
    MapData loadFile(MapLoader.MapFile file) throws IOException;

    /**
     * Return the namespace for this map, containing all referrable {@link EditorEntity}.
     */
    Map<String, EditorEntity<?>> getNamespace();

    /**
     * Nicer cast.
     */
    default <T2 extends MapData> T2 as() {
        return (T2) this;
    }
}
