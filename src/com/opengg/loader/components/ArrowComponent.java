package com.opengg.loader.components;

import com.opengg.core.engine.OpenGG;
import com.opengg.core.io.input.mouse.MouseButtonListener;
import com.opengg.core.io.input.mouse.MouseController;
import com.opengg.core.math.FastMath;
import com.opengg.core.math.Vector3f;
import com.opengg.core.math.geom.Ray;
import com.opengg.core.physics.collision.colliders.BoundingBox;
import com.opengg.core.render.Renderable;
import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.render.internal.opengl.OpenGLRenderer;
import com.opengg.core.render.objects.ObjectCreator;
import com.opengg.core.render.objects.TextureRenderable;
import com.opengg.core.render.shader.ShaderController;
import com.opengg.core.render.texture.Texture;
import com.opengg.core.render.window.WindowController;
import com.opengg.core.world.components.RenderComponent;
import com.opengg.loader.BrickBench;
import com.opengg.loader.Project;
import com.opengg.loader.editor.EditorState;

import java.awt.*;
import java.util.function.Consumer;

/**
 * A component used to present an optionally-draggable XYZ position indicator..
 */
public class ArrowComponent extends RenderComponent implements MouseButtonListener {
    private static Renderable xColorLine, yColorLine, zColorLine;

    private static float LENGTH = 0.2f, RENDER_WIDTH = 0.0015f, SELECTION_WIDTH = 0.015f;

    private Selection currentSelection = Selection.NONE;
    private float initialDistance = 0;

    private Consumer<Vector3f> onRelease;
    private Consumer<Vector3f> perFrame;


    /**
     * @param onRelease A consumer that runs whenever the user stops dragging this component.
     * @param perFrame A consumer that runs every frame the consumer is dragging this component.
     */
    public ArrowComponent(Consumer<Vector3f> onRelease, Consumer<Vector3f> perFrame){
        super(new SceneRenderUnit.UnitProperties().shaderPipeline("xFixOnly").transparency(true));

        if(zColorLine == null){
            var xLine = ObjectCreator.createQuadPrism(new Vector3f(0,-RENDER_WIDTH,-RENDER_WIDTH), new Vector3f(LENGTH, RENDER_WIDTH, RENDER_WIDTH));
            var yLine = ObjectCreator.createQuadPrism(new Vector3f(-RENDER_WIDTH,0,-RENDER_WIDTH), new Vector3f(RENDER_WIDTH, LENGTH, RENDER_WIDTH));
            var zLine = ObjectCreator.createQuadPrism(new Vector3f(-RENDER_WIDTH,-RENDER_WIDTH,0), new Vector3f(RENDER_WIDTH, RENDER_WIDTH, LENGTH));

            xColorLine = new TextureRenderable(xLine, Texture.ofColor(Color.RED));
            yColorLine = new TextureRenderable(yLine, Texture.ofColor(Color.GREEN));
            zColorLine = new TextureRenderable(zLine, Texture.ofColor(Color.BLUE));
        }

        MouseController.onButtonPress(this);

        this.onRelease = onRelease;
        this.perFrame = perFrame;
    }

    @Override
    public void render(){
        OpenGLRenderer.getOpenGLRenderer().setDepthTest(false);
        var normalScale = new Vector3f(this.getPosition().distanceTo(BrickBench.CURRENT.ingamePosition));

        this.setRenderable(xColorLine);
        if(currentSelection == Selection.X){
            this.setScaleOffset(normalScale.multiply(1,2,2));
        }else{
            this.setScaleOffset(normalScale);
        }
        super.render();

        this.setRenderable(yColorLine);
        if(currentSelection == Selection.Y){
            this.setScaleOffset(normalScale.multiply(2,1,2));
        }else{
            this.setScaleOffset(normalScale);
        }
        super.render();

        this.setRenderable(zColorLine);
        if(currentSelection == Selection.Z){
            this.setScaleOffset(normalScale.multiply(2,2,1));
        }else{
            this.setScaleOffset(normalScale);
        }
        super.render();

        OpenGLRenderer.getOpenGLRenderer().setDepthTest(true);
        this.setScaleOffset(normalScale);
    }

    @Override
    public void update(float delta){
        if(currentSelection != Selection.NONE){
            BrickBench.CURRENT.player.setUsingMouse(false);
            WindowController.getWindow().setCursorLock(false);

            var pos = this.getPosition();
            var ray = MouseController.getRay();

            var secondRay = switch (currentSelection){
                case X -> new Ray(pos.multiply(-1,1,1), new Vector3f(1,0,0));
                case Y -> new Ray(pos.multiply(-1,1,1), new Vector3f(0,1,0));
                case Z -> new Ray(pos.multiply(-1,1,1), new Vector3f(0,0,1));
                case NONE -> throw new IllegalStateException("Race condition?");
            };

            var approach = FastMath.closestApproach(ray.getRay(), secondRay);
            var intersection = approach[1].multiply(-1,1,1);

            var shift = switch (currentSelection){
                case X -> initialDistance - (intersection.x - pos.x);
                case Y -> initialDistance - (intersection.y - pos.y);
                case Z -> initialDistance - (intersection.z - pos.z);
                case NONE -> throw new IllegalStateException("Race condition?");
            };

            switch (currentSelection){
                case X -> this.setPositionOffset(pos.x - shift, pos.y, pos.z);
                case Y -> this.setPositionOffset(pos.x, pos.y - shift, pos.z);
                case Z -> this.setPositionOffset(pos.x, pos.y, pos.z - shift);
            }

            perFrame.accept(this.getPosition());
        }
    }

    @Override
    public void finalizeComponent() {
        super.finalizeComponent();
        MouseController.removeButtonListener(this);
    }

    @Override
    public void onButtonPress(int button) {
        if(!EditorState.getProject().isProject()) return;

        var ray = MouseController.getRay();

        var xBounds = new BoundingBox(
                getPosition().add(new Vector3f(0,-SELECTION_WIDTH,-SELECTION_WIDTH).multiply(getScaleOffset())).multiply(-1,1,1),
                getPosition().add(new Vector3f(LENGTH,SELECTION_WIDTH,SELECTION_WIDTH).multiply(getScaleOffset())).multiply(-1,1,1));

        var yBounds = new BoundingBox(
                getPosition().add(new Vector3f(-SELECTION_WIDTH,0,-SELECTION_WIDTH).multiply(getScaleOffset())).multiply(-1,1,1),
                getPosition().add(new Vector3f(SELECTION_WIDTH,LENGTH,SELECTION_WIDTH).multiply(getScaleOffset())).multiply(-1,1,1));

        var zBounds = new BoundingBox(
                getPosition().add(new Vector3f(-SELECTION_WIDTH,-SELECTION_WIDTH,0).multiply(getScaleOffset())).multiply(-1,1,1),
                getPosition().add(new Vector3f(SELECTION_WIDTH,SELECTION_WIDTH,LENGTH).multiply(getScaleOffset())).multiply(-1,1,1));

        var xColl = xBounds.getCollision(ray.getRay());
        var yColl = yBounds.getCollision(ray.getRay());
        var zColl = zBounds.getCollision(ray.getRay());

        if(xColl.isPresent() && yColl.isEmpty() && zColl.isEmpty()){
            currentSelection = Selection.X;
            initialDistance = (xColl.get().x - (this.getPosition().x*-1f))*-1f;
        }else if(xColl.isEmpty() && yColl.isPresent() && zColl.isEmpty()){
            currentSelection = Selection.Y;
            initialDistance = yColl.get().y - (this.getPosition().y);
        }else if(xColl.isEmpty() && yColl.isEmpty() && zColl.isPresent()){
            currentSelection = Selection.Z;
            initialDistance = zColl.get().z - (this.getPosition().z);
        }else{
            currentSelection = Selection.NONE;
        }
    }

    @Override
    public void onButtonRelease(int button) {
        OpenGG.asyncExec(() -> {
            if(currentSelection != Selection.NONE){
                onRelease.accept(getPosition());
                currentSelection = Selection.NONE;
            }
        });
    }

    private enum Selection{
        X,Y,Z,NONE
    }
}
