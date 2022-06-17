package com.opengg.loader.test.level;

import com.opengg.loader.MapXml;
import com.opengg.loader.ProjectStructure;
import com.opengg.loader.loading.ProjectIO;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProjectTest {

    @Test
    void testDepthFirstStructureSearch(){
        var emptyStructure = new ProjectStructure(new ProjectStructure.FolderNode("root", new ArrayList<>()));
        var testFile = new MapXml("test", null, MapXml.MapType.NORMAL, List.of(), Map.of(), Map.of());
        var goodPath = Path.of("C:\\test\\Lego Star Wars Saga\\LEVELS\\EPISODE_I\\NEGOTIATIONS\\NEGOTIATIONS_A");

        var node = ProjectIO.addNodeStructureFromPath(emptyStructure, goodPath);

        ((ProjectStructure.FolderNode)node).children().add(new ProjectStructure.MapNode(testFile));

        var resultingPath = emptyStructure.getFolderFor(new ProjectStructure.MapNode(testFile));
        assertEquals(List.of("levels", "episode_i", "negotiations", "negotiations_a"), resultingPath);

        var resultingPath2 = emptyStructure.getFolderFor(new ProjectStructure.FolderNode("negotiations", null));
        assertEquals(List.of("levels", "episode_i", "negotiations"), resultingPath2);
    }
}
