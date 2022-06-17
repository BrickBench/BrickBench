package com.opengg.loader.components;

import com.opengg.core.physics.collision.colliders.BoundingBox;

/**
 * Allows an EditorEntity to be selected with the mouse in BrickBench.
 */
public interface Selectable {
    /**
     * Returns the bounding box of this object in engine-space.
     */
    BoundingBox getBoundingBox();

    /**
     * Returns the selection order for this selectable object.
     * Objects with a higher selection order will be selected after objects with a lower one regardless of distance.
     */
    default int getSelectionOrder() {
        return 0;
    }
}
