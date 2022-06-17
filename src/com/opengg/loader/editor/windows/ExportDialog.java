package com.opengg.loader.editor.windows;

import com.opengg.core.Configuration;
import com.opengg.core.console.GGConsole;
import com.opengg.core.math.util.Tuple;
import com.opengg.loader.FileUtil;
import com.opengg.loader.BrickBench;
import com.opengg.loader.MapXml;
import com.opengg.loader.Project;
import com.opengg.loader.SwingUtil;
import com.opengg.loader.editor.components.FileSelectField;
import com.opengg.loader.loading.MapIO;
import com.opengg.loader.loading.ProjectIO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class ExportDialog extends JDialog {
    public ExportDialog(Project project){
        super(BrickBench.CURRENT.window);
        this.setLayout(new BorderLayout());

        var wholeProject = createProjectExportConfig(project);
        var perMap = createMapExportConfig(project);

        var exportProject = new JButton("Export");
        var close = new JButton("Close");

        var exportLabel = new JLabel("Export directory");
        var exportLocation = new FileSelectField(Path.of(Configuration.getConfigFile("recent.ini").getConfig("recent-export")), FileUtil.LoadType.DIRECTORY, "Export directory");

        var locationLine = new JPanel();
        locationLine.add(exportLabel);
        locationLine.add(exportLocation);

        var exportTabs = new JTabbedPane();
        exportTabs.setBorder(new EmptyBorder(5, 5, 5, 5));
        exportTabs.addTab("Project", wholeProject.x());
        exportTabs.addTab("Maps", perMap.x());

        var exportLine = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        exportLine.add(exportProject);
        exportLine.add(close);

        this.add(locationLine, BorderLayout.NORTH);
        this.add(exportTabs, BorderLayout.CENTER);
        this.add(exportLine, BorderLayout.SOUTH);

        exportProject.addActionListener(a -> {
            var runner = switch (exportTabs.getSelectedIndex()){
                case 0 -> wholeProject.y();
                case 1 -> perMap.y();
                default -> throw new IllegalStateException("No tab exists with " + exportTabs.getSelectedIndex());
            };

            GGConsole.log("Exporting project to " + exportLocation.getFile().toString());
            Configuration.getConfigFile("recent.ini").writeConfig("recent-export", exportLocation.getFile().toString());
            Configuration.writeFile(Configuration.getConfigFile("recent.ini"));
            runner.accept(exportLocation.getFile());
        });

        close.addActionListener(a -> this.dispose());

        this.setTitle("Export");
        this.pack();
        this.setSize(400, 400);
        this.setResizable(false);
        this.setLocation(MouseInfo.getPointerInfo().getLocation());
        this.setVisible(true);

        this.requestFocus();
    }

    private Tuple<JComponent, Consumer<Path>> createProjectExportConfig(Project project) {
        var wholeProject = new JPanel();
        wholeProject.setLayout(new BoxLayout(wholeProject, BoxLayout.Y_AXIS));

        var toggleLevelsRow = new JPanel();
        var noExport = new JRadioButton("None");
        noExport.setToolTipText("Do not export any AREAS.TXT file");

        var exportStub = new JRadioButton("Stub");
        exportStub.setToolTipText("Export an AREAS_" + project.projectName() + ".TXT file with only project maps for use in TTMM");

        var exportFull = new JRadioButton("Full");
        exportFull.setToolTipText("Export an AREAS.TXT file containing the new areas appended to the end.");

        var group = new ButtonGroup();
        group.add(noExport);
        group.add(exportStub);
        group.add(exportFull);

        toggleLevelsRow.add(new JLabel("Export AREAS.TXT as: "));
        toggleLevelsRow.add(noExport);
        toggleLevelsRow.add(exportStub);
        toggleLevelsRow.add(exportFull);

        wholeProject.add(toggleLevelsRow);

        Consumer<Path> onExport = path -> {
            try (var exit = SwingUtil.showLoadingAlert("Exporting...", "Exporting project " + project.projectName(), false)) {
                var exportType = GlobalTextExportType.NONE;
                if(exportStub.isSelected()) exportType = GlobalTextExportType.STUB;
                if(exportFull.isSelected()) exportType = GlobalTextExportType.FULL;

                ProjectIO.exportProject(project, path, exportType);
                JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "Exported project to " + path);
            } catch (Exception e) {
                SwingUtil.showErrorAlert("Failed to export project to " + path, e);
            }
        };
        return Tuple.of(wholeProject, onExport);
    }

    private Tuple<JComponent, Consumer<Path>> createMapExportConfig(Project project) {
        var mapSplit = new JSplitPane();

        record MapListItem(MapXml map){
            @Override
            public String toString() {
                return map.name();
            }
        }

        var maps = project.maps().stream()
                .map(MapListItem::new).toArray(MapListItem[]::new);
        var mapList = new JList<MapListItem>(maps);
        mapList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        mapSplit.setLeftComponent(mapList);
        mapSplit.setRightComponent(new JPanel());

        Consumer<Path> export = p -> {
            if(mapList.getSelectedIndex() == -1){
                JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "Please select a map before exporting");
                return;
            }

            try {
                MapIO.exportMap(project, p, mapList.getSelectedValue().map);
            } catch (IOException e) {
                SwingUtil.showErrorAlert("Failed to export map to " + p, e);
            }
        };

        return Tuple.of(mapSplit, export);
    }

    public enum GlobalTextExportType{
        NONE,
        STUB,
        FULL
    }
}
