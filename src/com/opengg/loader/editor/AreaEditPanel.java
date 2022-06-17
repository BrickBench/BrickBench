package com.opengg.loader.editor;

import com.opengg.core.engine.OpenGG;
import com.opengg.loader.ProjectStructure;
import com.opengg.loader.SwingUtil;
import com.opengg.loader.editor.components.JSplitPaneNoDivider;
import com.opengg.loader.editor.tabs.EditorPane;
import com.opengg.loader.game.nu2.Area;
import com.opengg.loader.game.nu2.AreaIO;
import com.opengg.loader.game.nu2.Area.AreaSuperCounter;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Supplier;

public class AreaEditPanel extends JPanel{
    private Area area;

    public AreaEditPanel(Area area){
        this.setLayout(new BorderLayout());
        this.area = area;

        JTabbedPane settingsTabbed = new JTabbedPane();
        settingsTabbed.add("General settings", createGeneralLevelSettings());
        settingsTabbed.add("Characters", createCharacterListEditor());
        settingsTabbed.add("Level streaming", createStreamingEditor());
        settingsTabbed.add("Super counters", createSuperCounterEditor());
        settingsTabbed.add("AI messages", createAIMessagesEditor());

        this.add(settingsTabbed,BorderLayout.CENTER);

        JPanel topLevel = new JPanel(new MigLayout("fillx"));

        JTextField nameField = new JTextField(area.name());
        nameField.addActionListener(a -> {
            OpenGG.asyncExec(() -> {
                try (var exit = SwingUtil.showLoadingAlert("Renaming...", "Renaming level files...", false)) {
                    AreaIO.renameArea(EditorState.getProject(), area, nameField.getText());
                    EditorState.updateProject(EditorState.getProject());
                }
            });
        });

        topLevel.add(new JLabel("Level Name: "));
        topLevel.add(nameField, "growx");
        topLevel.add(new JLabel("Directory: " + String.join("/", EditorState.getProject().structure().getFolderFor(area))), "right");
        topLevel.setBorder(new EmptyBorder(5,10,5,10));

        this.add(topLevel,BorderLayout.NORTH);
    }


    private JPanel createCharacterListEditor(){
        JPanel panel = new JPanel(new BorderLayout());
        JTable charTable = new JTable();
        Area.AreaProperties props = area.areaProperties();
        TableModel model = new AbstractTableModel(){
            private String[] columnNames = {"Creature Type","Map Type","Extra Toggle"};
            public String getColumnName(int col) {
                return columnNames[col];
            }
            public int getRowCount() {
                return props.creatures.size();
            }
            public Class<?> getColumnClass(int col) {
                return switch(col){
                    case 0:
                        yield String.class;
                    case 1:
                        yield Area.AreaCreature.Type.class;
                    case 2:
                        yield Boolean.class;
                    default:
                        throw new IllegalStateException("Unexpected value: " + col);
                };
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return row < this.getRowCount();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return switch(columnIndex){
                    case 0:
                        yield props.creatures.get(rowIndex).name;
                    case 1:
                        yield props.creatures.get(rowIndex).type;
                    case 2:
                        yield props.creatures.get(rowIndex).extraToggle;
                    default:
                        throw new IllegalStateException("Unexpected value: " + columnIndex);
                };
            }

            public int getColumnCount() {
                return columnNames.length;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                Area.AreaCreature creature = props.creatures.get(rowIndex);
                switch (columnIndex) {
                    case 0 -> creature.name = (String) aValue;
                    case 1 -> creature.type = (Area.AreaCreature.Type) aValue;
                    case 2 -> creature.extraToggle = (boolean) aValue;
                }
            }
        };
        charTable.setModel(model);
        charTable.getColumnModel().getColumn(1).setCellEditor(new EnumCellEditor<>(Area.AreaCreature.Type.values()));
        charTable.setShowVerticalLines(true);
        panel.add(new JScrollPane(charTable),BorderLayout.CENTER);
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addButton = SwingUtil.createIconButton(EditorIcons.add,genericTableAdd(Area.AreaCreature::new,props.creatures,model));
        JButton removeButton = SwingUtil.createIconButton(EditorIcons.minus,genericTableRemove(props.creatures,model,charTable));
        topRow.add(addButton);
        topRow.add(removeButton);
        panel.add(topRow,BorderLayout.NORTH);
        return panel;
    }

    private JPanel createStreamingEditor(){
        JPanel panel = new JPanel(new BorderLayout());
        JTable charTable = new JTable();
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Area.AreaProperties props = area.areaProperties();
        TableModel model = new AbstractTableModel (){
            private String[] columnNames = {"Level","Level 1","Level 2"};
            public String getColumnName(int col) {
                return columnNames[col];
            }
            public int getRowCount() {
                return props.streaming.size();
            }
            public Class<?> getColumnClass(int col) {
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return row < this.getRowCount();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return switch(columnIndex){
                    case 0:
                        yield props.streaming.get(rowIndex).levelName;
                    case 1:
                        yield props.streaming.get(rowIndex).level1;
                    case 2:
                        yield props.streaming.get(rowIndex).level2;
                    default:
                        throw new IllegalStateException("Unexpected value: " + columnIndex);
                };
            }

            public int getColumnCount() {
                return columnNames.length;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                switch (columnIndex) {
                    case 0 -> props.streaming.get(rowIndex).levelName = (String) aValue;
                    case 1 -> props.streaming.get(rowIndex).level1 = (String) aValue;
                    case 2 -> props.streaming.get(rowIndex).level2 = (String) aValue;
                }
            }
        };
        charTable.setModel(model);
        charTable.setShowVerticalLines(true);
        panel.add(new JScrollPane(charTable),BorderLayout.CENTER);
        JButton addButton = SwingUtil.createIconButton(EditorIcons.add, genericTableAdd(Area.AreaStreaming::new,props.streaming,model));
        JButton removeButton = SwingUtil.createIconButton(EditorIcons.minus, genericTableRemove(props.streaming,model,charTable));
        topRow.add(addButton);
        topRow.add(removeButton);
        panel.add(topRow,BorderLayout.NORTH);
        return panel;
    }

    private JPanel createSuperCounterEditor(){
        Area.AreaProperties props = area.areaProperties();
        JPanel panel = new JPanel(new BorderLayout());
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT));

        ListModel<Area.AreaSuperCounter> countModel = new AbstractListModel<>() {
            @Override
            public int getSize() {
                return props.superCounters.size();
            }

            @Override
            public AreaSuperCounter getElementAt(int index) {
                return props.superCounters.get(index);
            }
        };

        var counterList = new JList<>(countModel);
        counterList.setBorder(null);
        JButton addButton = SwingUtil.createIconButton(EditorIcons.add,(e)->{
            props.superCounters.add(new Area.AreaSuperCounter());
            counterList.updateUI();
        });
        JButton removeButton = SwingUtil.createIconButton(EditorIcons.minus,(e)->{
            if(counterList.getSelectedIndex() != -1){
                props.superCounters.remove(counterList.getSelectedIndex());
                counterList.setSelectedIndex(-1);
                counterList.updateUI();
            }
        });

        topRow.add(addButton);
        topRow.add(removeButton);
        panel.add(topRow,BorderLayout.NORTH);
        JScrollPane counterScroll = new JScrollPane(counterList);
        counterScroll.setBorder(null);
        JSplitPaneNoDivider edgeSplit = new JSplitPaneNoDivider(JSplitPane.VERTICAL_SPLIT);

        JPanel pickupPanel = new JPanel(new BorderLayout());
        JPanel topRowPickup = new JPanel();
        topRowPickup.setLayout(new BoxLayout(topRowPickup,BoxLayout.X_AXIS));

        var model = new AbstractTableModel (){
            private String[] columnNames = {"Name","Level","Draw at Type","Draw at Target"};
            public String getColumnName(int col) {
                return columnNames[col];
            }
            public int getRowCount() {
                return counterList.getSelectedIndex() == -1 ? 0 : props.superCounters.get(counterList.getSelectedIndex()).pickups.size();
            }
            public Class<?> getColumnClass(int col) {
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return row < this.getRowCount();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                if(counterList.getSelectedIndex() == -1) return null;
                return switch(columnIndex){
                    case 0:
                        yield props.superCounters.get(counterList.getSelectedIndex()).pickups.get(rowIndex).pickupName;
                    case 1:
                        yield props.superCounters.get(counterList.getSelectedIndex()).pickups.get(rowIndex).levelName;
                    case 2:
                        yield props.superCounters.get(counterList.getSelectedIndex()).pickups.get(rowIndex).type;
                    case 3:
                        yield props.superCounters.get(counterList.getSelectedIndex()).pickups.get(rowIndex).target;
                    default:
                        throw new IllegalStateException("Unexpected value: " + columnIndex);
                };
            }

            public int getColumnCount() {
                return columnNames.length;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                if(counterList.getSelectedIndex() == -1) return;
                switch (columnIndex) {
                    case 0 -> props.superCounters.get(counterList.getSelectedIndex()).pickups.get(rowIndex).pickupName= (String) aValue;
                    case 1 -> props.superCounters.get(counterList.getSelectedIndex()).pickups.get(rowIndex).levelName = (String) aValue;
                    case 2 -> props.superCounters.get(counterList.getSelectedIndex()).pickups.get(rowIndex).type = (Area.AreaSuperCounterPickup.Type) aValue;
                    case 3 -> props.superCounters.get(counterList.getSelectedIndex()).pickups.get(rowIndex).target = (String) aValue;
                }
            }
        };

        JPanel superCounterAttributes = new JPanel(new FlowLayout(FlowLayout.LEFT));

        superCounterAttributes.add(new JLabel("Name:"));
        JFormattedTextField nameField = new JFormattedTextField();
        nameField.setColumns(15);
        superCounterAttributes.add(nameField);
        superCounterAttributes.add(new JLabel(""));
        superCounterAttributes.add(new JLabel("Color"));
        JButton colorButton = new JButton("Edit Color");

        colorButton.addActionListener(e -> {
            if(counterList.getSelectedIndex() == -1) return;
            Color newColor = JColorChooser.showDialog(panel,"Pick Material Color",props.superCounters.get(counterList.getSelectedIndex()).color);
            if(newColor != null){
                colorButton.setBackground(newColor);
                colorButton.setForeground(((newColor.getRed()/255.0)*0.299 + (newColor.getGreen()/255.0)*0.587 +(newColor.getBlue()/255.0)*0.114) > 0.4 ? Color.BLACK : Color.WHITE);
            }
        });

        colorButton.setOpaque(true);
        superCounterAttributes.add(colorButton);

        JTable pickupTable = new JTable(model);
        counterList.addListSelectionListener(e -> {
            if(counterList.getSelectedIndex() == -1) return;
            model.fireTableDataChanged();
            nameField.setText(props.superCounters.get(counterList.getSelectedIndex()).name);
            Color newColor = (props.superCounters.get(counterList.getSelectedIndex()).color);
            colorButton.setBackground(newColor);
            colorButton.setForeground(((newColor.getRed()/255.0)*0.299 + (newColor.getGreen()/255.0)*0.587 +(newColor.getBlue()/255.0)*0.114) > 0.4 ? Color.BLACK : Color.WHITE);
        });

        pickupTable.getColumnModel().getColumn(2).setCellEditor(new EnumCellEditor<>(Area.AreaSuperCounterPickup.Type.values()));
        pickupTable.setShowVerticalLines(true);
        pickupPanel.add(new JScrollPane(pickupTable));
        JButton addPickupButton = SwingUtil.createIconButton(EditorIcons.add,(e) -> {
            if(counterList.getSelectedIndex() == -1) return;
            java.util.List<Area.AreaSuperCounterPickup> objects = props.superCounters.get(counterList.getSelectedIndex()).pickups;
            objects.add(new Area.AreaSuperCounterPickup());
            model.fireTableRowsInserted(objects.size()-1,objects.size()-1);
        });

        JButton removePickupButton = SwingUtil.createIconButton(EditorIcons.minus,(e) -> {
            if(counterList.getSelectedIndex() == -1) return;
            int selectRow = pickupTable.getSelectedRow();
            java.util.List<Area.AreaSuperCounterPickup> objects = props.superCounters.get(counterList.getSelectedIndex()).pickups;
            if(selectRow < 0) return;
            objects.remove(selectRow);
            model.fireTableRowsDeleted(selectRow,selectRow);
        });

        topRowPickup.add(new JLabel("<html><b>Pickups</b></html>"));
        topRowPickup.add(Box.createHorizontalGlue());
        topRowPickup.add(addPickupButton);
        topRowPickup.add(removePickupButton);
        topRowPickup.setBorder(new EmptyBorder(5,5,5,5));

        pickupPanel.add(topRowPickup,BorderLayout.NORTH);

        edgeSplit.setBottomComponent(pickupPanel);
        edgeSplit.setTopComponent(superCounterAttributes);

        JSplitPaneNoDivider vertSplit = new JSplitPaneNoDivider(JSplitPane.HORIZONTAL_SPLIT);
        vertSplit.setLeftComponent(counterScroll);
        vertSplit.setRightComponent(edgeSplit);
        vertSplit.setDividerLocation(110);
        panel.add(vertSplit,BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAIMessagesEditor(){
        JPanel panel = new JPanel(new BorderLayout());
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT));

        Area.AreaProperties props = area.areaProperties();
        TableModel model = new AbstractTableModel (){
            private String[] columnNames = {"Name","Output 0","Output 1","Output 2","Output 3"};
            public String getColumnName(int col) {
                return columnNames[col];
            }
            public int getRowCount() {
                return props.aiMessages.size();
            }
            public Class<?> getColumnClass(int col) {
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return row < this.getRowCount();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return switch(columnIndex){
                    case 0:
                        yield props.aiMessages.get(rowIndex).messageName;
                    case 1:
                        yield props.aiMessages.get(rowIndex).output0;
                    case 2:
                        yield props.aiMessages.get(rowIndex).output1;
                    case 3:
                        yield props.aiMessages.get(rowIndex).output2;
                    case 4:
                        yield props.aiMessages.get(rowIndex).output3;
                    default:
                        throw new IllegalStateException("Unexpected value: " + columnIndex);
                };
            }

            public int getColumnCount() {
                return columnNames.length;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                switch (columnIndex) {
                    case 0 ->  props.aiMessages.get(rowIndex).messageName = (String) aValue;
                    case 1 ->  props.aiMessages.get(rowIndex).output0 = (String) aValue;
                    case 2 ->  props.aiMessages.get(rowIndex).output1 = (String) aValue;
                    case 3 ->  props.aiMessages.get(rowIndex).output2 = (String) aValue;
                    case 4 ->  props.aiMessages.get(rowIndex).output3 = (String) aValue;
                }
            }
        };
        JTable table = new JTable(model);
        table.setShowVerticalLines(true);
        panel.add(new JScrollPane(table),BorderLayout.CENTER);
        JButton addButton = SwingUtil.createIconButton(EditorIcons.add,genericTableAdd(Area.AreaAIMessage::new,props.aiMessages,model));
        JButton removeButton = SwingUtil.createIconButton(EditorIcons.minus,genericTableRemove(props.aiMessages,model,table));
        topRow.add(addButton);
        topRow.add(removeButton);
        panel.add(topRow,BorderLayout.NORTH);
        return panel;
    }

    private JPanel createGeneralLevelSettings(){
        JPanel panel = new JPanel(new MigLayout("wrap 4"));
        var storyCoins = new JTextField(String.valueOf(area.areaProperties().storyCoins));
        ((AbstractDocument)storyCoins.getDocument()).setDocumentFilter(EditorPane.intFilter);
        storyCoins.addActionListener(a -> area.areaProperties().storyCoins = Integer.parseInt(storyCoins.getText()));

        var freeplayCoins = new JTextField(String.valueOf(area.areaProperties().freeplayCoins));
        ((AbstractDocument)freeplayCoins.getDocument()).setDocumentFilter(EditorPane.intFilter);
        freeplayCoins.addActionListener(a -> area.areaProperties().freeplayCoins = Integer.parseInt(freeplayCoins.getText()));

        var nameIdx = new JTextField(String.valueOf(area.areaProperties().globalProperties.nameId));
        ((AbstractDocument)nameIdx.getDocument()).setDocumentFilter(EditorPane.intFilter);
        nameIdx.addActionListener(a -> area.areaProperties().globalProperties.nameId = Integer.parseInt(nameIdx.getText()));

        var textIdx1 = new JTextField(String.valueOf(area.areaProperties().globalProperties.textId));
        ((AbstractDocument)textIdx1.getDocument()).setDocumentFilter(EditorPane.intFilter);
        textIdx1.addActionListener(a -> area.areaProperties().globalProperties.textId = Integer.parseInt(textIdx1.getText()));

        var textIdx2 = new JTextField(String.valueOf(area.areaProperties().globalProperties.textId2));
        ((AbstractDocument)textIdx2.getDocument()).setDocumentFilter(EditorPane.intFilter);
        textIdx2.addActionListener(a -> area.areaProperties().globalProperties.textId2 = Integer.parseInt(textIdx2.getText()));

        var musicName = new JTextField(area.areaProperties().music);
        musicName.addActionListener(a -> area.areaProperties().music = musicName.getText());

        var minikitName = new JTextField(area.areaProperties().globalProperties.minikit);
        minikitName.addActionListener(a -> area.areaProperties().globalProperties.minikit = minikitName.getText());

        var redBrickCheat = new JTextField(area.areaProperties().globalProperties.redbrickCheat);
        redBrickCheat.addActionListener(a -> area.areaProperties().globalProperties.redbrickCheat = redBrickCheat.getText());

        var createStatus = new JCheckBox("Create status screen", area.areaProperties().globalProperties.generateStatusScreen);
        createStatus.addActionListener(a -> area.areaProperties().globalProperties.generateStatusScreen = createStatus.isSelected());

        var freeplay = new JCheckBox("Allow freeplay", area.areaProperties().globalProperties.hasFreeplay);
        freeplay.addActionListener(a -> area.areaProperties().globalProperties.hasFreeplay = freeplay.isSelected());

        var vehicleArea = new JCheckBox("Vehicle area", area.areaProperties().globalProperties.isVehicleArea);
        vehicleArea.addActionListener(a -> area.areaProperties().globalProperties.isVehicleArea = vehicleArea.isSelected());

        var endingArea = new JCheckBox("Ending area", area.areaProperties().globalProperties.isEndingArea);
        endingArea.addActionListener(a -> area.areaProperties().globalProperties.isEndingArea = endingArea.isSelected());

        var hubArea = new JCheckBox("Hub area", area.areaProperties().globalProperties.isHubArea);
        hubArea.addActionListener(a -> area.areaProperties().globalProperties.isHubArea = hubArea.isSelected());

        var bonusArea = new JCheckBox("Bonus area", area.areaProperties().globalProperties.isBonusArea);
        bonusArea.addActionListener(a -> area.areaProperties().globalProperties.isBonusArea = bonusArea.isSelected());

        var superBonusArea = new JCheckBox("Super bonus area", area.areaProperties().globalProperties.isSuperBonusArea);
        superBonusArea.addActionListener(a -> area.areaProperties().globalProperties.isSuperBonusArea = superBonusArea.isSelected());

        var bonusTime = new JTextField(String.valueOf(area.areaProperties().globalProperties.bonusTimeTrialTime));
        ((AbstractDocument)bonusTime.getDocument()).setDocumentFilter(EditorPane.intFilter);
        bonusTime.addActionListener(a -> area.areaProperties().globalProperties.bonusTimeTrialTime = Integer.parseInt(bonusTime.getText()));

        var pickupGravity = new JCheckBox("Pickup gravity", area.areaProperties().globalProperties.hasPickupGravity);
        pickupGravity.addActionListener(a -> area.areaProperties().globalProperties.hasPickupGravity = pickupGravity.isSelected());

        var characterCollision = new JCheckBox("Character collision", area.areaProperties().globalProperties.hasCharacterCollision);
        characterCollision.addActionListener(a -> area.areaProperties().globalProperties.hasCharacterCollision = characterCollision.isSelected());

        var goldBrick = new JCheckBox("Gives gold bricks", area.areaProperties().globalProperties.givesGoldBrick);
        goldBrick.addActionListener(a -> area.areaProperties().globalProperties.givesGoldBrick = goldBrick.isSelected());

        var completion = new JCheckBox("Count for completion", area.areaProperties().globalProperties.givesCompletionPoints);
        completion.addActionListener(a -> area.areaProperties().globalProperties.givesCompletionPoints = completion.isSelected());

        panel.add(new JLabel("Area properties"));
        panel.add(new JSeparator(), "span, growx");

        panel.add(new JLabel("Name ID:") );
        panel.add(nameIdx);
        panel.add(new JLabel("Scroll text ID:"),"gap unrelated,");
        panel.add(textIdx1,"split 2");
        panel.add(textIdx2,"wrap");

        panel.add(new JLabel("Story studs:"));
        panel.add(storyCoins);
        panel.add(new JLabel("Freeplay studs:"),"gap unrelated");
        panel.add(freeplayCoins,"wrap");

        panel.add(new JLabel("Music name:"));
        panel.add(musicName,"span 2, growx, wrap");

        panel.add(new JLabel("Minikit name:"));
        panel.add(minikitName);

        panel.add(new JLabel("Red brick cheat:"), "gap unrelated");
        panel.add(redBrickCheat,"wrap");

        panel.add(freeplay, "wrap");
        panel.add(goldBrick, "wrap");
        panel.add(completion, "wrap");

        panel.add(new JLabel("Area types"));
        panel.add(new JSeparator(), "span, growx");

        panel.add(vehicleArea, "wrap");
        panel.add(endingArea, "wrap");
        panel.add(hubArea, "wrap");
        panel.add(superBonusArea, "wrap");
        panel.add(bonusArea, "");
        panel.add(new JLabel("Bonus time: "));
        panel.add(bonusTime, "wrap");

        panel.add(new JLabel("Advanced"));
        panel.add(new JSeparator(), "span, growx");

        panel.add(pickupGravity, "wrap");
        panel.add(characterCollision, "wrap");
        panel.add(createStatus, "wrap");

        return panel;
    }

    private static <T> ActionListener genericTableAdd(Supplier<T> constructor, java.util.List<T> objects, TableModel model){
        return (e)->{
            objects.add(constructor.get());
            ((AbstractTableModel)model).fireTableRowsInserted(objects.size()-1,objects.size()-1);};
    }

    private static <T> ActionListener genericTableRemove(java.util.List<T> objects, TableModel model,JTable table){
        return (e)->{
            int selectRow = table.getSelectedRow();
            if(selectRow < 0) return;
            objects.remove(selectRow);
            ((AbstractTableModel)model).fireTableRowsDeleted(selectRow,selectRow);};
    }
}
