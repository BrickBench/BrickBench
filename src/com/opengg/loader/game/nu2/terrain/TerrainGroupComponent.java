package com.opengg.loader.game.nu2.terrain;

import com.opengg.core.Configuration;
import com.opengg.core.math.Vector3f;
import com.opengg.core.render.SceneRenderUnit;
import com.opengg.core.render.internal.opengl.OpenGLRenderer;
import com.opengg.core.render.objects.DrawnObject;
import com.opengg.core.render.objects.TextureRenderable;
import com.opengg.core.render.shader.ShaderController;
import com.opengg.core.render.shader.VertexArrayBinding;
import com.opengg.core.render.shader.VertexArrayFormat;
import com.opengg.core.render.texture.Texture;
import com.opengg.loader.components.EditorEntityRenderComponent;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.terrain.TerrainGroup;

import java.awt.*;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.nio.ByteOrder;
import java.util.List;
import java.util.stream.Collectors;

import static com.opengg.loader.game.nu2.terrain.TerrainGroup.TerrainProperty.*;

public class TerrainGroupComponent extends EditorEntityRenderComponent {
    private static final VertexArrayFormat collisionFormat = new VertexArrayFormat(java.util.List.of(
            new VertexArrayBinding(0, 9 * 4, 0, List.of(
                    new VertexArrayBinding.VertexArrayAttribute("position", 3 * 4, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT3, 0),
                    new VertexArrayBinding.VertexArrayAttribute("normal", 3 * 4, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT3, 3 * 4),
                    new VertexArrayBinding.VertexArrayAttribute("color", 3 * 4, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT3, 6 * 4)
            ))
    ));

    private final TerrainGroup terrainGroup;

    public TerrainGroupComponent(TerrainGroup group) {
        super(group, new SceneRenderUnit.UnitProperties().transparency(false).shaderPipeline("collision").format(collisionFormat));

        this.terrainGroup = group;

        var allFaces = group.blocks().stream().flatMap(b -> b.faces().stream()).collect(Collectors.toList());

        try (var scope = MemorySession.openConfined()) {
            var fb = MemorySegment.allocateNative(allFaces.size() * 6 * collisionFormat.getPrimaryVertexLength(), scope).asByteBuffer().order(ByteOrder.nativeOrder());
            for(var face : allFaces){
                Vector3f newV1 = face.vec1();
                Vector3f newV2 = face.vec2();
                Vector3f newV3 = face.vec3();
                Vector3f newV4 = face.vec4();

                int flag1 = face.flag1();
                int flag2 = face.flag2();

                //flags
                TerrainGroup.TerrainProperty collisionType = switch (flag2){
                    case 0 -> switch (flag1){
                        case 0 -> NONE;
                        case 1 -> FASTKILL;
                        case 2 -> REFLECTIVE_FLOOR; //not visualized
                        case 6 -> BUTTON;
                        case 9 -> ICE;
                        case 12 -> ENERGY_WALL;
                        case 20, 14, 15 -> MAP_CUSTOM_FLOOR;
                        case 16 -> SLIP;
                        case 19 -> PUSHBLOCK_SURFACE;
                        case 22 -> EDGE;
                        case 24 -> GAME_MOVABLE;
                        case 25 -> FORCE_MOVABLE;
                        case 26 -> STOP_HOVER;
                        case 27 -> METAL_OBJECT;
                        case 30, 31 -> SPINNER_SIDE;
                        default -> // System.out.println(flag1 + " " + flag2);
                                UNKNOWN;
                    };
                    case 1 -> WATER;
                    case 3 -> INSTAKILL;
                    case 6 -> SLOWKILL;
                    case 8 -> PUSHBLOCK_SURFACE;
                    case 9 -> R2_SWAMP_WATER;
                    default -> //System.out.println(flag1 + " " + flag2);
                            UNKNOWN;
                };

                Vector3f norm2;
                if(face.norm2().equals(new Vector3f(0,65536.0f,0))){
                    newV4 = newV1;
                    norm2 = face.norm1();
                }else{
                    norm2 = face.norm2();
                }

                fb.putFloat(newV1.x).putFloat(newV1.y).putFloat(newV1.z).putFloat(face.norm1().x).putFloat(face.norm1().y).putFloat(face.norm1().z).putFloat(collisionType.color.x).putFloat(collisionType.color.y).putFloat(collisionType.color.z);
                fb.putFloat(newV2.x).putFloat(newV2.y).putFloat(newV2.z).putFloat(face.norm1().x).putFloat(face.norm1().y).putFloat(face.norm1().z).putFloat(collisionType.color.x).putFloat(collisionType.color.y).putFloat(collisionType.color.z);
                fb.putFloat(newV3.x).putFloat(newV3.y).putFloat(newV3.z).putFloat(face.norm1().x).putFloat(face.norm1().y).putFloat(face.norm1().z).putFloat(collisionType.color.x).putFloat(collisionType.color.y).putFloat(collisionType.color.z);
                fb.putFloat(newV2.x).putFloat(newV2.y).putFloat(newV2.z).putFloat(norm2.x).putFloat(norm2.y).putFloat(norm2.z).putFloat(collisionType.color.x).putFloat(collisionType.color.y).putFloat(collisionType.color.z);
                fb.putFloat(newV4.x).putFloat(newV4.y).putFloat(newV4.z).putFloat(norm2.x).putFloat(norm2.y).putFloat(norm2.z).putFloat(collisionType.color.x).putFloat(collisionType.color.y).putFloat(collisionType.color.z);
                fb.putFloat(newV3.x).putFloat(newV3.y).putFloat(newV3.z).putFloat(norm2.x).putFloat(norm2.y).putFloat(norm2.z).putFloat(collisionType.color.x).putFloat(collisionType.color.y).putFloat(collisionType.color.z);
            }
            fb.flip();
            setRenderable(new TextureRenderable(DrawnObject.create(collisionFormat, fb.asFloatBuffer()), Texture.ofColor(Color.getHSBColor((float) Math.random(), 1, 0.5f ))));
        }


        if(group.isTerrainPlatform()){
            if(Configuration.getBoolean("use-rotation-platform")){
                this.setOverrideMatrix(
                        terrainGroup.platformObject().get().iablObj().transform());
            }else{
                this.setPositionOffset(terrainGroup.platformObject().get().pos());
            }
        }else{
            this.setPositionOffset(group.position());
        }
    }

    @Override
    public void render(){
        OpenGLRenderer.getOpenGLRenderer().setBackfaceCulling(true);
        
        if(EditorState.CURRENT.shouldHighlight && EditorState.getSelectedObject().get() instanceof TerrainGroup obj && obj != this.terrainGroup) {
            ShaderController.setUniform("muteColors", 1);
        }else{
            ShaderController.setUniform("muteColors", 0);
        }

        super.render();

        OpenGLRenderer.getOpenGLRenderer().setBackfaceCulling(false);
    }

    public TerrainGroup getTerrainGroup() {
        return terrainGroup;
    }
}
