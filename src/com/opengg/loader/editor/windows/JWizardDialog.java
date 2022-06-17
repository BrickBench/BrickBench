package com.opengg.loader.editor.windows;

import com.opengg.core.math.util.Tuple;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.util.*;
import java.awt.*;
import java.util.function.Function;

public class JWizardDialog extends JDialog {
    private final Deque<Tuple<String, JWizardPanel>> returnCache = new ArrayDeque<>();
    private final Map<String, Function<Object, JWizardPanel>> panels = new HashMap<>();

    public JWizardDialog(){
        this.setLayout(new MigLayout("wrap 1, fill"));
    }

    public JWizardDialog(Frame owner){
        super(owner);
        this.setLayout(new MigLayout());
    }

    public void register(String panelName, Function<Object, JWizardPanel> panelCreator){
        panels.put(panelName, panelCreator);
    }

    public void swapTo(String panelName, Object args){
        var panelFunc = panels.get(panelName);
        var panel = panelFunc.apply(args);

        swapTo(panelName, panel);
    }


    private void swapTo(String panelName, JWizardPanel panel){
        var buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        var nextButton = new JButton(panel.nextButtonLabel());
        var previousButton = new JButton("Previous");

        nextButton.addActionListener(n -> panel.onNextPressed.run());
        previousButton.addActionListener(p -> {
            returnCache.pollLast();
            var previous = returnCache.pollLast();
            swapTo(previous.x(), previous.y());
        });

        if(returnCache.isEmpty()){
            previousButton.setEnabled(false);
        }

        buttonRow.add(previousButton);
        buttonRow.add(nextButton);

        this.getContentPane().removeAll();

        this.getContentPane().add(panel, "dock center, grow");
        this.getContentPane().add(buttonRow, "dock south");

        this.validate();

        returnCache.add(Tuple.of(panelName, panel));
    }

    public static class JWizardPanel extends JPanel{
        private Runnable onNextPressed = () -> {};
        private String nextButtonLabel = "Next";

        public JWizardPanel(){}
        public JWizardPanel(LayoutManager layoutManager){
            super(layoutManager);
        }

        public void nextButtonLabel(String nextName) {
            this.nextButtonLabel = nextName;
        }

        public String nextButtonLabel() {
            return nextButtonLabel;
        }

        public void setOnNextPressed(Runnable onNextPressed) {
            this.onNextPressed = onNextPressed;
        }

        public Runnable getOnNextPressed() {
            return onNextPressed;
        }
    }
}
