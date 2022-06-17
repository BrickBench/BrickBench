package com.opengg.loader.editor.windows;

import com.opengg.core.console.GGConsole;
import com.opengg.core.engine.OpenGG;
import com.opengg.loader.*;
import com.opengg.loader.editor.AssetViewPanel;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.editor.SearchableListPanel;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.FileTexture;
import com.opengg.loader.loading.MapWriter;
import com.opengg.loader.game.nu2.scene.DisplayImporter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class AssetImportDialog extends JWizardDialog {
    public AssetImportDialog(Project project) {
        super(BrickBench.CURRENT.window);

        if (!MapWriter.isProjectEditable()) {
            this.dispose();
            return;
        }

        this.register("assetSelect", p -> getSelectPanel(project));
        this.register("modelSettings", m -> getModelSettingsPanel(project, EditorState.getActiveMap().levelData().xmlData(), (Project.Assets.ModelDef) m));
        this.register("textureSettings", t -> getTexturePanel(project, EditorState.getActiveMap().levelData().xmlData(), (Project.Assets.TextureDef) t));

        this.swapTo("assetSelect", project);

        this.setTitle("Import asset");
        this.pack();
        this.setLocation(MouseInfo.getPointerInfo().getLocation());
        this.setSize(400, 600);
        this.setVisible(true);

        this.requestFocus();
    }

    public JWizardPanel getSelectPanel(Project project){
        var panel = new JWizardPanel(new MigLayout("fill"));
        var resourceView = new AssetViewPanel(project);
        panel.add(resourceView, "grow");

        panel.setOnNextPressed(() -> {
            if (EditorState.getActiveMap() == null) {
                JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "Cannot import asset to map, no map is open.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

           var selectedResource = resourceView.getSelectedAsset();

           if(selectedResource instanceof Project.Assets.ModelDef model){
                swapTo("modelSettings", model);
           }else if(selectedResource instanceof Project.Assets.TextureDef tex) {
                swapTo("textureSettings", tex);
            }
        });

        return panel;
    }

    public JWizardPanel getModelSettingsPanel(Project project, MapXml map, Project.Assets.ModelDef model){
        var modelPane = new JWizardPanel(new MigLayout("wrap 1"));

        var reverseWind = new JCheckBox("Reverse winding");
        var reverseX = new JCheckBox("Mirror on X axis");
        var shading = new JCheckBox("Generate with normal shading", true);

        modelPane.add(reverseWind);
        modelPane.add(reverseX);
        modelPane.add(shading);

        modelPane.nextButtonLabel("Import");
        modelPane.setOnNextPressed(() -> {
            OpenGG.asyncExec(() -> {
                var backupGsc = FileUtil.createBackup(EditorState.getActiveMap(), EditorState.getActiveMap().getFileOfExtension("gsc"));
                try (var exit = SwingUtil.showLoadingAlert("Importing mesh...", "Importing mesh contents...", false)) {
                    GGConsole.log("Importing model " + model.name());

                    var alreadyExists = map.loadedModels().values().stream().filter(t -> t.equals(model.name())).findFirst();
                    if(alreadyExists.isEmpty()){
                        String newModel = DisplayImporter.importModel(project.projectXml().resolveSibling(model.path()), reverseWind.isSelected(), reverseX.isSelected(), shading.isSelected());

                        if(newModel != null) {
                            map.loadedModels().put(newModel, model.name());
                        }
                    }else{
                        GGConsole.log("Model " + model.name() + " already exists");
                        JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "This model has already been imported.");
                    }
                    FileUtil.clearBackup(backupGsc);
                    this.dispose();

                } catch (Exception e) {
                    FileUtil.applyBackup(backupGsc);
                    SwingUtil.showErrorAlert("Failed to load model", e);
                }
            });
        });

        return modelPane;
    }

    public JWizardPanel getTexturePanel(Project project, MapXml map, Project.Assets.TextureDef texture){
        var panel = new JWizardPanel(new MigLayout("wrap 3, fill", "[]15px[]15px[]", "[][grow]"));
        panel.nextButtonLabel("Import");

        var importAsLabel = new JLabel("Import as:");
        var newTex = new JRadioButton("New texture");
        var replacement = new JRadioButton("Replace texture");

        var group = new ButtonGroup();
        group.add(newTex);
        group.add(replacement);

        var objectList = new SearchableListPanel("Render/Textures", null, true);
        objectList.setEnabled(false);

        newTex.addActionListener(a -> objectList.setEnabled(false));
        replacement.addActionListener(a -> objectList.setEnabled(true));

        panel.add(importAsLabel, "hmax 20px");
        panel.add(newTex, "hmax 20px");
        panel.add(replacement, "wrap, hmax 20px");
        panel.add(objectList, "grow, span 3");

        panel.setOnNextPressed(() -> OpenGG.asyncExec(() -> {
            GGConsole.log("Importing texture " + texture.name());

            var backupGsc = FileUtil.createBackup(EditorState.getActiveMap(), EditorState.getActiveMap().getFileOfExtension("gsc"));
            try (var exit = SwingUtil.showLoadingAlert("Importing...", "Importing texture...", false)){
                if(newTex.isSelected()){
                    var existingTex = EditorState.getActiveMap().levelData().<NU2MapData>as().scene().textures().stream().filter(t -> t.name().equalsIgnoreCase(texture.name())).findFirst();

                    if(existingTex.isPresent()) {
                        var opt = JOptionPane.showConfirmDialog(BrickBench.CURRENT.window, "This texture already exists. Would you like to overwrite it?", "Overwrite?", JOptionPane.YES_NO_OPTION);

                        if (opt == 0) {
                            DisplayImporter.replaceTexture(existingTex.get(), project.projectXml().resolveSibling(texture.path()));
                            map.loadedTextures().put(existingTex.get().name(), texture.name());
                        }
                    } else {
                        int newTexture = DisplayImporter.importTextures(List.of(project.projectXml().resolveSibling(texture.path())));
                        map.loadedTextures().put("Texture_" + newTexture, texture.name());
                    }
                }else if(replacement.isSelected()){
                    for(var replaceTexture : objectList.getMultipleSelected()){
                        DisplayImporter.replaceTexture((FileTexture) replaceTexture, project.projectXml().resolveSibling(texture.path()));
                        map.loadedTextures().put(replaceTexture.name(), texture.name());
                    }
                }

                FileUtil.clearBackup(backupGsc);
            } catch (IOException e) {
                FileUtil.applyBackup(backupGsc);
                GGConsole.error("Failed to import new texture: " + e.getMessage());
            }
            this.dispose();
        }));

        return panel;
    }
}
