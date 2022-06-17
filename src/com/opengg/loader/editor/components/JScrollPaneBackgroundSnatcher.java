package com.opengg.loader.editor.components;

import javax.swing.*;
import java.awt.*;

public class JScrollPaneBackgroundSnatcher extends JScrollPane {
    public JScrollPaneBackgroundSnatcher(JComponent comp) {
        super(comp);
    }

    @Override
    protected JViewport createViewport() {
        return new JViewportBackgroundSnatcher();
    }

    private static class JViewportBackgroundSnatcher extends JViewport{
        @Override
        public Color getBackground() {
            if(this.getView() != null) {
                return this.getView().getBackground();
            }else{
                return super.getBackground();
            }
        }
    }
}
