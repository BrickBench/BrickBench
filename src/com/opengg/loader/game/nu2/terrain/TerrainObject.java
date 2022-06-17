package com.opengg.loader.game.nu2.terrain;

import com.opengg.loader.MapEntity;

/**
 * An object located in an NU2 terrain file.
 *
 * Each object has a fixed-size header definition located in a list at the end of the TER file,
 * and a variable-sized contents area before the definition area.
 */
public interface TerrainObject extends MapEntity<TerrainObject> {
    /**
     * The address of the definition area for this object.
     */
    int objectDefinitionAddress();
    /**
     * The address of the contents area for this object.
     */
    int objectContentsAddress();
    /**
     * The size of this object's contents area.
     */
    int objectContentsSize();
}
