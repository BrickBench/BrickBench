package com.opengg.loader.editor.tabs;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.components.FlatToggleButton;
import com.opengg.core.engine.OpenGG;
import com.opengg.loader.EditorEntity;
import com.opengg.loader.editor.EditorIcons;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.editor.EditorTheme;
import com.opengg.loader.editor.MapInterface;
import com.opengg.loader.editor.components.*;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.table.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EditorTabAutoRegister
public class EditorPane extends JPanel implements EditorTab {
    private static final int COLLAPSE_OFFSET = 20;

    public static EditorPane instance;

    private JTextField name = new JTextField("Temp Object");
    private JLabel type = new JLabel("Spline");
    private JPanel defaultPanel;
    private JPanel holder;
    private JScrollPane pane;
    private JButton remove;
    private JButton export;
    private JPanel topBar;

    public EditorPane(){
        instance = this;
        this.setLayout(new BorderLayout());

        holder = new ScrollableJPanel();
        holder.setLayout(new BoxLayout(holder,BoxLayout.Y_AXIS));

        pane = new JScrollPaneBackgroundSnatcher(holder);
        //pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setBorder(BorderFactory.createMatteBorder(2,0,0,0,UIManager.getColor("Separator.foreground")));

        topBar = new JPanel();
        BoxLayout topLayout = new BoxLayout(topBar,BoxLayout.X_AXIS);
        topBar.setLayout(topLayout);
        topBar.add(type);
        topBar.add(Box.createRigidArea(new Dimension(40,0)));
        topBar.add(name);

        FlatSVGIcon trashSVG = EditorIcons.trash;
        trashSVG.setColorFilter(EditorTheme.iconFilter);
        remove = new JButton(trashSVG);
        topBar.add(remove);

        export = new JButton("Export");
        topBar.add(export);

        topBar.setBackground(new FlatToggleButton().getTabSelectedBackground());
        topBar.setBorder(new EmptyBorder(5,4,5,4));

        EditorState.addMapReloadListener(s -> refresh(EditorState.getSelectedObject()));
        EditorState.addSelectionChangeListener(s -> refresh(EditorState.getSelectedObject()));

        type.setIconTextGap(5);

        this.add(topBar, BorderLayout.NORTH);
        this.add(pane, BorderLayout.CENTER);

        this.refresh(EditorEntity.Ref.NULL);
    }

    public void refresh(EditorEntity.Ref<?> gameObject) {
        if (!gameObject.exists()){
            this.pane.setVisible(false);
            this.topBar.setVisible(false);
            return;
        } else {
            this.pane.setVisible(true);
            this.topBar.setVisible(true);
        }
        name.setEditable(false);
        Optional<EditorEntity.Property> nameProperty = gameObject.get().properties().stream().filter(e->e.name().equals("Name")).findFirst();

        if(nameProperty.isPresent() && nameProperty.get() instanceof EditorEntity.StringProperty nameSProperty){
            name.setText(nameSProperty.stringValue());
            if (nameSProperty.editable()) {
                name.setEditable(true);
                name.addActionListener(a -> gameObject.get().applyPropertyEdit("Name", new EditorEntity.StringProperty("Name", name.getText(), true, 0)));
            }
        }else {
            name.setText(gameObject.get().name());
        }

        String rawTypePath = gameObject.path().substring(0,gameObject.path().lastIndexOf('/'));
        String typeString = rawTypePath.substring(rawTypePath.lastIndexOf('/')+1);
        type.setText(typeString);
        type.setFont(new Font(type.getFont().getName(),Font.BOLD,type.getFont().getSize()));
        type.setIcon(EditorIcons.objectTreeIconMap.getOrDefault(typeString, null));
        holder.removeAll();

        remove.setEnabled(false);
        export.setVisible(false);
        JPanel buttonLayout = new JPanel(new WrapLayout());
        int numSpecialButtons = 0;
        int numNoDefaults = 0;

        for(var action : gameObject.get().getButtonActions().entrySet()){
            if(action.getKey().equals("Remove") || action.getKey().equals("Delete")){
                remove.setEnabled(true);
                clearButtonAction(remove);
                remove.addActionListener(e -> OpenGG.asyncExec(action.getValue()));
            }else if(action.getKey().equals("Export")){
                export.setVisible(true);
                clearButtonAction(export);
                export.addActionListener(e -> OpenGG.asyncExec(action.getValue()));
            }else {
                numSpecialButtons++;
                JButton newButton = new JButton(action.getKey());
                newButton.addActionListener(e -> OpenGG.asyncExec(action.getValue()));
                buttonLayout.add(newButton);
            }
        }

        JPanel defaultPanel = new JPanel(new MigLayout("fillx,wrap 2,gapy n:8:n","[]15:100:200[grow,fill]"));
        defaultPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        defaultPanel.setAlignmentY(java.awt.Component.TOP_ALIGNMENT);

        if(gameObject.get().properties().stream().anyMatch(e -> !(e instanceof EditorEntity.GroupProperty))){
            holder.add(defaultPanel);
        }

        for (var prop: gameObject.get().properties()) {
            switch (prop) {
                case EditorEntity.GroupProperty groupProp -> {
                    holder.add(new JSeparator());
                    JPanel inner = new JPanel(new MigLayout("fillx,wrap 2,gapy n:8:n", "10[]15:100:200[grow,fill]"));

                    for (var innerProp : groupProp.value()) {
                        attachPropEditor(inner, innerProp, gameObject);
                    }

                    EditorCollapsable collapsable = new EditorCollapsable(groupProp.name(), inner, groupProp.autoExpand());
                    collapsable.setAlignmentX(Component.LEFT_ALIGNMENT);
                    collapsable.setAlignmentY(Component.TOP_ALIGNMENT);
                    holder.add(collapsable);
                    numNoDefaults++;
                }
                case EditorEntity.ListProperty listProp -> {
                    ListPropertyComponent collapse = new ListPropertyComponent(listProp, gameObject);
                    collapse.setAlignmentX(Component.LEFT_ALIGNMENT);
                    collapse.setAlignmentY(Component.TOP_ALIGNMENT);
                    holder.add(collapse);
                    numNoDefaults++;
                }
                case EditorEntity.CustomUIProperty customProp when !customProp.insetInDefault() -> {
                    customProp.createNewInterface(holder, gameObject.get());
                    numNoDefaults++;
                }
                case default -> {
                    if (!prop.name().equals("Name")) {
                        attachPropEditor(defaultPanel, prop, gameObject);
                    }
                }
            }
        }

        if(numSpecialButtons != 0){
            buttonLayout.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            buttonLayout.setAlignmentY(Component.TOP_ALIGNMENT);
            holder.add(buttonLayout);
        }

        if(numNoDefaults == gameObject.get().properties().size()){
            defaultPanel.setVisible(false);
        }else{
            defaultPanel.setVisible(true);
        }

        defaultPanel.revalidate();
        defaultPanel.setMaximumSize( new Dimension(Integer.MAX_VALUE,defaultPanel.getPreferredSize().height));
        holder.add(Box.createVerticalGlue());
        holder.revalidate();
    }

    public static void attachPropEditor(JPanel panel, EditorEntity.Property prop, EditorEntity.Ref<?> entity){
        prop.createNewInterface(panel, entity.get());
    }

    private static void clearButtonAction(JButton button){
        for( ActionListener al : button.getActionListeners() ) {
            button.removeActionListener( al );
        }
    }

    public static DocumentFilter intFilter = new DocumentFilter(){
        private final static Pattern regEx = Pattern.compile("-?\\d*");

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            Matcher matcher = regEx.matcher(text);
            if(!matcher.matches()){
                return;
            }
            super.replace(fb, offset, length, text, attrs);
        }
    };

    public static DocumentFilter floatFilter = new DocumentFilter(){
        private final static Pattern regEx = Pattern.compile("^-?\\d*\\.?\\d*$");

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            Matcher matcher = regEx.matcher(text);
            if(!matcher.matches()){
                return;
            }
            super.replace(fb, offset, length, text, attrs);
        }
    };

    @Override
    public String getTabName() {
        return "Inspect";
    }

    @Override
    public String getTabID() {
        return "editor-pane";
    }

    @Override
    public MapInterface.InterfaceArea getPreferredArea() {
        return MapInterface.InterfaceArea.TOP_RIGHT;
    }

    @Override
    public boolean getDefaultActive() {
        return true;
    }
}
