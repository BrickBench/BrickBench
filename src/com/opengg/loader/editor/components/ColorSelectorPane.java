package com.opengg.loader.editor.components;

import com.opengg.loader.BrickBench;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

public class ColorSelectorPane extends JDialog {
    public ColorSelectorPane(Color initialColor, Consumer<Color> onSelect){
        super(BrickBench.CURRENT.window);
        this.setLayout(new BorderLayout());
        var colorSelector = new JColorChooser(initialColor);

        this.add(colorSelector, BorderLayout.CENTER);

        this.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e) {
                if(initialColor != colorSelector.getColor())
                    onSelect.accept(colorSelector.getColor());
            }
        });
        this.setTitle("Select color");
        this.setModal(true);
        this.pack();
        this.setVisible(true);
    }
}
