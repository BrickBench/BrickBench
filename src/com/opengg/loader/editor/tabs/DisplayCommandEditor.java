package com.opengg.loader.editor.tabs;

import com.opengg.core.engine.OpenGG;
import com.opengg.loader.BrickBench;
import com.opengg.loader.EditorEntity;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.editor.MapInterface;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.commands.DisplayCommand;
import com.opengg.loader.game.nu2.scene.commands.DisplayCommandResource;
import com.opengg.loader.game.nu2.scene.commands.UntypedCommandResource;
import com.opengg.loader.game.nu2.scene.DisplayList;
import com.opengg.loader.game.nu2.scene.DisplayWriter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@EditorTabAutoRegister
public class DisplayCommandEditor extends JPanel implements EditorTab {
    private JTable table;
    private DisplayCommandTableModel model;
    private JLabel label;

    private EditorEntity.Ref<DisplayList> currentObj = EditorEntity.Ref.NULL;

    public DisplayCommandEditor(){
        this.setLayout(new BorderLayout());
        model = new DisplayCommandTableModel();
        table = new JTable();
        table.setShowVerticalLines(true);
        table.setModel(model);
        table.getColumnModel().getColumn(0).setCellEditor(new CommandTypeCellEditor());
        table.getColumnModel().getColumn(1).setCellEditor(new DisplayCommandResourceCellEditor());

        var scroll = new JScrollPane(table);
        scroll.setBorder(null);
        this.add(scroll, BorderLayout.CENTER);

        label = new JLabel("");
        label.setBorder(new EmptyBorder(5,0,5,0));

        JPanel bottom = new JPanel();

        JButton add = new JButton("Add");
        JButton delete = new JButton("Delete");

        add.addActionListener(a -> {
            if(table.getSelectedRow() < 0) return;
            var command = model.commandList.get(table.getSelectedRow());
            OpenGG.asyncExec(() -> DisplayWriter.addDisplayCommands(
                    List.of(new UntypedCommandResource(1, DisplayCommand.CommandType.DUMMY)),
                            command.index() + 1));
        });

        delete.addActionListener(a -> {
            if(table.getSelectedRow() < 0) return;
            var command = model.commandList.get(table.getSelectedRow());
            OpenGG.asyncExec(() -> {
                try{
                    DisplayWriter.removeDisplayCommands(command, 1);
                }catch(IllegalArgumentException e){
                    JOptionPane.showMessageDialog(BrickBench.CURRENT.window,
                            "Cannot remove mesh as it is used elsewhere");
                }
            });
        });

        bottom.add(add);
        bottom.add(delete);

        this.add(label, BorderLayout.NORTH);
        this.add(bottom, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(event -> {
            if(table.getSelectedRow() < 0) {
                EditorState.selectObject(null);
                return;
            }

            var command = model.commandList.get(table.getSelectedRow());
            EditorState.selectObject(command.command());
        });

        EditorState.addMapReloadListener(m -> refreshCurrentObject());
        EditorState.addSelectionChangeListener(m -> {
            if (m.y().get() instanceof DisplayList) {
                setObject((EditorEntity.Ref<DisplayList>) m.y());
            }
        });
    }

    public void setObject(EditorEntity.Ref<DisplayList> staticObject){
        if(!staticObject.exists()) {
            var currentMap = EditorState.getActiveMap();
            if (currentMap != null && currentMap.levelData() instanceof NU2MapData nu2) {
                label.setText("All display commands");
                model.loadCommands(nu2.scene().renderCommandList());
            } else {
                label.setText("No static mesh selected");
            }
        }else{
            label.setText("Display commands for " + staticObject.get().name());
            model.loadCommands(staticObject.get().commands());
        }
        model.fireTableDataChanged();
        table.repaint();

        currentObj = staticObject;
    }

    public void refreshCurrentObject(){
        setObject(currentObj);
    }

    @Override
    public String getTabName() {
        return "Edit display list";
    }

    @Override
    public String getTabID() {
        return "display-list-pane";
    }

    @Override
    public MapInterface.InterfaceArea getPreferredArea() {
        return MapInterface.InterfaceArea.BOTTOM_RIGHT;
    }

    @Override
    public boolean getDefaultActive() {
        return false;
    }

    static class DisplayCommandTableModel extends AbstractTableModel {
        public List<DisplayCommand> commandList = new ArrayList<>();
        String[] columns = {"Type", "Resource","Flag"};

        public void loadCommands(List<DisplayCommand> commands){
           commandList = commands;
        }

        @Override
        public int getRowCount() {
            return commandList.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            var command = commandList.get(rowIndex);
            return switch (columnIndex){
                case 0 -> command.type().toString();
                case 1 -> command.command();
                case 2 -> commandList.get(rowIndex).flags();
                default -> null;
            };
        }

        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case 0 -> DisplayCommand.CommandType.class;
                case 1 -> DisplayCommandResource.class;
                case 2 -> Integer.class;
                default -> Boolean.class;
            };
        }

        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public boolean isCellEditable(int row, int cols) {
            return cols != 1;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex)
        {
            var command = commandList.get(rowIndex);
            if(columnIndex == 0) {
                var realValue = (DisplayCommand.CommandType) aValue;
                if(command.type() == realValue) return;
                command.applyPropertyEdit("Command type",
                        new EditorEntity.EnumProperty("Command type", realValue, false));
            }else if(columnIndex == 1){

            }else{
                command.applyPropertyEdit("Flags",
                        new EditorEntity.IntegerProperty("Flags", (Integer)aValue, false));
            }

        }
    }

     static class CommandTypeCellEditor extends AbstractCellEditor implements
            TableCellEditor {

        JComponent component = new JComboBox<>(DisplayCommand.CommandType.values());
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int rowIndex, int vColIndex) {
            ((JComboBox<DisplayCommand.CommandType>) component).setSelectedItem(value);
            return component;
        }

        public Object getCellEditorValue() {
            return ((JComboBox<DisplayCommand.CommandType>)component).getSelectedItem();
        }
    }

    static class DisplayCommandResourceCellEditor extends AbstractCellEditor implements
            TableCellEditor {

        DisplayCommandResource<?> entity;
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int rowIndex, int vColIndex) {
            entity = (DisplayCommandResource<?>) value;
            var panel = new JPanel();
            panel.add(new JLabel(entity.name()));

            var edit = new JButton("E");
            edit.addActionListener(e -> {

            });

            panel.add(edit);
            return panel;
        }

        public Object getCellEditorValue() {
            return entity;
        }
    }
}
