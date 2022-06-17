package com.opengg.loader.editor.tabs;

import com.opengg.loader.editor.EditorState;
import com.opengg.loader.editor.MapInterface;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.FileMaterial;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

@EditorTabAutoRegister
public class MaterialListPanel extends JPanel implements EditorTab {
    DefaultListModel<FileMaterial> listModel = new DefaultListModel<>();
    List<FileMaterial> objects = new ArrayList<>();

    public MaterialListPanel() {
        JList<FileMaterial> list = new JList<>();
        list.setVisibleRowCount(-1);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setBackground(UIManager.getDefaults().getColor("Button.background"));
        listModel = new DefaultListModel<>();
        list.setModel(listModel);
        list.setCellRenderer(new MaterialCellRenderer());
        listModel.clear();

        BorderLayout bl = new BorderLayout();
        setLayout(bl);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (list.getSelectedValue() != null) {
                    EditorState.selectObject(list.getSelectedValue());
                }
            }
        });

        EditorState.addMapReloadListener(m -> {
            switch (EditorState.getActiveMap().levelData()) {
                case NU2MapData nu2 -> setContents(List.copyOf(nu2.scene().materials().values()));
                default -> throw new IllegalStateException("Unexpected value: " + EditorState.getActiveMap().levelData());
            };}
        );
    }

    @Override
    public String getTabName() {
        return "Materials";
    }

    @Override
    public String getTabID() {
        return "material-pane";
    }

    @Override
    public MapInterface.InterfaceArea getPreferredArea() {
        return MapInterface.InterfaceArea.BOTTOM_CENTER;
    }

    @Override
    public boolean getDefaultActive() {
        return true;
    }

    static class MaterialCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list,
                                                      Object item,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {

            super.getListCellRendererComponent(list, item,
                    index, isSelected, cellHasFocus);
            if (item instanceof FileMaterial obj) {
                this.setHorizontalTextPosition(JLabel.CENTER);
                this.setVerticalTextPosition(JLabel.BOTTOM);
                setText(!obj.name().isBlank() ? obj.name() : ("UNNAMED " + obj.getClass().getSimpleName()));
                this.setIcon(obj.getIcon());
            }
            return this;
        }
    }

    public void setContents(List<FileMaterial> contents){
        this.objects = contents;
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            listModel.addAll(contents);

            this.validate();
            this.repaint();
        });
    }
}
