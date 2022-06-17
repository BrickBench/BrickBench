package com.opengg.loader.editor;

import com.opengg.core.Configuration;
import com.opengg.core.console.GGConsole;
import com.opengg.core.engine.OpenGG;
import com.opengg.core.engine.Resource;
import com.opengg.core.math.FastMath;
import com.opengg.core.math.Matrix4f;
import com.opengg.core.math.Vector3f;
import com.opengg.core.math.geom.Triangle;
import com.opengg.core.math.util.Tuple;
import com.opengg.core.physics.collision.PhysicsRay;
import com.opengg.loader.BrickBench;
import com.opengg.loader.EditorEntity;
import com.opengg.loader.Project;
import com.opengg.loader.components.Selectable;
import com.opengg.loader.editor.components.DnDTabbedPane;
import com.opengg.loader.editor.components.JSplitPaneNoDivider;
import com.opengg.loader.editor.hook.TCSHookPanel;
import com.opengg.loader.editor.tabs.*;
import com.opengg.loader.editor.tabs.GitPanel;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.terrain.TerrainGroup;
import com.opengg.loader.game.nu2.terrain.TerrainSerializer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.awt.event.KeyEvent.VK_H;
import static java.awt.event.KeyEvent.VK_L;

/**
 * The primary window for a BrickBench instance.
 */
public class MapInterface extends JFrame implements KeyListener {
    public TopBar topBar;
    public ButtonRow buttonRow;

    public JSplitPaneNoDivider left, right;
    public JTabbedPane bottomTabs, topLeft, topRight, bottomLeft, bottomRight;

    public Map<String, EditorTab> tabs = new HashMap<>();

    public MapInterface() {
        super("BrickBench");
        super.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize());
        super.setMinimumSize(new Dimension(1280, 1024));
        super.setIconImage(new ImageIcon(Resource.getTexturePath("icon.png")).getImage());
        super.setLayout(new BorderLayout());
        super.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        var wholeSplit = createPrimarySplitPanes(JSplitPane.HORIZONTAL_SPLIT);
        wholeSplit.setResizeWeight(0.85);

        var leftCenter = createPrimarySplitPanes(JSplitPane.HORIZONTAL_SPLIT);
        leftCenter.setResizeWeight(0.10);

        wholeSplit.add(leftCenter);

        var scenePane = new JPanel(new BorderLayout());
        scenePane.add(BrickBench.CURRENT.openggCanvasRegion, BorderLayout.CENTER);

        var centerTabs = new JTabbedPane();
        centerTabs.putClientProperty("JTabbedPane.tabHeight", EditorTheme.tabHeight);
        centerTabs.putClientProperty("JTabbedPane.leadingComponent", buttonRow = new ButtonRow());
        centerTabs.putClientProperty("JTabbedPane.tabAreaAlignment", "trailing");
        centerTabs.add("Scene", scenePane);
        centerTabs.add("Gameplay graph", new GitPanel());

        centerTabs.addChangeListener(e -> {
            OpenGG.enableRendering(centerTabs.getSelectedComponent() == scenePane);
        });

        bottomTabs = createPrimaryTabPane();

        var center = createPrimarySplitPanes(JSplitPane.VERTICAL_SPLIT);
        center.setResizeWeight(0.75);
        center.setDividerLocation((int)(Toolkit.getDefaultToolkit().getScreenSize().height/0.75));
        center.add(centerTabs);
        center.add(bottomTabs);

        topLeft = createPrimaryTabPane();
        bottomLeft = createPrimaryTabPane();

        left = createPrimarySplitPanes(JSplitPane.VERTICAL_SPLIT);
        left.setResizeWeight(0.75);
        left.add(topLeft);
        left.add(bottomLeft);
        left.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));

        leftCenter.add(left);
        leftCenter.add(center);

        bottomRight = createPrimaryTabPane();
        topRight = createPrimaryTabPane();

        right = createPrimarySplitPanes(JSplitPane.VERTICAL_SPLIT);
        right.setResizeWeight(0.75);
        right.add(topRight);
        right.add(bottomRight);
        right.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));

        wholeSplit.add(right);

        registerPanes();

        super.setJMenuBar(this.topBar = new TopBar());

        this.add(wholeSplit, BorderLayout.CENTER);
        this.add(new BottomRow(), BorderLayout.SOUTH);
        this.addKeyListener(this);
    }

    public DnDTabbedPane createPrimaryTabPane(){
        var pane = new DnDTabbedPane();
        pane.putClientProperty("JTabbedPane.tabHeight", EditorTheme.tabHeight);
        pane.putClientProperty("JTabbedPane.tabClosable",true);
        pane.putClientProperty("JTabbedPane.tabCloseCallback",
                (BiConsumer<JTabbedPane, Integer>) (tabbedPane, tabIndex) -> {
                    tabbedPane.remove(tabIndex);
                    topBar.refreshValues();
                });

        return pane;
    }

    private void registerPanes() {
        var panes = List.of(ObjectTreeViewerPane.class, EditorPane.class, ConsolePanel.class, DisplayCommandEditor.class, MaterialListPanel.class,
                ProjectStructurePanel.class, TextureList.class, TerrainPalette.class, TCSHookPanel.class);
        for (var pane : panes) {
            try {
                var newPane = pane.getDeclaredConstructors()[0].newInstance();
                registerPane((EditorTab) newPane);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                GGConsole.error("Failed to load tab " + pane.getSimpleName());
            }
        }
    }

    public void registerPane(EditorTab pane){
        ((JComponent)pane).addHierarchyListener(h -> savePane(pane, pane.getTabID()));
        tabs.put(pane.getTabID(), pane);

        if(getConfigTabActive(pane.getTabID())){
            insertIntoPosition(pane, getConfigTabPosition(pane.getTabID()));
        }
    }

    public JSplitPaneNoDivider createPrimarySplitPanes(int splitDirection){
        var pane = new JSplitPaneNoDivider(splitDirection);
        pane.setContinuousLayout(false);
        pane.setOneTouchExpandable(true);
        pane.setDividerDragSize(10);
        return pane;
    }

    public void insertIntoConfigLocation(EditorTab tab){
        insertIntoPosition(tab, getConfigTabPosition(tab.getTabID()));
        topBar.refreshValues();
    }

    public boolean getConfigTabActive(String name) {
        if(Configuration.getConfigFile("editor.ini").getConfig(name).isEmpty()){
            return tabs.get(name).getDefaultActive();
        }

        return Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig(name).split(";")[0]);
    }

    public InterfaceArea getConfigTabPosition(String name) {
        if(Configuration.getConfigFile("editor.ini").getConfig(name).isEmpty()){
            return tabs.get(name).getPreferredArea();
        }

        try {
            return InterfaceArea.valueOf(Configuration.getConfigFile("editor.ini").getConfig(name).split(";")[1]);
        }catch (IllegalArgumentException e){
            return InterfaceArea.BOTTOM_RIGHT;
        }
    }

    public boolean getTabActive(EditorTab tab){
        return SwingUtilities.getWindowAncestor((Component) tab) != null;
    }

    public InterfaceArea getTabPlacement(EditorTab tab){
        if(!getTabActive(tab)) return getConfigTabPosition(tab.getTabID());
        var parent = (DnDTabbedPane) ((JComponent)tab).getParent();

        if(parent == topLeft) return InterfaceArea.TOP_LEFT;
        if(parent == topRight) return InterfaceArea.TOP_RIGHT;
        if(parent == bottomLeft) return InterfaceArea.BOTTOM_LEFT;
        if(parent == bottomTabs) return InterfaceArea.BOTTOM_CENTER;
        if(parent == bottomRight) return InterfaceArea.BOTTOM_RIGHT;

        return InterfaceArea.OUT_OF_WINDOW;
    }

    public void insertIntoPosition(EditorTab panel, InterfaceArea area) {
        var pane = switch (area){
            case TOP_LEFT -> topLeft;
            case TOP_RIGHT -> topRight;
            case BOTTOM_LEFT -> bottomLeft;
            case BOTTOM_RIGHT -> bottomRight;
            case BOTTOM_CENTER -> bottomTabs;
            case OUT_OF_WINDOW -> null;
        };

        if(pane != null){
            pane.addTab(panel.getTabName(), (Component) panel);
            if(BrickBench.CURRENT.window != null)
                topBar.refreshValues();
        }else{
            OpenGG.asyncExec(() -> new TabDialog(panel.getTabName(), (Component) panel));
        }
    }

    private void savePane(EditorTab panel, String name){
        var state = getTabActive(panel);
        var position = getTabPlacement(panel);
        Configuration.getConfigFile("editor.ini").writeConfig(name, state + ";" + position);
    }

    public void onMouseRelease(PhysicsRay ray, boolean fastClick) {
        if (!fastClick) return;

        switch (EditorState.CURRENT.selectionMode) {
            case PAINT_TERRAIN -> {
                if (EditorState.getActiveMap() != null && EditorState.getProject().game().ENGINE == Project.EngineVersion.NU2) {
                    var terrainSelect = this.doMeshSelection(ray);
                    terrainSelect.ifPresent(t -> TerrainSerializer.editTerrainPieceProperty(t.y().y(), EditorState.CURRENT.property));
                }
            }
            case SELECT -> {
                if (EditorState.getActiveMap() != null) {
                    // Attempt to select a generic Selectable object.
                    var selection =
                            EditorState.getActiveMap().levelData().getNamespace()
                            .values().stream()
                            .filter(v -> v instanceof Selectable)
                            .map(v -> (Selectable & EditorEntity<?>) v)
                            .filter(EditorState::isObjectVisible)
                            .collect(Collectors.groupingBy(Selectable::getSelectionOrder))
                            .entrySet().stream()
                            .sorted(Comparator.comparingInt(Map.Entry::getKey))
                            .map(Map.Entry::getValue)
                            .map(group -> trySelectingListObject(group, ray))
                            .flatMap(Optional::stream)
                            .filter(s -> !s.name().equals("default_string"))
                            .findFirst();

                    if (selection.isPresent()) {
                        EditorState.selectObject(selection.get());
                        return;
                    }

                    switch (EditorState.getActiveMap().levelData()) {
                        case NU2MapData nu2 -> {
                            this.doMeshSelection(ray).ifPresentOrElse(t -> EditorState.selectObject(t.x()), () -> EditorState.selectObject(null));
                        }
                        default -> {}
                    }
                }
            }
            case PAN -> {}
        }
    }

    private Optional<EditorEntity<?>> trySelectingListObject(List<? extends EditorEntity<?>> objects, PhysicsRay ray) {
        Optional<Tuple<Selectable, Vector3f>> nearest = Optional.empty();
        for (var object : objects) {
            if (object instanceof Selectable selectable &&
                    selectable.getBoundingBox() != null) {
                var collision = selectable.getBoundingBox().getCollision(ray.getRay());
                if (collision.isPresent() &&
                        (nearest.isEmpty() || nearest.get().y().distanceTo(ray.pos()) > collision.get().distanceTo(ray.pos()))) {
                    nearest = Optional.of(Tuple.of(selectable, collision.get()));
                }
            }
        }

        return nearest.map(n -> (EditorEntity<?>) n.x());
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == VK_H) {
            topBar.getItemByName("shadow").doClick();
        }
        if (e.getKeyCode() == VK_L) {
            topBar.getItemByName("load").doClick();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    private Optional<Tuple<TerrainGroup, Tuple<TerrainGroup.TerrainMeshBlock, TerrainGroup.TerrainMeshFace>>> doMeshSelection(PhysicsRay ray) {
        TerrainGroup.TerrainMeshFace closest = null;
        TerrainGroup.TerrainMeshBlock closestBlock = null;
        TerrainGroup closestObj = null;
        float closestDistance = Float.MAX_VALUE;
        var revRay = ray.getRay().setDir(ray.getRay().dir().inverse());

        for (var terrainGroup : ((NU2MapData)EditorState.getActiveMap().levelData()).terrain().terrainGroups()) {
            if(!EditorState.isObjectVisible(terrainGroup)) continue;
            for (var block : terrainGroup.blocks()) {
                for (var face : block.faces()) {
                    Matrix4f transform;
                    if (terrainGroup.isTerrainPlatform()) {
                        if (Configuration.getBoolean("use-rotation-platform")) {
                            transform = terrainGroup.platformObject().get().iablObj().transform();
                        } else {
                            transform = Matrix4f.IDENTITY.translate(terrainGroup.platformObject().get().pos());
                        }
                    } else {
                        transform = Matrix4f.IDENTITY.translate(terrainGroup.position());
                    }

                    var p1 = transform.transform(face.vec1());
                    var p2 = transform.transform(face.vec2());
                    var p3 = transform.transform(face.vec3());
                    var p4 = transform.transform(face.vec4());

                    var tri1 = new Triangle(p1, p2, p3);
                    var tri2 = new Triangle(p2, p3, p4);
                    if (face.norm2().equals(new Vector3f(0, 65536.0f, 0))) {
                        tri2 = tri1;
                    }

                    var test1 = FastMath.getRayTriangleCollision(revRay, tri1);
                    var test2 = FastMath.getRayTriangleCollision(revRay, tri2);

                    if (test1.isPresent() && ray.pos().distanceTo(test1.get()) < closestDistance) {
                        closest = face;
                        closestObj = terrainGroup;
                        closestBlock = block;
                        closestDistance = ray.pos().distanceTo(test1.get());
                    }
                    if (test2.isPresent() && ray.pos().distanceTo(test2.get()) < closestDistance) {
                        closest = face;
                        closestObj = terrainGroup;
                        closestBlock = block;
                        closestDistance = ray.pos().distanceTo(test2.get());
                    }
                }
            }
        }

        if (closest == null) {
            return Optional.empty();
        } else {
            return Optional.of(Tuple.of(closestObj, Tuple.of(closestBlock, closest)));
        }
    }

    public enum InterfaceArea{TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM_CENTER, OUT_OF_WINDOW}

    public enum SelectionMode{SELECT, PAN, PAINT_TERRAIN}
}
