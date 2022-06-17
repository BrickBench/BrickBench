package com.opengg.loader.editor;

import com.opengg.core.console.GGConsole;
import com.opengg.loader.GameBaseManager;
import com.opengg.loader.MapXml;
import com.opengg.loader.Project;
import com.opengg.loader.SwingUtil;
import com.opengg.loader.editor.windows.FileImportDialog;
import com.opengg.loader.loading.MapLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class MapEditPanel extends JPanel{
    private MapXml map;

    public MapEditPanel(MapXml map){
        super(new BorderLayout());
        this.map = map;

        var files = new ArrayList<>(map.files());

        var model = new DefaultListModel<Path>();
        model.addAll(files);

        var fileList = new JList<>(model);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int row = fileList.getSelectedIndex();
                if(row != -1 && e.getClickCount() == 2){
                    var file = map.files().get(row);
                    openFileInEditor(EditorState.getProject().game(), file);
                }
            }
        });

        var bottomRow = new JPanel();
        var openButton = new JButton("Open");
        var addButton = new JButton("Add");

        bottomRow.add(openButton);
        bottomRow.add(addButton);

        openButton.addActionListener(a -> {
            if(fileList.getSelectedIndex() != -1){
                var file = map.files().get(fileList.getSelectedIndex());
                openFileInEditor(EditorState.getProject().game(), file);
            }
        });

        addButton.addActionListener(a -> new FileImportDialog(map));

        var listScroll = new JScrollPane(fileList);
        this.add(listScroll, BorderLayout.CENTER);
        this.add(bottomRow, BorderLayout.SOUTH);
    }

    private void openFileInEditor(Project.GameVersion game, Path file){
        var possibleDiff = Path.of(file.toString() + ".diff");

        var path = map.mapFilesDirectory().resolve(file);
        var possibleDiffPath = map.mapFilesDirectory().resolve(possibleDiff);

        try {
            if(!Files.exists(path) && Files.exists(possibleDiffPath)){
                MapLoader.applyDiffForFile(possibleDiff,
                        GameBaseManager.getBaseDirectoryOrPromptForNew(game).get(), EditorState.getProject().structure(), map);
            }

            Desktop.getDesktop().open(path.toFile());
        } catch (IOException ioException) {
            SwingUtil.showErrorAlert("Failed while opening the editor file", ioException);
        }
    }

}
