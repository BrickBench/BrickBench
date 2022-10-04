package com.opengg.loader;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.formdev.flatlaf.extras.components.FlatButton;
import com.formdev.flatlaf.icons.FlatFileViewFloppyDriveIcon;
import com.opengg.core.engine.OpenGG;
import com.opengg.core.math.Vector3f;
import com.opengg.core.world.WorldEngine;
import com.opengg.loader.components.ArrowComponent;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.editor.SearchableListPanel;
import com.opengg.loader.editor.components.FileSelectField;
import com.opengg.loader.editor.components.JVectorField;
import com.opengg.loader.editor.tabs.EditorPane;
import com.opengg.loader.editor.windows.BitfieldButton;
import com.opengg.loader.game.nu2.scene.FileMaterial;
import com.opengg.loader.game.nu2.scene.FileTexture;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A selectable and referenceable BrickBench object.
 *
 * A class should implement EditorEntity to be usable as a selected and editable object in BrickBench.
 * EditorEntities do not have to be mutable. As long as they can be recovered between state changes,
 * BrickBench will identify EditorEntities by their reference. {@link EditorEntity#Ref}
 *
 * EditorEntities are referenced by a namespace and path. The namespace represents the source of this entity, and can be
 * things like map names or the global namespace used for BrickBench types. The path represents the name of the entity,
 * including subcategories representing the groups this entity belongs to. For example, "Gizmo/Lever/lever_1" refers to an item
 * named "lever_1" and path "Gizmo/Lever".
 */
public interface EditorEntity<T extends EditorEntity<T>> {
    static String UNIQUE_CHAR = "#";

    /**
     * Returns the name of this object
     * @return
     */
    String name();

    /**
     * Returns the local (non-namespaced) path of this object
     * @return
     */
    String path();

    /**
     * Returns the namespace this object belongs in, or an empty string if unknown
     * @return
     */
    default String namespace() {
        return "";
    }

    /**
     * Returns the world position of this object, or null if it doesn't apply
     * @return
     */
    default Vector3f pos() {
        return null;
    }

    /**
     * Returns a list of editable UI properties
     * @return
     */
    List<Property> properties();

    /**
     * Returns a list of actions that can be applied onto this object.
     */
    @JsonIgnore
    default Map<String, Runnable> getButtonActions(){ return Map.of(); }

    /**
     * Applies a property edit to the given property. This will pass in a new property with the changes
     * made.
     */
    default void applyPropertyEdit(String propName, Property newValue){}

    interface Property{
        String name();
        String stringValue();

        default void createNewInterface(JPanel panel, EditorEntity object){}
    }

    private static FlatButton editButton(EditorEntity<?> object, String name, Supplier<Property> prop){
        FlatButton save = new FlatButton();
        save.setIcon(new FlatFileViewFloppyDriveIcon());
        save.addActionListener(e -> {
            if(object != null)
                object.applyPropertyEdit(name, prop.get());
        });
        return save;
    }

    record StringProperty (String name, String value, boolean editable, int maxLen) implements Property{
        @Override
        public String stringValue() { return value; }

        @Override
        public void createNewInterface(JPanel panel, EditorEntity object) {
            panel.add(new JLabel(name),"align label");
            if(editable){

                var textField = new JTextField(value());
                textField.setMargin(new Insets(3,2,3,0));
                ((AbstractDocument)textField.getDocument()).setDocumentFilter(new LimitDocumentFilter(maxLen));

                Supplier<Property> editedProp = () -> new EditorEntity.StringProperty(name, textField.getText(), true, maxLen);

                textField.addActionListener(a -> object.applyPropertyEdit(name, editedProp.get()));

                panel.add(textField,"grow, split 2");
                panel.add(editButton(object, name, editedProp));
            }else{
                panel.add(new JLabel(value()));
            }
        }
    }

    record EnumProperty (String name, Enum<?> value, boolean editable) implements Property{
        @Override
        public String stringValue() { return value.name(); }

        @Override
        public void createNewInterface(JPanel panel, EditorEntity object) {
            panel.add(new JLabel(name),"align label");
            if(editable){
                var constants = value().getDeclaringClass().getEnumConstants();
                var dropDown = new JComboBox<>(Arrays.stream(constants).map(Object::toString).toArray(String[]::new));
                dropDown.setSelectedItem(value().name());
                panel.add(dropDown);

                dropDown.addActionListener(a -> {
                    var newEnum = Arrays.stream(constants)
                            .map(c -> (Enum)c)
                            .filter(c -> c.toString().equals(dropDown.getSelectedItem()))
                            .findFirst().get();

                    object.applyPropertyEdit(name, new EnumProperty(name,newEnum, true));
                });
            }else{
                panel.add(new JLabel(stringValue()));
            }
        }
    }

    record IntegerProperty (String name, int value, boolean editable) implements Property{
        @Override
        public String stringValue() {
            return Integer.toString(value);
        }

        @Override
        public void createNewInterface(JPanel panel, EditorEntity object) {
            panel.add(new JLabel(name),"align label");
            if(editable){
                var inField = new JTextField(stringValue());
                inField.setMargin(new Insets(3,2,3,0));
                ((AbstractDocument)inField.getDocument()).setDocumentFilter(EditorPane.intFilter);
                panel.add(inField,"grow, split 2");
                panel.add(editButton(object,name,() -> new EditorEntity.IntegerProperty(name,Integer.parseInt(inField.getText()), true)));

            }else{
                panel.add(new JLabel(stringValue()));
            }
        }
    }

    record FloatProperty (String name, float value, boolean editable) implements Property{
        @Override
        public String stringValue() {
            return Float.toString(value);
        }

        @Override
        public void createNewInterface(JPanel panel, EditorEntity object) {
            panel.add(new JLabel(name),"align label");
            if(editable){
                var inField = new JTextField(stringValue());
                inField.setMargin(new Insets(3,2,3,0));
                ((AbstractDocument)inField.getDocument()).setDocumentFilter(EditorPane.floatFilter);
                panel.add(inField,"grow, split 2");
                panel.add(editButton(object,name,() -> new EditorEntity.FloatProperty(name,Float.parseFloat(inField.getText()),true)));

            }else{
                panel.add(new JLabel(stringValue()));
            }
        }
    }

    record BooleanProperty (String name, boolean value, boolean editable) implements Property{
        @Override
        public String stringValue() {
            return Boolean.toString(value);
        }

        @Override
        public void createNewInterface(JPanel panel, EditorEntity object) {
            panel.add(new JLabel(name),"align label");
            if(editable){
                var inField = new JCheckBox();
                inField.setSelected(value);
                inField.setMargin(new Insets(3,2,3,0));
                inField.addActionListener(a -> {
                    object.applyPropertyEdit(name, new EditorEntity.BooleanProperty(name, inField.isSelected(), true));
                });
                panel.add(inField);

            }else{
                panel.add(new JLabel(stringValue()));
            }
        }
    }

    record VectorProperty (String name, Vector3f value, boolean useArrows, boolean editable) implements Property{
        @Override
        public String stringValue() {
            return value.toFormattedString(3);
        }

        @Override
        public void createNewInterface(JPanel panel, EditorEntity object) {
            panel.add(new JLabel(name),"align label");
            if(editable){
                var vecPanel = new JVectorField(value);
                panel.add(vecPanel,"grow, split 2");

                Supplier<Property> onEdit = () -> new EditorEntity.VectorProperty(name,vecPanel.getValue(),true, true);

                if(useArrows()){
                    OpenGG.asyncExec(() -> {
                        var arrow = new ArrowComponent(
                                v -> object.applyPropertyEdit(name, onEdit.get()),
                                vecPanel::setValue)
                                .setPositionOffset(value());
                        WorldEngine.getCurrent().attach(arrow);
                        EditorState.CURRENT.temporaryComponents.add(arrow);
                    });
                }
                panel.add(editButton(object,name,onEdit));
            }else{
                panel.add(new JLabel(stringValue()));
            }
        }
    }

    record ListProperty(String name, List<Property> value, boolean autoExpand, Consumer<Property> addValueFunc,BiConsumer<Property, Integer> valueRemovedFunc, Function<List<Property>, Property> newValueFunc, String editSource) implements Property{
        public ListProperty(String name, List<Property> value, boolean autoExpand){
            this(name, value, autoExpand, null, null, null, null);
        }
        public ListProperty(String name, List<Property> value, boolean autoExpand, Consumer<Property> addValueFunc, BiConsumer<Property, Integer> removeValueFunc, String editSource){
            this(name, value, autoExpand, addValueFunc, removeValueFunc, null, editSource);
        }
        public ListProperty(String name, List<Property> value, boolean autoExpand, Consumer<Property> addValueFunc, BiConsumer<Property, Integer> removeValueFunc, Function<List<Property>, Property> newValueFunc){
            this(name, value, autoExpand, addValueFunc, removeValueFunc, newValueFunc, null);
        }

        public boolean editable() {
            return addValueFunc != null;
        }

        @Override
        public String stringValue() {
            return value.stream().map(Property::stringValue).collect(Collectors.joining());
        }
    }

    record GroupProperty(String name, List<Property> value, boolean autoExpand) implements Property{
        public GroupProperty(String name, List<Property> value){
            this(name,value,false);
        }
        @Override
        public String stringValue() {
            return value.stream().map(Property::stringValue).collect(Collectors.joining());
        }
    }

    record ColorProperty(String name, Vector3f value) implements Property{

        @Override
        public String stringValue() {
            return "#" + Integer.toHexString((int) (value.x * 255)) +
                    Integer.toHexString((int) (value.y * 255)) +
                    Integer.toHexString((int) (value.z * 255));
        }

        @Override
        public void createNewInterface(JPanel panel, EditorEntity object) {
            panel.add(new JLabel(name),"align label");
            var color = new Color(value.x, value.y, value.z);

            var tab = new JButton();
            tab.setText("#" + Integer.toHexString((int) (value.x * 255)) +
                    Integer.toHexString((int) (value.y * 255)) +
                    Integer.toHexString((int) (value.z * 255)));
            tab.setBackground(color);
            tab.setForeground((value.x*0.299 + value.y*0.587 +value.z*0.114) > 0.4 ? Color.BLACK : Color.WHITE);
            tab.setHorizontalAlignment(SwingConstants.CENTER);
            tab.setBorder(new EmptyBorder(3,1, 3, 1));
            tab.setOpaque(true);
            tab.addActionListener(e->{
                Color newColor = JColorChooser.showDialog(panel,"Pick Material Color",color);
                if(newColor != null) object.applyPropertyEdit(name, new ColorProperty(name,new Vector3f(newColor)));
            });

            panel.add(tab);
        }
    }

    record FileProperty(String name, Path value, boolean mustExist, String... extensions) implements Property{

        @Override
        public String stringValue() {
            return value.toString();
        }

        @Override
        public void createNewInterface(JPanel panel, EditorEntity object) {
            panel.add(new JLabel(name),"align label");
            var selector = new FileSelectField(Path.of(FileSystemView.getFileSystemView().getDefaultDirectory().getPath()), FileUtil.LoadType.BOTH, name, extensions);
            selector.onSelect(p -> {
                if(!mustExist || Files.exists(p)){
                    object.applyPropertyEdit(name, new FileProperty(name, p, true));
                } });

            panel.add(selector);
        }
    }

    record BitFieldProperty(String name, int length, long value, Map<Integer,String> propMap) implements Property{

        @Override
        public String stringValue() {
            return String.valueOf(value);
        }

        @Override
        public void createNewInterface(JPanel panel, EditorEntity object) {
            panel.add(new JLabel(name),"align label");
            panel.add(new BitfieldButton(propMap,name,value));
        }
    }

    record EditorEntityProperty(String name, EditorEntity value, boolean label, boolean editable, String editSource) implements Property{
        @Override
        public String stringValue() {
            return value == null ? "" : value.name();
        }

        @Override
        public void createNewInterface(JPanel panel, EditorEntity object) {
            var button = new JButton();
            button.setOpaque(false);
            panel.add(new JLabel(name),"align label");
            if(value instanceof FileTexture texture){
                ImageIcon icon = texture.icon().getNow(null);
                if(icon != null){
                    button.setIcon(icon);
                }
            }else if(value instanceof FileMaterial material){
                ImageIcon icon = material.getIcon();
                if(icon != null){
                    button.setIcon(icon);
                }
            }

            if(value == null){
                button.setText("No object found");
            }else{
                button.setText(value().name());
                button.addActionListener(a -> EditorState.selectObject(value));
            }

            var finalPanel = new JPanel();
            finalPanel.add(button);
            if(editable){
                var editButton = new JButton("Change");

                var searchPanel = new SearchableListPanel(editSource, c ->{
                    object.applyPropertyEdit(name, new EditorEntityProperty(name, c, true, true, editSource));
                    button.setText(c.name());
                }, false);
                searchPanel.setMinimumSize(new Dimension(100, 200));
                searchPanel.setPreferredSize(new Dimension(150, 250));

                JPopupMenu menu = new JPopupMenu();
                menu.setLayout(new BorderLayout());
                menu.add(searchPanel, BorderLayout.CENTER);

                editButton.addActionListener((e) -> menu.show(editButton, 0, ((JButton) e.getSource()).getHeight()));
                finalPanel.add(editButton);
            }

            panel.add(finalPanel);
        }
    }

    record TupleProperty(String name,List<Property> value) implements Property{

        @Override
        public String name() {
            return name;
        }

        @Override
        public String stringValue() {
            return value.stream().map(Property::stringValue).collect(Collectors.joining());
        }

        @Override
        public void createNewInterface(JPanel panel, EditorEntity object) {
            panel.add(new JLabel(name),"align label");
            JPanel holder = new JPanel(new MigLayout("fillx,wrap 2","[]15:100:200[grow,fill]"));
            //BoxLayout layout = new BoxLayout(holder,BoxLayout.Y_AXIS);
            //holder.setLayout(layout);
            for(var prop: value){
                prop.createNewInterface(holder,object);
            }
            panel.add(holder);
        }
    }

    record CustomUIProperty (String name, JComponent component, boolean insetInDefault) implements Property{

        @Override
        public String stringValue() {
            return name;
        }

        @Override
        public void createNewInterface(JPanel panel, EditorEntity object) {
            if(insetInDefault) {
                panel.add(component, "span,growx,push");
            }else {
                panel.add(component);
                component.setAlignmentX(Component.LEFT_ALIGNMENT);
                component.setAlignmentY(Component.TOP_ALIGNMENT);
            }
        }
    }

    class LimitDocumentFilter extends DocumentFilter {

        private int limit;

        public LimitDocumentFilter(int limit) {
            if (limit <= 0) {
                throw new IllegalArgumentException("Limit can not be <= 0");
            }
            this.limit = limit;
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            int currentLength = fb.getDocument().getLength();
            int overLimit = (currentLength + text.length()) - limit - length;
            if (overLimit > 0) {
                text = text.substring(0, text.length() - overLimit);
            }
            if (text.length() > 0) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

    }

    /**
     * Returns a reference to this object.
     */
    default Ref<T> ref(){
        return new Ref<>((T) this);
    }

    /**
     * Represents a reference to an EditorEntity. References can be used to refer to an EditorEntity
     * that creates a new copy of itself instead of mutating, such as those created during map reloads.
     * Refs identify their EditorEntities by namespace and path.
     */
    class Ref<T extends EditorEntity<T>>{
        private T latest = null;
        private Project.MapInstance latestMap = null;
        private boolean invalidated = false;

        private final String namespace;
        private final String path;

        public static final Ref NULL = new Ref();

        /**
         * Creates an empty reference
         */
        public Ref() {
            this(null, null);
        }

        /**
         * Creates a new reference to the given object, using automatic namespace resolution
         * @param object
         */
        public Ref(T object) {
            this("", object);
        }

        /**
         * Creates a new reference to the given {@link EditorEntity}
         * This creates a reference given a namespace and the object's path (derived from the object)
         * If the namespace given is empty, the namespace is resolved as follows:
         * - If the object provides its own namespace through {@link EditorEntity#namespace()}, that is used
         * - Otherwise, the active namespace is used
         * @param namespace The namespace to use for this reference, or empty if unknown
         * @param object The object to create a reference to
         */
        public Ref(String namespace, T object) {
            if (object == null) {
                this.namespace = "";
                this.path = "";
                invalidated = true;
            } else {
                if (namespace.isEmpty()) {
                    this.namespace = object.namespace().isEmpty() ? EditorState.getActiveNamespace() : object.namespace();
                } else {
                    this.namespace = namespace;
                }

                this.path = object.path();
                this.latest = object;
                this.latestMap = EditorState.getMapFromName(namespace);
            }
        }

        /**
         * Creates a reference given an object path (including an optional namespace)
         * The object being referred to must exist in the {@link EditorState} when creating this reference.
         * @param path
         */
        public Ref(String path){
            this(Util.getNamespace(path), (T) EditorState.getObject(path));
        }

        /**
         * Returns if this object still exists
         * @return
         */
        public boolean exists(){
            return get() != null;
        }

        /**
         * Returns the namespace this reference was created with
         * @return
         */
        public String namespace() {
            return namespace;
        }

        /**
         * Returns the path of this object
         * @return
         */
        public String path(){
            return path;
        }

        /**
         * Returns the object this reference refers to, or null if the object can no longer be found in the provided {@link Ref#namespace()}
         * @return
         */
        public T get(){
            if(invalidated) return null;

            var updatedMap = EditorState.getMapFromName(namespace);

            if (updatedMap != null && updatedMap == latestMap) {
                return latest;
            }

            var currentObject = EditorState.getObject(namespace, path);

            if(currentObject != null){
                latest = (T) currentObject;
                latestMap = updatedMap;

                return latest;
            }

            invalidated = true;
            latest = null;
            latestMap = null;

            return null;
        }

    }
}
