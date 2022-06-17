package com.opengg.loader.editor.hook;

import com.opengg.loader.MapXml;
import com.opengg.loader.SwingUtil;
import com.opengg.loader.editor.MapInterface;
import com.opengg.loader.editor.components.WrapLayout;
import com.opengg.loader.editor.tabs.EditorTabAutoRegister;
import com.opengg.loader.game.nu2.LevelsTXTParser;

import net.miginfocom.swing.MigLayout;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.opengg.core.Configuration;
import com.opengg.core.engine.Executor;
import com.opengg.loader.BrickBench;
import com.opengg.loader.editor.tabs.EditorTab;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@EditorTabAutoRegister
public class TCSHookPanel extends JPanel implements EditorTab {
    private AIMessageTableModel aiMessageModel = new AIMessageTableModel(new ArrayList<>());
    private JButton connectButton;
    private JTextField mapID;
    private JCheckBox door, reset;
    private JComboBox<String> mapCombo;
    private Map<String, Integer> mapNameToID = new HashMap<>();

    public TCSHookPanel() {
        setLayout(new BorderLayout());
        TCSHookManager.panel = this;

        JPanel hookManagerPanel = new JPanel(new WrapLayout());

        connectButton = new JButton("Start Hook");
        connectButton.addActionListener(a -> {
            if(!TCSHookManager.isEnabled()){
                TCSHookManager.beginHook();
            }else{
                TCSHookManager.endHook();
            }
        });
        hookManagerPanel.add(connectButton);

        mapID = new JTextField("0");
        mapID.setColumns(3);
        hookManagerPanel.add(mapID);

        mapCombo = new JComboBox<>();
        mapCombo.addActionListener(m -> mapID.setText(String.valueOf(mapNameToID.get((String) mapCombo.getSelectedItem()))));
        hookManagerPanel.add(mapCombo);

        var loadMap = new JButton("Load map");
        hookManagerPanel.add(loadMap);

        this.add(hookManagerPanel, BorderLayout.NORTH);

        JTabbedPane tabPane = new JTabbedPane();
        JPanel aiMessagePanel = new JPanel();
        aiMessagePanel.setLayout(new BorderLayout());

        JTable aiMessageTable = new JTable();
        aiMessageTable.setShowVerticalLines(true);
        aiMessageTable.setShowHorizontalLines(true);
        aiMessageTable.setModel(aiMessageModel);
        aiMessagePanel.add(new JScrollPane(aiMessageTable), BorderLayout.CENTER);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> {
            if(TCSHookManager.currentHook != null) {
                aiMessageModel.set(TCSHookManager.currentHook.getAIMessages());
                aiMessageModel.fireTableDataChanged();
            }
        });

        JCheckBox enableAutoRefresh = new JCheckBox("Auto-refresh");
        Executor.every(Duration.ofSeconds(1), () -> {
                if(enableAutoRefresh.isSelected()){
                    SwingUtilities.invokeLater(() -> {
                        if(TCSHookManager.currentHook != null) {
                            aiMessageModel.set(TCSHookManager.currentHook.getAIMessages());
                            aiMessageModel.fireTableDataChanged();
                        }
                    });
                }});

        JButton pushEdit = new JButton("Push edits");
        pushEdit.addActionListener(e->{
            if(TCSHookManager.currentHook != null) {
                pushEdit.setEnabled(false);
                List<AIMessage> updatedMessages = new ArrayList<>();
                HashMap<String,AIMessage> realMessages = new HashMap<>();

                TCSHookManager.currentHook.getAIMessages().forEach(a-> realMessages.put(a.name, a));

                for (AIMessage message : aiMessageModel.internal) {
                    AIMessage realMessage = realMessages.get(message.name);
                    if(realMessage != null){
                        message.address = realMessage.address;
                        updatedMessages.add(message);
                    }
                }

                TCSHookManager.currentHook.updateAIMessage(updatedMessages);
                aiMessageModel.set(TCSHookManager.currentHook.getAIMessages());
                aiMessageModel.fireTableDataChanged();
                pushEdit.setEnabled(true);
            }
        });

        JPanel buttonRow = new JPanel(new WrapLayout());
        buttonRow.add(enableAutoRefresh);
        buttonRow.add(refresh);
        buttonRow.add(pushEdit);

        aiMessagePanel.add(buttonRow, BorderLayout.NORTH);

        var configPanel = new JPanel(new MigLayout("wrap 1"));
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));

        var autoload = new JCheckBox("Autoload maps from current hooked game");
        reset = new JCheckBox("Reset map on load");
        door = new JCheckBox("Reset door on load");
        autoload.setSelected(Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig("autoload-hook")));
        autoload.addActionListener(a -> {
            Configuration.getConfigFile("editor.ini").writeConfig("autoload-hook", String.valueOf(autoload.isSelected())); BrickBench.CURRENT.reloadConfigFileData();
        });
        autoload.setToolTipText("Automatically loads the current map in the hooked game instance.");
        configPanel.add(autoload);
        configPanel.add(reset);
        configPanel.add(door);

        loadMap.addActionListener(a -> {
            loadCurrentMap();
        });

        var global = new JCheckBox("Enable global hotkeys");
        global.addActionListener(a -> {
            try {
                if(global.isSelected()){
                    GlobalScreen.registerNativeHook();
                    Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
                    logger.setLevel(Level.SEVERE);
                    logger.setUseParentHandlers(false);

                    GlobalScreen.addNativeKeyListener(BrickBench.CURRENT);

                    JOptionPane.showMessageDialog(this, "Registered global key hook.");
                }else{
                    GlobalScreen.removeNativeKeyListener(BrickBench.CURRENT);
                    JOptionPane.showMessageDialog(this, "De-registered global key hook.");

                }


            } catch (NativeHookException e) {
                SwingUtil.showErrorAlert("Failed to register the global hook", e);
            }
        });

        configPanel.add(global);

        tabPane.add("Options", configPanel);
        tabPane.add("AI Messages", aiMessagePanel);
        add(tabPane, BorderLayout.CENTER);
    }

    public void loadCurrentMap() {
        if(TCSHookManager.isEnabled()) {
            if(door.isSelected()){
                TCSHookManager.currentHook.resetDoor();
            }
            if(reset.isSelected()) {
                TCSHookManager.currentHook.setResetBit();
            }
            TCSHookManager.currentHook.setTargetMap(Integer.parseInt(mapID.getText()));
        }
    }

    public void reloadMaps(Path gamePath){
        var levelsFile = gamePath.resolve("levels/levels.txt");
        try {
            var levelsParser = new LevelsTXTParser();
            levelsParser.parseFile(levelsFile);

            var levels = levelsParser.getEntries().stream()
                    .filter(e -> e.type() != MapXml.MapType.TEST).toList();

            mapNameToID.clear();

            for(var level : levels){
                mapNameToID.put(level.name(), levels.indexOf(level));
            }

            var namesStream = levels.stream()
                    .map(LevelsTXTParser.LevelTXTEntry::name);

            if (Configuration.getBoolean("alphabetical-order-hook-map-list"))
                namesStream = namesStream.sorted(Comparator.comparing(String::toLowerCase));

            var names = namesStream.toArray(String[]::new);
            mapCombo.setModel(new DefaultComboBoxModel<>(names));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getTabName() {
        return "Runtime hook";
    }

    @Override
    public String getTabID() {
        return "hook-pane";
    }

    @Override
    public MapInterface.InterfaceArea getPreferredArea() {
        return MapInterface.InterfaceArea.TOP_RIGHT;
    }

    @Override
    public boolean getDefaultActive() {
        return false;
    }

    static class AIMessageTableModel extends AbstractTableModel {
        List<AIMessage> internal;
        String[] aiColumns = {"Name", "Value"};
        public AIMessageTableModel(List<AIMessage> initial) {
            internal = initial;
        }
        public void set(List<AIMessage> newList){
            internal = newList;
        }

        @Override
        public int getRowCount() {
            return internal.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex){
            return columnIndex == 1; //Or whatever column index you want to be editable
        }
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case 0 -> String.class;
                case 1 -> Float.class;
                default -> Boolean.class;
            };
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if(columnIndex == 0){
                return internal.get(rowIndex).name;
            }else if(columnIndex == 1){
                return internal.get(rowIndex).value;
            }
            return -1;
        }
        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex)
        {
            AIMessage row = internal.get(rowIndex);
            if(1 == columnIndex) {
                row.value = ((float) aValue);
            }
        }

        public String getColumnName(int column) {
            return aiColumns[column];
        }

    }
    public static class AIMessage{
        public String name;
        public float value;
        public int address;

        public AIMessage(String name,float value, int address){
            this.name = name;
            this.value = value;
            this.address = address;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AIMessage aiMessage = (AIMessage) o;
            return Objects.equals(name, aiMessage.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    public void updateConnectionUIState(){
        if(TCSHookManager.isEnabled()){
            reloadMaps(TCSHookManager.currentHook.getDirectory());
            connectButton.setText("Close Hook");
        }else{
            connectButton.setText("Open Hook");
        }
    }
}
