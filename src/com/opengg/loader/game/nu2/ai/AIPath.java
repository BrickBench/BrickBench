package com.opengg.loader.game.nu2.ai;

import com.opengg.core.math.Vector3f;
import com.opengg.loader.EditorEntity;
import com.opengg.loader.MapEntity;
import com.opengg.loader.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record AIPath(String name,List<AIPathConnection> connections, List<AIPathPoint> pathPoints) implements MapEntity<AIPath> {

    @Override
    public String path() {
        return "AI/Path/"+name();
    }

    @Override
    public List<EditorEntity.Property> properties() {
        return List.of(new EditorEntity.StringProperty("Name",name(), true, 16));
    }

    public static record AIPathPoint(AIPath parentPath,String name, Vector3f pos, int index, float xzSize, float minY, float maxY, List<AIPathConnection> connections, String specialObject, Vector3f specialObjectPos) implements MapEntity<AIPathPoint> {
        @Override
        public String name(){
            return name.isEmpty() ? "PathPoint" + index : name;
        }
        @Override
        public String path() {
            return "AI/Path/"+parentPath.name() + "/PathPoint/" +name();
        }

        @Override
        public List<Property> properties() {
            List<Property> connectionProp = new ArrayList<>();
            for(var cnx: connections){
                connectionProp.add(new EditorEntityProperty("Temp",cnx,false,true,"AI/Connections"));
            }
            return List.of(new StringProperty("Name",name(), true, 16),
                    new IntegerProperty("Index",index(),false),
                    new VectorProperty("Position",pos(), true,true),
                    new FloatProperty("XZ Size",xzSize(),true),
                    new FloatProperty("Y Min",minY(),true),
                    new FloatProperty("Y Max",maxY(),true),
                    new StringProperty("Special Object",specialObject(), true, 16),
                    new VectorProperty("Special Object Position",specialObjectPos(), true,true),
                    new ListProperty("Connections",connectionProp,false));
        }
    }

    public static final Map<Integer, String> cnxProps = Util.createOrderedMapFrom(Map.entry(2,"Double Jump"),
            Map.entry(4,"R2D2 Glide"),Map.entry(8,"Zip Up"),Map.entry(0x10,"Use Hatch"),Map.entry(0x20,"Jar Jar Jump"),
            Map.entry(0x40,"Hover Tube"),Map.entry(0x4000,"Swamp"),Map.entry(0x8000,"Take Over"),Map.entry(0x10000,"Vehicle"),
            Map.entry(0x20000,"Party"),Map.entry(0x80,"For Goodies"),Map.entry(0x100,"For Baddies"),Map.entry(0x20000000,"Obstacle"),
            Map.entry(0x400,"Jump Now"),Map.entry(0x800,"Don't Jump Now"),Map.entry(0x40000,"Blockage"),
            Map.entry(0x1,"Jump"),Map.entry(0x80000,"Don't Toggle"));

    public static record AIPathConnection(int to, int from, int toCNXFlags, int fromCNXFlags, int unk1, int unk2,
                                          float a, float b) implements MapEntity<AIPathConnection>{

        @Override
        public String name() {
            return to + " to " + from;
        }

        @Override
        public String path() {
            return "AI/Connections/"+name();
        }

        @Override
        public List<Property> properties() {
            return List.of(new StringProperty("Name",name(), false, 16));
        }
    }
}
