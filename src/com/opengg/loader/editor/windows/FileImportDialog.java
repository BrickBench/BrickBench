package com.opengg.loader.editor.windows;

import com.opengg.loader.BrickBench;
import com.opengg.loader.FileUtil;
import com.opengg.loader.MapXml;
import com.opengg.loader.SwingUtil;
import com.opengg.loader.editor.components.FileSelectField;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileImportDialog extends JDialog {
    public FileImportDialog(MapXml map){
        super(BrickBench.CURRENT.window);
        this.setModal(true);
        this.setLayout(new MigLayout());

        var mainPanel = new JPanel(new MigLayout("wrap 2, align center center"));

        var nameLabel = new JLabel("New file name & path");
        var nameInput = new JTextField();

        var ogFile = new JLabel("File to import (optional)");
        var ogPath = new FileSelectField(Path.of(""), FileUtil.LoadType.FILE, "Importable files", "scp");

        mainPanel.add(nameLabel);
        mainPanel.add(nameInput, "growx");
        mainPanel.add(ogFile);
        mainPanel.add(ogPath);

        var importRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        var importButton = new JButton("Create");
        var cancelButton = new JButton("Cancel");
        importRow.add(importButton);
        importRow.add(cancelButton);

        cancelButton.addActionListener(a -> this.dispose());
        importButton.addActionListener(i -> {
            var newFile = map.mapFilesDirectory().resolve(nameInput.getText());
            if(Files.exists(newFile)){
                JOptionPane.showMessageDialog(this, "This file already exists.");
                return;
            }

            try {
                if(Files.isRegularFile(ogPath.getFile())){
                    Files.copy(ogPath.getFile(), newFile);
                }else{
                    Files.createFile(newFile);
                }

                map.files().add(Path.of(nameInput.getText()));

                this.dispose();
            } catch (IOException e) {
                SwingUtil.showErrorAlert("Failed to create the new file", e);

            }});

        var topPanel = new JPanel();
        topPanel.add(new JLabel("Importing file for map " + map.name()));

        this.add(topPanel, "dock north, al center center");
        this.add(mainPanel, "dock center");
        this.add(importRow, "dock south");

        this.pack();

        this.setSize(340, 250);
        this.setResizable(false);
        this.setTitle("Import file");
        this.setVisible(true);

        this.requestFocus();
    }
}

