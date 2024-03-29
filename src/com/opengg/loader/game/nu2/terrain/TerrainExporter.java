package com.opengg.loader.game.nu2.terrain;

import com.opengg.core.math.Vector3f;
import com.opengg.loader.game.nu2.terrain.TerrainGroup;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TerrainExporter {
    public static void writeObjFile(TerrainGroup group, Path outFile) throws IOException {
        FileUtils.writeStringToFile(outFile.toFile(), writeObjString(group), StandardCharsets.UTF_8);
    }

    public static String writeObjString(TerrainGroup group){
        record Face(int v1, int v2, int v3){}

        List<Vector3f> vectors = new ArrayList<>();
        List<Face> faces = new ArrayList<>();

        for(var face : group.blocks().stream().flatMap(b -> b.faces().stream()).collect(Collectors.toList())){
            var v1 = addAndGetIndex(vectors, face.vec1());
            var v2 = addAndGetIndex(vectors, face.vec2());
            var v3 = addAndGetIndex(vectors, face.vec3());
            faces.add(new Face(v1, v3, v2));
            if(!face.norm2().equals(new Vector3f(0, 65536, 0))){
                var v4 = addAndGetIndex(vectors, face.vec4());
                faces.add(new Face(v2, v3, v4));
            }
        }

        var strBuilder = new StringBuilder();
        strBuilder.append("# Autogenerated by the BrickBench terrain exporter\n");

        for(var v : vectors){
            strBuilder.append("v ").append(" ").append(-v.x).append(" ")
                    .append(v.y).append(" ").append(v.z).append("\n");
        }

        strBuilder.append("\n");

        for(var f : faces){
            strBuilder.append("f ").append(" ").append(f.v1 + 1).append(" ").append(f.v2 + 1).append(" ")
                    .append(f.v3 + 1).append("\n");
        }

        return strBuilder.toString();
    }

    private static int addAndGetIndex(List<Vector3f> list, Vector3f vector){
        if(list.contains(vector)){
            return list.indexOf(vector);
        }else{
            list.add(vector);
            return list.size() - 1;
        }
    }
}
