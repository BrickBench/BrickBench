package com.opengg.loader.editor.tabs;

import com.opengg.core.console.GGConsole;
import com.opengg.core.engine.OpenGG;
import com.opengg.loader.*;
import com.opengg.loader.editor.EditorIcons;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.editor.MapInterface;
import com.opengg.loader.editor.ProjectTree;
import com.opengg.loader.editor.windows.ProjectImportDialog;
import com.opengg.loader.game.nu2.Area;
import com.opengg.loader.game.nu2.AreaIO;
import com.opengg.loader.loading.MapWriter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

@EditorTabAutoRegister
public class ProjectStructurePanel extends JPanel implements EditorTab {
    private JPanel projectTreePanel;
    private ProjectTree projectTree;
    private JLabel treeInfo;
    private JPanel emptyTreePanel;

    public ProjectStructurePanel() {
        emptyTreePanel = new JPanel(new BorderLayout());
        treeInfo = new JLabel("No Project Open", SwingConstants.CENTER);
        emptyTreePanel.add(treeInfo);

        projectTreePanel = new JPanel(new BorderLayout());
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addButton = new JButton(EditorIcons.add);
        JPopupMenu menu = createAddProjectTreeMenu();

        JButton removeButton = new JButton(EditorIcons.minus);
        addButton.setBackground(null);
        removeButton.setBackground(null);
        addButton.setBorder(null);
        removeButton.setBorder(null);

        topRow.add(addButton);
        topRow.add(removeButton);

        addButton.addActionListener((e)-> {
            if(!MapWriter.isProjectEditable()) return;
            menu.show(addButton,0,addButton.getHeight());
        });

        removeButton.addActionListener(e -> {
            if(!MapWriter.isProjectEditable()) return;
            if(projectTree.getLastSelectedPathComponent() instanceof DefaultMutableTreeNode node){
                if(node.getUserObject() instanceof ProjectTree.ProjectNodeUserObject pnuo){
                    OpenGG.asyncExec(() -> {
                        EditorState.getProject().structure().removeNode(EditorState.getProject(), pnuo.node());

                        if (pnuo.node() instanceof MapXml) {
                            if (EditorState.getProject().maps().isEmpty()) {
                                BrickBench.CURRENT.useMapFromCurrentProject(null);
                            } else {
                                BrickBench.CURRENT.useMapFromCurrentProject(EditorState.getProject().maps().get(0));
                            }
                        }

                        EditorState.updateProject(EditorState.getProject());
                    });
                }
            }
        });

        projectTreePanel.add(topRow, BorderLayout.NORTH);
        projectTreePanel.add(emptyTreePanel, BorderLayout.CENTER);

        JTextArea descriptor = new JTextArea("This tree shows your edited files and where they will be exported to in the final mod.");
        descriptor.setEditable(false);
        descriptor.setLineWrap(true);
        descriptor.setWrapStyleWord(true);
        descriptor.setColumns(0);
        descriptor.setRows(0);
        descriptor.setBorder(new EmptyBorder(10,10,10,10));

        var pane = new JScrollPane(descriptor);
        pane.setBorder(null);
        pane.add(projectTreePanel);

        this.setLayout(new BorderLayout());
        this.add(projectTreePanel, BorderLayout.CENTER);
        this.add(pane, BorderLayout.SOUTH);

        EditorState.addProjectChangeListener(this::setProject);
    }

    public void setProject(Project project) {
        projectTree = new ProjectTree(project);
        projectTree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int selRow = projectTree.getRowForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    var selectedNode = (DefaultMutableTreeNode) projectTree.getLastSelectedPathComponent();
                    var selectedNodeObject = (ProjectTree.ProjectNodeUserObject) selectedNode.getUserObject();
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        if (e.getClickCount() == 1) {
                            switch (selectedNodeObject.node()) {
                                case Area an -> EditorState.selectObject(an);
                                case MapXml mn -> EditorState.selectObject(mn);
                                case ProjectStructure.FolderNode fn -> EditorState.selectTemporaryObject(fn);
                                case ProjectResource pr -> EditorState.selectTemporaryObject(pr);

                                case null, default -> {
                                }
                            }
                        } else if(e.getClickCount() == 2) {
                            switch (selectedNodeObject.node()) {
                                case MapXml mn -> OpenGG.asyncExec(() -> BrickBench.CURRENT.useMapFromCurrentProject(mn));
                                case ProjectResource rn -> {
                                    try {
                                        Desktop.getDesktop().open(project.projectXml().resolveSibling(rn.path()).toFile());
                                    } catch (IOException ex) {
                                        SwingUtil.showErrorAlert("Failed to open resource", ex);
                                    }
                                }
                                default -> {}
                            }
                        }
                    }
                }
            }
        });

        projectTreePanel.remove(1);

        if(project == null){
            treeInfo.setText("No Project Open.");
            projectTreePanel.add(emptyTreePanel, BorderLayout.CENTER);
        }else if(project.structure().root().children().isEmpty()){
            treeInfo.setText("<html><div style='text-align: center;'>Project is empty.<br/>Add something to get started.</div></html>");
            projectTreePanel.add(emptyTreePanel, BorderLayout.CENTER);
        } else {
            projectTreePanel.add(new JScrollPane(projectTree, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        }

        this.repaint();
        this.validate();
    }
    private JPopupMenu createAddProjectTreeMenu(){
        JPopupMenu menu = new JPopupMenu();

        JMenuItem header = new JMenuItem("<html><b>Add</b></html>");
        header.setBackground(new JButton().getBackground());
        header.setEnabled(false);
        header.setOpaque(true);

        var importMap = new JMenuItem("Import");
        importMap.addActionListener(a -> new ProjectImportDialog(EditorState.getProject(), null));

        var newLevel = new JMenuItem("New Level");
        newLevel.addActionListener(a -> {
            var newArea = AreaIO.createEmptyArea("NewArea");

            if(projectTree.getSelectionPaths() != null){
                var head = projectTree.getSelectionPaths()[0];
                var last = (DefaultMutableTreeNode) head.getLastPathComponent();
                var lastCustom = (ProjectTree.ProjectNodeUserObject) last.getUserObject();

                if(lastCustom.node() instanceof ProjectStructure.FolderNode fn){
                    fn.children().add(newArea);
                    EditorState.updateProject(EditorState.getProject());

                    return;
                }
            }

            EditorState.getProject().structure().root().children().stream()
                    .filter(n -> n instanceof ProjectStructure.FolderNode)
                    .map(n -> (ProjectStructure.FolderNode) n)
                    .filter(n -> n.name().equalsIgnoreCase("levels"))
                    .findFirst().ifPresentOrElse(n -> n.children().add(newArea),
                            () -> {
                                var levelsNode = new ProjectStructure.FolderNode("levels", new ArrayList<>());
                                levelsNode.children().add(newArea);
                                EditorState.getProject().structure().root().children().add(levelsNode);
                            });

            EditorState.updateProject(EditorState.getProject());
        });

        var newFolder = new JMenuItem("New Folder");
        newFolder.addActionListener(a -> {
            if(projectTree.getSelectionPaths() != null){
                var head = projectTree.getSelectionPaths()[0];
                var last = (DefaultMutableTreeNode) head.getLastPathComponent();
                var lastCustom = (ProjectTree.ProjectNodeUserObject) last.getUserObject();

                if(lastCustom.node() instanceof ProjectStructure.FolderNode fn){
                    fn.children().add(new ProjectStructure.FolderNode("NewFolder", new ArrayList<>()));
                    EditorState.updateProject(EditorState.getProject());

                    return;
                }
            }

            EditorState.getProject().structure().root().children().add(new ProjectStructure.FolderNode("NewFolder", new ArrayList<>()));
            EditorState.updateProject(EditorState.getProject());

        });

        menu.add(header);
        menu.add(importMap);
        menu.addSeparator();
        menu.add(newLevel);
        menu.add(newFolder);
        return menu;
    }

    @Override
    public String getTabName() {
        return "Project Structure";
    }

    @Override
    public String getTabID() {
        return "project-structure";
    }

    @Override
    public MapInterface.InterfaceArea getPreferredArea() {
        return MapInterface.InterfaceArea.TOP_LEFT;
    }

    @Override
    public boolean getDefaultActive() {
        return true;
    }
}
