package com.opengg.loader.editor.tabs;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.icons.*;
import com.opengg.core.world.WorldEngine;
import com.opengg.loader.BrickBench;
import com.opengg.loader.EditorEntity;
import com.opengg.loader.editor.EditorIcons;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.editor.EditorTheme;
import com.opengg.loader.editor.components.IconNodeRenderer;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.List;

public class ObjectTree extends JPanel implements MouseListener, Scrollable {
    int rowHeight = 0;
    int forceRectTop = -1;
    int forceRectBot = -1;
    boolean forceType = false;
    public static final int toggleWidth = 18;
    public static final int iconDim = 20;
    private static final FlatSVGIcon eyeSVG = EditorIcons.visibleEye.derive(toggleWidth, 20);
    private static final FlatSVGIcon invisSVG = EditorIcons.invisibleEye.derive(toggleWidth, 20);
    private static Color disableColor;
    private JTree tree;

    private List<String> ignore = List.of(
            "Gameplay",
            "Render/Materials",
            "Render/Textures",
            "Render/Portals",
            "Render/GenericCommand",
            "Render/Lightmaps",
            "Render/Meshes",
            "Render/Lights",
            "Render/Transforms");

    private String lastSearch = "";

    public ObjectTree() {
        this.setLayout(new BorderLayout());

        var root = new DefaultMutableTreeNode(new TreeCategory("Objects"));
        tree = new JTree(root);
        this.add(tree, BorderLayout.CENTER);

        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(toggleWidth, this.getHeight()));

        this.add(panel, BorderLayout.WEST);
        this.setFocusable(true);
        this.addMouseListener(this);

        rowHeight = tree.getRowHeight();

        EditorState.addMapChangeListener(m -> this.clearTreeFilterSearch());
        EditorState.addMapReloadListener(m -> this.refresh());
        EditorState.addVisibilityChangeListener(c -> this.setNodeVisibility(c.x(), c.y()));
    }

    @Override
    public void updateUI() {
        super.updateUI();
        disableColor = UIManager.getDefaults().getColor("Button.disabledText");
    }

    @Override
    public Color getBackground() {
        if (tree != null) {
            return tree.getBackground();
        } else {
            return super.getBackground();
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (tree.getRowCount() < 1) return;
        rowHeight = tree.getRowHeight();

        Rectangle vis = this.getVisibleRect();
        g.setColor(tree.getBackground());
        //g.setColor(getBackground());
        g.fillRect(vis.x, vis.y, toggleWidth, vis.height);

        int start = tree.getClosestRowForLocation(vis.x, vis.y);
        int end = tree.getClosestRowForLocation(vis.x, vis.y + vis.height + rowHeight);
        int yOff = tree.getRowBounds(start).y;
        for (int i = start; i <= end; i++) {
            if (((DefaultMutableTreeNode) tree.getPathForRow(i).getLastPathComponent()).getUserObject() instanceof VisibilityToggleNode node) {
                if (forceRectBot != -1 && i <= forceRectBot && i >= forceRectTop) {
                    if (!forceType) {
                        invisSVG.paintIcon(this, g, 0, yOff);
                    }
                } else {
                    if (!node.isVisible) {
                        invisSVG.paintIcon(this, g, 0, yOff);
                    }
                }
            }
            yOff += rowHeight;
        }
        forceRectTop = -1;
        forceRectBot = -1;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();
        if (p.x > 0 && p.x < toggleWidth) {
            var node = ((DefaultMutableTreeNode) tree.getClosestPathForLocation(e.getX(), e.getY()).getLastPathComponent());
            if (node.getUserObject() instanceof VisibilityToggleNode vn) {
                var newVis = !vn.isVisible;
                forceType = newVis;
                forceRectTop = forceRectBot = -1;
                if (node.getChildCount() != 0) {
                    forceRectTop = tree.getClosestRowForLocation(e.getX(), e.getY());
                    forceRectBot = tree.getRowForPath(tree.getClosestPathForLocation(e.getX(), e.getY()).pathByAddingChild(node.getChildAt(node.getChildCount() - 1)));
                }

                setNodeVisibility(node, newVis);
            }
        }
    }

    public DefaultMutableTreeNode getNodeForPath(String path) {
        var nodes = path.split("/");
        var root = (DefaultMutableTreeNode) tree.getModel().getRoot();

        top:
        for (var node : nodes) {
            for (var e = root.children(); e.hasMoreElements(); ) {
                var next = e.nextElement();
                if (next.toString().equalsIgnoreCase(node)) {
                    root = (DefaultMutableTreeNode) next;
                    continue top;
                }
            }
            return null; //Failed to find
        }

        return root;
    }

    public void setNodeVisibility(String node, boolean visibility) {
        var realNode = getNodeForPath(node);
        if (realNode != null) {
            setNodeVisibility(realNode, visibility);
        }
    }

    private void setNodeVisibility(DefaultMutableTreeNode node, boolean visibility) {
        propagate(node, visibility);
        this.repaint();
    }

    public void toggleAll(boolean visible) {
        var root = (DefaultMutableTreeNode) tree.getModel().getRoot();

        for (int i = 0; i < root.getChildCount(); i++) {
            var child = root.getChildAt(i);
            propagate((DefaultMutableTreeNode) child, visible);
        }

        this.repaint();
    }

    private String getFullPath(DefaultMutableTreeNode node) {
        StringBuilder path = new StringBuilder();

        if (node.getUserObject() instanceof EditorEntityNode mon) {
            path.append(mon.object.name());
        } else if (node.getUserObject() instanceof TreeCategory cg) {
            path.append(cg.name);
        }

        var parent = (DefaultMutableTreeNode) node.getParent();
        while (!parent.isRoot()) {
            var object = (TreeCategory) parent.getUserObject();
            path.insert(0, object.name + "/");

            parent = (DefaultMutableTreeNode) parent.getParent();
        }

        return path.toString();
    }

    private void propagate(DefaultMutableTreeNode node, boolean visible) {
        ((VisibilityToggleNode) node.getUserObject()).isVisible = visible;
        var path = getFullPath(node);

        EditorState.CURRENT.objectVisibilities.put(path, visible);

        var components = WorldEngine.findEverywhereByName(path);
        components.forEach(c -> c.setEnabled(visible));

        for (int i = 0; i < node.getChildCount(); i++) {
            propagate((DefaultMutableTreeNode) node.getChildAt(i), visible);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    public void refresh() {
        refresh(lastSearch);
    }

    public void refresh(String searchTerm) {
        this.remove(tree);
        var root = new DefaultMutableTreeNode(new TreeCategory("Objects"));
        var namespace = EditorState.getActiveNamespace();
        var nodes = new LinkedHashMap<String, DefaultMutableTreeNode>();
        for (var item : EditorState.getNamespace(namespace).entrySet()) {
            if (ignore.stream().anyMatch(i -> item.getKey().contains(i))) {
                continue;
            }

            boolean isVisible = EditorState.isNodeVisible(item.getKey());

            WorldEngine.findEverywhereByName(item.getKey()).forEach(f -> f.setEnabled(isVisible));

            var found = false;
            if (item.getValue().path().toLowerCase().contains(searchTerm.toLowerCase(Locale.ROOT))) {
                found = true;
            } else {
                for (var property : item.getValue().properties()) {
                    if ((property instanceof EditorEntity.StringProperty ||
                            property instanceof EditorEntity.EnumProperty ||
                            property instanceof EditorEntity.EditorEntityProperty) &&
                            property.stringValue().toLowerCase(Locale.ROOT).contains(searchTerm.toLowerCase(Locale.ROOT))) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                continue;
            }


            var path = item.getValue().path().substring(0, item.getValue().path().lastIndexOf('/'));
            var pathItems = List.of(path.split("/"));
            for (int i = 0; i < pathItems.size(); i++) {
                var pathSoFar = String.join("/", pathItems.subList(0, i + 1));
                var needsToAdd = !nodes.containsKey(pathSoFar);

                if (needsToAdd) {
                    var intermediateIcon = EditorIcons.objectTreeIconMap.getOrDefault(pathItems.get(i), null); ;
                    var intermediateNode = intermediateIcon == null ?
                            new DefaultMutableTreeNode(new TreeCategory(pathItems.get(i))) :
                            new DefaultMutableTreeNode(new TreeCategory(pathItems.get(i), intermediateIcon));

                    ((TreeCategory) intermediateNode.getUserObject()).isVisible = EditorState.CURRENT.objectVisibilities.getOrDefault(pathSoFar, true);

                    if (i == 0) {
                        root.add(intermediateNode);
                    } else {
                        var parentPath = String.join("/", pathItems.subList(0, i));
                        nodes.get(parentPath).add(intermediateNode);
                    }
                    nodes.put(pathSoFar, intermediateNode);
                }
            }

            var nodeObject = new EditorEntityNode(item.getValue());
            nodeObject.isVisible = isVisible;
            var node = new DefaultMutableTreeNode(nodeObject);

            nodes.get(path).add(node);
            nodes.put(item.getKey(), node);
        }


        tree = new JTree(root);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(24);
        tree.setCellRenderer(new EditorEntityNodeRenderer());
        JPanel top = this;
        tree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int selRow = tree.getRowForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    var selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    if (selectedNode.getUserObject() instanceof ObjectTree.EditorEntityNode mon) {
                        if (e.getClickCount() == 1) {
                            EditorState.selectObject(namespace, mon.object);
                        } else if (e.getClickCount() == 2 && mon.object.pos() != null) {
                            BrickBench.CURRENT.player.setPositionOffset(mon.object.pos().multiply(-1, 1, 1));
                        }
                    }
                }
                top.repaint();
            }
        });
        this.add(tree, BorderLayout.CENTER);
        this.repaint();
        this.validate();

        lastSearch = searchTerm;
    }

    public void clearTreeFilterSearch() {
        lastSearch = "";
        EditorState.CURRENT.objectVisibilities.clear();
        refresh(lastSearch);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return tree.getPreferredScrollableViewportSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return tree.getScrollableUnitIncrement(visibleRect, orientation, direction);
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return tree.getScrollableBlockIncrement(visibleRect, orientation, direction);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public static class EditorEntityNodeRenderer extends IconNodeRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            var node = (DefaultMutableTreeNode) value;

            if (node.getUserObject() instanceof VisibilityToggleNode c4) {
                this.setForeground(c4.isVisible ? this.getForeground() : disableColor);
            }

            return this;
        }
    }

    private abstract static sealed class VisibilityToggleNode {
        public boolean isVisible = true;
    }

    private static final class TreeCategory extends VisibilityToggleNode implements IconNodeRenderer.IconNode {
        public String name;
        public Icon icon;

        public TreeCategory(String name) {
            this(name, null);
        }

        public TreeCategory(String name, Icon icon) {
            this.name = name;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public Icon getIcon() {
            return icon;
        }
    }

    private static final class EditorEntityNode extends VisibilityToggleNode {
        public EditorEntity<?> object;

        public EditorEntityNode(EditorEntity<?> object) {
            this.object = object;
        }

        @Override
        public String toString() {
            return object.name().isEmpty() ? "Unnamed " + object.getClass().getSimpleName() : object.name();
        }
    }
}
