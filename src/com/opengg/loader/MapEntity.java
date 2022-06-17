package com.opengg.loader;

/**
 * Marker for {@link EditorEntity}s that belong to specific maps.
 */
public interface MapEntity<T extends MapEntity<T>> extends EditorEntity<T> {}
