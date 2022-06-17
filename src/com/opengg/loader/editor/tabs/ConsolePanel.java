package com.opengg.loader.editor.tabs;

import com.opengg.loader.editor.MapInterface;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;

@EditorTabAutoRegister
public class ConsolePanel extends JPanel implements EditorTab {
    public static JTextArea consoleText = new JTextArea();

    public ConsolePanel(){
        this.setLocation(100,100);
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(400, 300));

        JScrollPane console = new JScrollPane();
        console.setLayout(new ScrollPaneLayout());
        console.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        console.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        console.setWheelScrollingEnabled(true);
        console.setViewportView(consoleText);
        consoleText.setEditable(false);
        consoleText.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 11));
        console.setBorder(BorderFactory.createEmptyBorder());
        DefaultCaret caret = (DefaultCaret) ConsolePanel.consoleText.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        this.add(console);
    }

    @Override
    public String getTabName() {
        return "OpenGG Console";
    }

    @Override
    public String getTabID() {
        return "console-pane";
    }

    @Override
    public MapInterface.InterfaceArea getPreferredArea() {
        return MapInterface.InterfaceArea.BOTTOM_RIGHT;
    }

    @Override
    public boolean getDefaultActive() {
        return true;
    }
}
