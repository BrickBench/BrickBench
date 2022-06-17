package com.opengg.loader.components;

import com.opengg.core.math.Quaternionf;
import com.opengg.core.math.Vector3f;
import com.opengg.core.render.Renderable;
import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.render.objects.ObjectCreator;
import com.opengg.core.render.objects.TextureRenderable;
import com.opengg.core.render.texture.Texture;
import com.opengg.core.world.components.RenderComponent;
import com.opengg.loader.editor.hook.TCSHookManager;

import java.awt.*;

/**
 * Renderer for characters sourced from an open game hook.
 */
public class HookCharacterManager extends RenderComponent {
    private Renderable enemies;
    private Renderable playerOne;
    private Renderable playerTwo;

    public HookCharacterManager() {
        super(null, new SceneRenderUnit.UnitProperties().shaderPipeline("xFixOnly"));
        this.setShader("xFixOnly");

        var object = ObjectCreator.createQuadPrism(new Vector3f(0,0f,-0.1f),
                new Vector3f(0.15f,0.5f,0.1f));

        enemies = new TextureRenderable(object, Texture.ofColor(Color.ORANGE));
        playerOne = new TextureRenderable(object, Texture.ofColor(Color.BLUE));
        playerTwo = new TextureRenderable(object, Texture.ofColor(Color.GREEN));
    }

    @Override
    public void render(){
        if(TCSHookManager.currentHook == null || TCSHookManager.playerOne == null) return;

        this.setPositionOffset(TCSHookManager.playerOne.pos());
        this.setRotationOffset(Quaternionf.createYXZ(new Vector3f(0,TCSHookManager.playerOne.rot(),0)));
        this.setRenderable(playerOne);
        super.render();

        this.setPositionOffset(TCSHookManager.playerTwo.pos());
        this.setRotationOffset(Quaternionf.createYXZ(new Vector3f(0,TCSHookManager.playerTwo.rot(),0)));
        this.setRenderable(playerTwo);
        super.render();

        for(var character : TCSHookManager.allCharacters){
            this.setPositionOffset(character.pos());
            this.setRotationOffset(Quaternionf.createYXZ(new Vector3f(0,character.rot(),0)));
            this.setRenderable(enemies);
            super.render();
        }
    }
}
