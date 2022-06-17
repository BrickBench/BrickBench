package com.opengg.loader.loading;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import com.opengg.loader.*;
import com.opengg.loader.game.nu2.Area;
import com.opengg.core.console.GGConsole;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProjectXmlIO {
    public static Project readProjectFile(Path projectXml, Path projectFile) throws IOException {
        var module = new JacksonXmlModule();
        module.addDeserializer(Color.class, new ColorDeserializer());

        var xmlMapper = new XmlMapper(module);
        xmlMapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
        xmlMapper.registerModule(new Jdk8Module());

        try {
            var builder = xmlMapper.readValue(Files.readAllBytes(projectXml), ProjectBuilder.class);
            return builder.build(projectFile, projectXml);
        } catch (IOException e) {
            GGConsole.warning("Failed to load as new project format, trying pre-0.3.4");
            try {
                var builder = xmlMapper.readValue(Files.readAllBytes(projectXml), ProjectBuilderPre034.class);
                return builder.build(projectFile, projectXml);
            } catch (IOException e2) {
                throw e;
            }
        }

    }

    public static void writeProjectFile(Project project, Path target) throws IOException {
        Files.createDirectories(target.getParent());

        var module = new JacksonXmlModule();
        module.addSerializer(Color.class, new ColorSerializer());

        var xmlMapper = new XmlMapper(module);
        xmlMapper.registerModule(new ParameterNamesModule());
        xmlMapper.registerModule(new Jdk8Module());
        xmlMapper.enable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION);
        xmlMapper.enable(ToXmlGenerator.Feature.WRITE_XML_1_1);
        xmlMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);

        xmlMapper.writeValue(target.toFile(), new ProjectBuilder(project));
    }

    @JsonRootName("project")
    private static record ProjectBuilder(String name, String version,
                                         Project.GameVersion game,
                                         Project.Assets assets,
                                         ProjectStructure structure) {

        ProjectBuilder(Project project) {
            this(project.projectName(), BrickBench.VERSION.version(), project.game(), project.assets(), project.structure());
        }

        public Project build(Path source, Path projectFile) throws IOException {
            if (BrickBench.VERSION.compareTo(new Version(version)) < 0) {
                throw new RuntimeException("Current BrickBench version is older than the project version.");
            }

            for (var map : structure.getNodesOfType(MapXml.class)) {
                map.setMapFilesDirectory(projectFile.resolveSibling(map.name()));
            }

            for (int i = 0; i < assets.textures().size(); i++) {
                var tex = assets.textures().get(i);
                var icon = new ImageIcon(Util.getScaledImage(96, 96, ImageIO.read(projectFile.resolveSibling(tex.path()).toFile())));
                assets.textures().set(i, new Project.Assets.TextureDef(tex.name(), tex.path(), icon));
            }

            return new Project(true, game, name, projectFile, source, assets, structure);
        }
    }

    /**
     * Deserialization support for pre-BrickBench 0.3.4 projects.
     */
    @JsonRootName("project")
    private static record ProjectBuilderPre034(String name, String version,
                                         Project.GameVersion game,
                                         Project.Assets assets,
                                         List<ProjectResource> resources,
                                         List<MapXml> maps,
                                         List<Area> areas,
                                         ProjectStructure structure) {
        public Project build(Path source, Path projectFile) throws IOException {
            if (BrickBench.VERSION.compareTo(new Version(version)) < 0) {
                throw new RuntimeException("Current BrickBench version is older than the project version.");
            }

            for (var map : maps) {
                map.setMapFilesDirectory(projectFile.resolveSibling(map.name()));
            }

            for (int i = 0; i < assets.textures().size(); i++) {
                var tex = assets.textures().get(i);
                var icon = new ImageIcon(Util.getScaledImage(96, 96, ImageIO.read(projectFile.resolveSibling(tex.path()).toFile())));
                assets.textures().set(i, new Project.Assets.TextureDef(tex.name(), tex.path(), icon));
            }

            return new Project(true, game, name, projectFile, source, assets, structure);
        }
    }

    public static class ColorSerializer extends JsonSerializer<Color> {
        @Override
        public void serialize(Color value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeFieldName("argb");
            gen.writeString(Integer.toHexString(value.getRGB()));
            gen.writeEndObject();
        }
    }

    public static class ColorDeserializer extends JsonDeserializer<Color> {
        @Override
        public Color deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            TreeNode root = p.getCodec().readTree(p);
            TextNode rgba = (TextNode) root.get("argb");
            return new Color(Integer.parseUnsignedInt(rgba.textValue(), 16), true);
        }
    }
}
