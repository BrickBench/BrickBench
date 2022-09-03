package com.opengg.loader.game.nu2.scene.commands;

import com.opengg.core.math.Vector4f;
import com.opengg.core.render.shader.ShaderController;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.FileTexture;

import java.util.List;

public class LightmapCommandResource implements DisplayCommandResource<LightmapCommandResource> {
    private FileTexture texture;
    private FileTexture texture2;
    private FileTexture texture3;
    private FileTexture texture4;
    private float offsetX;
    private float offsetY;
    private float offsetZ;
    private float offsetW;

    private int address;
    private int lightmapType;

    private boolean multipleMaps = false;
    public LightmapCommandResource(int address, int lightmapType, int id, int id2, int id3, int id4,float offsetX,float offsetY, float offsetZ, float offsetW, NU2MapData mapData){
        texture = mapData.scene().texturesByRealIndex().get(id);
        texture2 = mapData.scene().texturesByRealIndex().get(id2);
        texture3 = mapData.scene().texturesByRealIndex().get(id3);
        texture4 = mapData.scene().texturesByRealIndex().get(id4);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.offsetW = offsetW;
        this.lightmapType = lightmapType;
        this.address = address;
    }

    public LightmapCommandResource(int address, int id, NU2MapData mapData){
        this.address = address;
        texture = mapData.scene().texturesByRealIndex().get(id);
    }

    @Override
    public void run() {
        if(lightmapType == 2 || lightmapType == 4){
            ShaderController.setUniform("lightmap1", texture.nativeTexture().getNow(null));
            ShaderController.setUniform("lightmap2", texture2.nativeTexture().getNow(null));
            ShaderController.setUniform("lightmap3", texture3.nativeTexture().getNow(null));
            ShaderController.setUniform("lightmap4", texture4.nativeTexture().getNow(null));
            if(lightmapType == 4){
                ShaderController.setUniform("lightmapOffset",new Vector4f(offsetX,offsetY,offsetZ,offsetW));
            }else{
                ShaderController.setUniform("lightmapOffset",new Vector4f(offsetX,offsetY,1,1));
            }
        }else if(lightmapType == 1 || lightmapType == 3){
            ShaderController.setUniform("lightmap1", texture.nativeTexture().getNow(null));
            if(lightmapType == 3){
                ShaderController.setUniform("lightmapOffset",new Vector4f(offsetX,offsetY,offsetZ,offsetW));
            }else{
                ShaderController.setUniform("lightmapOffset",new Vector4f(offsetX,offsetY,1,1));
            }
        }else{
            System.out.println("edge case");
        }
    }

    @Override
    public int getAddress() {
        return 0;
    }

    @Override
    public DisplayCommand.CommandType getType() {
        return DisplayCommand.CommandType.LIGHTMAP;
    }

    @Override
    public String name() {
        return "Lightmap_" + address;
    }

    @Override
    public String path() {
        return "Render/Lightmaps/" + name();
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new EditorEntityProperty("Lightmap 1", texture, false, true, "Scene/Textures/"),
                new EditorEntityProperty("Lightmap 2", texture2, false, true, "Scene/Textures/"),
                new EditorEntityProperty("Lightmap 3", texture3, false, true, "Scene/Textures/"),
                new EditorEntityProperty("Lightmap 4", texture4, false, true, "Scene/Textures/"));
    }
}
