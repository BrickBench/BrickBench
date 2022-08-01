package com.opengg.loader.game.nu2.scene;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.opengg.core.console.GGConsole;
import com.opengg.core.Configuration;
import com.opengg.core.engine.Resource;
import com.opengg.core.render.texture.DDSLoader;
import com.opengg.core.render.texture.Texture;
import com.opengg.core.render.texture.TextureData;
import com.opengg.core.util.HashUtil;
import com.opengg.loader.ImageUtil;
import com.opengg.loader.SwingUtil;
import com.opengg.loader.Util;
import com.opengg.loader.editor.EditorState;
import com.opengg.loader.MapEntity;
import com.opengg.loader.game.nu2.scene.blocks.SceneExporter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public record FileTexture(String name, ByteBuffer contents, TextureData ggTexture, int textureAddress, int textureEnd, Descriptor descriptor,
                          CompletableFuture<ImageIcon> icon, CompletableFuture<Texture> nativeTexture) implements MapEntity<FileTexture> {

    public FileTexture(String name, ByteBuffer contents, int textureAddress, int textureEnd, Descriptor descriptor){
        this(name, contents, FileTextureCache.getCachedTextureData(contents, descriptor.trueIndex),
                textureAddress, textureEnd, descriptor, new CompletableFuture<>(), new CompletableFuture<>());

        FileTextureCache.submitIcon(contents, icon);
    }

    public record Descriptor(int address, int width, int height, int size, int trueIndex){}

    public void exportFunction() {
        exportFunction("texture");
    }

    public void exportFunction(String subDir){
        try {
            var file = Resource.getUserDataPath().resolve(Path.of("export", subDir, EditorState.getActiveMap().levelData().name() + "_" + String.format("%03d", descriptor.trueIndex) + ".dds"));
            Files.createDirectories(file.getParent());
            SceneExporter.exportTexture(this, file);
        } catch (IOException e) {
            SwingUtil.showErrorAlert("Failed while exporting texture", e);
        }
    }

    @Override
    public String path() {
        return "Render/Textures/" + name();
    }

    @Override
    public List<Property> properties() {
        return List.of(
                new EditorEntityProperty("Texture", this, true, false, null),
                new StringProperty("Dimensions",descriptor.width + "x" + descriptor.height + " px", false, 0));
    }

    @Override
    public Map<String, Runnable> getButtonActions() {
        return Map.of("Export", this::exportFunction);
    }

    public static class FileTextureCache {
        private static ExecutorService iconExecutor = Executors.newCachedThreadPool();
        private static final Cache<Long, ImageIcon> iconsByHash;
        private static final Cache<Long, TextureData> texturesByHash;

        static {
            iconsByHash = Caffeine.newBuilder()
                    .maximumSize(500)
                    .expireAfterWrite(15, TimeUnit.MINUTES)
                    .build();

            texturesByHash = Caffeine.newBuilder()
                    .maximumSize(500)
                    .expireAfterWrite(15, TimeUnit.MINUTES)
                    .build();
        }

        private static ImageIcon getIconFromBuffer(ByteBuffer contents) {
            byte[] array = new byte[contents.limit()];
            contents.get(array);
            contents.rewind();

            try(ByteArrayInputStream bis = new ByteArrayInputStream(array)){
                BufferedImage image = ImageIO.read(bis);
                return new ImageIcon(Util.getScaledImage(96, 96, image));
            } catch (IOException e) {
                if(Byte.toUnsignedInt(array[0x54]) == 0x74){
                    return new ImageIcon(Util.getScaledImage(96, 96, ImageUtil.fromABGR8888DDS(contents)));
                }
                GGConsole.warning("Failed icon load: " + e.getMessage());
                return null;
            }
        }

        public static void submitIcon(ByteBuffer contents, CompletableFuture<ImageIcon> icon) {
            iconExecutor.submit(() -> {
                if (Boolean.parseBoolean(Configuration.get("cache-textures"))) {
                    long hash = HashUtil.getMeowHash(contents);
                    icon.complete(iconsByHash.get(hash, h -> getIconFromBuffer(contents)));
                } else {
                    icon.complete(getIconFromBuffer(contents));
                }
            });
        }

        public static TextureData getCachedTextureData(ByteBuffer contents, int textureIndex){
            if (Boolean.parseBoolean(Configuration.get("cache-textures"))) {
                return texturesByHash.get(HashUtil.getMeowHash(contents), c -> DDSLoader.loadFromBuffer(contents, textureIndex + ".dds"));
            } else {
                return DDSLoader.loadFromBuffer(contents, textureIndex + ".dds");
            }
        }

        public static void haltIconLoader(){
            iconExecutor.shutdownNow();
        }

        public static void restartIconLoader(){
            haltIconLoader();
            iconExecutor = Executors.newFixedThreadPool(2);
        }
    }}

