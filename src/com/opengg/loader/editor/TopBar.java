package com.opengg.loader.editor;

import com.opengg.core.Configuration;
import com.opengg.core.engine.OpenGG;
import com.opengg.core.engine.Resource;
import com.opengg.core.world.WorldEngine;
import com.opengg.loader.FileUtil;
import com.opengg.loader.Project;
import com.opengg.loader.BrickBench;
import com.opengg.loader.components.PlayerView;
import com.opengg.loader.editor.windows.*;
import com.opengg.loader.loading.ProjectIO;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class TopBar extends JMenuBar {
    private final Map<String, JMenuItem> topBarItems = new HashMap<>();

    public TopBar(){
        EditorState.addMapChangeListener(p -> refreshValues());
    }

    public void refreshValues(){
        this.removeAll();
        topBarItems.clear();

        var fileMenu = new JMenu();
        fileMenu.setText("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        var viewMenu = new JMenu();
        viewMenu.setText("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);

        var importMenu = new JMenu();
        importMenu.setText("Import");
        importMenu.setMnemonic(KeyEvent.VK_I);

        var toolsMenu = new JMenu();
        toolsMenu.setText("Tools");
        toolsMenu.setMnemonic(KeyEvent.VK_T);

        var help = new JMenu();
        help.setText("Help");
        help.setMnemonic(KeyEvent.VK_H);

        this.add(fileMenu);
        this.add(viewMenu);
        this.add(toolsMenu);
        this.add(importMenu);
        this.add(help);

        var newProj = makeByName("new", new JMenuItem("New Project"));
        newProj.addActionListener(a -> new ProjectCreationDialog());
        newProj.setToolTipText("Create a new project.");
        newProj.setMnemonic(KeyEvent.VK_N);
        newProj.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));

        var loadProject = makeByName("project", new JMenuItem("Load Project"));
        loadProject.addActionListener(a -> BrickBench.CURRENT.openProjectChooser());
        loadProject.setToolTipText("Load a BrickBench project.");
        loadProject.setMnemonic(KeyEvent.VK_L);
        loadProject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK));

        var loadRecent = makeByName("loadRecent", new JMenu("Load Recent"));
        loadRecent.setMnemonic(KeyEvent.VK_R);
        for(int i = 0; i < BrickBench.RECENT_SAVES; i++){
            var file = Configuration.getConfigFile("recent.ini").getConfig("recent_" + i);
            if(!file.isEmpty()){
                var recent = new JMenuItem(file);
                recent.setMnemonic(KeyEvent.VK_1 + i);
                recent.addActionListener(a -> OpenGG.asyncExec(() -> BrickBench.CURRENT.loadNewProject(Path.of(file))));
                loadRecent.add(recent);
            }
        }

        var saveProject = makeByName("save", new JMenuItem("Save Project"));
        saveProject.addActionListener(a -> {
            if(EditorState.getProject() != null && EditorState.getProject().isProject()){
                ProjectIO.saveProject(EditorState.getProject(), EditorState.getProject().projectSource());
                JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "Saved project at " + EditorState.getProject().projectSource());
            }else{
                JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "No project is currently open.");
            }
        });
        saveProject.setToolTipText("Saves the current project file.");
        saveProject.setMnemonic(KeyEvent.VK_S);
        saveProject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));

        var saveAsProject = makeByName("saveAs", new JMenuItem("Save As"));
        saveAsProject.addActionListener(a -> {
            if(EditorState.getProject() != null && EditorState.getProject().isProject()){
                var newPath = FileUtil.openSaveDialog(EditorState.getProject().projectSource().getParent().toString(), FileUtil.LoadType.FILE, "BrickBench Project", "brickbench");

                newPath.ifPresent(p -> {
                    ProjectIO.saveProject(EditorState.getProject(), p);
                    JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "Saved project at " + p);
                });
            }else{
                JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "No project is currently open.");
            }
        });
        saveAsProject.setToolTipText("Saves the current project file with a new name.");
        saveAsProject.setMnemonic(KeyEvent.VK_S);
        saveAsProject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));

        var importMap = makeByName("importMap", new JMenuItem("Import Map/Area"));
        importMap.addActionListener(a -> {
            if (EditorState.getProject() != null && EditorState.getProject().isProject()) {
                new ProjectImportDialog(EditorState.getProject(), null);
            } else {
                JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "No project is currently open.");
            }
        });
        importMap.setToolTipText("Import a map or area into the current project.");
        importMap.setMnemonic(KeyEvent.VK_I);
        importMap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK));

        var loadMap = makeByName("map", new JMenuItem("Load Read-only Map"));
        loadMap.addActionListener(a -> BrickBench.CURRENT.openProjectChooser());
        loadMap.setToolTipText("Load a map for viewing.");
        loadMap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));

        var importThings = new JMenuItem();
        importThings.setText("Import THINGS_PC.GSC");
        importThings.addActionListener(a -> BrickBench.CURRENT.importThings());
        importThings.setToolTipText("Import THINGS_PC.GSC to more accurately render gizmos.");

        var cleanFiles = makeByName("cleanFiles", new JMenuItem("Clear Game Files"));
        cleanFiles.addActionListener(a -> BrickBench.CURRENT.cleanGameDirectories());
        cleanFiles.setToolTipText("Cleans the project working files and game directories. This allows you to reimport a clean copy of the game.");

        var settingsMenu = makeByName("settings", new JMenuItem("Settings"));
        settingsMenu.addActionListener(a -> new SettingsDialog(BrickBench.CURRENT.window));
        settingsMenu.setToolTipText("Open the settings menu.");
        settingsMenu.setMnemonic(KeyEvent.VK_T);
        settingsMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));

        var export = makeByName("export", new JMenuItem("Export"));
        export.addActionListener(e -> {
            if(EditorState.getProject() != null && EditorState.getProject().isProject()){
                new ExportDialog(EditorState.getProject());
            }else{
                JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "No project is currently open.");
            }
        });
        export.setMnemonic(KeyEvent.VK_E);
        export.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK ));

        var test = makeByName("test", new JMenuItem("Test Project"));
        test.addActionListener(e -> {
            if(EditorState.getProject() != null && EditorState.getProject().isProject()){
                ProjectIO.testProject(EditorState.getProject());
            }else{
                JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "No project is currently open.");
            }
        });
        test.setMnemonic(KeyEvent.VK_T);
        test.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));


        var quit = new JMenuItem();
        quit.setText("Quit");
        quit.addActionListener(a -> BrickBench.CURRENT.exit());
        quit.setToolTipText("Quit BrickBench.");

        fileMenu.add(newProj);
        fileMenu.add(loadProject);
        fileMenu.add(loadRecent);
        fileMenu.add(saveProject);
        fileMenu.add(saveAsProject);
        fileMenu.addSeparator();
        fileMenu.add(importMap);
        fileMenu.add(loadMap);
        fileMenu.addSeparator();
        fileMenu.add(importThings);
        fileMenu.add(cleanFiles);
        fileMenu.addSeparator();
        fileMenu.add(export);
        fileMenu.add(test);
        fileMenu.addSeparator();
        fileMenu.add(settingsMenu);
        fileMenu.add(quit);

        var compact = makeByName("compact", createStandardMenuItem("Compact Mode", "compact-mode",
                "Enables compact mode, removing the sidebars and expanding the viewport to fill the screen."));
        compact.setMnemonic(KeyEvent.VK_C);
        compact.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));

        var showBoundingBoxes = makeByName("showBoundingBoxes", createStandardMenuItem("Show Special Object Bounds", "show-specobj-bounds",
                "Displays the bounding boxes used for special object culling."));
        showBoundingBoxes.setMnemonic(KeyEvent.VK_B);
        showBoundingBoxes.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK));

        var showLabels = makeByName("showBoundingBoxes", createStandardMenuItem("Show Object Labels", "show-object-titles",
                "Displays object labels on named objects."));
        showLabels.setMnemonic(KeyEvent.VK_L);
        showLabels.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK));

                
        var speedArea = new JMenu();
        speedArea.setText("Speed presets");
        speedArea.setMnemonic(KeyEvent.VK_S);


        var slow = makeByName("slow", new JMenuItem("Slow"));
        slow.addActionListener(a -> ((PlayerView) WorldEngine.findEverywhereByName("player").get(0)).setSpeed(4f));
        slow.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, KeyEvent.ALT_DOWN_MASK));
        slow.setMnemonic(KeyEvent.VK_S);

        var medium = makeByName("medium", new JMenuItem("Medium"));
        medium.addActionListener(a -> ((PlayerView) WorldEngine.findEverywhereByName("player").get(0)).setSpeed(12f));
        medium.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.ALT_DOWN_MASK));
        medium.setMnemonic(KeyEvent.VK_M);

        var fast = makeByName("fast", new JMenuItem("Fast"));
        fast.addActionListener(a -> ((PlayerView) WorldEngine.findEverywhereByName("player").get(0)).setSpeed(24f));
        fast.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, KeyEvent.ALT_DOWN_MASK));
        fast.setMnemonic(KeyEvent.VK_F);

        speedArea.add(slow);
        speedArea.add(medium);
        speedArea.add(fast);

        var depthEmu = makeByName("depthEmu", createStandardMenuItem("View Material Depth Properties", "emulate-zbuffer",
                """
                <html>Enables Z-buffer material settings emulation. This improves the visuals of objects behind or inside of translucent objects,
                 but can interfere with viewing certain BrickBench objects. <br>
                 This should be enabled by default.</html>
                """));
        depthEmu.setMnemonic(KeyEvent.VK_D);
        depthEmu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK));

        var alphaEmu = makeByName("alphaEmu", createStandardMenuItem("Enable Alpha Blending", "emulate-alpha",
                """
            <html>Enables material alpha blending settings emulation. This option improves the visuals of layered objects,
             but can be disabled if it causes artifacts. <br> This should be enabled by default. </html>
            """));
        alphaEmu.setMnemonic(KeyEvent.VK_A);
        alphaEmu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK));

        var enhanced = makeByName("enhanced", createStandardMenuItem("Enable Enhanced Graphics", "enhanced-graphics",
                """
                        <html> Enable advanced graphics. <br> This enables advanced effects, such as specular highlights. </html>
                        """));
        enhanced.setMnemonic(KeyEvent.VK_E);

        var skybox = makeByName("showSkybox", createStandardMenuItem("Show Skybox", "emulate-skybox",
                """
                        <html> Enable rendering of the skybox the same way it would render ingame. </html>
                        """));
        skybox.setMnemonic(KeyEvent.VK_B); 

        var wireframe = makeByName("wireframe", createStandardMenuItem("Show Wireframes", "wireframe",
                """
                        <html> Sets the mesh renderer to use wireframes for static objects. </html>
                        """));
        wireframe.setMnemonic(KeyEvent.VK_W);
        wireframe.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));


        viewMenu.add(speedArea);
        viewMenu.addSeparator();
        viewMenu.add(compact);
        viewMenu.add(wireframe);
        viewMenu.addSeparator();
        viewMenu.add(enhanced);
        viewMenu.add(skybox);
        viewMenu.add(showBoundingBoxes);
        viewMenu.add(depthEmu);
        viewMenu.add(alphaEmu);

        var importMap2 = makeByName("importMap", new JMenuItem("Import Map/Area"));
        importMap2.addActionListener(a -> {
            if (EditorState.getProject() != null && EditorState.getProject().isProject()) {
                new ProjectImportDialog(EditorState.getProject(), null);
            } else {
                JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "No project is currently open.");
            }
        });
        importMap2.setToolTipText("Import a map or area into the current project.");
        importMap2.setMnemonic(KeyEvent.VK_I);

        var importAsset = makeByName("import", new JMenuItem("Import Asset"));
        importAsset.addActionListener(a -> {
            if(EditorState.getProject() != null && EditorState.getProject().isProject()){
                new AssetImportDialog(EditorState.getProject());
            }else{
                JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "No project is currently open.");
            }
        });

        importAsset.setToolTipText("Import a model or texture file for terrain or rendering.");
        importAsset.setMnemonic(KeyEvent.VK_M);
        importAsset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_DOWN_MASK));

        importMenu.add(importAsset);
        importMenu.add(importMap2);

        var window = BrickBench.CURRENT.window;
        for(var tab : BrickBench.CURRENT.window.tabs.values()){
            var select = new JCheckBoxMenuItem();
            select.setText(tab.getTabName());
            select.setSelected(SwingUtilities.getWindowAncestor((Component) tab) != null);
            select.addActionListener(a -> {
                if(this.isSelected()){
                    window.insertIntoConfigLocation(tab);
                }});
            toolsMenu.add(select);
        }

        var docsOnline = new JMenuItem();
        docsOnline.setText("Open Documentation");
        docsOnline.addActionListener(a -> openHelpPage(true));
        docsOnline.setToolTipText("Open the BrickBench Documentation online.");
        docsOnline.setMnemonic(KeyEvent.VK_W);

        var docsLocal = new JMenuItem();
        docsLocal.setText("Open Documentation (offline)");
        docsLocal.addActionListener(a -> openHelpPage(false));
        docsLocal.setToolTipText("Open the BrickBench Documentation locally as a PDF.");
        docsLocal.setMnemonic(KeyEvent.VK_L);

        var issues = new JMenuItem();
        issues.setText("Report Issues");
        issues.addActionListener(a -> openIssuesPage());
        issues.setToolTipText("Opens the BrickBench Github issues page to report issues");
        issues.setMnemonic(KeyEvent.VK_R);

        help.add(docsOnline);
        help.add(docsLocal);
        help.add(issues);
    }

    private <V extends JMenuItem> V makeByName(String name, V item){
        topBarItems.put(name, item);
        return item;
    }

    public JMenuItem getItemByName(String name){
        return topBarItems.get(name);
    }

    public JMenuItem createStandardMenuItem(String label, String property, String tooltip){
        var menuItem = makeByName(property, new JCheckBoxMenuItem(label));
        menuItem.setSelected(Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig(property)));
        menuItem.addActionListener(a -> {
            Configuration.getConfigFile("editor.ini").writeConfig(property, String.valueOf(menuItem.isSelected())); BrickBench.CURRENT.reloadConfigFileData();
        });
        menuItem.setToolTipText(tooltip);
        return menuItem;
    }

    private void openIssuesPage(){
        try {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;

            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE))
                desktop.browse(new URL("https://github.com/BrickBench/BrickBench/issues").toURI());


        } catch (Exception e) {
            StringSelection stringSelection = new StringSelection("https://github.com/BrickBench/BrickBench/issues");
            Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
            clpbrd.setContents(stringSelection, null);
            JOptionPane.showMessageDialog(this, "BrickBench failed to open the issues page." + "\n"
                    + "The URL has been copied to your clipboard, paste into your browser to access.");
        }
    }

    private void openHelpPage(boolean online){
        try {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;

            if (online) {
                if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE))
                    desktop.browse(new URL("https://brickbench.readthedocs.io").toURI());
            } else {
                if (desktop != null && desktop.isSupported(Desktop.Action.OPEN))
                    desktop.open(Resource.getApplicationPath().resolve("documentation.pdf").toFile());
            }

        } catch (Exception e) {
            StringSelection stringSelection = new StringSelection("https://brickbench.readthedocs.io");
            Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
            clpbrd.setContents(stringSelection, null);
            JOptionPane.showMessageDialog(this, "BrickBench failed to open the wiki page." + "\n"
                            + "The URL has been copied to your clipboard, paste into your browser to access.");
        }
    }
}
