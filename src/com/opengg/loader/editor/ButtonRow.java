package com.opengg.loader.editor;

import com.opengg.core.Configuration;
import com.opengg.core.engine.OpenGG;
import com.opengg.loader.BrickBench;
import com.opengg.loader.MapXml;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

class ButtonRow extends JToolBar {
    public ButtonRow() {
        super(JToolBar.HORIZONTAL);
        this.setFloatable(false);

        var nextButton = new JButton(">");
        var previousButton = new JButton("<");

        nextButton.addActionListener(a -> EditorState.redoSelection());
        previousButton.addActionListener(a -> EditorState.undoSelection());

        var clickButton = new JToggleButton(EditorIcons.select);
        var panButton = new JToggleButton(EditorIcons.pan);
        var paintbrush = new JToggleButton(EditorIcons.paint);
        var lighting = new JToggleButton(EditorIcons.light);
        var labels = new JToggleButton(EditorIcons.messages);
        var highlight = new JToggleButton(EditorIcons.highlight);

        var mapOptions = new JComboBox<>();
        mapOptions.setBorder(new EmptyBorder(0, 0, 0, 0));
        mapOptions.addActionListener(a -> {
            var selectedMap = (MapBoxItem) mapOptions.getSelectedItem();
            if (selectedMap == null) return;

            var currentMap = EditorState.getActiveMap();
            if(currentMap == null || currentMap.levelData().xmlData() != selectedMap.map){
                OpenGG.asyncExec(() -> {
                    BrickBench.CURRENT.useMapFromCurrentProject(selectedMap.map);
                });
            }
        });

        this.add(clickButton);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(panButton);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(paintbrush);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.addSeparator();
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(lighting);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(labels);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(highlight);
        this.addSeparator();
        this.add(previousButton);
        this.add(nextButton);
        this.add(Box.createGlue());
        this.add(Box.createGlue());
        this.add(Box.createGlue());
        this.add(Box.createGlue());
        this.add(Box.createGlue());

        panButton.setToolTipText("Enable camera pan mode");
        panButton.setSelected(true);
        panButton.addActionListener(a -> EditorState.setSelectionMode(MapInterface.SelectionMode.PAN));

        clickButton.setToolTipText("Enable selection mode");
        clickButton.addActionListener(a -> EditorState.setSelectionMode(MapInterface.SelectionMode.SELECT));

        paintbrush.setToolTipText("Enable terrain painting mode");
        paintbrush.addActionListener(a -> {
            EditorState.CURRENT.selectionMode = MapInterface.SelectionMode.PAINT_TERRAIN;
            EditorState.selectObject(null);
        });

        var selectTypes = new ButtonGroup();
        selectTypes.add(panButton);
        selectTypes.add(clickButton);
        selectTypes.add(paintbrush);

        lighting.setToolTipText("Toggle lights");
        lighting.setSelected(Configuration.getBoolean("show-lights"));
        lighting.addActionListener(a -> {
            Configuration.getConfigFile("editor.ini").writeConfig("show-lights", String.valueOf(lighting.isSelected()));
            BrickBench.CURRENT.reloadConfigFileData();
        });

        labels.setToolTipText("Show object labels");
        labels.setSelected(Configuration.getBoolean("show-object-titles"));
        labels.addActionListener(a -> {
            Configuration.getConfigFile("editor.ini").writeConfig("show-object-titles", String.valueOf(labels.isSelected()));
            BrickBench.CURRENT.reloadConfigFileData();
        });

        highlight.setToolTipText("Highlight selected item");
        highlight.setSelected(Configuration.getBoolean("highlight-selected"));
        highlight.addActionListener(a -> {
            Configuration.getConfigFile("editor.ini").writeConfig("highlight-selected", String.valueOf(highlight.isSelected()));
            BrickBench.CURRENT.reloadConfigFileData();
        });

        EditorState.addProjectChangeListener(p ->
                mapOptions.setModel(new DefaultComboBoxModel<>(p.maps().stream().map(MapBoxItem::new).toArray(MapBoxItem[]::new)))
        );

        EditorState.addMapChangeListener(m -> {
            if(m != null) {
                var map = EditorState.getActiveMap() == null ? null : EditorState.getActiveMap().levelData().xmlData();
                mapOptions.setSelectedIndex(map == null ? -1 : m.maps().indexOf(map));
            }
        });
    }

    record MapBoxItem(MapXml map) {
        @Override
        public String toString() {
            return map.name();
        }
    }
}
