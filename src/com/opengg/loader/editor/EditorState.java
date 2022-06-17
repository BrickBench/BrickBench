package com.opengg.loader.editor;

import com.opengg.core.math.Quaternionf;
import com.opengg.core.math.Vector3f;
import com.opengg.core.math.util.Tuple;
import com.opengg.core.world.WorldEngine;
import com.opengg.core.world.components.Component;
import com.opengg.loader.*;
import com.opengg.loader.components.MapComponent;
import com.opengg.loader.game.nu2.Area;
import com.opengg.loader.game.nu2.terrain.TerrainGroup;
import com.opengg.loader.loading.MapLoader;
import com.opengg.loader.loading.MapWriter;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class EditorState {
    public static EditorState CURRENT = new EditorState();

    public MapComponent currentMap;

    private Project project;
    private String currentMapName = "";

    private Deque<String> selectionStack = new ArrayDeque<>();
    private Deque<String> futureSelectionStack = new ArrayDeque<>();

    private Map<String, Map<String, EditorEntity<?>>> namespaces = new LinkedHashMap<>();
    private Map<String, Project.MapInstance> loadedMaps = new HashMap<>();
    private Map<String, Tuple.OrderedTuple<Vector3f, Quaternionf>> savedPlayerPositions = new HashMap<>();

    private EditorEntity.Ref<?> selectedObject = EditorEntity.Ref.NULL; 
    public Map<String, Boolean> objectVisibilities = new LinkedHashMap<>();
    public List<Component> temporaryComponents = new ArrayList<>();

    public boolean shouldHighlight;
    public MapInterface.SelectionMode selectionMode = MapInterface.SelectionMode.PAN;
    public TerrainGroup.TerrainProperty property = TerrainGroup.TerrainProperty.NONE;

    public List<Consumer<Project>> onProjectChangeListeners = new ArrayList<>();
    public List<Consumer<Project>> onMapChangeListeners = new ArrayList<>();
    public List<Consumer<Project>> onMapReloadListeners = new ArrayList<>();
    private List<Consumer<Tuple<Project, EditorEntity.Ref<?>>>> onObjectSelectListeners  = new ArrayList<>();
    private List<Consumer<Tuple<String, Boolean>>> onVisibilityChangeListeners  = new ArrayList<>();

    /**
     * Clears the editor state
     */
    public static void resetEditorState() {
        CURRENT.selectionStack.clear();
        CURRENT.selectedObject = EditorEntity.Ref.NULL; 
        CURRENT.objectVisibilities.clear();
        CURRENT.savedPlayerPositions.clear();
        CURRENT.namespaces.clear();
        CURRENT.project = null;

        for (var map : List.copyOf(CURRENT.loadedMaps.values())) {
            closeProjectMap(map.levelData().xmlData());
        }
    }

    /**
     * Returns the currently active project
     * @return
     */
    public static Project getProject() {
        return CURRENT.project;
    }

    /**
     * Updates the current project with the newly added project and resets the editor state.
     * @param project
     */
    public static void updateProject(Project project) {
        CURRENT.project = project;
        var allLists = List.of(
                CURRENT.project.structure().getNodesOfType(Area.class),
                CURRENT.project.maps()
        );

        var namespace = new HashMap<String, EditorEntity<?>>();
        for(var list : allLists){
            for(var obj : list){
                namespace.put(obj.path(), obj);
            }
        }

        CURRENT.namespaces.put("Project", namespace);
        CURRENT.onProjectChangeListeners.forEach(o -> o.accept(EditorState.getProject()));
    }

    /**
     * Returns a loaded MapInstance by name
     * @param name The map name for the instance
     * @return
     */
    public static Project.MapInstance getMapFromName(String name) {
        return CURRENT.loadedMaps.get(name);
    }

    /**
     * Add the given map instance to be managed by the state manager
     * @param instance The instance to manage
     */ 
    public static void addMapInstance(Project.MapInstance instance) {
        CURRENT.namespaces.put(instance.levelData().xmlData().name(), new HashMap<>());
        CURRENT.loadedMaps.put(instance.levelData().xmlData().name(), instance);
    }

    /**
     * Returns the currently active map
     * This corresponds to the map currently loaded as a MapComponent in the engine
     * @return
     */
    public static Project.MapInstance getActiveMap() {
        return CURRENT.loadedMaps.get(CURRENT.currentMapName);
    }

    /**
     * Sets the given map as the active map
     * @param map
     */
    public static void updateMap(Project.MapInstance map) {
        CURRENT.loadedMaps.put(map.levelData().xmlData().name(), map);
        CURRENT.namespaces.put(map.levelData().xmlData().name(), map.levelData().getNamespace());
    }

    /**
     * Regenerates engine components after changes have been applied to the current map state
     */
    public static void recreateEngineStateFromChanges(MapWriter.WritableObject writtenType) {
        try {
            updateMap(MapLoader.reloadIndividualFile(writtenType.usedExtension));
            CURRENT.currentMap.updateMapData(EditorState.getActiveMap().levelData());
            CURRENT.currentMap.updateItemType(writtenType);
            CURRENT.onMapReloadListeners.forEach(o -> o.accept(EditorState.getProject()));
        } catch (Exception e) {
            SwingUtil.showErrorAlert("Failed to reload file after edit", e);
        }
    }

    /**
     * Closes the active map and sets the new active map to null
     * @see EditorState#closeProjectMap(MapXml)
     */
    public static void closeActiveMap() {
        if (getActiveMap() != null) {
            closeProjectMap(getActiveMap().levelData().xmlData());
        }
    }

    /**
     * Creates a map instance from the given project map if non-null
     * @param map Project map from the given project to use
     * @throws IOException
     */
    public static void addProjectMap(MapXml map) throws IOException {
        if(map != null) {
            updateMap(MapLoader.loadMapFromXml(CURRENT.project, map));
        }
    }

    /**
     * Creates a map instance from the given project map if non-null, and sets it as the active map
     * @param map Project map from the given project to use
     * @return The newly created map instance, or null if either map is null or an error happened while loading the map
     */
    public static Project.MapInstance addAndActivateProjectMap(MapXml map){
        closeActiveMap();
        selectObject(null);

        if (map != null) {
            try {
                addProjectMap(map);
                CURRENT.currentMapName = map.name();
                WorldEngine.getCurrent().findByName("mainView").get(0).attach(EditorState.getActiveMap().levelData().createEngineComponent());

                var oldPos = CURRENT.savedPlayerPositions.getOrDefault(map.name(), new Tuple.OrderedTuple<>(new Vector3f(), new Quaternionf()));
                BrickBench.CURRENT.player.setPositionOffset(oldPos.x().multiply(new Vector3f(-1, 1, 1)));
                BrickBench.CURRENT.player.setRotationOffset(oldPos.y());

            } catch (Exception e) {
                SwingUtil.showErrorAlert("Failed to use project map " + map.name(), e);
            }
        }

        CURRENT.onMapChangeListeners.forEach(o -> o.accept(EditorState.getProject()));

        return getActiveMap();
    }

    /**
     * Closes the {@link com.opengg.loader.Project.MapInstance MapInstance} of the given project map
     * This deletes the instance, removes the map namespaces, and clears the active map if the given project map corresponds
     * to the current {@link EditorState#getActiveMap() active map}.
     * @param map
     */
    public static void closeProjectMap(MapXml map) {
        CURRENT.namespaces.remove(map.name());
        CURRENT.savedPlayerPositions.put(map.name(), new Tuple.OrderedTuple<>(BrickBench.CURRENT.ingamePosition, BrickBench.CURRENT.player.getRotation()));
        var instance = CURRENT.loadedMaps.remove(map.name());

        if (instance.levelData().xmlData().name().equals(CURRENT.currentMapName)) {
            WorldEngine.getCurrent().findByName("map").forEach(Component::delete);
            CURRENT.currentMapName = "";
        }

        instance.dispose();
    }

    /**
     * Returns the contents of a namespace.
     * Namespaces contain the {@link EditorEntity EditorEntities} of a given structure
     * @param name The namespace name. This can be either the name of a project map or Project (accesses project entities)
     * @return The values of the given namespace, or an empty map if the namespace does not exist
     */
    public static Map<String, EditorEntity<?>> getNamespace(String name) {
        return CURRENT.namespaces.getOrDefault(name, new HashMap<>());
    }

    /**
     * Returns the namespace name corresponding to the current active namespace
     * This namespace name is equivalent to the name of the current {@link EditorState#getActiveMap() active map instance},
     * or empty if no instance is active
     * @return
     */
    public static String getActiveNamespace() {
        if (getActiveMap() != null) {
            return getActiveMap().levelData().xmlData().name();
        } else {
            return "";
        }
    }

    /**
     * Returns an object given its path and namespace
     * If either the namespace or the path do not exist, returns null.
     * @param namespace
     * @param path
     * @return
     */
    public static EditorEntity<?> getObject(String namespace, String path) {
        return CURRENT.namespaces.getOrDefault(namespace, Map.of()).get(path);
    }

    /**
     * Returns an object given its path.
     * The path consists of an optional namespace part and a path, separated by semicolons (eg <code>Negotiations_A:Render/Models/Model_1</code>).
     *
     * The path can be resolved in two ways:
     * - If a namespace is explicitly provided, it will find the object by path in that namespace
     * - If no namespace is provided, the namespaces is assumed to be the  {@link EditorState#getActiveNamespace() currently active namespace}
     * @param path Path to search object in
     * @return
     */
    public static EditorEntity<?> getObject(String path) {
        var namespacePath = path.split(":");
        if (namespacePath.length == 2) {
            return CURRENT.namespaces.get(namespacePath[0]).get(namespacePath[1]);
        } else {
            return CURRENT.namespaces.get(getActiveNamespace()).get(path);
        }
    }

    /**
     * Selects a temporary object.
     * This adds the object to the <code>Temporary</code> namespace, which gets cleared after another object is selected
     * @param tempObject
     */
    public static <T extends EditorEntity<T>> EditorEntity.Ref<T> selectTemporaryObject(EditorEntity<T> tempObject) {
        CURRENT.namespaces.computeIfAbsent("Temporary", t -> new LinkedHashMap<>()).put(tempObject.path(), tempObject);
        return selectObject("Temporary", tempObject);
    }

    /**
     * Selects an object
     * This method selects the given object and creates a reference through default namespace resolution rules:
     * - If a namespace is provided through {@link EditorEntity#namespace()}, it is used
     * - Otherwise, the current namespace is used
     * @param mapObject
     * @param <T>
     * @return
     */
    public static <T extends EditorEntity<T>> EditorEntity.Ref<T>  selectObject(EditorEntity<T> mapObject) {
        return selectObject("", mapObject);
    }

    /**
     * Selects an object with an explicit namespace for future accesses.
     * This method selects the given object from the explicitly given namespace.
     * @param namespace Namespace t
     * @param mapObject
     * @param <T>
     * @return
     */
    public static <T extends EditorEntity<T>> EditorEntity.Ref<T>  selectObject(String namespace, EditorEntity<T> mapObject) {
        if (!namespace.equals("Temporary")) {
            getNamespace("Temporary").clear();
        }

        CURRENT.temporaryComponents.forEach(Component::delete);
        CURRENT.temporaryComponents.clear();
        CURRENT.selectedObject = Optional.ofNullable(mapObject)
                .map(m -> new EditorEntity.Ref(namespace, mapObject))
                .orElse(EditorEntity.Ref.NULL);

        if (!CURRENT.selectedObject.path().isEmpty()) {
            addToSelectionStack(CURRENT.selectedObject.namespace() + ":" + CURRENT.selectedObject.path());
        }

        for (var listener : CURRENT.onObjectSelectListeners) {
            listener.accept(Tuple.of(EditorState.getProject(), CURRENT.selectedObject));
        }

        return (EditorEntity.Ref<T>) CURRENT.selectedObject;
    }

    /**
     * Returns a reference to the currently selected {@link EditorEntity}
     * @return
     */
    public static EditorEntity.Ref<?> getSelectedObject() {
        return CURRENT.selectedObject;
    }

    private static void addToSelectionStack(String selection) {
        if (selection.equals(CURRENT.selectionStack.peekLast())) return;

        if (!selection.isEmpty()) CURRENT.selectionStack.add(selection);

        var nextRedoSelection = CURRENT.futureSelectionStack.peekLast();
        if (selection.equals(nextRedoSelection)) {
            CURRENT.futureSelectionStack.removeLast();
        } else {
            CURRENT.futureSelectionStack.clear();
        }
    }

    private static String getLatestValidPreviousSelection() {
        var iter = CURRENT.selectionStack.descendingIterator();

        while (iter.hasNext()) {
            var next = iter.next();
            if (!next.isEmpty()) {
                var namespace = next.split(":")[0];
                if (namespace.equals("Project") || namespace.equals(getActiveNamespace())) {
                    return next;
                }
            }
        }

        return null;
    }

    /**
     * Returns if the editor has an item that it can undo selection to
     * @return
     */
    public static boolean hasAvailablePreviousSelection() {
        return getLatestValidPreviousSelection() != null;
    }

    /**
     * Undoes the current selection
     * This goes back to the last item that is still accessible and pushes the current item to the redo stack
     */
    public static void undoSelection() {
        if (CURRENT.selectionStack.size() <= 1) return;
        CURRENT.futureSelectionStack.addLast(CURRENT.selectionStack.pollLast());
        while (!CURRENT.selectionStack.isEmpty()) {
            var selection = CURRENT.selectionStack.pollLast();
            var object = getObject(selection);
            if (object != null) {
                CURRENT.futureSelectionStack.addLast(selection);
                selectObject(selection.split(":")[0], object);
                return;
            }
        }
    }

    /**
     * Selects the next selectable object in the redo selection stack, if available
     */
    public static void redoSelection() {
        while (!CURRENT.futureSelectionStack.isEmpty()) {
            var selection = CURRENT.futureSelectionStack.peekLast();
            var object = getObject(selection);
            if (object != null) {
                selectObject(selection.split(":")[0], object);
                return;
            }

            CURRENT.futureSelectionStack.pollLast();
        }
    }

    /**
     * Add a listener that triggers whenever the project changes (including to null).
     */
    public static void addProjectChangeListener(Consumer<Project> listener) {
        CURRENT.onProjectChangeListeners.add(listener);
    }

    /**
     * Add a listener that triggers whenever the map changes to a new map (including to null).
     */
    public static void addMapChangeListener(Consumer<Project> listener) {
        CURRENT.onMapChangeListeners.add(listener);
    }

    /**
     * Add a listener that triggers whenever the map state is changed, both on new maps and on existing maps.
     * This does not include when a map changes to a null map. 
     */
    public static void addMapReloadListener(Consumer<Project> listener) {
        CURRENT.onMapReloadListeners.add(listener);
    }

    /**
     * Add a listener that triggers whenever an object is selected.
     */
    public static void addSelectionChangeListener(Consumer<Tuple<Project, EditorEntity.Ref<?>>> listener) {
        CURRENT.onObjectSelectListeners.add(listener);
    }

    /**
     * Add a listener that triggers whenever the visibility of an object changes.
     */
    public static void addVisibilityChangeListener(Consumer<Tuple<String, Boolean>> listener) {
        CURRENT.onVisibilityChangeListeners.add(listener);
    }

    /**
     * Sets the visibility of the given object.
     */
    public static void setObjectVisibility(EditorEntity<?> object, boolean visibility) {
        setNodeVisibility(object.path(), visibility);
    }

    /**
     * Sets the visibility of the given path node, including both objects and branches.
     */
    public static void setNodeVisibility(String node, boolean visibility) {
        CURRENT.objectVisibilities.put(node, visibility);
        CURRENT.onVisibilityChangeListeners.forEach(c -> c.accept(Tuple.of(node, visibility)));
    }

    /**
     * Returns if a node is currently visible.
     */
    public static boolean isNodeVisible(String node){
        return CURRENT.objectVisibilities.getOrDefault(node, true);
    }

    /**
     * Returns if an object is currently visible.
     */
    public static boolean isObjectVisible(EditorEntity<?> object){
        return isNodeVisible(object.path());
    }

    /**
     * Sets the selection mode of the editor.
     */
    public static void setSelectionMode(MapInterface.SelectionMode selectionMode) {
        CURRENT.selectionMode = selectionMode;
    }

    /**
     * Sets the currently active terrain property.
     */
    public static void setSelectedProperty(TerrainGroup.TerrainProperty property) {
        CURRENT.property = property;
    }
}
