package com.opengg.loader.editor;

import com.opengg.core.engine.Executor;
import com.opengg.core.engine.PerformanceManager;
import com.opengg.loader.BrickBench;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class BottomRow extends JPanel {
    private JLabel loadStateLabel;
    private JProgressBar loadProgressBar;

    private static List<BottomRow> rows = new ArrayList<>();

    public BottomRow(){
        this.setLayout(new GridLayout(1, 0));
        var loaderDetailsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 3));
        var engineDetailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 3));

        this.setBorder(BorderFactory.createEtchedBorder());

        loadProgressBar = new JProgressBar();
        loadProgressBar.setStringPainted(true);

        loadStateLabel = new JLabel("No map loaded");
        loaderDetailsPanel.add(loadStateLabel);
        loaderDetailsPanel.add(loadProgressBar);

        var cameraPosition = new JLabel();
        var speed = new JLabel();
        var fps = new JLabel();

        engineDetailsPanel.add(fps);
        engineDetailsPanel.add(cameraPosition);
        engineDetailsPanel.add(speed);

        Executor.every(Duration.ofMillis(250), () -> {
            fps.setText(String.format("Framerate: %.2f", 1/ PerformanceManager.getComputedFrameTime()));
            cameraPosition.setText("Position: " + BrickBench.CURRENT.ingamePosition.toFormattedString(2));
            speed.setText("Speed (u/s): " + (BrickBench.CURRENT.player != null ? BrickBench.CURRENT.player.getSpeed() : 0));
        });

        this.add(engineDetailsPanel);
        this.add(loaderDetailsPanel);

        rows.add(this);
    }

    public static void setLoadState(String state){
        for(var br : rows){
            br.loadStateLabel.setText(state);
        }
    }

    public static void setLoadProgress(int progress){
        for(var br : rows){
            br.loadProgressBar.setValue(progress);
        }
    }

    public static void setLoadProgressMax(int progress){
        for(var br : rows){
            br.loadProgressBar.setMaximum(progress);
        }
    }
}
