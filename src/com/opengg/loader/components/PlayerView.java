package com.opengg.loader.components;

import com.opengg.core.Configuration;
import com.opengg.core.math.Quaternionf;
import com.opengg.core.math.Vector2f;
import com.opengg.core.math.Vector3f;
import com.opengg.core.math.Vector3fm;
import com.opengg.core.world.Action;
import com.opengg.core.world.ActionType;
import com.opengg.core.world.Actionable;
import com.opengg.core.world.components.ActionTransmitterComponent;
import com.opengg.core.world.components.CameraComponent;
import com.opengg.core.world.components.ControlledComponent;
import com.opengg.core.world.components.WorldObject;

/**
 * The user view for the OpenGG instance.
 */
public class PlayerView extends ControlledComponent implements Actionable {

    private final Vector3fm control = new Vector3fm();

    private float speed = 8;
    private boolean usingMouse = true;

    public PlayerView(){
        ActionTransmitterComponent actionTransmitter = new ActionTransmitterComponent();
        CameraComponent camera = new CameraComponent();
        WorldObject head = new WorldObject();

        attach(actionTransmitter);
        attach(head);
        head.attach(camera);
    }

    @Override
    public void update(float delta){
        if(usingMouse){
            Vector2f mousepos = getMouse();
            Vector3f currot = new Vector3f(-mousepos.y, -mousepos.x, 0);
            if(Configuration.getBoolean("camera-lock"))
                currot = new Vector3f(Math.min(90, Math.max(-90, -mousepos.y)), -mousepos.x, 0);

            this.setRotationOffset(Quaternionf.createYXZ(currot));
        }

        Vector3f vel = this.getRotation().transform(new Vector3f(control).multiply(delta * speed));
        setPositionOffset(getPositionOffset().add(vel));
    }

    @Override
    public void onAction(Action action) {
        if(action.type == ActionType.PRESS){
            switch (action.name) {
                case "forward" -> control.z -= 1;
                case "backward" -> control.z += 1;
                case "left" -> control.x -= 1;
                case "right" -> control.x += 1;
                case "up" -> control.y += 1;
                case "down" -> control.y -= 1;
                case "fire" -> System.exit(0);
            }
        }else{
            switch (action.name) {
                case "forward" -> control.z += 1;
                case "backward" -> control.z -= 1;
                case "left" -> control.x += 1;
                case "right" -> control.x -= 1;
                case "up" -> control.y -= 1;
                case "down" -> control.y += 1;
            }
        }
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public boolean isUsingMouse() {
        return usingMouse;
    }

    public void setUsingMouse(boolean usingMouse) {
        this.usingMouse = usingMouse;
    }
}
