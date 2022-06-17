package com.opengg.loader.editor.components;

import com.formdev.flatlaf.icons.FlatAbstractIcon;
import com.formdev.flatlaf.icons.FlatFileViewDirectoryIcon;
import com.formdev.flatlaf.icons.FlatFileViewFileIcon;
import com.opengg.loader.FileUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.nio.file.Path;
import java.util.function.Consumer;

public class FileSelectField extends JPanel {
    JTextField field;
    Consumer<Path> onSelect = f -> {};
    public FileSelectField(Path defaultPath, FileUtil.LoadType type, String description, String... filter){
        field = new JTextField(defaultPath.toString());
        FlatAbstractIcon icon;

        this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        if(type == FileUtil.LoadType.DIRECTORY){
            icon = new FlatFileViewDirectoryIcon();
        }else{
            icon = new FlatFileViewFileIcon();
        }

        var button = new JButton(icon);

        field.setColumns(12);

        button.addActionListener(b -> FileUtil.openFileDialog(field.getText(), type, description, filter)
                .ifPresent(f -> {
                    field.setText(f.toAbsolutePath().toString());
                    onSelect.accept(getFile());
                }));
        button.setBorder(new EmptyBorder(0, 0, 0, 0));
        button.setBorder(null);

        field.addActionListener(a -> onSelect.accept(getFile()));

        this.setBorder(field.getBorder());

        //this.setBackground(field.getBackground());
        button.setBackground(field.getBackground());

        //field.setBackground(null);
        field.setBorder(new EmptyBorder(0, 0, 0, 0));
        field.setBorder(null);
        this.add(field);
        this.add(button);
    }

    public void onSelect(Consumer<Path> onSelect){
        this.onSelect = onSelect;
    }

    public Path getFile(){
        return Path.of(field.getText());
    }


}
