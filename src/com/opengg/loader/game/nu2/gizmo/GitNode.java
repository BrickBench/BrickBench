package com.opengg.loader.game.nu2.gizmo;

import com.opengg.loader.editor.EditorState;
import com.opengg.loader.MapEntity;
import com.opengg.loader.game.nu2.NU2MapData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GitNode implements MapEntity<GitNode> {
    public String name = "";
    public String type;
    public float x;
    public float y;
    public int parent;
    public int id;
    public boolean selected;
    public ArrayList<Integer> children = new ArrayList<>();
    public ArrayList<String> conditions = new ArrayList<>();
    public ArrayList<String> actions = new ArrayList<>();
    public Map<String, String> gizmos = new LinkedHashMap<>();

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String path() {
        return "Gameplay/" + name;
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new StringProperty("Name",name, false, 128),
                new IntegerProperty("ID",id,false),
                new StringProperty("Type",type, false, 128),
                new ListProperty("Conditions",
                        conditions.stream()
                                .map(c -> new StringProperty("q", c, false, 128))
                                .collect(Collectors.toList()), true),
                new ListProperty("Actions",
                        actions.stream()
                                .map(a -> new StringProperty("q", a, false, 128))
                                .collect(Collectors.toList()), true),
                new ListProperty("Gizmos",
                        gizmos.entrySet().stream()
                                .map(g -> switch (g.getKey()) {
                                    case "Name" ->
                                            ((NU2MapData)EditorState.getActiveMap().levelData())
                                                .getGizmoByName(g.getValue().replace("\"", ""))
                                                .map(m -> (Property) new EditorEntityProperty(g.getKey(), m, false, false, "Gizmos/"))
                                                .orElse(new StringProperty(g.getKey(), g.getValue(), false, 128));
                                    default -> new StringProperty(g.getKey(), g.getValue(), false, 128);
                                }).collect(Collectors.toList()), true));
    }
}
