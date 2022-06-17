package com.opengg.loader.test.loading;

import com.opengg.loader.MapXml;
import com.opengg.loader.ProjectStructure;
import com.opengg.loader.loading.MapIO;
import com.opengg.loader.loading.MapXmlIO;
import com.opengg.loader.loading.ProjectIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapXmlIOTest {

    @Test
    void testEqualityAfterSaveLoadSave(@TempDir Path dir) throws IOException {
        var file = dir.resolve("test.xml");
        var file2 = dir.resolve("test2.xml");

        var assocFiles = List.of(new MapXml.ReferenceFileDef(Path.of("test.gsc"), "gsc"),
                new MapXml.ReferenceFileDef(Path.of("test.ter"), "ter"));
        MapXml xmlData = new MapXml("test", file, MapXml.MapType.NORMAL, assocFiles, Map.of(), Map.of());

        MapXmlIO.saveMapXml(xmlData, file);
        var newXmlData = MapXmlIO.loadMapXml(file);
        MapXmlIO.saveMapXml(newXmlData, file2);

        assertEquals(Files.readString(file), Files.readString(file2));
    }

    @Test
    void testMapLocalPath(){
        var emptyStructure = new ProjectStructure(new ProjectStructure.FolderNode("test", new ArrayList<>()));
        var testFile = new MapXml("test_intro", null, MapXml.MapType.INTRO, List.of(), Map.of(), Map.of());
        var goodPath = Path.of("C:\\test\\Lego Star Wars Saga\\LEVELS\\EPISODE_I\\NEGOTIATIONS\\NEGOTIATIONS_INTRO");

        var node = ProjectIO.addNodeStructureFromPath(emptyStructure, goodPath);

        ((ProjectStructure.FolderNode)node).children().add(new ProjectStructure.MapNode(testFile));

        var resultingPath = MapIO.generateRelativePathForFile(emptyStructure, testFile, Path.of("TEST_A_PC.gsc"));
        assertEquals(Path.of("levels", "episode_i", "negotiations", "negotiations_intro", "test_a_PC.gsc"), resultingPath);

        var resultingPath2 = MapIO.generateRelativePathForFile(emptyStructure, testFile, Path.of("ai", "TEST_A.ai2"));
        assertEquals(Path.of("levels", "episode_i", "negotiations", "negotiations_intro", "ai", "test_a.ai2"), resultingPath2);
    }
}
