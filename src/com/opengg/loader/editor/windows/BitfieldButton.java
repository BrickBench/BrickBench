package com.opengg.loader.editor.windows;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class BitfieldButton extends JButton {
    long value;
    public BitfieldButton(Map<Integer,String> props, String buttonValue,long value){
        this.value = value;
        this.setText(buttonValue);
        addActionListener(e->{
            JDialog editDialog = new JDialog();
            editDialog.setSize(new Dimension(500,500));
            editDialog.getContentPane().setLayout(new BoxLayout(editDialog.getContentPane(),BoxLayout.Y_AXIS));
            for (var entry : props.entrySet()) {
                JCheckBox box = new JCheckBox(entry.getValue(), (value & entry.getKey()) != 0);
                editDialog.add(box);
            }
            editDialog.setLocation(MouseInfo.getPointerInfo().getLocation().x -100,MouseInfo.getPointerInfo().getLocation().y);
            editDialog.show();
        });
    }
    public long getValue(){
        return value;
    }
}
