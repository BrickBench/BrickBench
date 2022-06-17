package com.opengg.loader.editor;

import com.formdev.flatlaf.icons.*;
import com.opengg.core.console.GGConsole;
import com.opengg.loader.*;
import com.opengg.loader.editor.components.IconNodeRenderer;
import com.opengg.loader.game.nu2.Area;
import com.opengg.loader.loading.MapIO;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.*;

public class ProjectTree extends JTree{
    private Project project;

    public ProjectTree(Project project){
        this.project = project;

        this.setRootVisible(false);
        this.setShowsRootHandles(true);
        this.setDragEnabled(true);
        this.setModel(new DefaultTreeModel(getNodeFor(project.structure().root())));
        this.setDropMode(DropMode.ON_OR_INSERT);
        this.setTransferHandler(new TreeTransferHandler());
        this.setCellRenderer(new IconNodeRenderer());
        this.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        expand(this);
    }

    public void updateProjectStructure(){
        this.setModel(new DefaultTreeModel(getNodeFor(project.structure().root())));
        expand(this);
        this.repaint();
    }

    private void expand(JTree tree){
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    public record ProjectNodeUserObject(ProjectStructure.Node<?> node) implements IconNodeRenderer.IconNode {
        @Override
        public String toString() {
            return node.name();
        }

        @Override
        public Icon getIcon() {
            return switch (node) {
                case ProjectStructure.FolderNode fn -> new FlatTreeOpenIcon();
                case Area an -> EditorIcons.areas;
                case MapXml mn -> EditorIcons.maps;
                case ProjectResource rn -> new FlatTreeLeafIcon();
                case null -> null;
            };
        }
    }

    private DefaultMutableTreeNode getNodeFor(ProjectStructure.Node<?> node){
        var tNode = new DefaultMutableTreeNode(node.name());
        tNode.setUserObject(new ProjectNodeUserObject(node));
        switch (node) {
            case ProjectStructure.FolderNode fn -> {
                for (var child : fn.children()) {
                    tNode.add(getNodeFor(child));
                }
            }
            case Area an -> {
                for (var child : an.maps()) {
                    tNode.add(getNodeFor(child));
                }
            }
            default -> {
                tNode.setAllowsChildren(false);
            }
        }

        return tNode;
    }


    class TreeTransferHandler extends TransferHandler {
        DataFlavor nodesFlavor;
        DataFlavor[] flavors = new DataFlavor[1];
        DefaultMutableTreeNode[] nodesToRemove;

        public TreeTransferHandler() {
            try {
                String mimeType = DataFlavor.javaJVMLocalObjectMimeType +
                        ";class=\"" +
                        javax.swing.tree.DefaultMutableTreeNode[].class.getName() +
                        "\"";
                nodesFlavor = new DataFlavor(mimeType);
                flavors[0] = nodesFlavor;
            } catch(ClassNotFoundException e) {
                GGConsole.exception(e);
            }
        }

        public boolean canImport(TransferHandler.TransferSupport support) {
            if(!support.isDrop()) {
                return false;
            }
            support.setShowDropLocation(true);
            if(!support.isDataFlavorSupported(nodesFlavor)) {
                return false;
            }
            // Do not allow a drop on the drag source selections.
            JTree.DropLocation dl =
                    (JTree.DropLocation)support.getDropLocation();
            JTree tree = (JTree)support.getComponent();
            int dropRow = tree.getRowForPath(dl.getPath());
            int[] selRows = tree.getSelectionRows();
            for (int selRow : selRows) {
                if (selRow == dropRow) {
                    return false;
                }
            }

            // Do not allow a non-leaf node to be copied to a level
            // which is less than its source level.
            TreePath dest = dl.getPath();
            DefaultMutableTreeNode target =
                    (DefaultMutableTreeNode)dest.getLastPathComponent();
            TreePath path = tree.getPathForRow(selRows[0]);
            DefaultMutableTreeNode firstNode =
                    (DefaultMutableTreeNode)path.getLastPathComponent();
            /*if(firstNode.getChildCount() > 0 &&
                    target.getLevel() < firstNode.getLevel()) {
                return false;
            }*/

            if(firstNode.toString().equals("root")){
                return false;
            }

            var targetObject = (ProjectNodeUserObject) target.getUserObject();
            var selectedObject = (ProjectNodeUserObject) firstNode.getUserObject();

            if(targetObject.node() instanceof MapXml){
                return false;
            }

            if(targetObject.node() instanceof ProjectResource){
                return false;
            }

            if(targetObject.node() instanceof Area an && selectedObject.node() instanceof MapXml mn){
                return !an.maps().contains(mn);
            }

            return true;
        }


        protected Transferable createTransferable(JComponent c) {
            JTree tree = (JTree) c;
            TreePath[] paths = tree.getSelectionPaths();
            if (paths == null) {
                return null;
            }
            // Make up a node array of copies for transfer and
            // another for/of the nodes that will be removed in
            // exportDone after a successful drop.
            List<DefaultMutableTreeNode> copies =
                    new ArrayList<>();
            List<DefaultMutableTreeNode> toRemove =
                    new ArrayList<>();
            DefaultMutableTreeNode firstNode =
                    (DefaultMutableTreeNode) paths[0].getLastPathComponent();
            HashSet<TreeNode> doneItems = new LinkedHashSet<>(paths.length);
            DefaultMutableTreeNode copy = copy(firstNode, doneItems, tree);
            copies.add(copy);
            toRemove.add(firstNode);
            for (int i = 1; i < paths.length; i++) {
                DefaultMutableTreeNode next =
                        (DefaultMutableTreeNode) paths[i].getLastPathComponent();
                if (doneItems.contains(next)) {
                    continue;
                }
                // Do not allow higher level nodes to be added to list.
                if (next.getLevel() < firstNode.getLevel()) {
                    break;
                } else if (next.getLevel() > firstNode.getLevel()) {  // child node
                    copy.add(copy(next, doneItems, tree));
                    // node already contains child
                } else {                                        // sibling
                    copies.add(copy(next, doneItems, tree));
                    toRemove.add(next);
                }
                doneItems.add(next);
            }
            DefaultMutableTreeNode[] nodes =
                    copies.toArray(new DefaultMutableTreeNode[0]);
            nodesToRemove =
                    toRemove.toArray(new DefaultMutableTreeNode[0]);
            return new NodesTransferable(nodes);
        }

        private DefaultMutableTreeNode copy(DefaultMutableTreeNode node, HashSet<TreeNode> doneItems, JTree tree) {
            DefaultMutableTreeNode copy = new DefaultMutableTreeNode(node);
            doneItems.add(node);
            for (int i=0; i<node.getChildCount(); i++) {
                copy.add(copy((DefaultMutableTreeNode)((TreeNode)node).getChildAt(i), doneItems, tree));
            }
            int row = tree.getRowForPath(new TreePath(copy.getPath()));
            tree.expandRow(row);
            return copy;
        }

        protected void exportDone(JComponent source, Transferable data, int action) {
            if((action & MOVE) == MOVE) {
                JTree tree = (JTree)source;
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                // Remove nodes saved in nodesToRemove in createTransferable.
                for (DefaultMutableTreeNode defaultMutableTreeNode : nodesToRemove) {
                    model.removeNodeFromParent(defaultMutableTreeNode);
                }
            }
        }

        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }

        public boolean importData(TransferHandler.TransferSupport support) {
            if(!canImport(support)) {
                return false;
            }
            // Extract transfer data.
            DefaultMutableTreeNode[] nodes = null;
            try {
                Transferable t = support.getTransferable();
                nodes = (DefaultMutableTreeNode[])t.getTransferData(nodesFlavor);
            } catch(Exception ufe) {
                return false;
            }
            // Get drop location info.
            JTree.DropLocation dl =
                    (JTree.DropLocation)support.getDropLocation();
            int childIndex = dl.getChildIndex();
            TreePath dest = dl.getPath();
            DefaultMutableTreeNode destinationNode =
                    (DefaultMutableTreeNode)dest.getLastPathComponent();
            JTree tree = (JTree)support.getComponent();
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            // Configure for drop mode.
            int index = childIndex;    // DropMode.INSERT
            if(childIndex == -1) {     // DropMode.ON
                index = destinationNode.getChildCount();
            }
            var newNodeParent = ((ProjectNodeUserObject) destinationNode.getUserObject()).node;

            // Add data to model.
            for(int i = 0; i < Objects.requireNonNull(nodes).length; i++) {
                var oldNode = ((ProjectNodeUserObject)((DefaultMutableTreeNode) nodes[i].getUserObject()).getUserObject()).node;
                var oldNodeParent = project.structure().getParent(oldNode);

                if(oldNode instanceof MapXml mn){
                    MapIO.applyAllDiffs(mn, project);
                }else if(oldNode instanceof Area an){
                    for(var map : an.maps()){
                        MapIO.applyAllDiffs(map, project);
                    }
                }

                if(oldNodeParent instanceof ProjectStructure.FolderNode fn){
                    fn.children().removeIf(n -> n.name().equalsIgnoreCase(oldNode.name()));
                }else if(oldNodeParent instanceof Area an){
                    an.maps().removeIf(m -> m.name().equalsIgnoreCase(oldNode.name()));
                }

                if(newNodeParent instanceof ProjectStructure.FolderNode fn){
                    if(oldNodeParent instanceof Area && oldNode instanceof MapXml mn){
                        fn.children().add(new ProjectStructure.FolderNode(oldNode.name(), List.of(oldNode)));
                    }else{
                        fn.children().add(oldNode);
                    }
                }else if(newNodeParent instanceof Area an){
                    an.maps().add(((MapXml) oldNode));
                }

                model.insertNodeInto((MutableTreeNode) nodes[i].getUserObject(), destinationNode, index++);
            }
            updateProjectStructure();
            return true;
        }

        public String toString() {
            return getClass().getName();
        }

        public class NodesTransferable implements Transferable {
            DefaultMutableTreeNode[] nodes;

            public NodesTransferable(DefaultMutableTreeNode[] nodes) {
                this.nodes = nodes;
            }

            public Object getTransferData(DataFlavor flavor)
                    throws UnsupportedFlavorException {
                if(!isDataFlavorSupported(flavor))
                    throw new UnsupportedFlavorException(flavor);
                return nodes;
            }

            public DataFlavor[] getTransferDataFlavors() {
                return flavors;
            }

            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return nodesFlavor.equals(flavor);
            }
        }
    }
}

