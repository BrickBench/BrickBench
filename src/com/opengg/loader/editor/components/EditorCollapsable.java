package com.opengg.loader.editor.components;

import com.formdev.flatlaf.extras.components.FlatToggleButton;
import com.formdev.flatlaf.icons.FlatTreeCollapsedIcon;
import com.formdev.flatlaf.icons.FlatTreeExpandedIcon;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class EditorCollapsable extends JPanel {
    JPanel top;
    public EditorCollapsable(String name,JComponent contents,boolean initialState){
        top = new JPanel(new FlowLayout(FlowLayout.LEFT,1,0));
        FlatToggleButton minimize = new ToggleButtonNoSelectBackground();
        minimize.setIcon(new FlatTreeCollapsedIcon());
        minimize.setSelectedIcon(new FlatTreeExpandedIcon());
        minimize.setBorder(BorderFactory.createEmptyBorder());
        minimize.setOpaque(false);
        this.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
        minimize.addActionListener((e)->{
            contents.setVisible(!contents.isVisible());
            this.doLayout();
            this.revalidate();
            this.repaint();
        });
        minimize.setSelected(initialState);
        top.add(minimize);
        JLabel containerName = new JLabel(name);
        containerName.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
        top.add(containerName);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
        contents.setVisible(initialState);
        this.setLayout(new BorderLayout());
        this.add(top,BorderLayout.NORTH);
        this.add(contents,BorderLayout.CENTER);
        top.setBorder(new EmptyBorder(10,5,10,5));
    }
    public EditorCollapsable(String name,JComponent contents,boolean initialState,JComponent topBar,boolean isTop){
        top = new JPanel(new FlowLayout(FlowLayout.LEFT,1,0));
        FlatToggleButton minimize = new ToggleButtonNoSelectBackground();
        minimize.setIcon(new FlatTreeCollapsedIcon());
        minimize.setSelectedIcon(new FlatTreeExpandedIcon());
        minimize.setBorder(BorderFactory.createEmptyBorder());
        minimize.setOpaque(false);
        this.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
        JPanel collapseHolder = new JPanel(new BorderLayout());
        minimize.addActionListener((e)->{
            collapseHolder.setVisible(!collapseHolder.isVisible());
            this.doLayout();
            this.revalidate();
            this.repaint();
        });
        minimize.setSelected(initialState);
        top.add(minimize);
        JLabel containerName = new JLabel(name);
        containerName.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
        top.add(containerName);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
        collapseHolder.setVisible(initialState);
        collapseHolder.add(contents,BorderLayout.CENTER);
        this.setLayout(new BorderLayout());

        JPanel topHolder = new JPanel(new BorderLayout());
        topHolder.add(top,BorderLayout.WEST);
        if(isTop) {
            topHolder.add(topBar, BorderLayout.EAST);
        }else {
            collapseHolder.add(topBar,BorderLayout.SOUTH);
        }
        this.add(topHolder,BorderLayout.NORTH);
        this.add(collapseHolder,BorderLayout.CENTER);
        top.setBorder(new EmptyBorder(10,5,10,5));
    }

}
