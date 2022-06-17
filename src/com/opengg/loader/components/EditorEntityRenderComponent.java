package com.opengg.loader.components;

import com.opengg.core.render.Renderable;
import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.world.components.RenderComponent;
import com.opengg.loader.EditorEntity;

/**
 * A utility component used to simplify the management of {@link EditorEntity}s in the OpenGG engine.
 *
 * If this is used, the entity path will be set as the name of this component so that this component
 * can be easily retrieved with {@code WorldEngine.getAllByName(entity.path())}.
 */
public class EditorEntityRenderComponent extends RenderComponent {
    private EditorEntity<?> mapObject;

    public EditorEntityRenderComponent(EditorEntity<?> object, SceneRenderUnit.UnitProperties unitProperties) {
        super(unitProperties);
        this.mapObject = object;
        this.setName(mapObject.path());
    }

    public EditorEntityRenderComponent(EditorEntity<?> object, Renderable renderable, SceneRenderUnit.UnitProperties unitProperties) {
        super(renderable, unitProperties);
        this.mapObject = object;
        this.setName(mapObject.path());
    }
}
