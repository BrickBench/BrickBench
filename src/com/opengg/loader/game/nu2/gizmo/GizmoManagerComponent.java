package com.opengg.loader.game.nu2.gizmo;

import com.opengg.core.engine.Resource;
import com.opengg.core.math.Matrix4f;
import com.opengg.core.math.Quaternionf;
import com.opengg.core.math.Vector3f;
import com.opengg.core.render.Renderable;
import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.render.objects.MatrixRenderable;
import com.opengg.core.render.objects.ObjectCreator;
import com.opengg.core.render.objects.RenderableGroup;
import com.opengg.core.render.objects.TextureRenderable;
import com.opengg.core.render.texture.Texture;
import com.opengg.core.world.components.Component;
import com.opengg.loader.BrickBench;
import com.opengg.loader.components.EditorEntityRenderComponent;
import com.opengg.loader.components.BillBoardRenderable;
import com.opengg.loader.components.NativeCache;

import java.awt.*;
import java.util.List;

public class GizmoManagerComponent extends Component {
    public void addGizmo(Gizmo gizmo){
        var shader = BrickBench.CURRENT.getNU2Things() == null ? "xFixOnly" : "ttNormal";
        final float SIZE = 0.08f;

        if(gizmo instanceof Gizmo.GizPickup pickup){
            if(BrickBench.CURRENT.getNU2Things() != null && !pickup.type().specialObject.isEmpty()){
                var pickupRender = BrickBench.CURRENT.getNU2Things().getSpecialObjectByName(pickup.type().specialObject).get().model();
                Renderable pos;
                if(pickup.type() == Gizmo.GizPickup.PickupType.BLUE_STUD || pickup.type() ==Gizmo.GizPickup.PickupType.GOLD_STUD
                        || pickup.type() ==Gizmo.GizPickup.PickupType.SILVER_STUD || pickup.type() == Gizmo.GizPickup.PickupType.PURPLE_STUD){
                    pos = new BillBoardRenderable(pickupRender,pickup.pos());
                    var gameRenderComponent = new EditorEntityRenderComponent(pickup, pos, new SceneRenderUnit.UnitProperties().shaderPipeline("ttNormal"))
                            .setRenderDistance(10f).setUpdateEnabled(false).setPositionOffset(pickup.pos().multiply(-1,1,1));
                    this.attach(gameRenderComponent);
                }else{
                    pos = new MatrixRenderable(pickupRender, new Matrix4f().translate(pickup.pos()));
                    this.attach(new EditorEntityRenderComponent(pickup, pos, new SceneRenderUnit.UnitProperties().shaderPipeline("ttNormal"))
                            .setRenderDistance(10f).setUpdateEnabled(false).setPositionOffset(pickup.pos().multiply(-1,1,1)));
                }

            }else{
                var renderable = new TextureRenderable(
                        ObjectCreator.createInstancedQuadPrism(new Vector3f((SIZE/2),(SIZE/2), 0),
                                new Vector3f(-(SIZE/2),-(SIZE/2), 0)), Resource.getTexture(pickup.type().path));
                this.attach(new EditorEntityRenderComponent(pickup, new MatrixRenderable(renderable, new Matrix4f().translate(pickup.pos())),
                        new SceneRenderUnit.UnitProperties().shaderPipeline("ttNormal")));
            }
        }else if (gizmo instanceof Gizmo.ZipUp zipup){
            var line = new TextureRenderable(
                    ObjectCreator.createLineList(List.of(zipup.start(), zipup.axis(), zipup.end())),
                    Texture.ofColor(Color.ORANGE));
            this.attach(new EditorEntityRenderComponent(zipup, line, new SceneRenderUnit.UnitProperties().shaderPipeline("xFixOnly")));

            if(BrickBench.CURRENT.getNU2Things() != null){
                var floorTarget = BrickBench.CURRENT.getNU2Things().getSpecialObjectByName("zipup_target").get().model();
                var hook = BrickBench.CURRENT.getNU2Things().getSpecialObjectByName("zipup_hook").get().model();

                var pos1 = new MatrixRenderable(floorTarget, new Matrix4f().translate(zipup.start()));
                var pos2 = new MatrixRenderable(floorTarget, new Matrix4f().translate(zipup.end()));
                var hookPos = new MatrixRenderable(hook, new Matrix4f().translate(zipup.axis()));

                this.attach(new EditorEntityRenderComponent(zipup, RenderableGroup.of(pos1, pos2, hookPos), new SceneRenderUnit.UnitProperties().shaderPipeline("ttNormal")));
            }

        }else if (gizmo instanceof Gizmo.GizPanel panel){

            Renderable renderable;
            if(BrickBench.CURRENT.getNU2Things() == null){
                renderable = new TextureRenderable(
                        ObjectCreator.createQuadPrism(new Vector3f(0f, -0.25f, 0f), new Vector3f(0.2f, 0.2f, 0.2f)), Texture.ofColor(Color.ORANGE));
            }else{
                var baseSpecialObjectName = switch (panel.panelType()){
                    case STORMTROOPER -> "Storm_base";
                    case BOUNTY_HUNTER -> "Bounty_base";
                    case PROTOCOL_DROID -> "C3PO_base";
                    case ASTROMECH -> panel.alternativeColor() == Gizmo.Option.YES ? "R2_base1" : "R2_base";
                    case UNDEFINED -> "C3PO_base";
                };
                var baseRenderable = BrickBench.CURRENT.getNU2Things().getSpecialObjectByName(baseSpecialObjectName).get().model();
                var targetSpecialObjectName = switch (panel.panelType()){
                    case STORMTROOPER -> "Storm_Target";
                    case BOUNTY_HUNTER -> "Bounty_Target";
                    default -> "zipup_target";
                };

                var platformRenderable = BrickBench.CURRENT.getNU2Things().getSpecialObjectByName(targetSpecialObjectName).get().model();
                renderable = RenderableGroup.of(baseRenderable, new MatrixRenderable(platformRenderable, new Matrix4f().translate(panel.activationPos())));
            }

            this.attach(new EditorEntityRenderComponent(panel, renderable, new SceneRenderUnit.UnitProperties().shaderPipeline(shader))
                            .setPositionOffset(panel.pos())
                            .setRotationOffset(Quaternionf.createXYZ(new Vector3f(0, panel.angle(), 0))));
        }else if (gizmo instanceof Gizmo.Lever lever){
            Renderable renderable;
            if(BrickBench.CURRENT.getNU2Things() == null){
                renderable = new TextureRenderable(
                        ObjectCreator.createQuadPrism(new Vector3f(0f, -0.25f, 0f), new Vector3f(0.2f, 0.2f, 0.2f)), Texture.ofColor(Color.ORANGE));
            }else {
                var colorName = switch (lever.studColor()){
                    case YELLOW -> "lever_nob1";
                    case ORANGE -> "lever_nob2";
                    case RED -> "lever_nob3";
                    case BLUE -> "lever_nob4";
                    case GREEN -> "lever_nob5";
                    case PURPLE -> "lever_nob6";
                   // case BR -> "lever_nob7"; //brown?
                    case LIGHT_BLUE -> "lever_nob8";
                  //  case YELLOW -> "lever_nob9"; lime?
                    case NONE -> "lever_nob1";
                };
                var baseRenderable = BrickBench.CURRENT.getNU2Things().getSpecialObjectByName("lever_base").get().model();
                var armRenderable = BrickBench.CURRENT.getNU2Things().getSpecialObjectByName("lever").get().model();
                var targetRenderable = BrickBench.CURRENT.getNU2Things().getSpecialObjectByName("zipup_target").get().model();
                var nubRenderable = BrickBench.CURRENT.getNU2Things().getSpecialObjectByName(colorName).get().model();

                renderable = RenderableGroup.of(baseRenderable, armRenderable, nubRenderable, new MatrixRenderable(targetRenderable, new Matrix4f().translate(lever.activationPos())));
            }

            this.attach(
                    new EditorEntityRenderComponent(lever, renderable, new SceneRenderUnit.UnitProperties().shaderPipeline(shader))
                            .setPositionOffset(lever.pos())
                            .setRotationOffset(Quaternionf.createXYZ(new Vector3f(0, lever.angle(), 0))));
        }else if (gizmo instanceof Gizmo.HatMachine hat){
            Renderable renderable;
            if(BrickBench.CURRENT.getNU2Things() == null){
                renderable = new TextureRenderable(
                        ObjectCreator.createQuadPrism(new Vector3f(0f, -0.25f, 0f), new Vector3f(0.2f, 0.2f, 0.2f)), Texture.ofColor(Color.ORANGE));
            }else {
                var hatName = switch (hat.type()){
                    case BOUNTY_HUNTER -> "bountyHelmet";
                    case STORMTROOPER -> "stormTrooperHelmet";
                    case FEDORA -> "hat_3";
                    case LEIA ->  "hat_1";
                    case TOP_HAT -> "hat_4";
                    case BASEBALL_CAP -> "hat_5";
                    case DROID_PANEL -> "R2_base";
                    case RANDOM -> "info";
                };
                var colorName = switch (hat.studColor()){
                    case YELLOW -> "lever_nob1";
                    case ORANGE -> "lever_nob2";
                    case RED -> "lever_nob3";
                    case BLUE -> "lever_nob4";
                    case GREEN -> "lever_nob5";
                    case PURPLE -> "lever_nob6";
                    // case BR -> "lever_nob7"; //brown?
                    case LIGHT_BLUE -> "lever_nob8";
                    //  case YELLOW -> "lever_nob9"; lime?
                    case NONE -> "lever_nob1";
                };

                var baseRenderable = BrickBench.CURRENT.getNU2Things().getSpecialObjectByName("Hat_machine_base").get().model();
                var dragRenderable = BrickBench.CURRENT.getNU2Things().getSpecialObjectByName("Hat_machine_out").get().model();
                var leverRenderable = BrickBench.CURRENT.getNU2Things().getSpecialObjectByName("lever_helmet").get().model();
                var hatRenderable = BrickBench.CURRENT.getNU2Things().getSpecialObjectByName(hatName).get().model();
                var targetRenderable = BrickBench.CURRENT.getNU2Things().getSpecialObjectByName("zipup_target").get().model();
                var nubRenderable = BrickBench.CURRENT.getNU2Things().getSpecialObjectByName(colorName).get().model();

                renderable = RenderableGroup.of(nubRenderable, baseRenderable, dragRenderable, leverRenderable, hatRenderable, new MatrixRenderable(targetRenderable, new Matrix4f().translate(hat.activationPos())));
            }
            this.attach(
                    new EditorEntityRenderComponent(hat, renderable, new SceneRenderUnit.UnitProperties().shaderPipeline(shader))
                            .setPositionOffset(hat.pos())
                            .setRotationOffset(Quaternionf.createXYZ(new Vector3f(0, hat.angle(), 0))));
        }else if(gizmo instanceof Gizmo.Tube gizTube){
            var renderable = new MatrixRenderable(
                new TextureRenderable(
                        NativeCache.CYLINDER,
                    Texture.ofColor(Color.PINK, 0.6f)),
                new Matrix4f().scale(gizTube.radius(), gizTube.height(), gizTube.radius())
                    .translate(gizTube.pos().add(new Vector3f(0, gizTube.height(), 0))));
            this.attach(new EditorEntityRenderComponent(gizmo, renderable, 
                new SceneRenderUnit.UnitProperties().shaderPipeline("xFixOnly").transparency(true)).setPositionOffset(gizmo.pos()));
        }
    }
}
