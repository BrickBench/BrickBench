package com.opengg.loader.editor.windows;

import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.util.UIScale;
import com.opengg.core.Configuration;
import com.opengg.core.console.GGConsole;
import com.opengg.core.engine.Resource;
import com.opengg.loader.FileUtil;
import com.opengg.loader.BrickBench;
import com.opengg.loader.editor.EditorTheme;
import com.opengg.loader.editor.components.FileSelectField;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

public class SettingsDialog extends JDialog {

    JPanel currentSettingsPanel = new JPanel();
    JPanel settingsPanel = new JPanel();
    JPanel settingsMenuPanel = new JPanel();

    public SettingsDialog(Window window){
        super(window, "Settings");

        setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        this.setIconImage(new ImageIcon(Resource.getTexturePath("icon.png")).getImage());
        this.setLocation(MouseInfo.getPointerInfo().getLocation());
        this.setLayout(new GridBagLayout());
        this.setResizable(false);

        settingsMenuPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        settingsMenuPanel.setPreferredSize(new Dimension(350, 400));
        settingsMenuPanel.setLayout(new BoxLayout(settingsMenuPanel, BoxLayout.Y_AXIS));
        String[] s = new String[]{"Look and Feel", "Controls", "Editor", "Game Hook", "Advanced"};
        var list = new JList<>(s); //data has type Object[]
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(10);
        list.setFixedCellHeight(40);
        list.setFixedCellWidth(100);
        list.addMouseListener(new MouseAdapter() {
                                  public void mouseClicked(MouseEvent evt) {
                                      switch (((JList<String>) evt.getSource()).getSelectedIndex()) {
                                          case 0 -> useLafPanel();
                                          case 2 -> useEditorPanel();
                                          case 1 -> useControlsPanel();
                                          case 3 -> useHookPanel();
                                          case 4 -> useAdvancedPanel();
                                          default -> GGConsole.warning("Invalid Menu Option");
                                      }
                                  }
                              });

        settingsPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        settingsPanel.setPreferredSize(new Dimension(500, 400));

        JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,list,settingsPanel);
        pane.setEnabled(false);
        this.add(pane);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Configuration.writeFile(Configuration.getConfigFile("editor.ini"));
            }
        });

        this.pack();
        this.setVisible(true);
    }

    private void useControlsPanel(){
        resetPanel();

        var sensitivity = new JSlider(JSlider.HORIZONTAL, 1, 30, (int) (Float.parseFloat(Configuration.get("sensitivity"))*20));
        sensitivity.setPaintTicks(true);
        sensitivity.setPaintLabels(true);
        sensitivity.setMajorTickSpacing(20);
        sensitivity.setMinorTickSpacing(2);
        sensitivity.addChangeListener(s -> Configuration.getConfigFile("editor.ini").writeConfig("sensitivity",
                String.valueOf(sensitivity.getValue()/20f)));
        sensitivity.setToolTipText("Sets the mouse sensitivity for the camera.");

        var fov = new JSlider(JSlider.HORIZONTAL, 30, 150, Integer.parseInt(Configuration.get("fov")));
        fov.setPaintTicks(true);
        fov.setPaintLabels(true);
        fov.setMajorTickSpacing(20);
        fov.setMinorTickSpacing(2);
        fov.addChangeListener(s -> Configuration.getConfigFile("editor.ini").writeConfig("fov",
                String.valueOf(fov.getValue())));
        fov.setToolTipText("Sets the camera field of view.");

        var camLock = new JCheckBox();
        camLock.setSelected(Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig("camera-lock")));
        camLock.addActionListener(s -> Configuration.getConfigFile("editor.ini").writeConfig("camera-lock",
                String.valueOf(camLock.isSelected())));
        camLock.setToolTipText("Locks the camera to +/- 90 degrees, preventing it from going upside down.");

        var retainPos = new JCheckBox();
        retainPos.setSelected(Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig("retain-position")));
        retainPos.addActionListener(s -> Configuration.getConfigFile("editor.ini").writeConfig("retain-position",
                String.valueOf(retainPos.isSelected())));
        retainPos.setToolTipText("Sets if the camera position should be reset to (0, 0, 0) when a new map is loaded.");

        addItem("In-map mouse sensitivity", sensitivity);
        addItem("Field of view", fov);
        addItem("Lock camera", camLock);
        addCompactItem("Retain position when loading new maps", retainPos);

        this.settingsPanel.validate();
        this.settingsPanel.repaint();
        this.validate();
    }

    private void useEditorPanel(){
        resetPanel();

        String currentHome = Configuration.getConfigFile("editor.ini").getConfig("home");
        var homeDirectory = new FileSelectField(currentHome.length() == 0 ? Path.of(FileSystemView.getFileSystemView().getDefaultDirectory().getPath()) : Path.of(currentHome),
                FileUtil.LoadType.DIRECTORY, "Home folder");
        homeDirectory.onSelect(f -> Configuration.getConfigFile("editor.ini").writeConfig( "home", f.toString()));
        homeDirectory.setToolTipText("Sets the directory to launch the file chooser into.");

        String tcsRootStr = Configuration.getConfigFile("editor.ini").getConfig("lswtcs-clean-game-root");
        var tcsRoot = new FileSelectField(tcsRootStr.length() == 0 ? Path.of(FileSystemView.getFileSystemView().getDefaultDirectory().getPath()) : Path.of(tcsRootStr),
                FileUtil.LoadType.DIRECTORY, "TCS custom root directory");
        tcsRoot.onSelect(f -> Configuration.getConfigFile("editor.ini").writeConfig( "lswtcs-clean-game-root", f.toString()));
        tcsRoot.setToolTipText("Sets a custom directory to treat as a clean game copy of TCS. Only set this if you're sure that the directory will never accidentally be edited. \n Leave empty to use the internal copy.");

        var discordBox = new JCheckBox();
        discordBox.setSelected(Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig("discord-integration")));
        discordBox.addActionListener(s -> Configuration.getConfigFile("editor.ini").writeConfig("discord-integration",
                String.valueOf(discordBox.isSelected())));
        discordBox.setToolTipText("Enable Discord Rich Presence (requires restart).");

        var discordShowProject = new JCheckBox();
        discordShowProject.setSelected(Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig("discord-integration-show-project")));
        discordShowProject.addActionListener(s -> Configuration.getConfigFile("editor.ini").writeConfig("discord-integration-show-project",
                String.valueOf(discordShowProject.isSelected())));
        discordShowProject.setToolTipText("Show active project and map in Discord Rich Presence.");

        addItem("Home directory", homeDirectory);
        addItem("TCS custom root directory", tcsRoot);

        addItem("Enable Discord Rich Presence", discordBox);
        addItem("Show project in rich presence", discordShowProject);


        this.settingsPanel.validate();
        this.settingsPanel.repaint();
        this.validate();
    }

    private void useLafPanel(){
        resetPanel();

        var laf = new JComboBox<>(new String[]{"Flat Light", "Flat Dark", "High Contrast", "Atom One Dark", "Material Darker","Arc Dark","Carbon"
        ,"Gradianto Deep Ocean"});
        laf.setSelectedItem(Configuration.get("laf"));
        laf.addActionListener(a ->
        {
            FlatAnimatedLafChange.stop();
            Configuration.getConfigFile("editor.ini").writeConfig("laf", (String) laf.getSelectedItem());
            FlatAnimatedLafChange.showSnapshot();
            try {
                EditorTheme.applyTheme();
            } catch (UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }
            SwingUtilities.updateComponentTreeUI(this);
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        });
        laf.setToolTipText("Sets the user interface look-and-feel of BrickBench.");

        var menuLaf = new JCheckBox();
        menuLaf.setSelected(Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig("use-native-file-dialog")));
        menuLaf.addActionListener(s -> Configuration.getConfigFile("editor.ini").writeConfig("use-native-file-dialog",
                String.valueOf(menuLaf.isSelected())));
        menuLaf.setToolTipText("Uses the system file dialog instead of the file selector from the look-and-feel.");

        addItem("Editor Theme", laf);
        addCompactItem("Use system file dialog", menuLaf);
        JSlider ui = new JSlider(0,200,100);
        ui.addChangeListener(s->{
            UIScale.scale((ui.getValue()/100.0f));
            if(BrickBench.CURRENT != null && BrickBench.CURRENT.window != null) {
                /*try {
                    UIManager.setLookAndFeel(CURRENT_LAF);
                } catch (UnsupportedLookAndFeelException e) {
                    e.printStackTrace();
                }*/

                SwingUtilities.updateComponentTreeUI(BrickBench.CURRENT.window);
                SwingUtilities.updateComponentTreeUI(this);

                BrickBench.CURRENT.window.pack();
            }
        });
       // addItem("UI Scale",ui);

        this.settingsPanel.validate();
        this.settingsPanel.repaint();
        this.validate();
    }

    private void useHookPanel(){
        resetPanel();

        var hook = new JTextField(Configuration.get("hook-executable-name"));
        hook.addActionListener(a -> Configuration.getConfigFile("editor.ini").writeConfig("hook-executable-name", hook.getText()));
        hook.setToolTipText("Sets the TCS executable name to hook into.");

        var alphaOrderMaps = new JCheckBox();
        alphaOrderMaps.setSelected(Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig("alphabetical-order-hook-map-list")));
        alphaOrderMaps.addActionListener(s -> Configuration.getConfigFile("editor.ini").writeConfig("alphabetical-order-hook-map-list",
                String.valueOf(alphaOrderMaps.isSelected())));
        alphaOrderMaps.setToolTipText("Sets if the list in the dropdown for maps in the hook should be in game order or alphabetical order.");


        addItem("TCS hook executable name", hook);
        addCompactItem("Map list in alphabetical order", alphaOrderMaps);

        this.settingsPanel.validate();
        this.settingsPanel.repaint();
        this.validate();
    }

    private void useAdvancedPanel() {
        resetPanel();

        var rotateTerrain = new JCheckBox();
        rotateTerrain.setSelected(Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig("use-rotation-platform")));
        rotateTerrain.addActionListener(s -> Configuration.getConfigFile("editor.ini").writeConfig("use-rotation-platform",
                String.valueOf(rotateTerrain.isSelected())));
        rotateTerrain.setToolTipText("Applies rotation to terrain platforms.");

        var format = new JComboBox<>(new String[]{"Autodetect", "LSW", "LIJ1/Batman"});
        format.setSelectedItem(Configuration.get("gsc-format"));
        format.addActionListener(a -> Configuration.getConfigFile("editor.ini").writeConfig("gsc-format", (String) format.getSelectedItem()));
        format.setToolTipText("<html>Sets the file format to load GSC files as. <br>Autodetect is almost always correct, but can be changed if a map is failing to load.</html>");

        addItem("Load GSC files as", format);
        addCompactItem("Rotate terrain platforms", rotateTerrain);

        this.settingsPanel.validate();
        this.settingsPanel.repaint();
        this.validate();
    }

    private void resetPanel(){
        settingsPanel.remove(currentSettingsPanel);

        currentSettingsPanel = new JPanel();
        settingsPanel.add(currentSettingsPanel);
        currentSettingsPanel.setLayout(new MigLayout("wrap 2, fillx, align left top", "[]10[]", "[]15[]"));
    }

    public void addItem(String label, JComponent component){
        var jlabel = new JLabel(label);
        jlabel.setToolTipText(component.getToolTipText());

        currentSettingsPanel.add(jlabel);
        currentSettingsPanel.add(component, "growx");

    }

    public void addCompactItem(String label, JComponent component){
        var jlabel = new JLabel(label);
        jlabel.setToolTipText(component.getToolTipText());

        var panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.add(component);
        panel.add(jlabel);
        currentSettingsPanel.add(panel, "span 2");
    }
}
