package com.opengg.loader.editor;

import com.opengg.loader.EditorEntity;
import com.opengg.loader.editor.components.TextPrompt;
import com.opengg.loader.game.nu2.scene.FileMaterial;
import com.opengg.loader.game.nu2.scene.FileTexture;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SearchableListPanel extends JPanel {
    public List<? extends EditorEntity<?>>  objects;
    private final DefaultListModel<EditorEntity<?>> listModel;
    private final JList<EditorEntity<?>> list;
    private final JTextField searchBar = new JTextField();

    public SearchableListPanel(String originPath, Consumer<EditorEntity<?>> onSelect, boolean allowMultiple) {
        this(EditorState.getActiveNamespace(), originPath, onSelect, allowMultiple);
    }

    public SearchableListPanel(String namespace, String originPath, Consumer<EditorEntity<?>> onSelect, boolean allowMultiple) {
        var allObjects = EditorState.getNamespace(namespace);
        this.objects = allObjects.entrySet().stream()
                            .filter(e -> e.getKey().contains(originPath))
                            .map(Map.Entry::getValue)
                            .map(o -> (EditorEntity<?>) o)
                            .collect(Collectors.toList());

        setLayout(new BorderLayout());

        var searchPrompt = new TextPrompt("Search...", searchBar);
        searchPrompt.setShow(TextPrompt.Show.FOCUS_LOST);
        searchPrompt.setIcon(EditorIcons.search);

        list = new JList<>();
        listModel = new DefaultListModel<>();
        list.setModel(listModel);
        list.setCellRenderer(new LegoObjectCellRenderer());
        list.setSelectionMode(allowMultiple ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
        listModel.clear();
        filterModel("");

        searchBar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterModel(searchBar.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterModel(searchBar.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterModel(searchBar.getText());
            }
        });


        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (list.getSelectedValue() != null) {
                    if (evt.getClickCount() == 2) {
                        if(onSelect != null)
                            onSelect.accept(list.getSelectedValue());
                    }
                }
            }
        });

        var enterButton = new JButton("Select");
        enterButton.addActionListener(a -> {
                    if (list.getSelectedValue() != null) {
                        onSelect.accept(list.getSelectedValue());
                    }});

        add(searchBar, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);

        if(onSelect != null)
            add(enterButton, BorderLayout.SOUTH);
    }

    public void filterModel(String filter) {
        listModel.clear();
        for (var obj : objects) {
            if (filter.trim().isEmpty() ||
                    obj.path().toLowerCase().contains(filter.toLowerCase()) ||
                    obj.properties().stream().anyMatch(p -> p.stringValue().toLowerCase().contains(filter.toLowerCase()))) {
                listModel.addElement(obj);
            }
        }
    }

    public EditorEntity<?> getSelected(){
        return list.getSelectedValue();
    }

    public List<EditorEntity<?>> getMultipleSelected(){
        return list.getSelectedValuesList();
    }

    public void setEnabled(boolean enabled){
        list.setEnabled(enabled);
    }

    static class LegoObjectCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list,
                                                      Object item,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {

            super.getListCellRendererComponent(list, item,
                    index, isSelected, cellHasFocus);
            if (item instanceof EditorEntity<?> obj) {
                setText(!obj.name().isBlank() ? obj.name() : ("UNNAMED " + obj.getClass().getSimpleName()));

                try {
                    if (obj instanceof FileTexture texture && texture.icon().isDone() && texture.icon().get() != null) {
                        setIcon(texture.icon().get());
                        setHorizontalTextPosition(JLabel.CENTER);
                        setVerticalTextPosition(JLabel.BOTTOM);
                    }

                    if (obj instanceof FileMaterial material && material.getIcon() != null) {
                        setIcon(material.getIcon());
                        setHorizontalTextPosition(JLabel.CENTER);
                        setVerticalTextPosition(JLabel.BOTTOM);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            return this;
        }
    }
}

