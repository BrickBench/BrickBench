package com.opengg.loader.game.nu2.scene.commands;

import com.opengg.loader.MapEntity;

/**
 * A resource that can be executed in a display command.
 *
 * These often correspond to the items a display command points to.
 */
public interface DisplayCommandResource<T extends DisplayCommandResource<T>> extends MapEntity<T> {
    /**
     * Execute this resource.
     */
    void run();

    /**
     * Return the address of this resource.
     */
    int getAddress();

    /**
     * Return the command type that this resource is used in.
     */
    DisplayCommand.CommandType getType();
}

