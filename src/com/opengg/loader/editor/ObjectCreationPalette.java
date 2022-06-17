package com.opengg.loader.editor;

import com.formdev.flatlaf.icons.FlatFileViewFileIcon;
import com.opengg.core.engine.OpenGG;
import com.opengg.core.world.WorldEngine;
import com.opengg.loader.BrickBench;
import com.opengg.loader.Util;
import com.opengg.loader.components.TemporaryMeshComponent;
import com.opengg.loader.editor.components.IconNodeRenderer;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.loading.MapWriter;
import com.opengg.loader.game.nu2.ai.AIWriter;
import com.opengg.loader.game.nu2.gizmo.GizWriter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

public class ObjectCreationPalette extends JTree {
    static void select(String path, String name) {
        if (name == null) return;
        OpenGG.asyncExec(0.3f, () -> EditorState.selectObject(EditorState.getObject(path + name)));
    }

    public ObjectCreationPalette() {
        super(getNodes());
        Map<String, Map<String, Runnable>> objectCreationFuncs = Map.of(
            NU2MapData.class.getName(), Util.createOrderedMapFrom(
                    Map.entry("Pickup", () -> select("Gizmo/Pickups/", GizWriter.addNewGizmo("GizmoPickup"))),
                    Map.entry("ZipUp", () -> select("Gizmo/ZipUps/", GizWriter.addNewGizmo("ZipUp"))),
                    Map.entry("Lever", () -> select("Gizmo/Levers/", GizWriter.addNewGizmo("Lever"))),
                    Map.entry("HatMachine", () -> select("Gizmo/HatMachines/", GizWriter.addNewGizmo("HatMachine"))),
                    Map.entry("Panel", () -> select("Gizmo/Panels/", GizWriter.addNewGizmo("Panel"))),
                    Map.entry("Tube", () -> select("Gizmo/Tubes/", GizWriter.addNewGizmo("Tube"))),
                    Map.entry("Creature Spawn", () -> select("AI/Creatures/", AIWriter.addAICreature())),
                    Map.entry("Locator Set", () -> select("AI/LocatorSets/", AIWriter.addLocatorSet())),
                    Map.entry("Model", () -> {
                        var map = (NU2MapData) EditorState.getActiveMap().levelData();
                        var tempMesh = new TemporaryMeshComponent(map.scene().gameModels().get(0));
                        tempMesh.setPositionOffset(BrickBench.CURRENT.ingamePosition);

                        WorldEngine.getCurrent().attach(tempMesh);
                        EditorState.selectTemporaryObject(tempMesh);
                        EditorState.CURRENT.temporaryComponents.add(tempMesh);
                    })));

        var tree = this;
        this.setCellRenderer(new IconNodeRenderer());
        this.setLayout(new BorderLayout());
        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if(!MapWriter.isProjectEditable()){
                    return;
                }

                int selRow = tree.getRowForLocation(e.getX(), e.getY());
                if(selRow != -1) {
                    if(e.getClickCount() == 2) {
                        var selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                        if(EditorState.getActiveMap() != null && selectedNode != null
                                && objectCreationFuncs.containsKey(EditorState.getActiveMap().levelData().getClass().getName())
                                && objectCreationFuncs.get(EditorState.getActiveMap().levelData().getClass().getName()).containsKey(selectedNode.toString())){
                            EditorState.selectObject(null);
                            OpenGG.asyncExec(() -> objectCreationFuncs.get(EditorState.getActiveMap().levelData().getClass().getName()).get(selectedNode.toString()).run());
                        }
                    }
                }
            }
        });
    }

    private static DefaultMutableTreeNode getNodes(){
        var root = new DefaultMutableTreeNode("Add Object");
        var gizmos = new DefaultMutableTreeNode(new TreeCategory("Gizmo", EditorIcons.objectTreeIconMap.get("Gizmo")));
        var ai = new DefaultMutableTreeNode(new TreeCategory("AI", EditorIcons.objectTreeIconMap.get("AI")));

        var gizmoPickup = new DefaultMutableTreeNode("Pickup");
        var gizmoZipUp = new DefaultMutableTreeNode("ZipUp");
        var gizmoLever = new DefaultMutableTreeNode("Lever");
        var gizmoHats = new DefaultMutableTreeNode("HatMachine");
        var gizmoTubes = new DefaultMutableTreeNode("Tube");
        var gizmoPanel = new DefaultMutableTreeNode("Panel");

        var aiCharacter = new DefaultMutableTreeNode(new TreeCategory("Creature Spawn", EditorIcons.objectTreeIconMap.get("Creatures")));
        var aiLocatorSet = new DefaultMutableTreeNode(new TreeCategory("Locator Set", EditorIcons.objectTreeIconMap.get("LocatorSets")));

        gizmos.add(gizmoPickup);
        gizmos.add(gizmoZipUp);
        gizmos.add(gizmoLever);
        gizmos.add(gizmoHats);
        gizmos.add(gizmoTubes);
        gizmos.add(gizmoPanel);

        ai.add(aiCharacter);
        ai.add(aiLocatorSet);

        root.add(ai);
        root.add(gizmos);
        root.add(new DefaultMutableTreeNode(new TreeCategory("Model", EditorIcons.objectTreeIconMap.get("Models"))));

        return root;
    }

    private static final class TreeCategory implements IconNodeRenderer.IconNode{

        public String name;
        public Icon icon;
        private static Icon defaultIcon = new FlatFileViewFileIcon();

        public TreeCategory(String name) {
            this(name, defaultIcon);
        }

        public TreeCategory(String name, Icon icon) {
            this.name = name;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public Icon getIcon() {
            return icon;
        }
    }
}
