package com.opengg.loader;

import com.opengg.core.console.GGConsole;
import com.opengg.loader.editor.LoadingAlert;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Utilities for Swing.
 */
public class SwingUtil {
    /**
     * Open a loading alert with the given name and description.
     *
     * @return A {@link com.opengg.loader.editor.LoadingAlert} to manage the opened alert.
     */
    public static LoadingAlert showLoadingAlert(String name, String description, boolean showExit){
        var jop = new JOptionPane(description, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
        var dialog = jop.createDialog(BrickBench.CURRENT.window, name);
        dialog.setModal(false);
        dialog.setVisible(true);

        if(!showExit){
            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

            for (var wl : dialog.getWindowListeners()) {
                dialog.removeWindowListener(wl);
            }
        }

        return new LoadingAlert(description, "", jop, dialog);
    }

    /**
     * Open a blocking error alert that displays the given exception and message.
     *
     * This also logs the exception.
     */
    public static void showErrorAlert(String message, Exception exception) {
        showErrorAlert(message + ": " + exception.getMessage());
        GGConsole.exception(exception);
    }

    /**
     * Open a blocking error alert that displays the given message.
     *
     * This also logs the message.
     */
    public static void showErrorAlert(String message) {
        JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "<html>" + message + "</html>", "Error", JOptionPane.ERROR_MESSAGE);
        GGConsole.error(message);
    }

    /**
     * Add a keybind to trigger the given button with the given keystroke.
     *
     * This keystroke applies whenever the window is focused.
     */
    public static void addHotkey(JButton button, KeyStroke keyStroke) {
        int condition = JComponent.WHEN_IN_FOCUSED_WINDOW;
        InputMap inputMap = button.getInputMap(condition);
        ActionMap actionMap = button.getActionMap();
        inputMap.put(keyStroke, keyStroke.toString());
        actionMap.put(keyStroke.toString(), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                button.doClick();
            }
        });
    }

    /**
     * Create a button with the given icon.
     */
    public static JButton createIconButton(Icon icon, ActionListener actionListener){
        JButton button = new JButton(icon);
        button.setBorder(null);
        button.setBackground(null);
        button.addActionListener(actionListener);
        return button;
    }
}
