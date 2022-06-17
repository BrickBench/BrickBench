package com.opengg.loader.test.loading;

import com.opengg.loader.MapXml;
import com.opengg.loader.ProjectStructure;
import com.opengg.loader.loading.ProjectIO;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectIOTest {

    @Test
    void testDesiredPath() {
        var goodFile = Path.of("LEVELS\\EPISODE_I\\NEGOTIATIONS\\NEGOTIATIONS_A");
        assertArrayEquals(new String[]{"levels", "episode_i", "negotiations", "negotiations_a"}, ProjectIO.parseDesiredDirectory(goodFile));

        var oddCapitalization = Path.of("levels\\EPISODE_I\\negotiations\\NEGOTIATIONS_A");
        assertArrayEquals(new String[]{"levels", "episode_i", "negotiations", "negotiations_a"}, ProjectIO.parseDesiredDirectory(oddCapitalization));

        var oddRoot = Path.of("LEVELS\\EPISODE_I\\NEGOTIATIONS\\NEGOTIATIONS_A");
        assertArrayEquals(new String[]{"levels", "episode_i", "negotiations", "negotiations_a"}, ProjectIO.parseDesiredDirectory(oddRoot));

        var things = Path.of("STUFF\\THINGS_PC.GSC");
        assertArrayEquals(new String[]{"stuff", "things_pc.gsc"}, ProjectIO.parseDesiredDirectory(things));

        var detectLoose = Path.of("LEVELS/NEGOTIATIONS/NEGOTIATIONS_A");
        assertArrayEquals(new String[]{"levels", "negotiations", "negotiations_a"}, ProjectIO.parseDesiredDirectory(detectLoose));
    }

    @Test
    void testMapLocalPathDeduction() {
        var goodFile = Path.of("C:\\test\\Lego Star Wars Saga\\LEVELS\\EPISODE_I\\NEGOTIATIONS\\NEGOTIATIONS_A");
        assertEquals(Path.of("levels", "episode_i", "negotiations", "negotiations_a"), ProjectIO.getLocalPathFromGameRoot(goodFile));

        var oddCapitalization = Path.of("C:\\test\\Lego Star Wars Saga\\levels\\EPISODE_I\\negotiations\\NEGOTIATIONS_A");
        assertEquals(Path.of("levels", "episode_i", "negotiations", "negotiations_a"), ProjectIO.getLocalPathFromGameRoot(oddCapitalization));

        var oddRoot = Path.of("C:\\test\\LegoGaming\\LEVELS\\EPISODE_I\\NEGOTIATIONS\\NEGOTIATIONS_A");
        assertEquals(Path.of("levels", "episode_i", "negotiations", "negotiations_a"), ProjectIO.getLocalPathFromGameRoot(oddRoot));

        var things = Path.of("C:\\test\\LegoGaming\\STUFF\\THINGS_PC.GSC");
        assertEquals(Path.of("stuff", "things_pc.gsc"), ProjectIO.getLocalPathFromGameRoot(things));

        var detectLoose = Path.of("C:\\test\\EPISODE_I\\NEGOTIATIONS\\NEGOTIATIONS_A");
        assertEquals(Path.of("negotiations_a"), ProjectIO.getLocalPathFromGameRoot(detectLoose));
    }

    @Test
    void testMapAddToStructure(){
        var emptyStructure = new ProjectStructure(new ProjectStructure.FolderNode("root", new ArrayList<>()));
        var testFile = new MapXml("test", null, MapXml.MapType.NORMAL, List.of(), Map.of(), Map.of());
        var goodPath = Path.of("C:\\test\\Lego Star Wars Saga\\LEVELS\\EPISODE_I\\NEGOTIATIONS\\NEGOTIATIONS_A");

        ProjectIO.addNodeStructureFromPath(emptyStructure, ProjectIO.getLocalPathFromGameRoot(goodPath));
        assertEquals(1, emptyStructure.root().children().size());
        assertEquals("levels", emptyStructure.root().children().get(0).name());
        assertTrue( emptyStructure.root().children().get(0) instanceof ProjectStructure.FolderNode fn);

        var levelsNode = (ProjectStructure.FolderNode) emptyStructure.root().children().get(0);
        assertEquals(1, levelsNode.children().size());
        assertEquals("episode_i", levelsNode.children().get(0).name());
        assertTrue( levelsNode.children().get(0) instanceof ProjectStructure.FolderNode fn);

        var episodeNode = (ProjectStructure.FolderNode) levelsNode.children().get(0);
        assertEquals(1, episodeNode.children().size());
        assertEquals("negotiations", episodeNode.children().get(0).name());
        assertTrue( episodeNode.children().get(0) instanceof ProjectStructure.FolderNode fn);

        var levelNode = (ProjectStructure.FolderNode) episodeNode.children().get(0);
        assertEquals(1, levelNode.children().size());
        assertEquals("negotiations_a", levelNode.children().get(0).name());
        assertTrue(levelNode.children().get(0) instanceof ProjectStructure.FolderNode fn);

        var emptyStructure2 = new ProjectStructure(new ProjectStructure.FolderNode("root", new ArrayList<>()));
        var badPath = Path.of("C:\\test\\LevelRepo\\EPISODE_I\\NEGOTIATIONS\\NEGOTIATIONS_A");

        ProjectIO.addNodeStructureFromPath(emptyStructure2, ProjectIO.getLocalPathFromGameRoot(badPath));
        assertEquals(1, emptyStructure2.root().children().size());
        assertEquals("negotiations_a", emptyStructure2.root().children().get(0).name());
        assertTrue( emptyStructure2.root().children().get(0) instanceof ProjectStructure.FolderNode fn);


    }
}
