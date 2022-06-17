package com.opengg.loader.editor.tabs;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.components.FlatButton;
import com.formdev.flatlaf.extras.components.FlatToggleButton;
import com.opengg.loader.editor.EditorIcons;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.editor.MapInterface;
import com.opengg.loader.editor.ObjectCreationPalette;
import com.opengg.loader.editor.components.JScrollPaneBackgroundSnatcher;
import com.opengg.loader.editor.components.TextPrompt;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

@EditorTabAutoRegister
public class ObjectTreeViewerPane extends JPanel implements EditorTab {
    public ObjectTree tree;
    JScrollPane treeScroll;
    public ObjectTreeViewerPane(){
        this.setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());

        FlatSVGIcon eyeSVG = EditorIcons.visibleEye.derive(ObjectTree.toggleWidth,17);
        FlatSVGIcon invisibleSVG = EditorIcons.invisibleEye.derive(ObjectTree.toggleWidth,17);

        var label = new JLabel();
        FlatToggleButton button = new FlatToggleButton();
        button.setIcon(eyeSVG);
        button.setSelectedIcon(invisibleSVG);
        button.setBackground(label.getBackground());
        button.setTabSelectedBackground(label.getBackground());
        button.setTabUnderlineColor(label.getBackground());
        button.setButtonType(FlatButton.ButtonType.tab);
        button.setBorder(null);
        label.setBorder(new EmptyBorder(0,7,0,0));
        button.setSize(new Dimension(ObjectTree.toggleWidth,22));
        button.addActionListener(a -> tree.toggleAll(!button.isSelected()));

        var search = new JTextField();
        var searchPrompt = new TextPrompt("Search...", search);
        searchPrompt.setShow(TextPrompt.Show.FOCUS_LOST);
        searchPrompt.setIcon(EditorIcons.search);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (EditorState.getActiveMap() == null) return;
                tree.refresh(search.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (EditorState.getActiveMap() == null) return;
                tree.refresh(search.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (EditorState.getActiveMap() == null) return;
                tree.refresh(search.getText());
            }
        });

        JButton add = new JButton();
        add.setIcon(EditorIcons.add);

        var palette = new ObjectCreationPalette();

        var pane = new JScrollPane(palette);
        pane.setPreferredSize(new Dimension(300, 500));

        var menu = new JPopupMenu();
        menu.setLayout(new BorderLayout());
        menu.add(pane, BorderLayout.CENTER);
        add.addActionListener((e) -> {
            menu.show(add, 0, ((JButton) e.getSource()).getHeight());
        });

        top.add(button,BorderLayout.WEST);
        top.add(search, BorderLayout.CENTER);
        top.add(add, BorderLayout.EAST);
        top.setBorder(null);
        this.add(top, BorderLayout.NORTH);

        tree = new ObjectTree();
        treeScroll = new JScrollPaneBackgroundSnatcher(tree);
        treeScroll.setBorder(null);
        treeScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(treeScroll,BorderLayout.CENTER);
    }


    @Override
    public String getTabName() {
        return "Objects";
    }

    @Override
    public String getTabID() {
        return "object-pane";
    }

    @Override
    public MapInterface.InterfaceArea getPreferredArea() {
        return MapInterface.InterfaceArea.TOP_LEFT;
    }

    @Override
    public boolean getDefaultActive() {
        return true;
    }

}
