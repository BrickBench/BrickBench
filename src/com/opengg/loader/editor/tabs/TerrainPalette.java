package com.opengg.loader.editor.tabs;

import com.opengg.core.math.Vector3f;
import com.opengg.core.math.util.Tuple;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.editor.MapInterface;
import com.opengg.loader.game.nu2.terrain.TerrainGroup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

@EditorTabAutoRegister
public class TerrainPalette extends JPanel implements EditorTab {
    public TerrainPalette(){
        this.setBounds(0,0,100,500);
        BorderLayout borderLayout = new BorderLayout();
        this.setLayout(borderLayout);
        DefaultListModel<Tuple<String,Vector3f>> model = new DefaultListModel<>();
        JList<Tuple<String,Vector3f>> list = new JList<>();

        list.setCellRenderer(new DefaultListCellRenderer(){
            public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus ) {
                JLabel label = (JLabel) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
                Vector3f col = ((Tuple<String,Vector3f>)value).y();
                label.setText(((Tuple<String,Vector3f>)value).x());
                label.setBackground(new Color(col.x,col.y,col.z));
                label.setForeground((col.x*0.299 + col.y*0.587 +col.z*0.114) > 0.4 ? Color.BLACK : Color.WHITE);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                if(!isSelected) {
                    label.setBorder(new EmptyBorder(1,1, 1, 1));
                }else{
                    label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                }
                return label;
            }
        });

        for(var property : TerrainGroup.TerrainProperty.values()){
            var color = property.color;
            model.addElement(Tuple.of(property.name().replace("_", " "),color));
        }

        list.setModel(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(l -> {
            var color = TerrainGroup.TerrainProperty.valueOf(list.getSelectedValue().x().replace(" ", "_"));
            EditorState.setSelectedProperty(color);
        });

        var scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        this.add(scroll, BorderLayout.CENTER);
    }

    @Override
    public String getTabName() {
        return "Paint terrain";
    }

    @Override
    public String getTabID() {
        return "terrain-pane";
    }

    @Override
    public MapInterface.InterfaceArea getPreferredArea() {
        return MapInterface.InterfaceArea.BOTTOM_LEFT;
    }

    @Override
    public boolean getDefaultActive() {
        return true;
    }
}
