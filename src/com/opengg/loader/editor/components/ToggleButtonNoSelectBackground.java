package com.opengg.loader.editor.components;

import com.formdev.flatlaf.extras.components.FlatToggleButton;
import com.formdev.flatlaf.ui.FlatToggleButtonUI;

import javax.swing.*;
import java.awt.*;

public class ToggleButtonNoSelectBackground extends FlatToggleButton {
    @Override
    public void updateUI() {
        setUI(new FlatToggleButtonUI(){
            protected void paintBackground(Graphics g, JComponent c ) {}
        });
    }
}
