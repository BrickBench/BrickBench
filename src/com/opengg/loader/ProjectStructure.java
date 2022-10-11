package com.opengg.loader;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.opengg.core.engine.Resource;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.Area;
import com.opengg.loader.game.nu2.AreaIO;
import com.opengg.loader.loading.ProjectIO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A structure containing a set of nodes representing the output hierarchy of a project.
 */
public record ProjectStructure(FolderNode root) {

    /**
     * A node in the project structure.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = FolderNode.class, name = "folder"),
            @JsonSubTypes.Type(value = Area.class, name = "area"),
            @JsonSubTypes.Type(value = ProjectResource.class, name = "resource"),
            @JsonSubTypes.Type(value = MapXml.class, name = "map")
    })
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class, property = "id", scope = Node.class)
    public sealed interface Node<T extends Node<T>> extends EditorEntity<T> permits FolderNode, Area, MapXml, ProjectResource{
        String name();
    }

    /**
     * A node representing a plain folder, which can contain other nodes.
     */
    public record FolderNode(String name, List<Node<?>> children) implements Node<FolderNode>{
        @Override
        @JsonIgnore
        public String namespace() {
            return "Project";
        }

        @Override
        public String path() {
            return "Folders/" + name();
        }

        @Override
        @JsonIgnore
        public List<Property> properties() {
            return List.of(new StringProperty("Name", name(), true, 100));
        }

        @Override
        public void applyPropertyEdit(String propName, Property newValue) {
            if (newValue instanceof StringProperty sp) {
                var project = EditorState.getProject();

                var structure = project.structure();

                var parent = structure.getParent(this);

                ProjectIO.unpackAllMapsForRoot(this);

                ((ProjectStructure.FolderNode) parent).children().removeIf(c -> c.name().equalsIgnoreCase(name()));
                ((ProjectStructure.FolderNode) parent).children().add(new ProjectStructure.FolderNode(sp.value(), this.children()));

                EditorState.updateProject(project);
            }
        }
    }

    /**
     * Remove the given node from the given project.
     */
    public void removeNode(Project project, Node<?> node) {
        removeNode(project, node, root);
    }

    private static boolean removeNode(Project project, Node<?> target, Node<?> current) {
        return switch (current) {
            case FolderNode(var name, var children) -> {
                if (name.equalsIgnoreCase(target.name())) {
                    for (var child : children) {
                        removeNode(project, child, child);
                    }
                    yield true;
                } else {
                    for (int i = 0; i < children.size(); i++) {
                        var child = children.get(i);
                        var doRemove = removeNode(project, target, child);
                        if (doRemove) {
                            children.remove(child);
                        }
                    }

                    yield false;
                }
            }

            case ProjectResource rn -> {
                yield target.name().equals(rn.name());
            }

            case MapXml fn -> {
                yield target.name().equals(fn.name());
            }

            case Area an -> {
                if (target instanceof Area an2 && an.name().equals(an2.name())) {
                    yield true;
                } else {
                    for (int i = 0; i < an.maps().size(); i++) {
                        var map = an.maps().get(i);
                        if (map.name().equals(target.name())) {
                            an.maps().remove(map);
                        }
                    }
                    yield false;
                }
            }

            // case null -> throw new NullPointerException();
        };
    }

    /**
     * Return the parent of the given node in this structure.
     *
     * @return The node that is the parent of (contains) the given node, or {@code null} if the given node is
     * the root or if the given node does not exist in the structure.
     */
    public Node<?> getParent(Node<?> child) {
        return getParent(child, root);
    }

    private static Node<?> getParent(Node<?> target, Node<?> current) {
        return switch (current) {
            case FolderNode(var name, var children) -> {
                for (var child : children) {
                    if (child.name().equalsIgnoreCase(target.name())){
                        yield current;
                    }

                    var result = getParent(target, child);
                    if (result != null) {
                        yield result;
                    }
                }

                yield null;
            }
            case Area an -> {
                for (var child : an.maps()) {
                    if (child.name().equalsIgnoreCase(target.name())) {
                        yield current;
                    }
                }

                yield null;
            }
            case null, default -> null;
        };
    }

    /**
     * Return the true folder hierarchy for the give node.
     *
     * This returns the folders that would have to be traversed in the final exported
     * project to reach this node. 
     *
     * For example, in a project that looks like {@code Folder {name = "LEVELS", children = { Area { name = "GUNGAN", maps = { Map { name = "GUNGAN_A" }}}}}}, 
     * {@code getFolderFor} returns {@code ["LEVELS", "GUNGAN"]}
     */
    public List<String> getFolderFor(Node<?> target) {
        var result = getFolderFor(target, root);
        return result.subList(1, result.size());
    }

    private static List<String> getFolderFor(Node<?> target, Node<?> current) {
        return switch (current) {
            case FolderNode(var name, var children)-> {
                if (name.equalsIgnoreCase(target.name())) {
                    yield List.of(target.name());
                }

                for (var child : children) {
                    var result = getFolderFor(target, child);
                    if (result != null) {
                        var list = new ArrayList<String>();
                        list.add(current.name());
                        list.addAll(result);
                        yield list;
                    }
                }

                yield null;
            }
            case Area an -> {
                if (an.name().equalsIgnoreCase(target.name())) {
                    yield List.of(target.name());
                }

                for (var child : an.maps()) {
                    if (child.name().equals(target.name())) {
                        yield switch (child.mapType()) {
                            case NORMAL, NEW_GAME, LOAD_GAME, TEST -> {
                                var list = new ArrayList<String>();
                                list.add(current.name());
                                list.add(child.name());
                                yield list;
                            }
                            case INTRO -> {
                                var list = new ArrayList<String>();
                                list.add(current.name());
                                list.add(current.name() + "_INTRO");
                                yield list;
                            }
                            case OUTRO -> {
                                var list = new ArrayList<String>();
                                list.add(current.name());
                                list.add(current.name() + "_OUTRO");
                                yield list;
                            }
                            case MIDTRO -> {
                                var list = new ArrayList<String>();
                                list.add(current.name());
                                list.add(current.name() + "_MIDTRO");
                                yield list;
                            }
                            case STATUS -> {
                                var list = new ArrayList<String>();
                                list.add(current.name());
                                list.add(current.name() + "_STATUS");
                                yield list;
                            }
                            case SINGLE_FILE -> throw new IllegalStateException("Cannot have loose files in maps");
                        };
                    }
                }

                yield null;
            }
            default -> {
                if (current.name().equalsIgnoreCase(target.name())) {
                    yield List.of();
                }
                yield null;
            }
            case null -> null;
        };
    }

    /**
     * Returns the node located at the given path. 
     *
     * For example, if path is {@literal "LEVELS/GUNGAN/GUNGAN_A" }, this returns the map named GUNGAN_A.
     */
    public ProjectStructure.Node<?> getNodeFromPath(String path) {
        for (var root : this.root().children()) {
            var result = getNodeFromPath(root, List.of(path.split("/")), "");
            if (result != null) return result;
        }

        return null;
    }

    private ProjectStructure.Node<?> getNodeFromPath(ProjectStructure.Node<?> node, List<String> path, String current) {
        if ((current + "/" + node.name()).substring(1).equals(String.join("/", path))) return node;
        switch (node) {
            case ProjectStructure.FolderNode fn -> {
                for (var folder : fn.children()) {
                    var value = getNodeFromPath(folder, path, current + "/" + fn.name());
                    if (value != null) return value;
                }
            }
            case Area an -> {
                for (var map : an.maps()) {
                    var value = getNodeFromPath(map, path, current + "/" + AreaIO.getFolderFor(an, map));
                    if (value != null) return value;
                }
            }
            default -> {}
        }

        return null;
    }

    public <T> List<T> getNodesOfType(Class<T> type) {
        return List.copyOf(getNodesOfType(root(), type));    
    }

    public <T> List<T> getNodesOfType(ProjectStructure.Node<?> node, Class<T> type) {
        var list = new ArrayList<T>();
        if (node.getClass() == type) list.add((T) node);

        switch (node) {
            case ProjectStructure.FolderNode fn -> list.addAll(fn.children().stream().flatMap(n -> getNodesOfType(n, type).stream()).toList());
            case Area an -> list.addAll(an.maps().stream().flatMap(m -> getNodesOfType(m, type).stream()).toList());
            default -> {}
        }

        return list;
    }
}
