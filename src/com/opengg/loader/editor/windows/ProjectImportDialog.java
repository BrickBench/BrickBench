package com.opengg.loader.editor.windows;

import com.opengg.core.Configuration;
import com.opengg.core.console.GGConsole;
import com.opengg.core.engine.OpenGG;
import com.opengg.loader.BrickBench;
import com.opengg.loader.FileUtil;
import com.opengg.loader.Project;
import com.opengg.loader.SwingUtil;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.editor.components.FileSelectField;
import com.opengg.loader.game.nu2.AreaIO;
import com.opengg.loader.game.nu2.AreaIO.MapLocation;
import com.opengg.loader.loading.MapIO;
import com.opengg.loader.loading.ProjectIO;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Collectors;

public class ProjectImportDialog extends JWizardDialog {
    public ProjectImportDialog(Project project, Path path){
        super(BrickBench.CURRENT.window);

        this.register("directoryPanel", p -> getInitialPanel((Path)p));
        this.register("mapConfig", p -> getMapPanel(project, (Path)p));
        this.register("areaConfig", p -> getAreaPanel(project, (Path)p));
        this.register("resourceConfig", p -> getResourcePanel(project, (Path)p));

        this.swapTo("directoryPanel", path);

        this.pack();
        this.setLocation(MouseInfo.getPointerInfo().getLocation());
        this.setTitle("Import Existing Asset");
        this.setResizable(false);
        this.setSize(450, 350);
        this.setVisible(true);

        this.requestFocus();
    }

    public JWizardPanel getInitialPanel(Path path){
        var panel = new JWizardPanel(new MigLayout("al center top"));

        var mapFileLabel = new JLabel("Asset file/folder");
        var mapFile = new FileSelectField(path == null ? Path.of(Configuration.getConfigFile("editor.ini").getConfig("home")) : path, FileUtil.LoadType.BOTH, "Initial map/level folder",
                "gsc", "ogg", "scp", "txt", "dds");

        panel.add(mapFileLabel);
        panel.add(mapFile, "grow");

        panel.setOnNextPressed(() -> {
            try {
                 if(!AreaIO.findMapsInDirectory(mapFile.getFile()).isEmpty()){
                    this.swapTo("areaConfig", mapFile.getFile());
                }else if(MapIO.isMap(mapFile.getFile())){
                     var parent = Files.isDirectory(mapFile.getFile()) ? mapFile.getFile().getParent() : mapFile.getFile().getParent().getParent();
                     if(!AreaIO.findMapsInDirectory(parent).isEmpty()){
                         var options = JOptionPane.showConfirmDialog(this, "Would you like to load the area this level is in instead?");
                         if(options == 0){
                             this.swapTo("areaConfig", parent);
                         }else{
                             this.swapTo("mapConfig", mapFile.getFile());
                         }
                     }else{
                         this.swapTo("mapConfig", mapFile.getFile());
                     }
                 }else{
                     this.swapTo("resourceConfig", mapFile.getFile());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return panel;
    }

    public JWizardPanel getResourcePanel(Project project, Path resourcePath){
        var cfgPanel = new JWizardPanel(new MigLayout("wrap 2, al center top"));

        var originalName = resourcePath.getFileName().toString();

        var newNameLabel = new JLabel("Resource file name");
        var newNameText = new JTextField(originalName);

        var newPathLabel = new JLabel("Resource export path");
        var newPath = new JTextField("stuff/");

        cfgPanel.add(newNameLabel);
        cfgPanel.add(newNameText, "growx");

        cfgPanel.add(newPathLabel);
        cfgPanel.add(newPath, "growx");

        cfgPanel.nextButtonLabel("Add");
        cfgPanel.setOnNextPressed(() -> {
            if (EditorState.getProject().structure().getNodeFromPath(Path.of(newPath.getText(), newNameText.getText()).toString().replace("\\", "/").toUpperCase(Locale.ROOT)) != null) {
                SwingUtil.showErrorAlert("A file/folder with this name already exists in the project tree. \n Please rename either the area you are importing or the file/folder already in your project.");
            } else {
                try {
                    ProjectIO.importResource(project, resourcePath, originalName, Path.of(newPath.getText()));
                    EditorState.updateProject(project);
                    this.dispose();
                } catch (IOException e) {
                    SwingUtil.showErrorAlert("Failed to import resource", e);
                }
            }

        });

        return cfgPanel;
    }

    public JWizardPanel getAreaPanel(Project project, Path areaPath){
        var cfgPanel = new JWizardPanel(new MigLayout("wrap 3, fill, al center top"));

        var directory = ProjectIO.getLocalPathFromGameRoot(areaPath).toString();
        var targetName = new JTextField(directory.equalsIgnoreCase(areaPath.getFileName().toString()) ? "levels/" + directory : directory);
        var maps = AreaIO.findMapsInDirectory(areaPath);

        var unusedMapsModel = new DefaultListModel<MapLocationListEntry>();
        var usedMapsModel = new DefaultListModel<MapLocationListEntry>();
        usedMapsModel.addAll(maps.stream().map(MapLocationListEntry::new).collect(Collectors.toList()));

        var unusedMaps = new JList<>(unusedMapsModel);
        var usedMaps = new JList<>(usedMapsModel);

        var applyButton = new JButton(">");
        var removeButton = new JButton("<");

        applyButton.addActionListener(a -> {
            if(unusedMaps.getSelectedValue() != null){
                usedMapsModel.addElement(unusedMaps.getSelectedValue());
                unusedMapsModel.removeElement(unusedMaps.getSelectedValue());
            }});

        removeButton.addActionListener(a -> {
            if(usedMaps.getSelectedValue() != null){
                unusedMapsModel.addElement(usedMaps.getSelectedValue());
                usedMapsModel.removeElement(usedMaps.getSelectedValue());
            }});

        cfgPanel.add(new JLabel("Target directory"));
        cfgPanel.add(targetName, "span, grow, wrap");

        cfgPanel.add(new JLabel("Ignore"), "growy 0");
        cfgPanel.add(new JLabel("Import"), "skip 1, growy 0");

        cfgPanel.add(unusedMaps, "grow, spany 2, sg lists, height 90%");
        cfgPanel.add(applyButton);
        cfgPanel.add(usedMaps, "grow, spany 2, sg lists, height 90%");
        cfgPanel.add(removeButton);
        cfgPanel.nextButtonLabel("Import");
        cfgPanel.setOnNextPressed(() -> {
            if (EditorState.getProject().structure().getNodeFromPath(targetName.getText().replace("\\", "/").toUpperCase(Locale.ROOT)) != null) {
                SwingUtil.showErrorAlert("A file/folder with this name already exists in the project tree. \n Please rename either the area you are importing or the file/folder already in your project.");
            } else if(!usedMapsModel.isEmpty()){
                var orderedUsedMaps = new ArrayList<AreaIO.MapLocation>();
                for(var map : maps){
                    for(int i = 0; i < usedMapsModel.getSize(); i++){
                        if(map == usedMapsModel.getElementAt(i).map()){
                            orderedUsedMaps.add(map);
                        }
                    }
                }

                try {
                    var jop = new JOptionPane();
                    jop.setMessageType(JOptionPane.PLAIN_MESSAGE);
                    jop.setMessage("Importing area...");
                    var dialog = jop.createDialog(BrickBench.CURRENT.window, "Importing...");
                    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                    dialog.setModal(false);
                    SwingUtilities.invokeLater(() -> dialog.setVisible(true));

                    AreaIO.createAreaFromDirectory(project, areaPath, Path.of(targetName.getText()), orderedUsedMaps);
                    EditorState.updateProject(project);

                    if (EditorState.getActiveMap() == null) {
                        OpenGG.asyncExec(() -> {
                            BrickBench.CURRENT.useMapFromCurrentProject(project.maps().get(0));
                        });
                    }

                    SwingUtilities.invokeLater(dialog::dispose);
                    this.dispose();
                } catch (IOException e) {
                    SwingUtil.showErrorAlert("Failed to import map", e);
                }
            }
        });


        return cfgPanel;
    }

    public record MapLocationListEntry(MapLocation map){
        @Override
        public String toString(){
            return map.name();
        }
    }

    public JWizardPanel getMapPanel(Project project, Path mapPath){

        var cfgPanel = new JWizardPanel(new MigLayout("wrap 2, al center top"));

        String originalName;
        if (Files.isRegularFile(mapPath)) originalName = FilenameUtils.removeExtension(mapPath.getFileName().toString()).replace("_PC", "");
        else originalName = mapPath.getFileName().toString();

        var newNameLabel = new JLabel("Map name");
        var newNameText = new JTextField(originalName);

        var directory = ProjectIO.getLocalPathFromGameRoot(mapPath).toString();
        if (directory.equalsIgnoreCase(mapPath.getFileName().toString())) {
            directory = "LEVELS/" + originalName;
        }

        if (!Files.isDirectory(mapPath)) {
            directory = Path.of(directory).getParent().toString();
        }

        var newPathLabel = new JLabel("Map path");
        var newPath = new JTextField(directory);

        cfgPanel.add(newNameLabel);
        cfgPanel.add(newNameText, "growx");

        cfgPanel.add(newPathLabel);
        cfgPanel.add(newPath, "growx");

        cfgPanel.nextButtonLabel("Import");

        cfgPanel.setOnNextPressed(() -> {
            if (EditorState.getProject().structure().getNodeFromPath(Path.of(newPath.getText(),  newNameText.getText()).toString().replace("\\", "/").toUpperCase(Locale.ROOT)) != null) {
                SwingUtil.showErrorAlert("A file/folder with this name already exists in the project tree. \n Please rename either the area you are importing or the file/folder already in your project.");
            } else {
                try {
                    var jop = new JOptionPane();
                    jop.setMessageType(JOptionPane.PLAIN_MESSAGE);
                    jop.setMessage("Importing map...");
                    var dialog = jop.createDialog(BrickBench.CURRENT.window, "Importing...");
                    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                    dialog.setModal(false);
                    SwingUtilities.invokeLater(() -> dialog.setVisible(true));

                    MapIO.importNewMap(project, mapPath, Path.of(newPath.getText()), originalName, newNameText.getText());
                    EditorState.updateProject(project);
                    SwingUtilities.invokeLater(dialog::dispose);

                    this.dispose();

                } catch (IOException e) {
                    SwingUtil.showErrorAlert("Failed to import new map", e);
                }
            }
        });

        return cfgPanel;
    }

}
