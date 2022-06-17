package com.opengg.loader.game.nu2.scene;

import com.opengg.core.render.Renderable;
import com.opengg.loader.game.nu2.scene.commands.DisplayCommandResource;

public interface GameRenderable<T extends DisplayCommandResource<T>> extends Renderable, DisplayCommandResource<T> {
}
