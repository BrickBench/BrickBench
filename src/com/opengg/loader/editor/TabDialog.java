package com.opengg.loader.editor;

import com.opengg.core.engine.Resource;
import com.opengg.loader.BrickBench;
import com.opengg.loader.editor.components.DnDTabbedPane;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

public class TabDialog extends JDialog{
    public TabDialog(String label, Component component){
        super(BrickBench.CURRENT.window);

        var dnd = new DnDTabbedPane(this::dispose);
        dnd.add(label, component);
        dnd.putClientProperty("JTabbedPane.tabHeight", EditorTheme.tabHeight);
        dnd.putClientProperty("JTabbedPane.tabClosable",true);
        dnd.putClientProperty("JTabbedPane.tabCloseCallback",
                (BiConsumer<JTabbedPane, Integer>) (tabbedPane, tabIndex) -> {
                    tabbedPane.remove(tabIndex);
                    BrickBench.CURRENT.window.topBar.refreshValues();
                });

        this.add(dnd);
        this.setIconImage(new ImageIcon(Resource.getTexturePath("icon.png")).getImage());
        this.setTitle("BrickBench");
        this.setAlwaysOnTop(false);
        this.setMinimumSize(new Dimension(100,100));
        this.pack();
        this.setLocation(MouseInfo.getPointerInfo().getLocation());
        this.setVisible(true);
    }
}
