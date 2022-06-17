package com.opengg.loader.components;

import com.opengg.core.world.components.Component;
import com.opengg.loader.MapData;
import com.opengg.loader.loading.MapWriter;

/**
 * Abstract parent for components used to render the state of a {@link MapData}
 * in the OpenGG instance.
 */
public abstract class MapComponent<T extends MapData> extends Component {
    public abstract void updateMapData(T data);
    public abstract void updateItemType(MapWriter.WritableObject type);
}

