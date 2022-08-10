package com.opengg.loader.game.nu2.scene;

import com.opengg.core.Configuration;
import com.opengg.core.math.Vector3f;
import com.opengg.core.render.GraphicsBuffer;
import com.opengg.core.render.RenderEngine;
import com.opengg.core.render.Renderable;
import com.opengg.core.render.objects.DrawnObject;
import com.opengg.core.render.shader.ShaderController;
import com.opengg.core.render.shader.UniformContainer;
import com.opengg.core.render.shader.VertexArrayBinding;
import com.opengg.core.render.shader.VertexArrayFormat;
import com.opengg.loader.EditorEntity;
import com.opengg.loader.Project;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.commands.DisplayCommand;
import com.opengg.loader.game.nu2.rtl.RTLLight;
import com.opengg.loader.loading.MapLoader;

import java.util.ArrayList;
import java.util.List;

public class GSCMesh implements GameRenderable<GSCMesh>{
    public final int address;

    public final int vertexCount;
    public final int vertexSize;
    public final int vertexOffset;
    public final int vertexListID;

    public final int triangleCount;
    public final int indexOffset;
    public final int indexListID;

    public final int dynamicBuffer;
    public final int useDynamicByffer;


    Renderable renderedObject;
    public FileMaterial material;
    public VertexArrayFormat format;

    public GSCMesh(int address, int vertexCount, int vertexSize, int vertexOffset, int vertexListID, int triangleCount,
                   int indexOffset, int indexListID, int useDynamicBuffer, int dynamicBuffer) {
        this.address = address;
        this.vertexCount = vertexCount;
        this.vertexSize = vertexSize;
        this.vertexOffset = vertexOffset;
        this.vertexListID = vertexListID;
        this.triangleCount = triangleCount;
        this.indexOffset = indexOffset;
        this.indexListID = indexListID;
        this.useDynamicByffer = useDynamicBuffer;
        this.dynamicBuffer = dynamicBuffer;
    }

    public void generateObject(List<GraphicsBuffer> vertices, List<GraphicsBuffer> indices){
        var drawable = DrawnObject.createFromGPUMemory(RenderEngine.getDefaultFormat(), indices.get(indexListID), triangleCount + 2, vertices.get(vertexListID));
        drawable.setRenderType(DrawnObject.DrawType.TRIANGLE_STRIP);
        drawable.setIndexType(DrawnObject.IndexType.SHORT);
        drawable.setBaseVertex(vertexOffset);
        drawable.setBaseElement(indexOffset);

        this.renderedObject = drawable;
    }
/*
    public void export() {
        var objName =  EditorState.getActiveMap().levelData().name() + "_" + this.name();

        var channel = Path.of(Resource.getUserDataPath(), "export", "meshes", objName, objName + ".obj");
        try {
            Files.createDirectories(channel.getParent());
            SceneExporter.exportRenderable(this, channel);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }*/

    @Override
    public void render() {
        if (material != FileMaterial.currentMaterial) {
            material = FileMaterial.currentMaterial;
            format = new VertexArrayFormat(List.of(new VertexArrayBinding(0, vertexSize, 0, FileMaterial.currentMaterial.getArrayBindings())));
        }

        if(EditorState.CURRENT.shouldHighlight && EditorState.getSelectedObject().get() instanceof GSCMesh obj && obj != this) {
            return;
        }else if(FileMaterial.currentMaterial.muteMaterial()) {
            ShaderController.setUniform("muteColors", 1);
        }else{
            ShaderController.setUniform("muteColors", 0);
        }

        ((DrawnObject)renderedObject).setFormat(format);
        renderedObject.render();
    }


    @Override
    public String toString() {
        return "GSCRenderable{" +
                "vertexCount=" + vertexCount +
                ", vertexSize=" + vertexSize +
                ", vertexOffset=" + vertexOffset +
                ", vertexListID=" + vertexListID +
                ", triangleCount=" + triangleCount +
                ", indexOffset=" + indexOffset +
                ", indexListID=" + indexListID +
                ", renderedObject=" + renderedObject +
                '}';
    }

    @Override
    public String name() {
        return "Mesh " + Integer.toHexString(address);
    }

    @Override
    public String path() {
        return "Render/Meshes/" + name();
    }

    public int getAddress() {
        return address;
    }

    @Override
    public DisplayCommand.CommandType getType() {
        return DisplayCommand.CommandType.GEOMCALL;
    }

    @Override
    public List<EditorEntity.Property> properties() {
        return List.of(
                new EditorEntity.IntegerProperty("Vertex count",vertexCount, false),
                new EditorEntity.IntegerProperty("Vertex offset",vertexOffset, false),
                new EditorEntity.IntegerProperty("Vertex size",vertexSize, false),
                new EditorEntity.IntegerProperty("Index offset",indexOffset, false),
                new EditorEntity.IntegerProperty("Triangle count",triangleCount, false),
                new EditorEntity.IntegerProperty("Vertex buffer index",vertexListID, false),
                new EditorEntity.IntegerProperty("Index buffer index",indexListID, false)
        );
    }

    @Override
    public void run() {
        render();
    }

    @Override
    public int hashCode() {
        return address;
    }
}
