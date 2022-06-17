package com.opengg.loader.editor;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

public class EnumCellEditor<T> extends AbstractCellEditor implements
        TableCellEditor {
    JComponent component;
    public EnumCellEditor(T[] types){
        this.component = new JComboBox<>(types);
    }
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int rowIndex, int vColIndex) {
        ((JComboBox<T>) component).setSelectedItem(value);
        return component;
    }

    public Object getCellEditorValue() {
        return ((JComboBox<T>)component).getSelectedItem();
    }
}
