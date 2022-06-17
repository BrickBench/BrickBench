package com.opengg.loader.editor.windows;

import com.opengg.core.engine.Resource;
import com.opengg.loader.BrickBench;

import javax.swing.*;
import java.awt.*;

public class SplashScreen extends JDialog {
    public SplashScreen(){
        getContentPane().setLayout(new BoxLayout( getContentPane(), BoxLayout.PAGE_AXIS));
        ImageIcon icon = new ImageIcon(Resource.getTexturePath("icon.png"));
        JLabel label = new JLabel(icon);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(Box.createVerticalGlue() );
        this.add(label);
        JLabel version = new JLabel("BrickBench - " + BrickBench.VERSION.version() + " Alpha");
        version.setFont(new Font(version.getName(), Font.PLAIN, 20));
        version.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(Box.createVerticalStrut(20));
        this.add(version);
        this.add(Box.createVerticalGlue());
        this.setUndecorated(true);

        var size = Toolkit.getDefaultToolkit().getScreenSize();
        this.setBounds(size.width/3, size.height/3, size.width/3, size.height/3);
        this.setVisible(true);
        this.setTitle("BrickBench");
        this.setIconImage(icon.getImage());
    }
}
