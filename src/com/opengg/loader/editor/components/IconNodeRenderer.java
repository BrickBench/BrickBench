package com.opengg.loader.editor.components;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.formdev.flatlaf.icons.FlatFileViewFileIcon;

import java.awt.*;

public class IconNodeRenderer extends DefaultTreeCellRenderer {
    private static Icon defaultIcon = new FlatFileViewFileIcon();

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;

        this.setIcon(defaultIcon);

        var currentNode = node;
        while (currentNode != null) {
            if (currentNode.getUserObject() instanceof IconNode inode && inode.getIcon() != null) {
                this.setIcon(inode.getIcon());
                break;
            }

            currentNode = (DefaultMutableTreeNode) currentNode.getParent();
        }
        
        return this;
    }

    public interface IconNode {
        Icon getIcon();
    }
}
