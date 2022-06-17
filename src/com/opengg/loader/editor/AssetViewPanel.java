package com.opengg.loader.editor;

import com.opengg.loader.FileUtil;
import com.opengg.loader.Project;
import com.opengg.loader.loading.ProjectIO;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.stream.Collectors;

public class AssetViewPanel extends JPanel {
    JTabbedPane assetTypeTab;
    JList<Project.Assets.TextureDef> textureList;
    JList<ModelContainer> modelList;

    public AssetViewPanel(Project project){
        this.setLayout(new MigLayout());

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton addButton = new JButton(EditorIcons.add);
        JButton removeButton = new JButton(EditorIcons.minus);

        addButton.setBackground(null);
        removeButton.setBackground(null);
        addButton.setBorder(BorderFactory.createEmptyBorder());
        removeButton.setBorder(BorderFactory.createEmptyBorder());

        topRow.add(addButton);
        topRow.add(removeButton);

        assetTypeTab = new JTabbedPane();
        assetTypeTab.putClientProperty("JTabbedPane.trailingComponent", topRow);
        assetTypeTab.add(createTextureList(project), "Textures");
        assetTypeTab.add(createModelList(project), "Models");

        this.add(assetTypeTab, "dock center, grow");

        addButton.addActionListener(a -> {
            var file = FileUtil.openFileDialog("", FileUtil.LoadType.FILE, "Map asset", true, "dds", "obj", "dae");
            file.stream().flatMap(Collection::stream).forEach(f -> ProjectIO.importAsset(project, f));

            var lastSelectedIdx = assetTypeTab.getSelectedIndex();

            assetTypeTab.removeAll();

            assetTypeTab.add(createTextureList(project), "Textures");
            assetTypeTab.add(createModelList(project), "Models");

            assetTypeTab.setSelectedIndex(lastSelectedIdx);
        });

        removeButton.addActionListener(a -> {
            var asset = getSelectedAsset();
            if (asset != null ) {
                if (asset instanceof Project.Assets.TextureDef texDef) {
                    project.assets().textures().remove(texDef);
                } else if (asset instanceof Project.Assets.ModelDef modelDef) {
                    project.assets().models().remove(modelDef);
                }
            }

            var lastSelectedIdx = assetTypeTab.getSelectedIndex();

            assetTypeTab.removeAll();

            assetTypeTab.add(createTextureList(project), "Textures");
            assetTypeTab.add(createModelList(project), "Models");

            assetTypeTab.setSelectedIndex(lastSelectedIdx);
        });
    }

    public JPanel createTextureList(Project project){
        var panel = new JPanel(new MigLayout());

        var textureModel = new DefaultListModel<Project.Assets.TextureDef>();
        textureModel.addAll(project.assets().textures());

        textureList = new JList<>();
        textureList.setVisibleRowCount(-1);
        textureList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        textureList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        textureList.setCellRenderer(new TextureCellRenderer());
        textureList.setModel(textureModel);

        var scroll = new JScrollPane(textureList);
        scroll.setBorder(null);

        panel.add(scroll, "dock center, grow");
        return panel;
    }

    public JPanel createModelList(Project project){
        var panel = new JPanel(new MigLayout());

        var modelModel = new DefaultListModel<ModelContainer>();
        modelModel.addAll(project.assets().models().stream().map(ModelContainer::new).collect(Collectors.toList()));

        modelList = new JList<>();
        modelList.setModel(modelModel);
        modelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        var scroll = new JScrollPane(modelList);
        scroll.setBorder(null);

        panel.add(scroll, "dock center, grow");
        return panel;
    }

    public Project.Assets.AssetDef getSelectedAsset(){
        if(assetTypeTab.getSelectedIndex() == 0){
            return textureList.getSelectedValue();
        }else{
            var modelContainer = modelList.getSelectedValue();
            if (modelContainer != null) return modelContainer.model();
            return null;
        }
    }

    private record ModelContainer(Project.Assets.ModelDef model){
        @Override
        public String toString(){
            return model.name();
        }
    }

    private static class TextureCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list,
                                                      Object item,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {

            super.getListCellRendererComponent(list, item,
                    index, isSelected, cellHasFocus);
            if (item instanceof Project.Assets.TextureDef texture) {
                this.setHorizontalTextPosition(JLabel.CENTER);
                this.setVerticalTextPosition(JLabel.BOTTOM);
                this.setText(texture.name());
                this.setIcon(texture.icon());
            }
            return this;
        }
    }
}
