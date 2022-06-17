package com.opengg.loader.editor.components;

import com.opengg.core.math.Vector3f;
import com.opengg.loader.editor.tabs.EditorPane;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;

public class JVectorField extends JComponent {
    private Vector3f value;
    private JTextField x, y, z;

    public JVectorField(){
        this(new Vector3f());
    }

    public JVectorField(Vector3f value) {
        this(value, null);
    }

    public JVectorField(Vector3f value, String name){
        this.value = value;

        var layout = new GridLayout(1,3);
        this.setLayout(layout);

        x = new JTextField(String.valueOf(value.x));
        y = new JTextField(String.valueOf(value.y));
        z = new JTextField(String.valueOf(value.z));

        x.setColumns(6);
        y.setColumns(6);
        z.setColumns(6);

        x.setMargin(new Insets(3,2,3,0));
        y.setMargin(new Insets(3,0,3,0));
        z.setMargin(new Insets(3,0,3,2));

        ((AbstractDocument)x.getDocument()).setDocumentFilter(EditorPane.floatFilter);
        ((AbstractDocument)y.getDocument()).setDocumentFilter(EditorPane.floatFilter);
        ((AbstractDocument)z.getDocument()).setDocumentFilter(EditorPane.floatFilter);

        ActionListener onEdit = a ->{
            this.value = new Vector3f(Float.parseFloat(x.getText()), Float.parseFloat(y.getText()), Float.parseFloat(z.getText()));
            this.fireActionPerformed();
        };

        x.addActionListener(onEdit);
        y.addActionListener(onEdit);
        z.addActionListener(onEdit);

        if(name != null){
            this.add(new JLabel(name));
        }

        this.add(x);
        this.add(y);
        this.add(z);
    }

    public void setValue(Vector3f value){
        this.value = value;
        x.setText(String.valueOf(value.x));
        y.setText(String.valueOf(value.y));
        z.setText(String.valueOf(value.z));

    }

    public Vector3f getValue(){
        this.value = new Vector3f(Float.parseFloat(x.getText()), Float.parseFloat(y.getText()), Float.parseFloat(z.getText()));
        return value;
    }

    public synchronized void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    @SuppressWarnings("deprecation")
    protected void fireActionPerformed() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        int modifiers = 0;
        AWTEvent currentEvent = EventQueue.getCurrentEvent();
        if (currentEvent instanceof InputEvent) {
            modifiers = ((InputEvent)currentEvent).getModifiers();
        } else if (currentEvent instanceof ActionEvent) {
            modifiers = ((ActionEvent)currentEvent).getModifiers();
        }
        ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, value.toString(),
                        EventQueue.getMostRecentEventTime(), modifiers);

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ActionListener.class) {
                ((ActionListener)listeners[i+1]).actionPerformed(e);
            }
        }
    }
}
