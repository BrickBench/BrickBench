package com.opengg.loader.editor;

import javax.swing.*;

/**
 * A modifiable and closeable loading alert.
 *
 * This class should be created through {@link com.opengg.loader.SwingUtil#showLoadingAlert}.
 * It is recommended to use this alert through a try-with-resources statement/
 */
public class LoadingAlert implements AutoCloseable{
    private final JOptionPane optionPane;
    private final JDialog dialog;

    private String description;
    private String state;

    public LoadingAlert(String description, String state, JOptionPane optionPane, JDialog dialog) {
        this.optionPane = optionPane;
        this.dialog = dialog;
        this.description = description;
        this.state = state;
    }

    /**
     * Set the current description of the loading alert.
     */
    void setDescription(String description) {
        this.description = description;
        updateUI();
    }

    /**
     * Set the current state of the loading alert.
     */
    public void setState(String state) {
        this.state = state;
        updateUI();
    }

    private void updateUI() {
        optionPane.setMessage("<html>" + description + "<br/><small>" + state + "</small></html>");
    }

    @Override
    public void close(){
        dialog.dispose();
    }
}
