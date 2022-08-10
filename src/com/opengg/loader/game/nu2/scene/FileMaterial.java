package com.opengg.loader.game.nu2.scene;

import com.opengg.core.engine.Resource;
import com.opengg.core.math.Vector3f;
import com.opengg.core.math.Vector4f;
import com.opengg.core.render.internal.opengl.OpenGLRenderer;
import com.opengg.core.render.shader.ShaderController;
import com.opengg.core.render.shader.VertexArrayBinding;
import com.opengg.core.render.texture.Texture;
import com.opengg.loader.BrickBench;
import com.opengg.loader.Project;
import com.opengg.loader.Util;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.scene.commands.DisplayCommand;
import com.opengg.loader.game.nu2.scene.commands.DisplayCommandResource;
import com.opengg.loader.loading.MapLoader;
import com.opengg.loader.loading.MapWriter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FileMaterial implements DisplayCommandResource<FileMaterial> {
    public static FileMaterial currentMaterial;
    public static boolean ENABLE_DEPTH_EMULATION = true;
    public static boolean ENABLE_ALPHA_EMULATION = true;

    private int formatBits = 0;
    private int inputDefinesBits = 0;
    private int shaderDefinesBits = 0;
    private int uvSetCoords = 0;
    private int lightmapSetIndex = 0;
    private int specularSetIndex = 0;
    private int surfaceUVIndex = 0;
    private int textureFlags = 0;

    private AlphaBlendType alphaBlendType;
    private DepthType depthType;

    public int mysteryPointer;

    private int alphaType = 0;
    private Vector4f color = new Vector4f(0, 0, 0, 1);
    private Vector4f specular = new Vector4f();
    private Vector4f reflectivity = new Vector4f();
    
    private Map<String, Integer> defines = new LinkedHashMap<>();

    boolean loadedTextures = false;

    private FileTexture fileDiffuse;
    private FileTexture fileNormal;
    private FileTexture fileSpecular;
    
    private CompletableFuture<ImageIcon> icon;

    private final List<VertexArrayBinding.VertexArrayAttribute> arrayFormat = new ArrayList<>();
    private int ID;
    private int fileAddress;

    public FileMaterial(int fileAddress) {
        this.fileAddress = fileAddress;
    }

    public void loadTextures() {
        if (!loadedTextures) {
            if (fileDiffuse != null) {
                icon = fileDiffuse.icon();
            }else{
                var image = new BufferedImage(95, 95, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();

                var colorPart = color.truncate();
                colorPart = new Vector3f(Math.max(0,Math.min(1, colorPart.x)), Math.max(0,Math.min(1, colorPart.y)), Math.max(0,Math.min(1, colorPart.z)));

                graphics.setPaint(new Color(colorPart.x, colorPart.y, colorPart.z));
                graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                icon = new CompletableFuture<>();
                icon.complete(new ImageIcon(Util.getScaledImage(95, 95, image)));
            }
        }
    }

    public void generateFormat() {
        if (fileDiffuse != null) {
            if ((formatBits & 0x3800) == 0) {
                formatBits = formatBits & 0xffffcfff | 0x800;
            }
        }

        int normalType;
        int texType;
        int tangentType;
        int local_24;
        int tangentType2;
        int local_1c;
        int halfFloatTexType;
        int colorFlag1;

        if (((formatBits & 8) == 0) && ((formatBits & 0x880000) == 0)) {
            normalType = formatBits >> 2 & 1;
        } else {
            normalType = 2;
        }
        if (((formatBits & 0x20) == 0) && ((formatBits & 0x1000000) == 0)) {
            tangentType = formatBits >> 4 & 1;
        } else {
            tangentType = 2;
        }

        tangentType2 = 2;
        if ((formatBits & 0x2000080) == 0) {
            tangentType2 = formatBits & 0x40 & 1;
        }

        colorFlag1 = formatBits >> 8 & 1;
        if ((formatBits >> 0x1b & 1) == 0) {
            texType = formatBits >> 0xb & 7;
            halfFloatTexType = 0;
        } else {
            texType = 0;
            halfFloatTexType = formatBits >> 0xb & 7;
        }
        if ((formatBits & 0x8000) == 0) {
            local_1c = formatBits >> 0xe & 1;
        } else {
            local_1c = 2;
        }
        if ((formatBits & 0x20000) == 0) {
            local_24 = formatBits >> 0x10 & 1;
        } else {
            local_24 = 2;
        }
        int local_c = formatBits >> 0x1a & 1;
        int local_4 = formatBits >> 0x16 & 1;
        arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("position", 3 * 4, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT3, 0));
        int offset = 0xc;

        if (normalType == 1) {
            arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("vs_normal", 3 * 4, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT3, 3 * 4));
            offset += 0xc;
        } else if (normalType == 2) {
            arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("vs_normal", 4, VertexArrayBinding.VertexArrayAttribute.Type.UNSIGNED_BYTE, 3 * 4));
            offset += 4;
        }

        if (tangentType == 1) {
            arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("tangent", 3 * 4, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT3, offset));
            offset = offset + 0xc;
        } else if (tangentType == 2) {
            arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("tangent", 4, VertexArrayBinding.VertexArrayAttribute.Type.BYTE, offset));
            offset = offset + 4;
        }

        if (tangentType2 == 1) {
            arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("bitangent", 3 * 4, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT3, offset));
            offset = offset + 0xc;
        } else if (tangentType2 == 2) {
            arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("bitangent", 4, VertexArrayBinding.VertexArrayAttribute.Type.BYTE, offset));
            offset = offset + 4;
        }

        if (colorFlag1 != 0) {
            arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("color", 4, VertexArrayBinding.VertexArrayAttribute.Type.UNSIGNED_BYTE, offset));
            defines.put("LAYER0_COLORSET", 1);
            offset = offset + 4;
        } else {
            defines.put("LAYER0_COLORSET", 0);

        }
        if ((formatBits & 0x600) != 0) {
            arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("color2", 4, VertexArrayBinding.VertexArrayAttribute.Type.UNSIGNED_BYTE, offset));
            offset = offset + 4;
        }

        if (texType != 0) {
            for (int i = 0; i < Math.min(texType, 4); i++) {
                arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("vs_uv" + i, 4 * 2, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT2, offset));
                offset = offset + 8;
            }
        } else {
            for (int i = 0; i < Math.min(halfFloatTexType, 4); i++) {
                arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("vs_uv" + i, 2 * 2, VertexArrayBinding.VertexArrayAttribute.Type.HALF_FLOAT2, offset));
                offset = offset + 4;
            }
        }

        if (local_1c == 1) {
            arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("transparency", 2 * 4, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT2, offset));
            offset = offset + 8;
        } else if (local_1c == 2) {
            arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("transparency", 4, VertexArrayBinding.VertexArrayAttribute.Type.BYTE, offset));
            offset = offset + 4;
        }

        if (local_24 == 1) {
            arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("blendIndices", 4 * 3, VertexArrayBinding.VertexArrayAttribute.Type.FLOAT3, offset));
            offset = offset + 0xc;
        } else if (local_24 == 2) {
            arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("blendIndices", 4, VertexArrayBinding.VertexArrayAttribute.Type.BYTE, offset));
            offset = offset + 4;
        }

        if (local_c != 0) {
            arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("lightDir", 4, VertexArrayBinding.VertexArrayAttribute.Type.BYTE, offset));
            offset = offset + 4;
            arrayFormat.add(new VertexArrayBinding.VertexArrayAttribute("lightColor", 4, VertexArrayBinding.VertexArrayAttribute.Type.BYTE, offset));
        }
    }

    public void applyDepthAlphaFormat() {
        if (ENABLE_DEPTH_EMULATION) {
            switch (depthType) {
                case NORMAL -> {
                    OpenGLRenderer.getOpenGLRenderer().setDepthTest(true);
                    OpenGLRenderer.getOpenGLRenderer().setDepthWrite(true);
                    OpenGLRenderer.getOpenGLRenderer().setDepthFunc(OpenGLRenderer.DepthTestFunction.LEQUAL);
                }
                case NO_WRITE -> {
                    OpenGLRenderer.getOpenGLRenderer().setDepthTest(true);
                    OpenGLRenderer.getOpenGLRenderer().setDepthWrite(false);
                    OpenGLRenderer.getOpenGLRenderer().setDepthFunc(OpenGLRenderer.DepthTestFunction.LEQUAL);
                }
                case ALWAYS_PASS -> {
                    OpenGLRenderer.getOpenGLRenderer().setDepthTest(true);
                    OpenGLRenderer.getOpenGLRenderer().setDepthWrite(true);
                    OpenGLRenderer.getOpenGLRenderer().setDepthFunc(OpenGLRenderer.DepthTestFunction.ALWAYS);
                }
                case IGNORE_DEPTH -> OpenGLRenderer.getOpenGLRenderer().setDepthTest(false);
            }
        }

        if (ENABLE_ALPHA_EMULATION) {
            int alphaCutoffFormat = alphaType >> 0x14 & 0xff;
            float alphaCutoff;
            if (alphaCutoffFormat == 5) {
                alphaCutoff = ((float) (alphaType >> 0x17 & 0xff)) / 255f;
            } else {
                alphaCutoff = 2f / 255f;
            }

            ShaderController.setUniform("alphaCutoff", alphaCutoff);

            switch (alphaBlendType) {
                case NONE -> OpenGLRenderer.getOpenGLRenderer().setAlphaBlendEnable(false);
                case TRANSPARENT -> {
                    OpenGLRenderer.getOpenGLRenderer().setAlphaBlendEnable(true);
                    OpenGLRenderer.getOpenGLRenderer().setAlphaBlendFunction(OpenGLRenderer.AlphaBlendFunction.ADD);
                    OpenGLRenderer.getOpenGLRenderer().setAlphaBlendSource(OpenGLRenderer.AlphaBlendSource.SRC_ALPHA,
                            OpenGLRenderer.AlphaBlendSource.ONE_MINUS_SRC_ALPHA);
                }
                case TRANSPARENT_IGNORE_DEST -> {
                    OpenGLRenderer.getOpenGLRenderer().setAlphaBlendEnable(true);
                    OpenGLRenderer.getOpenGLRenderer().setAlphaBlendFunction(OpenGLRenderer.AlphaBlendFunction.ADD);
                    OpenGLRenderer.getOpenGLRenderer().setAlphaBlendSource(OpenGLRenderer.AlphaBlendSource.SRC_ALPHA,
                            OpenGLRenderer.AlphaBlendSource.ONE);
                }
                case REVERSE_TRANSPARENT -> {
                    OpenGLRenderer.getOpenGLRenderer().setAlphaBlendEnable(true);
                    OpenGLRenderer.getOpenGLRenderer().setAlphaBlendFunction(OpenGLRenderer.AlphaBlendFunction.REV_SUBTRACT);
                    OpenGLRenderer.getOpenGLRenderer().setAlphaBlendSource(OpenGLRenderer.AlphaBlendSource.ZERO,
                            OpenGLRenderer.AlphaBlendSource.ONE_MINUS_SRC_ALPHA);
                }
                case NONE_FIXED_ALPHA -> {
                    OpenGLRenderer.getOpenGLRenderer().setAlphaBlendEnable(false);
                    ShaderController.setUniform("alphaCutoff", ((float) (alphaType >> 0x17 & 0xff)) / 255f);
                }
            }
        } else {
            OpenGLRenderer.getOpenGLRenderer().setAlphaBlendEnable(true);
            OpenGLRenderer.getOpenGLRenderer().setAlphaBlendFunction(OpenGLRenderer.AlphaBlendFunction.ADD);
            OpenGLRenderer.getOpenGLRenderer().setAlphaBlendSource(OpenGLRenderer.AlphaBlendSource.SRC_ALPHA,
                    OpenGLRenderer.AlphaBlendSource.ONE_MINUS_SRC_ALPHA);
            ShaderController.setUniform("alphaCutoff", 0.0f);
        }
    }

    private void generateDepthAlpha() {
        alphaBlendType = switch (alphaType & 0xf) {
            case 0 -> AlphaBlendType.NONE;
            case 1 -> AlphaBlendType.TRANSPARENT;
            case 2 -> AlphaBlendType.TRANSPARENT_IGNORE_DEST;
            case 3 -> AlphaBlendType.REVERSE_TRANSPARENT;
            case 10 -> AlphaBlendType.NONE_FIXED_ALPHA;
            default -> AlphaBlendType.NONE;//throw new UnsupportedOperationException("Unsupported alpha type");
        };

        depthType = switch (alphaType >> 0xe & 0x3) {
            case 0 -> DepthType.NORMAL;
            case 1 -> DepthType.NO_WRITE;
            case 2 -> DepthType.ALWAYS_PASS;
            case 3 -> DepthType.IGNORE_DEPTH;
            default -> throw new UnsupportedOperationException("Unsupported depth type");

        };
    }

    private void generateDefines() {
        defines.put("PRELIGHT_FX", 0);
        defines.put("PRELIGHT_FX_LIVE_SPECULAR", 0);
        defines.put("LIGHTING_STAGE", 1);
        defines.put("LIGHTMAP_STAGE", 0);
        defines.put("SURFACE_TYPE2", 0);
        defines.put("FRESNEL_STAGE", 0);
        defines.put("REFLECTIVITY_STAGE", 0);
        defines.put("SPECULAR_SPECULARENABLE", 0);
        defines.put("REFRACTION_STAGE", 0);
        defines.put("COMBINE_OP_0", isLayerEnabled(0) ? 1 : 0);
        defines.put("LAYER0_DIFFUSEENABLE", hasDiffuseMap(0) ? 0 : 1);

        if ((shaderDefinesBits & 0x1000) == 0) {
            if ((shaderDefinesBits >> 0x11 & 1) != 0) {
                defines.put("LIGHTING_STAGE", 1); //gooch
            } else if ((shaderDefinesBits >> 0x13 & 1) != 0) {
                defines.put("LIGHTING_STAGE", 2); //envmap
            } else if ((shaderDefinesBits >> 4 & 1) == 0) {
                if ((shaderDefinesBits >> 3 & 1) == 0) {
                    defines.put("LIGHTING_STAGE", 4); //lambert
                } else {
                    defines.put("LIGHTING_STAGE", 5); //phong
                }
            } else {
                defines.put("LIGHTING_STAGE", 3); //aniso
            }

            if ((shaderDefinesBits >> 0x19 & 1) != 0) {
                defines.put("ANISO_FLIP", 0);
            }
        } else {
            if ((shaderDefinesBits & 0x80000000) == 0) {
                defines.put("LIGHTING_STAGE", 0); //disable
            } else {
                defines.put("PRELIGHT_FX", 1);
                if ((shaderDefinesBits >> 4 & 1) != 0 || (shaderDefinesBits >> 3 & 1) != 0) {
                    defines.put("PRELIGHT_FX_LIVE_SPECULAR", 1);
                }
                if ((shaderDefinesBits >> 3 & 1) == 0) {
                    defines.put("LIGHTING_STAGE", 4); //lambert
                } else {
                    defines.put("LIGHTING_STAGE", 5); //phong
                }
            }
        }

        if (lightmapSetIndex != 0 && textureFlags < 0) {
            var otherIndex = MapLoader.CURRENT_GAME_VERSION == Project.GameVersion.LIJ1 || MapLoader.CURRENT_GAME_VERSION == Project.GameVersion.LB1 ?
                    uvSetCoords >> 0x12 & 3 :
                    uvSetCoords >> 0x2 & 3;

            if (otherIndex != 0) {
                defines.put("LIGHTMAP_UVSET", otherIndex);
            } else {
                defines.put("LIGHTMAP_UVSET", lightmapSetIndex - 1);
            }

            if ((textureFlags & 0x800000) != 0) {
                defines.put("LIGHTMAP_STAGE", 1); //smooth
            } else {
                defines.put("LIGHTMAP_STAGE", 2); //directional
            }
        } else {
            defines.put("LIGHTMAP_STAGE", 0);
        }

        defines.put("SURFACE_UVSET", surfaceUVIndex - 1);

        if ((shaderDefinesBits >> 1 & 1) == 0) {
            if ((shaderDefinesBits & 1) == 0) {
                defines.put("SURFACE_TYPE", 0); //smooth
            } else {
                defines.put("SURFACE_TYPE", 1); //normal map
            }
        } else {
            defines.put("SURFACE_TYPE", 2); //parallax map
        }

        if ((shaderDefinesBits >> 3 & 1) != 0 || 
            (shaderDefinesBits >> 4 & 1) != 0) {
            if (fileSpecular != null) {
                defines.put("SPECULAR_UVSET", specularSetIndex - 1);
            } else {
                defines.put("SPECULAR_SPECULARENABLE", 1);
            }

            defines.put("REFLECTIVITY_STAGE", 1);

            if ((shaderDefinesBits >> 2 & 1) != 0) {
                defines.put("FRESNEL_STAGE", 1);
            }

            if ((shaderDefinesBits >> 10 & 1) != 0) {
                if ((shaderDefinesBits >> 9 & 1) == 0) {
                    if ((shaderDefinesBits >> 0x1d & 1) == 0) {
                        defines.put("REFRACTION_STAGE", 1); //DEFAULT
                    } else {
                        defines.put("REFRACTION_STAGE", 2); //GLASS
                    }
                } else {
                    defines.put("REFRACTION_STAGE", 3); // WATER
                }
            }
        }
    }

    private boolean isLayerEnabled(int layer){
        return switch (layer){
            case 0 -> true;
            case 1 -> (shaderDefinesBits & 0x40) != 0;
            case 2 -> (shaderDefinesBits & 0x80) != 0;
            case 3 -> (shaderDefinesBits & 0x100) != 0;

            default -> throw new IllegalStateException("Unexpected layer: " + layer);
        };
    }

    private boolean hasDiffuseMap(int id){
      //  if((inputDefinesBits & 0xff) != 0){
            return switch (id){
                case 0 -> fileDiffuse != null;
                default -> false;
            };
    //    }
      //  return false;
    }

    public void apply() {
        applyDepthAlphaFormat();
        FileMaterial.currentMaterial = this;
        if (this.getTexture() != null)
            ShaderController.setUniform("layer0_sampler", this.getTexture());

        if (this.getNormalTexture() != null)
            ShaderController.setUniform("surface_sampler", this.getNormalTexture());

        if (this.getSpecularTexture() != null)
            ShaderController.setUniform("specular_sampler", this.getSpecularTexture());
        else
            ShaderController.setUniform("specular_specular", reflectivity);

        ShaderController.setUniform("layer0_diffuse", this.getColor());
        ShaderController.setUniform("specular_params", specular);
        for (var define : this.getDefines().entrySet()) {
            ShaderController.setUniform(define.getKey(), define.getValue());
        }
    }

    public boolean muteMaterial() {
        if (EditorState.CURRENT.shouldHighlight) {
            var currentObj = EditorState.getSelectedObject().get();
            if (currentObj instanceof FileMaterial obj && obj != this) {
                return true;
            }else if (currentObj instanceof FileTexture obj && (obj != this.fileDiffuse && obj != this.fileNormal)) {
                return true;
            }
        }

        return false;
    }

    public void setFormatBits(int formatBits) {
        this.formatBits = formatBits;
    }

    public void setInputDefinesBits(int definesBits) {
        this.inputDefinesBits = definesBits;
    }

    public void setShaderDefinesBits(int definesBits) {
        this.shaderDefinesBits = definesBits;
    }

    public void setUVSetCoords(int uvsetCoords) {
        this.uvSetCoords = uvsetCoords;
    }

    public void generateShaderSettings() {
        generateFormat();
        generateDefines();
        generateDepthAlpha();
    }

    public FileTexture getDiffuseFileTexture() {
        return fileDiffuse;
    }

    public void setDiffuseFileTexture(FileTexture diffuseFileTexture) {
        this.fileDiffuse = diffuseFileTexture;
    }

    public FileTexture getNormalFileTexture() {
        return this.fileNormal;
    }

    public void setSpecularFileTexture(FileTexture fileSpecular) {
        this.fileSpecular = fileSpecular;
    }

    public void setNormalIndex(FileTexture fileNormal) {
        this.fileNormal = fileNormal;
    }

    public Texture getTexture() {
        if (fileDiffuse == null) return null;

        return fileDiffuse.nativeTexture().getNow(null);
    }

    public Texture getSpecularTexture() {
        if (fileSpecular == null) return null;

        return fileSpecular.nativeTexture().getNow(null);
    }

    public Texture getNormalTexture() {
        if (fileNormal == null) return null;

        return fileNormal.nativeTexture().getNow(null);
    }

    public Vector4f getColor() {
        return color;
    }

    public void setReflectivityColor(Vector4f reflectivity) {
        this.reflectivity = reflectivity;
    }

    public void setSpecular(Vector4f specular) {
        this.specular = specular;
    }

    public void setColor(Vector4f color) {
        this.color = color;
    }

    public void setAlphaType(int alphaType) {
        this.alphaType = alphaType;
    }

    public void setLightmapSetIndex(int lightmapSetIndex) {
        this.lightmapSetIndex = lightmapSetIndex;
    }

    public void setSpecularIndex(int specularSetIndex) {
        this.specularSetIndex = specularSetIndex;
    }

    public void setSurfaceUVIndex(int normalvs_uvSetIndex) {
        this.surfaceUVIndex = normalvs_uvSetIndex;
    }

    public List<VertexArrayBinding.VertexArrayAttribute> getArrayBindings() {
        return arrayFormat;
    }

    public Map<String, Integer> getDefines() {
        return defines;
    }

    public void setTextureFlags(int flags) {
        this.textureFlags = flags;
    }

    public void setID(int id) {
        this.ID = id;
    }

    public int getID() {
        return ID;
    }

    @Override
    public String name() {
        return "Material_" + getID();
    }

    @Override
    public String path() {
        return "Render/Materials/" + name();
    }

    public void exportBinary() {
        try {
            var file = Resource.getUserDataPath().resolve(Path.of("export", "material", EditorState.getActiveMap().levelData().name() + "_" + this.name() + ".mtl"));
            Files.createDirectories(file.getParent());

            var bytes = ByteBuffer.allocate(0x2C4);
            var gscFile = EditorState.getActiveMap().getFileOfExtension("gsc").channel();
            gscFile.position(this.fileAddress);
            gscFile.read(bytes);

            Files.write(file, bytes.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Runnable> getButtonActions() {
        if (BrickBench.DEVMODE)
            return Map.of("Export bin", this::exportBinary);
        else
            return Map.of();
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new IntegerProperty("Material Index",getID(), false),
                new EnumProperty("Blending type",alphaBlendType, true),
                new EnumProperty("Depth type",depthType, true),
                new GroupProperty("Textures & Colors",List.of(
                        new EditorEntityProperty("Diffuse texture", fileDiffuse, true, true, "Render/Textures/"),
                        new EditorEntityProperty("Specular texture", fileSpecular, true, true, "Render/Textures/"),
                        new EditorEntityProperty("Normal texture", fileNormal, true, true, "Render/Textures/"),
                        new ColorProperty("Surface color", color.truncate()),
                       // new ColorProperty("Specular color", reflectivity.truncate()),
                        new FloatProperty("Specular multiplier", specular.y, true),
                        new FloatProperty("Specular exponent", specular.x, true),
                        new FloatProperty("Fresnel multiplier", specular.z, true),
                        new FloatProperty("Fresnel exponent", specular.w, true),
                        new FloatProperty("Transparency", color.w, false)
                )),
                new GroupProperty("Vertex Definition",
                        IntStream.range(0, arrayFormat.size()).mapToObj(o ->
                                new StringProperty("Attribute " + o, arrayFormat.get(o).name() + " at " + arrayFormat.get(o).offset() + " of type "
                                        + arrayFormat.get(o).type() + " size " + arrayFormat.get(o).size(), false, 128)).collect(Collectors.toList())
                ),
                new GroupProperty("Shader Defines",
                        defines.entrySet().stream().map(e -> new IntegerProperty(e.getKey(), e.getValue(), false)).collect(Collectors.toList())
                ));
    }

    @Override
    public void applyPropertyEdit(String propName, Property newValue) {
        switch (newValue) {
            case EditorEntityProperty mp && propName.equals("Diffuse texture") -> {
                var texture = (FileTexture) mp.value();
                MapWriter.applyPatch(MapWriter.WritableObject.SCENE, this.fileAddress + 0x74, Util.littleEndian((short) texture.descriptor().trueIndex()));
                MapWriter.applyPatch(MapWriter.WritableObject.SCENE, this.fileAddress + 0xB4 + 0x4, Util.littleEndian((short) texture.descriptor().trueIndex()));
            }
            case FloatProperty fp && propName.equals("Specular exponent") -> MapWriter.applyPatch(MapWriter.WritableObject.SCENE, this.fileAddress + 0xB4 + 0x7C, Util.littleEndian(fp.value()));
            case FloatProperty fp && propName.equals("Specular multiplier") -> MapWriter.applyPatch(MapWriter.WritableObject.SCENE, this.fileAddress + 0xB4 + 0x78, Util.littleEndian(fp.value()));
            case FloatProperty fp && propName.equals("Fresnel multiplier") -> MapWriter.applyPatch(MapWriter.WritableObject.SCENE, this.fileAddress + 0xB4 + 0x90, Util.littleEndian(fp.value()));
            case FloatProperty fp && propName.equals("Fresnel exponent") -> MapWriter.applyPatch(MapWriter.WritableObject.SCENE, this.fileAddress + 0xB4 + 0x94, Util.littleEndian(fp.value()));
            case ColorProperty cp && propName.equals("Surface color") -> MapWriter.applyPatch(MapWriter.WritableObject.SCENE, this.fileAddress + 0x54, cp.value().toLittleEndianByteBuffer());
            case EnumProperty ep && propName.equals("Blending type") -> {
                int blendId = switch ((AlphaBlendType) ep.value()) {
                    case NONE -> 0;
                    case TRANSPARENT -> 1;
                    case TRANSPARENT_IGNORE_DEST -> 2;
                    case REVERSE_TRANSPARENT -> 3;
                    case NONE_FIXED_ALPHA -> 10;
                };
                int editedFlag = (alphaType & ~(0xf)) | blendId;
                MapWriter.applyPatch(MapWriter.WritableObject.SCENE, this.fileAddress + 0x40, Util.littleEndian(editedFlag));
            }
            case null, default -> {
            }
        }
    }

    public ImageIcon getIcon() {
        return icon.getNow(null);
    }

    @Override
    public void run() {
        apply();
    }

    @Override
    public int getAddress() {
        return fileAddress;
    }

    @Override
    public DisplayCommand.CommandType getType() {
        return DisplayCommand.CommandType.MTL;
    }

    enum AlphaBlendType {
        NONE, TRANSPARENT, TRANSPARENT_IGNORE_DEST, REVERSE_TRANSPARENT, NONE_FIXED_ALPHA
    }

    enum DepthType {
        NORMAL, NO_WRITE, ALWAYS_PASS, IGNORE_DEPTH
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public int hashCode() {
        return this.fileAddress;
    }
}
