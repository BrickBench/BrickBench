package com.opengg.loader.editor.windows;

import com.formdev.flatlaf.icons.FlatMenuArrowIcon;
import com.opengg.core.Configuration;
import com.opengg.core.console.GGConsole;
import com.opengg.core.engine.Resource;
import com.opengg.loader.BrickBench;
import com.opengg.loader.Util;
import com.opengg.loader.editor.EditorIcons;
import com.opengg.loader.editor.hook.TCSHookPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class InitialProjectWindow extends JFrame {
    private record VideoRecord(URI link, String text){

    }
    private static List<VideoRecord> videoRecordList = List.of(
            new VideoRecord(URI.create("https://brickbench.readthedocs.io/en/latest/install.html"),"Installation & Setup"),
            new VideoRecord(URI.create("https://brickbench.readthedocs.io/en/latest/editing.html"),"Editing"),
            new VideoRecord(URI.create("https://brickbench.readthedocs.io/en/latest/project.html"),"Projects & Assets"),
            new VideoRecord(URI.create("https://brickbench.readthedocs.io/en/latest/test.html"),"Testing"),
            new VideoRecord(URI.create("https://brickbench.readthedocs.io/en/latest/glossary.html"),"Glossary"),
            new VideoRecord(URI.create("https://brickbench.readthedocs.io/en/latest/faq.html"),"FAQ")

            );

    public InitialProjectWindow(Consumer<String> onOpSelect) {
        super("BrickBench");

        var image = new ImageIcon(Resource.getTexturePath("icon.png")).getImage();
        var scaledImage = Util.getScaledImage(44, 44, image);

        super.setIconImage(image);

        this.setLayout(new BorderLayout());

        var tabs = new JTabbedPane();
        tabs.setTabPlacement(JTabbedPane.LEFT);

        var infoPanel = new JPanel(new MigLayout(""));

        infoPanel.add(new JLabel(new ImageIcon(scaledImage)));
        infoPanel.add(new JLabel("<html><b>BrickBench</b><br/><small>" + BrickBench.VERSION.version() + "</small></html>"));
        infoPanel.setBorder(new EmptyBorder(10,3,15,0));

        tabs.putClientProperty("JTabbedPane.minimumTabWidth", 140);
        tabs.putClientProperty("JTabbedPane.tabAlignment", SwingConstants.LEADING);
        tabs.putClientProperty("JTabbedPane.leadingComponent", infoPanel);

        //var mainPanel = new JPanel(new MigLayout("wrap 2, fill, align center", "[align center]", "[align center]10[]"));
        var mainPanel = new JPanel(new BorderLayout());

        var newProject = new JButton("<html><b>Create project</b></html>");
        var loadMap = new JButton("<html><b>Load map/project</b></html>");
        var openEmpty = new JButton("<html><b>Open hook</b></html>");

        //newProject.putClientProperty("JButton.buttonType", "toolBarButton");
        //loadMap.putClientProperty("JButton.buttonType", "toolBarButton");

        newProject.addActionListener(a -> {
            onOpSelect.accept("NEW");
            this.dispose();
        });

        loadMap.addActionListener(a -> {
            onOpSelect.accept("LOAD");
            this.dispose();
        });

        openEmpty.addActionListener(a -> {
            TCSHookPanel.generateGameSelectMenu(() -> {
                onOpSelect.accept("HOOK");
                this.dispose();
            }).show(openEmpty, 0 , openEmpty.getHeight());
        });

        var topRow = Box.createHorizontalBox();
        JLabel projectTitle = new JLabel("<html><span style='font-size:16px'><b>Projects</b></span></html>");
        topRow.add(projectTitle);
        topRow.add(Box.createGlue());
        topRow.add(Box.createHorizontalStrut(25));
        topRow.add(newProject);
        topRow.add(Box.createHorizontalStrut(10));
        topRow.add(loadMap);
        topRow.add(Box.createHorizontalStrut(10));
        topRow.add(openEmpty);
        topRow.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        mainPanel.add(topRow,BorderLayout.NORTH);

        var recentFile = Configuration.getConfigFile("recent.ini");

        var recents = IntStream.range(0, BrickBench.RECENT_SAVES)
                .mapToObj(i -> recentFile.getConfig("recent_" + i))
                .filter(s -> !s.isEmpty()).toList();

        var list = new JList<>(recents.toArray(new String[]{}));
        list.setCellRenderer(new RecentRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(50);
        list.setBorder(null);

        topRow.setBackground(list.getBackground());

        var window = this;
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (list.getSelectedIndex() != -1 && e.getClickCount() == 2) {
                    var item = list.getSelectedValue();
                    if (item != null) {
                        onOpSelect.accept(item);
                        window.dispose();
                    }
                }
            }
        });

        var listPane = new JScrollPane(list);
        listPane.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));

        //mainPanel.add(listPane, "grow, span 2");
        mainPanel.add(listPane,BorderLayout.CENTER);
        mainPanel.setBackground(list.getBackground());

        tabs.add(mainPanel, "Projects");

        //var learnPanel = Box.createVerticalBox();
        var learnPanel = new JPanel(new BorderLayout());
        tabs.add(learnPanel, "Learn");
        JTable videoList = new JTable();

        DefaultTableCellRenderer rightRenderer = new VideoRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        Icon menuIcon = EditorIcons.longarrow;
        TableModel model = new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return videoRecordList.size();
            }

            @Override
            public int getColumnCount() {
                return 4;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex){
                    case 0 -> ImageIcon.class;
                    default -> String.class;
                };
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return switch(columnIndex){
                    case 0 -> menuIcon;
                    case 1 -> videoRecordList.get(rowIndex).text;
                    case 3 ->"";
                    default -> null;
                };
            }
        };

        videoList.setModel(model);
        videoList.setTableHeader(null);
        videoList.setRowHeight(50);
        videoList.getColumnModel().getColumn(0).setMaxWidth(45);
        videoList.getColumnModel().getColumn(1).setCellRenderer(new VideoRenderer());
        videoList.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        videoList.getColumnModel().getColumn(3).setMinWidth(15);
        videoList.getColumnModel().getColumn(3).setMaxWidth(15);
        videoList.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
        videoList.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table =(JTable) mouseEvent.getSource();
                Point point = mouseEvent.getPoint();
                int row = table.rowAtPoint(point);
                if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    try {
                        Desktop.getDesktop().browse(videoRecordList.get(row).link);
                    } catch(Exception uriExcept) {
                        GGConsole.exception(uriExcept);
                    }
                }
            }
        });
        var videoScroll = new JScrollPane(videoList);
        videoScroll.setBorder(BorderFactory.createEmptyBorder());
        learnPanel.add(videoScroll,BorderLayout.CENTER);
        var vidTopRow = Box.createHorizontalBox();

        JButton website = new JButton("<html><b>BrickBench website</b></html>");
        website.addActionListener(e -> {try {
            Desktop.getDesktop().browse(URI.create("https://brickbench.opengg.dev"));
        } catch(Exception uriExcept) {
            GGConsole.exception(uriExcept);
        }});

        JButton issues = new JButton("<html><b>Report bugs</b></html>");
        issues.addActionListener(e -> {try {
            Desktop.getDesktop().browse(URI.create("https://www.github.com/BrickBench/BrickBench/issues"));
        } catch(Exception uriExcept) {
            GGConsole.exception(uriExcept);
        }});

        JButton docs = new JButton("<html><b>Offline documentation</b></html>");
        docs.addActionListener(e -> {try {
            Desktop.getDesktop().open(Resource.getApplicationPath().resolve("documentation.pdf").toFile());
        } catch(Exception uriExcept) {
            GGConsole.exception(uriExcept);
        }});

        JLabel learnTitle = new JLabel("<html><span style='font-size:16px'><b>Learn</b></span></html>");
        vidTopRow.add(learnTitle);
        vidTopRow.add(Box.createGlue());
        vidTopRow.add(Box.createHorizontalStrut(35));
        vidTopRow.add(website);
        vidTopRow.add(issues);
        vidTopRow.add(docs);

        vidTopRow.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        vidTopRow.setBackground(videoList.getBackground());
        learnPanel.setBackground(videoList.getBackground());
        learnPanel.add(vidTopRow,BorderLayout.NORTH);

        this.add(tabs, BorderLayout.CENTER);

        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);

        this.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                e.getWindow().dispose();
                onOpSelect.accept("CLOSE");
            }
        });

        this.requestFocus();
    }
    static class VideoRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                                                       int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            this.setValue(table.getValueAt(row, column));
            this.setFont(this.getFont().deriveFont(Font.BOLD));
            return this;
        }
    }

    static class RecentRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object item, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, item, index, isSelected, cellHasFocus);

            setText("<html>" + Path.of((String)item).getFileName().toString() + "<br/><small>" + item + "</small></html>");


            return this;
        }

    }
}

