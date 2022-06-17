package com.opengg.loader.game.nu2.scene.commands;

import java.util.List;

/**
 * A resource that is either unimplemented or has no special functionality.
 */
public record UntypedCommandResource(int address, DisplayCommand.CommandType type) implements DisplayCommandResource<UntypedCommandResource> {
    @Override
    public void run() { }

    @Override
    public int getAddress() {
        return address;
    }

    @Override
    public DisplayCommand.CommandType getType() {
        return type;
    }

    @Override
    public String name() {
        return type.name() + "_" + address;
    }

    @Override
    public String path() {
        return "Render/GenericCommand/" + type;
    }

    @Override
    public List<Property> properties() {
        return List.of();
    }
}
