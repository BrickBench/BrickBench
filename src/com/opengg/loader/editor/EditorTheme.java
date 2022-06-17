package com.opengg.loader.editor;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.intellijthemes.*;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.*;
import com.opengg.core.Configuration;
import com.opengg.loader.BrickBench;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Global manager for editor theming.
 */
public class EditorTheme {
    /**
     * Height of editor tabs.
     */
    public static final int tabHeight = 26;

    /**
     * Filter to apply to icons.
     */
    public static FlatSVGIcon.ColorFilter iconFilter = new FlatSVGIcon.ColorFilter();

    /**
     * Color for tree backgrounds.
     */
    public static Color treeBackground;

    /**
     * Color for panel backgrounds.
     */
    public static Color panelBackground;

    private EditorTheme(){}

    /**
     * Apply the currently defined theme to the editor.
     *
     * Note, this may fail to update components that are created but not in the Swing
     * hierarchy.
     *
     * @throws UnsupportedLookAndFeelException When theme isn't supported on the current platform.
     */
    public static void applyTheme() throws UnsupportedLookAndFeelException {
         var currentLaf = switch (Configuration.get("laf")) {
            case "Flat Dark" -> new FlatDarkFlatIJTheme();
            case "High Contrast" -> new FlatHighContrastIJTheme();
            case "Atom One Dark" -> new FlatAtomOneDarkContrastIJTheme();
            case "Material Darker" -> new FlatMaterialDarkerContrastIJTheme();
            case "Arc Dark"->new FlatArcDarkContrastIJTheme();
            case "Carbon"->new FlatCarbonIJTheme();
            case "Gradianto Deep Ocean"->new FlatGradiantoDeepOceanIJTheme();
            default -> new FlatDarkFlatIJTheme();
        };

        UIManager.setLookAndFeel(currentLaf);
        //Manual Adjustments

        switch(Configuration.get("laf")){
            case "Flat Dark" -> {
                UIManager.put("Separator.foreground", Color.decode("#6C6C6C"));
            }
        }

        UIManager.put( "ScrollBar.showButtons", true );
        UIManager.put( "flatlaf.useWindowDecorations", true );
        UIManager.put( "flatlaf.menuBarEmbedded", true );
        UIManager.put("Button.showMnemonics", Boolean.TRUE);
        iconFilter.remove(Color.BLACK);
        iconFilter.add(Color.BLACK, UIManager.getDefaults().getColor("Label.foreground"));

        if(BrickBench.CURRENT != null && BrickBench.CURRENT.window != null) {
            SwingUtilities.updateComponentTreeUI(BrickBench.CURRENT.window);
            BrickBench.CURRENT.window.pack();
        }
        panelBackground = new JPanel().getBackground();
        treeBackground = new JTree().getBackground();
    }


    public record RoundedBorder(int radius) implements Border {
        public Insets getBorderInsets(Component c) {
            return new Insets(this.radius + 1, this.radius + 1, this.radius + 2, this.radius);
        }

        public boolean isBorderOpaque() {
            return true;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }
    }
}
