package com.opengg.loader.editor.windows;

import com.opengg.core.engine.OpenGG;
import com.opengg.core.engine.Resource;
import com.opengg.loader.FileUtil;
import com.opengg.loader.BrickBench;
import com.opengg.loader.Project;
import com.opengg.loader.ProjectStructure;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.editor.components.FileSelectField;
import com.opengg.loader.loading.ProjectIO;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.util.ArrayList;

public class ProjectCreationDialog extends JDialog {
    public ProjectCreationDialog(){
        super(BrickBench.CURRENT.window);
        this.setLayout(new BorderLayout());
        this.setModal(true);

        var cfgPanel = new JPanel(new GridBagLayout());

        var gbc = new GridBagConstraints();

        var nameLabel = new JLabel("Project name");
        var name = new JTextField("NewProject");

        var pathLabel = new JLabel("Project directory");
        var path = new FileSelectField(Path.of(FileSystemView.getFileSystemView().getDefaultDirectory().getPath()), FileUtil.LoadType.DIRECTORY, "Project directory");

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        cfgPanel.add(nameLabel, gbc);
        gbc.gridx = 1;
        cfgPanel.add(name, gbc);

        gbc.gridy++;
        gbc.gridy++;
        gbc.gridx = 0;
        cfgPanel.add(new JSeparator(), gbc);


        gbc.gridy++;
        gbc.gridx = 0;
        cfgPanel.add(pathLabel, gbc);
        gbc.gridx = 1;
        cfgPanel.add(path, gbc);


        this.add(cfgPanel, BorderLayout.CENTER);

        var bottomRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        var make = new JButton("Create project");
        var close = new JButton("Cancel");

        bottomRow.add(make);
        bottomRow.add(close);

        this.add(bottomRow, BorderLayout.SOUTH);

        make.addActionListener(a -> {
            if(!Files.exists(path.getFile())){
                JOptionPane.showMessageDialog(
                        BrickBench.CURRENT.window,
                        "Project or base map directory do not exist", "Failed project", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if(name.getText().isEmpty() ){
                JOptionPane.showMessageDialog(
                        BrickBench.CURRENT.window,
                        "Project name cannot be empty", "Failed project", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if(Files.exists(Path.of(path.getFile().toString(), name.getText()))){
                var overwrite = JOptionPane.showConfirmDialog(BrickBench.CURRENT.window, "A project with this name already exists. Do you want to overwrite it?", "Overwrite?",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                if(overwrite == 1){
                    return;
                }
            }

            try {
                EditorState.closeActiveMap();

                var projectPath = Resource.getUserDataPath().resolve("project");
                var outputFile = path.getFile().resolve(name.getText().trim() + ".brickbench");

                FileUtils.deleteDirectory(projectPath.toFile());
                Files.createDirectories(projectPath);
                var project = new Project(
                        true, Project.GameVersion.LSW_TCS, name.getText().trim(), projectPath.resolve("project.xml"),
                        null,
                        new Project.Assets(new ArrayList<>(), new ArrayList<>()), 
                        new ProjectStructure(new ProjectStructure.FolderNode("root", List.of()))
                );

                var success = ProjectIO.saveProject(project, outputFile);
                if(success) OpenGG.asyncExec(() -> BrickBench.CURRENT.loadNewProject(outputFile));
                this.dispose();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        close.addActionListener(a -> this.dispose());

        this.setPreferredSize(new Dimension(400, 300));
        this.setLocation(MouseInfo.getPointerInfo().getLocation());
        this.setTitle("Create project");
        this.pack();
        this.setResizable(false);
        this.setVisible(true);

        this.requestFocus();
    }
}
