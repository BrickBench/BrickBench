package com.opengg.loader.editor.components;

import com.opengg.loader.EditorEntity;
import com.opengg.loader.editor.EditorIcons;
import com.opengg.loader.editor.SearchableListPanel;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class ListPropertyComponent extends JPanel {
    Component selected;
    static int PADDING = 20;
    JPanel holder;

    public ListPropertyComponent(EditorEntity.ListProperty prop, EditorEntity.Ref<?> gameObject){
        this(prop,gameObject,false);
    }

    public ListPropertyComponent(EditorEntity.ListProperty prop, EditorEntity.Ref<?> gameObject,boolean sizeStop){
        holder = new ScrollableJPanel();
        BoxLayout layout = new BoxLayout(holder,BoxLayout.Y_AXIS);
        holder.setLayout(layout);
        JScrollPane scrollHolder = new JScrollPaneBackgroundSnatcher(holder);
        EditorCollapsable collapse;
        if(prop.editable()) {
            JPanel container = new JPanel(new WrapLayout());
            if(prop.editable()) {
                JButton addButton = new JButton("Add");

                addButton.addActionListener(e -> {
                    //Case where you want to generate a dummy
                    if(prop.newValueFunc() != null) {
                        var newItem = prop.newValueFunc().apply(prop.value());
                        prop.addValueFunc().accept(newItem);
                    }else{
                        JPopupMenu menu = new JPopupMenu();
                        var searchPanel = new SearchableListPanel(prop.editSource(), c -> {
                            prop.addValueFunc().accept(new EditorEntity.EditorEntityProperty(c.name(), c, true, true,prop.editSource()));
                            menu.setVisible(false);
                        }, false);

                        searchPanel.setMinimumSize(new Dimension(100, 200));
                        searchPanel.setPreferredSize(new Dimension(150, 250));

                        menu.setLayout(new BorderLayout());
                        menu.add(searchPanel, BorderLayout.CENTER);
                        menu.show(addButton, 0, ((JButton) e.getSource()).getHeight());
                    }
                });
                container.add(addButton);
            }

            if(prop.valueRemovedFunc() != null){
                JButton removeButton = new JButton("Remove");
                removeButton.addActionListener(e -> {
                    int index = ArrayUtils.indexOf(selected.getParent().getComponents(), selected);

                    prop.valueRemovedFunc().accept(prop.value().get(index), index);
                });
                container.add(removeButton);
            }

            collapse = new EditorCollapsable(prop.name(),scrollHolder,prop.autoExpand(),container,false);
        }else{
            collapse = new EditorCollapsable(prop.name(), scrollHolder, prop.autoExpand());
        }
        if(sizeStop) {
            scrollHolder.setPreferredSize(new Dimension(0, 200));
            scrollHolder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        }
        SwingUtilities.invokeLater(()->{
            generateSubProps(prop,gameObject);
        });
        setLayout(new BorderLayout());
        add(collapse,BorderLayout.CENTER);
        scrollHolder.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                MouseEvent ev = SwingUtilities.convertMouseEvent(e.getComponent(),e, scrollHolder);
                Component component = scrollHolder.findComponentAt(5,ev.getPoint().y);
                if(selected != null){
                    selected.setBackground(UIManager.getColor("Panel.background"));
                }
                if(component instanceof JPanel){
                    selected = component;
                }else if(component instanceof JLabel){
                    selected = component.getParent();
                }
                if(selected != null) {
                    selected.setBackground(UIManager.getColor("Table.selectionBackground"));
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
    }
    private void generateSubProps(EditorEntity.ListProperty prop,EditorEntity.Ref<?> gameObject){
        holder.removeAll();
        JPanel panel = new JPanel();
        int i = 0;
        for(var subProp:prop.value()){
            subProp.createNewInterface(panel, gameObject.get());
            var realLength = panel.getComponents().length;
            JPanel subHolder = new JPanel(new BorderLayout(PADDING,0));
            subHolder.add(new JLabel(""+i),BorderLayout.WEST);
            if(realLength == 1){
                subHolder.add(panel.getComponent(0),BorderLayout.CENTER);
            }else if(realLength >= 2){
                subHolder.add(panel.getComponent(1),BorderLayout.CENTER);
                if(realLength>2){
                    subHolder.add(panel.getComponent(1),BorderLayout.EAST);
                }
            }
            subHolder.setBorder(BorderFactory.createEmptyBorder(1,PADDING,1,PADDING));
            holder.add(subHolder);
            panel.removeAll();
            i++;
        }
    }
}
