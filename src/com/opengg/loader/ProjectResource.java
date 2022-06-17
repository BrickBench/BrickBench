package com.opengg.loader;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.opengg.loader.editor.EditorState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

/**
 * Represents a generic file/folder placed somewhere in a project structure.
 */
@JsonSerialize
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class ProjectResource implements ProjectStructure.Node<ProjectResource> {
    private final String name;
    private final String path;

    @JsonCreator
    public ProjectResource(@JsonProperty("name") String name, @JsonProperty("path") String path) {
        this.name = name;
        this.path = path;
    }

    /**
     * The in-structure filename of this file/folder
     */
    public String name() {
        return name;
    }

    /**
     * The location of this resource in the BrickBench project (not in the structure).
     */
    public String path() {
        return path;
    }

    @Override
    @JsonIgnore
    public List<Property> properties() {
        return List.of(new StringProperty("Name", name, true, 1000));
    }

    @Override
    public void applyPropertyEdit(String propName, Property newValue) {
        if (newValue instanceof StringProperty sp && propName.equals("Name")) {
            var project = EditorState.getProject();

            var structure = project.structure();

            var newPath = Path.of(path).getParent().resolve(sp.value());

            var oldAbs = project.projectXml().resolveSibling(path);
            var newAbs = project.projectXml().resolveSibling(newPath);

            var parent = (ProjectStructure.FolderNode) structure.getParent(this);
            var newResource = new ProjectResource(sp.value(), newPath.toString());

            try {
                Files.move(oldAbs, newAbs, StandardCopyOption.REPLACE_EXISTING);

                parent.children().set(parent.children().indexOf(this), newResource);

                EditorState.updateProject(project);
            } catch (IOException e) {
                SwingUtil.showErrorAlert("Failed while renaming resource", e);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ProjectResource) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, path);
    }

    @Override
    public String toString() {
        return "ProjectResource[" +
                "name=" + name + ", " +
                "path=" + path + ']';
    }

}
