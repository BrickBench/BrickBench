package com.opengg.loader.game.nu2.scene;

import com.opengg.core.engine.Resource;
import com.opengg.loader.Util;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.loading.MapLoader;
import com.opengg.loader.loading.MapWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.opengg.loader.loading.MapWriter.WritableObject.SCENE;

public class MaterialWriter {
    public static List<Integer> createMaterials(List<MaterialType> newMaterialTypes) throws IOException {
        var scene = EditorState.getActiveMap().levelData().<NU2MapData>as().scene();

        var materials = List.copyOf(scene.materials().values());
        int lastMaterialAddr = materials.get(materials.size()-1).getAddress() + 0x2C4;
        int materialCountAddr = scene.blocks().get("MS00").address() + 8;
        int gsnhMaterialCountAddr = scene.blocks().get("GSNH").address() + 8 + 0x10;
        MapWriter.applyPatch(MapWriter.WritableObject.SCENE, materialCountAddr, Util.littleEndian(materials.size() + newMaterialTypes.size()));
        MapWriter.applyPatch(MapWriter.WritableObject.SCENE, gsnhMaterialCountAddr, Util.littleEndian(materials.size() + newMaterialTypes.size()));

        var addresses = new ArrayList<Integer>();
        var materialBuffer = ByteBuffer.allocate(0x2C4 * newMaterialTypes.size()).order(ByteOrder.LITTLE_ENDIAN);
        for(int i = 0; i < newMaterialTypes.size(); i++){
            int startAddr = materialBuffer.position();
            addresses.add(startAddr + lastMaterialAddr);
            materialBuffer.put(newMaterialTypes.get(i).getBytes())
                    .position(startAddr + 0x38)
                    .putInt(materials.size() + i)
                    .putInt(materials.get(0).mysteryPointer - (lastMaterialAddr + startAddr + 0x3C - (0x2C4 * newMaterialTypes.size())));

            materialBuffer.position(startAddr + 0x2C4);
        }

        SceneFileWriter.addSpace(lastMaterialAddr, 0x2C4 * newMaterialTypes.size());
        MapWriter.applyPatch(MapWriter.WritableObject.SCENE, lastMaterialAddr, materialBuffer);
        for(int i = 0; i < newMaterialTypes.size(); i++){
            int addr = lastMaterialAddr + (i * 0x2C4) + 0x3C;
            SceneFileWriter.addPointer(addr);
        }

        EditorState.updateMap(MapLoader.reloadIndividualFile("gsc"));

        createEntriesInGSNH(lastMaterialAddr, newMaterialTypes.size());

        return addresses;
    }

    public static void createEntriesInGSNH(int start, int amount) throws IOException {
        var scene = EditorState.getActiveMap().levelData().<NU2MapData>as().scene();

        var doublePtrAddress = scene.blocks().get("GSNH").address() + 8 +
                scene.materialListAddressFromGSNH().get() +
                scene.materials().size() * 4 - (4 * amount);

        SceneFileWriter.addSpace(doublePtrAddress, 0x4 * amount);
        for(int i = 0; i < amount;  i++){
            int target = start + (i * 0x2C4);
            int ptr = doublePtrAddress + i*4;
            MapWriter.applyPatch(SCENE, ptr, Util.littleEndian(target - ptr));
            SceneFileWriter.addPointer(ptr);
        }
    }

    public enum MaterialType{
        COLOR_FLAT("color_flat.bin"),
        COLOR_PHONG("color_phong.bin"),
        TEXTURE_FLAT("texture_flat.bin");

        String file;
        private byte[] bytes;

        MaterialType(String file){
            this.file = file;
        }

        public byte[] getBytes() throws IOException {
            if(bytes == null){
                bytes = Files.readAllBytes(Path.of(Resource.getApplicationPath().toString(), "resources", "dat", file));
            }

            return bytes;
        }
    }
}
