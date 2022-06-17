package com.opengg.loader.game.nu2.scene.commands;

import com.opengg.loader.Util;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.MapEntity;
import com.opengg.loader.game.nu2.scene.FileMaterial;
import com.opengg.loader.game.nu2.scene.GSCMesh;
import com.opengg.loader.loading.MapWriter;

import java.util.List;

/**
 * A DisplayCommand in an NU2 command list.
 */
public record DisplayCommand(int index, int address, int flags, CommandType type, DisplayCommandResource<?> command) implements MapEntity<DisplayCommand> {
    
    /**
     * Execute the command.
     */
    public void run(){
        command.run();
    }

    @Override
    public String name() {
        return "DisplayCommand_" + index;
    }

    @Override
    public String path() {
        return "Render/DisplayCommands/" + name();
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new EnumProperty("Command type", type, false),
                new IntegerProperty("Flags", flags, false),
                new IntegerProperty("Resource address", flags, false),
                new EditorEntityProperty("Resource", command, true, false, null)
        );
    }

    @Override
    public void applyPropertyEdit(String propName, Property newValue) {
        switch (newValue) {
            case EnumProperty ep && propName.equals("Command type") -> {
                var objects = EditorState.getNamespace(EditorState.getActiveNamespace());
                var newResource = switch ((CommandType) ep.value()) {
                    case OTHER, FACEON, DYNAMIC_GEOMETRY, TERMINATE, MTL_CLIP, DUMMY, END, NEXT -> 0;
                    case MTL -> objects.values().stream()
                            .filter(mapObject -> mapObject instanceof FileMaterial)
                            .map(mapObject -> (FileMaterial) mapObject)
                            .findFirst().get().getAddress();
                    case GEOMCALL -> objects.values().stream()
                            .filter(mapObject -> mapObject instanceof GSCMesh)
                            .map(mapObject -> (GSCMesh) mapObject)
                            .findFirst().get().address;
                    case MTXLOAD -> objects.values().stream()
                            .filter(mapObject -> mapObject instanceof MatrixCommandResource)
                            .map(mapObject -> (MatrixCommandResource) mapObject)
                            .findFirst().get().address();
                    case LIGHTMAP -> objects.values().stream()
                            .filter(mapObject -> mapObject instanceof LightmapCommandResource)
                            .map(mapObject -> (LightmapCommandResource) mapObject)
                            .findFirst().get().getAddress();
                };
                var newFlag = switch (((CommandType) ep.value())) {
                    case OTHER, FACEON, DYNAMIC_GEOMETRY, MTL_CLIP, DUMMY, NEXT -> 0;
                    case MTL, GEOMCALL, MTXLOAD, LIGHTMAP -> 3;
                    case END, TERMINATE -> 4;
                };
                MapWriter.applyPatch(MapWriter.WritableObject.SCENE, address,
                        new byte[]{(byte) ((CommandType) ep.value()).id});
                MapWriter.applyPatch(MapWriter.WritableObject.SCENE, address + 1,
                        new byte[]{(byte) newFlag});
                if (newResource != 0) {
                    MapWriter.applyPatch(MapWriter.WritableObject.SCENE, address + 4,
                            Util.littleEndian(newResource - (address + 4)));
                } else {
                    MapWriter.applyPatch(MapWriter.WritableObject.SCENE, address + 4,
                            Util.littleEndian(0));
                }
            }
            case IntegerProperty ip && propName.equals("Flags") -> MapWriter.applyPatch(MapWriter.WritableObject.SCENE, address + 1, new byte[]{(byte) ip.value()});
            case IntegerProperty ip && propName.equals("Resource address") -> MapWriter.applyPatch(MapWriter.WritableObject.SCENE, address + 4, Util.littleEndian(ip.value() - (address + 4)));
            case null, default -> {
            }
        }
    }


    /**
     * An enum of all command types, using the internal NU2 engine names.
     */
    public enum CommandType {
        MTL(0x80),
        GEOMCALL(0x82),
        MTXLOAD(0x83),
        TERMINATE(0x84),
        MTL_CLIP(0x85),
        DUMMY(0x87),
        DYNAMIC_GEOMETRY(0x8b),
        END(0x8e),
        NEXT(0x8d),
        FACEON(0x8f),
        LIGHTMAP(0xb0),
        OTHER(0x0);

        public int id;

        CommandType(int id) {
            this.id = id;
        }

        /**
         * Return the command corresponding to the given command ID.
         */
        public static CommandType getByID(int id){
            return switch (id){
                case 0x80 -> MTL;
                case 0x82 -> GEOMCALL;
                case 0x83 -> MTXLOAD;
                case 0x84 -> TERMINATE;
                case 0x85 -> MTL_CLIP;
                case 0x87 -> DUMMY;
                case 0x8b -> DYNAMIC_GEOMETRY;
                case 0x8d -> NEXT;
                case 0x8e -> END;
                case 0x8f -> FACEON;
                case 0xb0 -> LIGHTMAP;
                default -> OTHER;
            };
        }
    }
}
