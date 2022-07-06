package com.opengg.loader.editor.components;

import com.formdev.flatlaf.extras.components.FlatToggleButton;
import com.formdev.flatlaf.ui.FlatToggleButtonUI;

public class ToggleButtonNoSelectBackground extends FlatToggleButton {
    @Override
    public void updateUI() {
        setUI(FlatToggleButtonUI.createUI(this));
    }
}
