package com.opengg.loader.editor.tabs;

import com.opengg.loader.editor.EditorState;
import com.opengg.loader.editor.MapInterface;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.FileTexture;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@EditorTabAutoRegister
public class TextureList extends JPanel implements EditorTab {
    private final DefaultListModel<FileTexture> list;
    int selection = -1;

    public TextureList() {
        JList<FileTexture> texOpt = new JList<>();
        this.setLayout(new BorderLayout());
        texOpt.setVisibleRowCount(-1);
        texOpt.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list = new DefaultListModel<>();
        texOpt.setCellRenderer(new TextureCellRenderer());
        texOpt.setModel(list);
        var scroll = new JScrollPane(texOpt);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        this.add(scroll, BorderLayout.CENTER);
        texOpt.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (texOpt.getSelectedValue() != null) {
                    EditorState.selectObject(texOpt.getSelectedValue());
                }
            }
        });

        EditorState.addMapReloadListener(m -> {
            switch (EditorState.getActiveMap().levelData()) {
                case NU2MapData nu2 -> setContents(nu2.scene().textures());
                default -> throw new IllegalStateException("Unexpected value: " + EditorState.getActiveMap().levelData());
            };}
        );
    }

    public void setContents(java.util.List<FileTexture> textureList){
        list.clear();
        list.addAll(textureList);
        this.validate();
        this.repaint();
    }

    public int run() {
        this.setVisible(true);
        return selection;
    }

    @Override
    public String getTabName() {
        return "Textures";
    }

    @Override
    public String getTabID() {
        return "texture-pane";
    }

    @Override
    public MapInterface.InterfaceArea getPreferredArea() {
        return MapInterface.InterfaceArea.BOTTOM_CENTER;
    }

    @Override
    public boolean getDefaultActive() {
        return true;
    }

    static class TextureCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list,
                                                      Object item,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {

            super.getListCellRendererComponent(list, item,
                    index, isSelected, cellHasFocus);
            if (item instanceof FileTexture obj) {
                this.setHorizontalTextPosition(JLabel.CENTER);
                this.setVerticalTextPosition(JLabel.BOTTOM);
                this.setText(((FileTexture)item).name());
                this.setIcon(obj.icon().getNow(null));
            }
            return this;
        }
    }
}
