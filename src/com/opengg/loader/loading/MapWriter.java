package com.opengg.loader.loading;

import com.opengg.loader.BrickBench;
import com.opengg.loader.SwingUtil;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.editor.windows.ProjectCreationDialog;

import javax.swing.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.*;

public final class MapWriter {
    private static Queue<WritableObject> objectsEdited = new LinkedList<>();

    public static MapLoader.MapFile getFileFromExtension(String ext){
        return EditorState.getActiveMap().getFileOfExtension(ext);
    }

    public static ByteBuffer readAtLocation(WritableObject type, int start, int size) throws IOException {
        var buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        getFileFromExtension(type.usedExtension).channel().position(start);
        getFileFromExtension(type.usedExtension).channel().read(buffer); 
        buffer.rewind();

        return buffer;
    }

    public static void applyPatch(WritableObject type, int start, byte[] contents) {
        applyPatch(type, start, ByteBuffer.wrap(contents));
    }

    public static void applyPatch(WritableObject type, int start, ByteBuffer contents){
        contents.rewind();
        if(!MapWriter.isProjectEditable()){
            return;
        }
        applyPatch(getFileFromExtension(type.usedExtension).channel(), start, contents);
        if(!objectsEdited.contains(type)){
            objectsEdited.add(type);
        }
    }

    private static void applyPatch(FileChannel writer, int start, ByteBuffer contents){
        try {
            writer.position(start);
            writer.write(contents);
        } catch (IOException e) {
            SwingUtil.showErrorAlert("Failed to apply patch", e);
        }
    }

    public static void addSpaceAtLocation(WritableObject type, int location, int newBytes){
        if(!isProjectEditable()) return;

        addSpaceAtLocation(getFileFromExtension(type.usedExtension).channel(), location, newBytes);
        if(!objectsEdited.contains(type)){
            objectsEdited.add(type);
        }
    }

    private static void addSpaceAtLocation(FileChannel writer, int location, int newBytes){
        try {
            writer.position(location);

            var tempBuf = ByteBuffer.allocate((int) (writer.size() - location));
            writer.read(tempBuf);

            writer.position(location);
            writer.write(ByteBuffer.allocate(newBytes));
            writer.write(tempBuf.flip());

        } catch (IOException e) {
            SwingUtil.showErrorAlert("Failed to add space to the file", e);
        }
    }

    public static void removeSpaceAtLocation(WritableObject type, int location, int removeCount){
        if(!isProjectEditable()) return;

        removeSpaceAtLocation(getFileFromExtension(type.usedExtension).channel(), location, removeCount);
        if(!objectsEdited.contains(type)){
            objectsEdited.add(type);
        }
    }

    private static void removeSpaceAtLocation(FileChannel writer, int location, int removeCount){
        try {
            writer.position(location + removeCount);

            var tempBuf = ByteBuffer.allocate((int) (writer.size() - location - removeCount));
            writer.read(tempBuf); tempBuf.flip();

            writer.truncate(writer.size() - removeCount);

            writer.position(location);
            writer.write(tempBuf);
        } catch (IOException e) {
            SwingUtil.showErrorAlert("Failed to remove space in file", e);
        }
    }

    public static void applyChangesToMapState(){
        while(!objectsEdited.isEmpty()){
            EditorState.recreateEngineStateFromChanges(objectsEdited.remove());
        }
    }

    public static boolean isProjectEditable(){
        if(EditorState.getProject() == null){
            JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "No project is open for editing.");

            return false;
        }else if(!EditorState.getProject().isProject()){
            var doEdit = JOptionPane.showConfirmDialog(BrickBench.CURRENT.window,
                    "This map is currently read-only. Would you like to create a project and begin editing?",
                    "Create new project?",
                    JOptionPane.YES_NO_OPTION);

            if(doEdit == 0){
                new ProjectCreationDialog();
            }

            return false;
        }
        return true;
    }

    public enum WritableObject{
        GIZMO("giz"),
        SCENE("gsc"),
        SPLINE("gsc"),
        DOOR("txt"),
        AI_LOCATOR("ai2"),
        CREATURE_SPAWN("ai2"),
        TERRAIN("ter"),
        TRIGGER("ai2"),
        LIGHTS("rtl");

        public String usedExtension;

        WritableObject(String extension){
            this.usedExtension = extension;
        }
    }
}
