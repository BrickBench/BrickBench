package com.opengg.loader;

import com.opengg.core.physics.collision.PhysicsRay;
import com.opengg.core.util.SystemUtil;
import com.opengg.loader.editor.tabs.ConsolePanel;
import com.opengg.loader.editor.windows.InitialProjectWindow;
import com.opengg.loader.editor.windows.ProjectCreationDialog;
import com.opengg.loader.game.nu2.scene.IABLComponent;
import com.opengg.loader.game.nu2.scene.ParallaxComponent;
import com.opengg.loader.game.nu2.scene.SceneFileLoader;
import com.opengg.loader.internal.X11;
import com.opengg.loader.loading.ProjectIO;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.opengg.core.ConfigFile;
import com.opengg.core.Configuration;
import com.opengg.core.GGInfo;
import com.opengg.core.console.DefaultLoggerOutputConsumer;
import com.opengg.core.console.GGConsole;
import com.opengg.core.console.Level;
import com.opengg.core.engine.*;
import com.opengg.core.io.ControlType;
import com.opengg.core.io.input.keyboard.KeyboardController;
import com.opengg.core.io.input.keyboard.KeyboardListener;
import com.opengg.core.io.input.mouse.MouseButton;
import com.opengg.core.io.input.mouse.MouseButtonListener;
import com.opengg.core.io.input.mouse.MouseController;
import com.opengg.core.io.input.mouse.MouseScrollChangeListener;
import com.opengg.core.math.Quaternionf;
import com.opengg.core.math.Vector3f;
import com.opengg.core.math.util.Tuple;
import com.opengg.core.physics.PhysicsEngine;
import com.opengg.core.render.ProjectionData;
import com.opengg.core.render.RenderEngine;
import com.opengg.core.render.internal.opengl.OpenGLRenderer;
import com.opengg.core.render.light.Light;
import com.opengg.core.render.shader.ShaderController;
import com.opengg.core.render.text.Font;
import com.opengg.core.render.texture.TextureManager;
import com.opengg.core.render.window.WindowController;
import com.opengg.core.render.window.WindowOptions;
import com.opengg.core.render.window.awt.window.GGCanvas;
import com.opengg.core.world.WorldEngine;
import com.opengg.core.world.components.Component;
import com.opengg.core.world.components.LightComponent;
import com.opengg.core.world.components.WorldObject;
import com.opengg.loader.Project.GameVersion;
import com.opengg.loader.components.*;
import com.opengg.loader.editor.windows.SplashScreen;
import com.opengg.loader.editor.*;
import com.opengg.loader.editor.components.JVectorField;
import com.opengg.loader.editor.hook.TCSHookManager;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.FileMaterial;
import com.opengg.loader.game.nu2.scene.FileTexture;
import com.opengg.loader.editor.hook.JNativeHookLibraryLocator;
import com.opengg.loader.loading.MapLoader;
import com.opengg.loader.loading.MapWriter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static com.opengg.core.io.input.keyboard.Key.*;

/**
 * An instance of BrickBench
 */
public class BrickBench extends GGApplication implements KeyboardListener, MouseButtonListener, MouseScrollChangeListener, NativeKeyListener {
    /**
     * The current editor version.
     */
    public static final Version VERSION = new Version("v0.3.4");
    public static final boolean DEVMODE = false;

    /**
     * Whether or not to add a pause at the start of the load.
     */
    public static final boolean RENDERDOC_PAUSE = false;

    /**
     * How many items to save in the "Recent Maps" list.
     */
    public static final int RECENT_SAVES = 8;

    public static Font WORLD_OBJECT_FONT;

    /**
     * The current BrickBench instance.
     */
    public static BrickBench CURRENT;
    private static SplashScreen splashScreen;

    /**
     * A lock held until the UI is initialized.
     */
    private static final Object initLock = new Object();

    /**
     * The command-line arguments used.
     */
    public static final Map<String, String> args = new HashMap<>();

    /**
     * The initial map to be loaded once BrickBench loads.
     */
    public static Path initialMap = null;
    public static List<Vector3f> pointsToView = new ArrayList<>();

    public PlayerView player;
    public Vector3f ingamePosition = new Vector3f();

    public NU2MapData currentThings;

    public MapInterface window;
    public JPanel openggCanvasRegion;

    private long lastLeftClick;

    private boolean nativeCtrlPressed = false;

    private static void testLoad() {
        try {
            Configuration.load(Path.of(System.getProperty("user.home"), "/Documents/BrickBench Files/config/editor.ini"));
            Configuration.load(Path.of(System.getProperty("user.home"), "/Documents/BrickBench Files/config/recent.ini"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        GGInfo.setUserDataDirectoryName("BrickBench Files");

        var sourceDir = Configuration.getConfigFile("editor.ini").getConfig("home");

        var levelSources = List.of("LEVELS/EPISODE_I", "LEVELS/EPISODE_II",
                "LEVELS/EPISODE_III", "LEVELS/EPISODE_IV", "LEVELS/EPISODE_V", "LEVELS/EPISODE_VI");

        var allLevels = levelSources.stream()
                .map(m -> sourceDir + "/" + m)
                .map(File::new)
                .flatMap(f -> Arrays.stream(Objects.requireNonNull(f.listFiles())))
                .filter(File::isDirectory)
                .flatMap(f -> Arrays.stream(Objects.requireNonNull(f.listFiles())))
                .filter(File::isDirectory)
                .map(File::toPath).toList();
/*
        for(var map : allLevels){
            try {
               // System.out.println("Loading " + map.toString());
                //var mapEnd = MapLoader.loadMap(map);
               // System.out.println(mapEnd.currentMapInstance().levelData().scene().correctlySizedMeshIndex().get());
               // if(mapEnd.currentMapInstance().levelData().scene().correctlySizedMeshIndex().get() == -1){
              //      System.out.println("WZOO");
                }
              //  mapEnd.currentMapInstance().dispose();
            } catch (Exception e) {
                System.out.println(map.toString() + " CRASHES: " + e.getMessage());
                e.printStackTrace();
            }
        }*/
    }

    public static void main(String... args) throws UnsupportedLookAndFeelException, InterruptedException, IOException {
        GGConsole.initialize();

        var parser = ArgumentParsers.newFor("BrickBench").addHelp(true)
                .build()
                .description("Map editor for Lego Star Wars")
                .version(VERSION.version());

        parser.addArgument("project")
                .metavar("project")
                .type(Arguments.fileType().verifyCanRead())
                .nargs("?")
                .help("The filename for the map to open");

        try {
            var result = parser.parseArgs(args);
            if (result.get("project") != null) {
                initialMap = ((File) result.get("project")).toPath().toAbsolutePath();
            }
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        if (SystemUtil.IS_WINDOWS && Files.exists(Path.of(System.getProperty("java.home")).resolveSibling("resources"))) { //Run modular windows release
            Resource.setApplicationDirectory(Path.of(System.getProperty("java.home")).getParent());
            JNativeHookLibraryLocator.setJNativeHookLocator();
        } else if (SystemUtil.IS_LINUX && Files.exists(Path.of(System.getProperty("java.home")).resolve("resources")))   { //Run modular linux release
            Resource.setApplicationDirectory(Path.of(System.getProperty("java.home")));
        } else { //Run IntelliJ release
            Resource.setApplicationDirectory(new File("").getAbsoluteFile().toPath());
        }

        String dataName;
        Path dataPath;

        if (SystemUtil.IS_WINDOWS) {
            dataName = "BrickBench Files";
            dataPath = Path.of(System.getProperty("user.home"), "Documents", "BrickBench Files");
        } else {
            dataName = "brickbench";
            dataPath = Path.of(System.getProperty("user.home"), ".local", "share", "brickbench");

            X11.Lib.XInitThreads();
        }

        var defaults = Map.ofEntries(
            Map.entry("sensitivity", "0.5"),
            Map.entry("laf", "Flat Dark"),
            Map.entry("fov", "90"),
            Map.entry("use-backup-lij-vao", "true"),
            Map.entry("autodelete-ai-pak", "true"),
            Map.entry("show-shadow-maps", "true"),
            Map.entry("emulate-zbuffer", "true"),
            Map.entry("show-vertex-color", "true"),
            Map.entry("show-vertex-transparency", "true"),
            Map.entry("emulate-alpha", "true"),
            Map.entry("show-dynamic-lights", "true"),
            Map.entry("use-rotation-platform", "true"),
            Map.entry("show-lights", "true"),
            Map.entry("cache-textures", "true"),
            Map.entry("enhanced-graphics", "true"),
            Map.entry(GameVersion.LSW_TCS.SHORT_NAME + "-hook-executable-name", GameVersion.LSW_TCS.EXECUTABLE),
            Map.entry(GameVersion.LIJ1.SHORT_NAME + "-hook-executable-name", GameVersion.LIJ1.EXECUTABLE)
        );

        try {
            Configuration.load(Path.of(dataPath.toString() , "config" , "editor.ini"));
            Configuration.load(Path.of(dataPath.toString() , "config", "recent.ini"));
            
        } catch (IOException e) {
            ConfigFile file = new ConfigFile("editor.ini", new LinkedHashMap<>());
            Configuration.addConfigFile(file);
            Configuration.addConfigFile(new ConfigFile("recent.ini", new LinkedHashMap<>()));
        }

        for (var entry : defaults.entrySet()) {
            if (Configuration.getConfigFile("editor.ini").getConfig(entry.getKey()).isEmpty()) {
                Configuration.getConfigFile("editor.ini").writeConfig(entry.getKey(), entry.getValue());
            }
        }

        Files.createDirectories(dataPath.resolve("export"));
        Files.createDirectories(dataPath.resolve("export/meshes"));
        Files.createDirectories(dataPath.resolve("config"));

        EditorTheme.applyTheme();

        CURRENT = new BrickBench();
        Thread ui = new Thread(CURRENT::initSwing, "UI Thread");
        ui.start();

        synchronized (initLock) {
            try {
                initLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        GGCanvas.container = CURRENT.openggCanvasRegion;

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (RENDERDOC_PAUSE) {
            Thread.sleep(5000);
        }

        var close = OpenGG.initialize(CURRENT, new InitializationOptions()
                .setApplicationName("BrickBench")
                .setUserDataDirectory(dataName)
                .setRedirectStandardIO(true)
                .setConfigInUserData(true)
                .setInitializeSound(false)
                .setWindowOptions(new WindowOptions()
                        .setType("AWT")
                        .setName("BrickBench")
                        .setWidth(1366)
                        .setHeight(720)
                        .setResizable(true)
                        .setRenderer(WindowOptions.RendererType.OPENGL)
                        .setVsync(true)));

        var code = 0;
        if (close == OpenGG.EngineCloseType.ERROR) {
            SwingUtil.showErrorAlert("BrickBench has closed abnormally. Please view the logs and report this to the developers.");
            if (splashScreen.isDisplayable()) {
                splashScreen.dispose();
            }

            CURRENT.exit();
            code = 1;
        }
        
        if (SystemUtils.IS_OS_LINUX) {
            SwingUtilities.invokeLater(() -> {
                CURRENT.window.dispose();
            });
        } else {
            System.exit(0);
        }
    }

    public void showInitialWindow() {
        Consumer<String> runner = s -> {
            window.setVisible(true);
            window.requestFocus();

            OpenGG.asyncExec(() -> {
                if (s.equals("NEW")) {
                    new ProjectCreationDialog();
                } else if (s.equals("LOAD")) {
                    openProjectChooser();
                } else if (s.equals("CLOSE")) {
                    exit();
                } else if (s.equals("HOOK")) {
                } else {
                    loadNewProject(Path.of(s));
                }
            });
        };

        new InitialProjectWindow(runner);
    }

    public void initSwing() {
        splashScreen = new SplashScreen();

        openggCanvasRegion = new JPanel(new BorderLayout());
        openggCanvasRegion.setTransferHandler(new MapLoadFileHandler());

        window = new MapInterface();

        setupConsole();

        window.setVisible(true);
        window.setLocation(10000, 10000);

        synchronized (initLock) {
            initLock.notifyAll();
        }

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
    }

    private void setupConsole() {
        GGConsole.addOutputConsumer(new DefaultLoggerOutputConsumer(Level.DEBUG, s -> ConsolePanel.consoleText.append(s + "\n")));
    }

    /**
     * Opens a file chooser to load a map/project, and loads the map/project that results.
     */
    public void openProjectChooser() {
        FileUtil.openFileDialog(Configuration.getConfigFile("editor.ini").getConfig("home"), FileUtil.LoadType.BOTH, "BrickBench Project/Map", "brickbench", "xml", "gsc", "ter")
                .ifPresent(f -> OpenGG.asyncExec(() -> loadNewProject(f)));
    }

    /**
     * Applies config file data to the OpenGG engine shaders and BrickBench UI
     */
    public void reloadConfigFileData() {
        TextBillboardComponent.SHOW = Boolean.parseBoolean(Configuration.get("show-object-titles"));
        IABLComponent.SHOW = Boolean.parseBoolean(Configuration.get("show-specobj-bounds"));
        FileMaterial.ENABLE_ALPHA_EMULATION = Boolean.parseBoolean(Configuration.get("emulate-alpha"));
        FileMaterial.ENABLE_DEPTH_EMULATION = Boolean.parseBoolean(Configuration.get("emulate-zbuffer"));
        ParallaxComponent.ENABLE_PARALLAX = Boolean.parseBoolean(Configuration.get("emulate-skybox"));
        
        if (Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig("compact-mode"))) {
            window.left.setVisible(false);
            window.right.setVisible(false);
            window.bottomTabs.setVisible(false);

        } else {
            window.left.setVisible(true);
            window.right.setVisible(true);
            window.bottomTabs.setVisible(true);
        }

        EditorState.CURRENT.shouldHighlight = Boolean.parseBoolean(Configuration.get("highlight-selected"));
        window.validate();

        OpenGG.asyncExec(() -> {
            ShaderController.setUniform("globalEnhancedGraphics", Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig("enhanced-graphics")));
            ShaderController.setUniform("globalApplyLights", Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig("show-lights")));
            ShaderController.setUniform("globalUseLightmaps", Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig("show-shadow-maps")));
            ShaderController.setUniform("globalUseDynamicLights", Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig("show-dynamic-lights")));
            ShaderController.setUniform("globalUseMeshTransparency", Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig("show-vertex-transparency")));
            ShaderController.setUniform("globalUseVertexColor", Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig("show-vertex-color")));
            ShaderController.setUniform("globalForceLightmapUsage", Boolean.parseBoolean(Configuration.getConfigFile("editor.ini").getConfig("force-shadow")));
        });
    }

    @Override
    public void setup() {
        reloadConfigFileData();
        NativeCache.initialize();

        if (Boolean.parseBoolean(Configuration.get("discord-integration"))) {
            DiscordManager.startDiscord();
        }

        if (DEVMODE) {
            ClojureDebugger.startDebugRepl();
        }

        PhysicsEngine.setEnabled(false);

        WORLD_OBJECT_FONT = Resource.getTruetypeFont("consolas.ttf");
        KeyboardController.addKeyboardListener(this);
        MouseController.onButtonPress(this);
        MouseController.addScrollChangeListener(this);

        WorldEngine.getCurrent().attach(new WorldObject("mainView"));
        WorldEngine.getCurrent().attach(new WorldObject("meshView").setEnabled(false));


        player = new PlayerView();
        player.setName("player");
        WorldEngine.getCurrent().attach(player);
        WorldEngine.getCurrent().attach(new LightComponent(Light.createDirectional(Quaternionf.createXYZ(new Vector3f(0, 0, -0.5f)), new Vector3f(1, 1, 1))));
        WorldEngine.getCurrent().attach(TCSHookManager.enemyManager = new HookCharacterManager());

        BindController.addBind(ControlType.KEYBOARD, "forward", KEY_W);
        BindController.addBind(ControlType.KEYBOARD, "backward", KEY_S);
        BindController.addBind(ControlType.KEYBOARD, "left", KEY_A);
        BindController.addBind(ControlType.KEYBOARD, "right", KEY_D);
        BindController.addBind(ControlType.KEYBOARD, "up", KEY_SPACE);
        BindController.addBind(ControlType.KEYBOARD, "down", KEY_LEFT_SHIFT);

        RenderEngine.setProjectionData(ProjectionData.getPerspective(110, 0.2f, 3000f));

        ShaderController.use("wiiobject.vert", "wiiobject.frag");
        ShaderController.saveCurrentConfiguration("ttNormal");

        ShaderController.use("reverse.vert", "object.frag");
        ShaderController.saveCurrentConfiguration("xFixOnly");

        ShaderController.use("collision.vert", "barycentric.geom", "collision.frag");
        ShaderController.saveCurrentConfiguration("collision");

        ShaderController.use("noprocess.vert", "infwall.geom", "infwall.frag");
        ShaderController.saveCurrentConfiguration("infwall");

        player.setUsingMouse(false);

        splashScreen.dispose();
        window.topBar.refreshValues();

        window.setVisible(false);
        window.pack();
        window.setLocation(0, 0);
        window.setExtendedState(JFrame.MAXIMIZED_BOTH);

        if (Files.exists(Resource.getUserDataPath().resolve("THINGS_PC.GSC"))) {
            try {
                this.currentThings = MapLoader.loadThings();
            } catch (IOException e) {
                GGConsole.error("Failed to load THINGS: " + e.getMessage());
            }
        }

        if (initialMap != null) {
            loadNewProject(initialMap);
            window.setVisible(true);
            window.requestFocus();
        } else {
            showInitialWindow();
        }

        if (SystemUtil.IS_WINDOWS) {
            var updateThread = new Thread(Updater::checkForUpdates);
            updateThread.setDaemon(true);
            updateThread.start();
        }
    }

    /**
     * Loads a new project from a BrickBench project archive or loose map/scene
     *
     * @param projectFile
     * @return
     */
    public boolean loadNewProject(Path projectFile) {
        return this.loadNewProject(projectFile, true);
    }

    /**
     * Shows a prompt to the user asking if they would like to save the current project before continuing.
     * @return True if the user decided to either save or not save the current project, and false if the user decided to cancel the current operation.
     */
    public boolean showSaveProjectPrompt() {
        if (EditorState.getProject() != null && EditorState.getProject().isProject()) {
            var save = JOptionPane.showConfirmDialog(window, "Would you like to save and close your current open project?", "Save?", JOptionPane.YES_NO_CANCEL_OPTION);
            if (save == 0) {
                ProjectIO.saveProject(EditorState.getProject(), EditorState.getProject().projectSource());
                return true;
            } else if (save == 1) {
                return true;
            } else if (save == 2) {
                return false;
            }
        }
        return true;
    }

    /**
     * Loads a new project from a BrickBench project archive or loose map/scene
     *
     */
    public boolean loadNewProject(Path projectFile, boolean saveToRecents) {
        if (!showSaveProjectPrompt()) return false;

        pointsToView.clear();
        TextureManager.clearCache();

        WorldEngine.findEverywhereByName("map").forEach(Component::delete);
        EditorState.closeActiveMap();

        Project project;
        try {
            if (FilenameUtils.getExtension(projectFile.toString()).equals("brickbench")) {
                project = ProjectIO.loadProject(projectFile);
            } else {
                project = ProjectIO.createReadOnlyProject(projectFile);
            }
        } catch (Exception e) {
            SwingUtil.showErrorAlert("Failed to load map/project at " + projectFile, e);
            return false;
        }

        if (project == null) return false;

        if (saveToRecents)
            pushRecentMapToList(projectFile);

        return useProject(project);
    }

    /**
     * Use a new loaded Project
     *
     * @param project
     * @return
     */
    public boolean useProject(Project project) {
        EditorState.resetEditorState();
        EditorState.updateProject(project);
        MapXml map = null;

        if (EditorState.getProject().maps().isEmpty()) {
            GGConsole.log("No maps found in project");
        } else {
            map = EditorState.getProject().maps().get(0);
            GGConsole.log("Opening project with map " + map.name());

        }

        return useMapFromCurrentProject(map);
    }

    /**
     * Use a project map from the current project
     *
     * @param map
     * @return
     */
    public boolean useMapFromCurrentProject(MapXml map) {
        var newInstance = EditorState.addAndActivateProjectMap(map);

        if (EditorState.getProject().isProject()) {
            if (newInstance != null) {
                window.setTitle("BrickBench - " + EditorState.getProject().projectName() + " - " + map.name());
            } else {
                window.setTitle("BrickBench - " + EditorState.getProject().projectName());
            }
        } else if (newInstance != null) {
            window.setTitle("BrickBench - " + EditorState.getProject().projectName() + " [Read only]");
        } else {
            window.setTitle("BrickBench");
        }

        if (newInstance == null) {
            return true;
        } else {
            GGConsole.log("Loaded " + EditorState.getActiveMap().levelData().name());
        }

        EditorState.CURRENT.onMapReloadListeners.forEach(o -> o.accept(EditorState.getProject()));

        return true;
    }

    private void pushRecentMapToList(Path map) {
        var recentFile = Configuration.getConfigFile("recent.ini");

        if (recentFile.getConfig("recent_0").equals(map.toString())) return;
        int startPoint = IntStream.range(0, RECENT_SAVES)
                .filter(i -> recentFile.getConfig("recent_" + i).equals(map.toString()))
                .findFirst().orElse(RECENT_SAVES);

        for (var i = startPoint - 1; i >= 0; i--) {
            var entry = recentFile.getConfig("recent_" + i);
            recentFile.writeConfig("recent_" + (i + 1), entry);
        }

        recentFile.writeConfig("recent_0", map.toString());
        Configuration.writeFile(recentFile);
    }

    public NU2MapData getNU2Things() {
        if (EditorState.getProject() != null) {
            var things = EditorState.getProject().structure().getNodeFromPath("STUFF/THINGS");
            if (things instanceof MapXml map) {
                try {
                    var instance = EditorState.getMapFromName("THINGS");
                    if (instance == null) {
                        EditorState.addProjectMap(map);
                        instance = EditorState.getMapFromName("THINGS");
                        SceneFileLoader.initializeGraphicsData((NU2MapData) instance.levelData());
                    }

                    return (NU2MapData) instance.levelData();
                } catch (IOException e) {
                    GGConsole.warning("Failed to load map THINGS_PC: " + e.getMessage());
                    return currentThings;
                }
            } else {
                return currentThings;
            }
        } else {
            return currentThings;
        }
    }

    public void importThings() {
        FileUtil.openFileDialog(Configuration.getConfigFile("editor.ini").getConfig("home"), FileUtil.LoadType.FILE, "THINGS_PC.GSC copy")
                .ifPresent(v -> OpenGG.asyncExec(() -> {
                    try {
                        Files.copy(v, Resource.getUserDataPath().resolve("THINGS_PC.GSC"), StandardCopyOption.REPLACE_EXISTING);
                        currentThings = MapLoader.loadThings();
                    } catch (IOException e) {
                        SwingUtil.showErrorAlert("Failed to copy THINGS to work directory", e);
                    }
                }));
    }

    private void takeScreenshot() {
        OpenGG.asyncExec(() -> {
            var buffer = OpenGLRenderer.getOpenGLRenderer().getLastPassContents();
            var width = WindowController.getWidth();
            var height = WindowController.getHeight();

            var arrayBuffer = new byte[width * height * 4];
            buffer.get(arrayBuffer);

            var dbuffer = new DataBufferByte(arrayBuffer, arrayBuffer.length);
            var raster = Raster.createInterleavedRaster(dbuffer, width, height, 4 * width, 4, new int[]{0, 1, 2, 3}, null);

            var colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
            var image = new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);

            var at = new AffineTransform();
            at.concatenate(AffineTransform.getScaleInstance(1, -1));
            at.concatenate(AffineTransform.getTranslateInstance(0, -image.getHeight()));

            var newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            var g = newImage.createGraphics();
            g.transform(at);
            g.drawImage(image, 0, 0, null);
            g.dispose();

            try {
                var date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
                var sdf = new SimpleDateFormat("yyyy.MM.dd a hh.mm.ss");
                var formattedDate = sdf.format(date);
                var filename = "BrickBench Screenshot " + formattedDate + ".png";
                var file = Resource.getUserDataPath().resolve("screenshot").resolve(filename);

                Resource.getUserDataPath().resolve("screenshot").toFile().mkdirs();
                ImageIO.write(newImage, "PNG", file.toFile());
                JOptionPane.showMessageDialog(this.window, "Saved screenshot to " + file);
            } catch (IOException e) {
                SwingUtil.showErrorAlert("Failed to save screenshot", e);
            }
        });
    }

    @Override
    public void render() {
        RenderEngine.setProjectionData(ProjectionData.getPerspective(Configuration.getFloat("fov"), 0.2f, 3000f));
    }

    boolean recorded = false;
    List<Tuple<Vector3f, Vector3f>> recordedList = new ArrayList<>();

    @Override
    public void update(float delta) {
        MapWriter.applyChangesToMapState();
        TCSHookManager.update();
        DiscordManager.update();

        ingamePosition = player.getPosition().multiply(-1, 1, 1);

        if (recorded) {
            var pos = ingamePosition;
            var dir = player.getRotation().toEuler().multiply(-1, 1, 1);
            recordedList.add(Tuple.of(pos, dir));
        }
    }

    boolean exited = false;
    public void exit() {
        if (exited) return;
        if (!showSaveProjectPrompt()) return;

        GGConsole.log("Writing config file on exit");
        FileTexture.FileTextureCache.haltIconLoader();
        Configuration.writeFile(Configuration.getConfigFile("editor.ini"));
        
        OpenGG.endApplication();
        exited = true;
    }

    public void cleanGameDirectories() {
        var clean = JOptionPane.showOptionDialog(window, "" +
                        """
                                Clearing the caches and game files will remove the reference game copies you've imported.
                                This can help if you have imported a non-clean game on accident.
                                Do you want to save your work and continue? BrickBench will exit after this operation.
                                  """,
                "Delete copied game files?",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{"Yes", "No"}, "yes");

        if (clean == 0) {
            try (var exit = SwingUtil.showLoadingAlert("Clearing...", "Clearing cached files...", false)) {
                if (EditorState.getProject() != null && EditorState.getProject().isProject()) {
                    ProjectIO.saveProject(EditorState.getProject(), EditorState.getProject().projectSource());
                }

                EditorState.closeActiveMap();

                var root = Configuration.getConfigFile("editor.ini").getConfig("lswtcs-clean-game-root");
                if (root.isEmpty()) {
                    var gamePath = Resource.getUserDataPath().resolve(Path.of("games", "lswtcs"));
                    FileUtils.deleteDirectory(gamePath.toFile());
                }

                var projectDirectory = Resource.getUserDataPath().resolve("project");
                FileUtils.deleteDirectory(projectDirectory.toFile());

                var testOutput = Resource.getUserDataPath().resolve("test");
                FileUtils.deleteDirectory(testOutput.toFile());


                JOptionPane.showMessageDialog(window, "Cleaning was successful, BrickBench will now close.");

                exit();
            } catch (IOException e) {
                SwingUtil.showErrorAlert("Failed to finish the game cleaning operation", e);
            }
        }
    }

    @Override
    public void onScrollUp() {
        player.setSpeed(Math.max(0, player.getSpeed() - 0.1f));
    }

    @Override
    public void onScrollDown() {
        player.setSpeed(Math.max(0, player.getSpeed() + 0.1f));
    }

    @Override
    public void keyPressed(int key) {
        if (key == KEY_F2) {
            takeScreenshot();
        }
        if (key == KEY_T && TCSHookManager.currentHook != null) {
            if (KeyboardController.isKeyPressed(KEY_LEFT_CONTROL)) {
                TCSHookManager.currentHook.teleportToPosition(2, ingamePosition);
            } else {
                TCSHookManager.currentHook.teleportToPosition(1, ingamePosition);
            }
        }
        if (key == KEY_ENTER) {
            player.setUsingMouse(!player.isUsingMouse());
            WindowController.getWindow().setCursorLock(player.isUsingMouse());
        }
        if (key == KEY_R) {
            player.setPositionOffset(0, player.getPosition().y, 0);
            player.setAbsoluteRotation(true).setRotationOffset(new Vector3f(0, -90, 0));
        }
        if (key >= KEY_0 && key <= KEY_9) {
            int index = key == KEY_0 ? 9 : key - KEY_1;
            if (KeyboardController.isKeyPressed(KEY_LEFT_CONTROL)) index += 10;
            var toggleName = switch (index) {
                case 0 -> "AI";
                case 1 -> "Doors";
                case 2 -> "Gizmo";
                case 3 -> "Render";
                case 4 -> "Splines";
                case 5 -> "Terrain";
                default -> "";
            };

            EditorState.setNodeVisibility(toggleName, !EditorState.isNodeVisible(toggleName));
        }

        if (key == KEY_G) {
            JVectorField vectorField = new JVectorField();
            Object[] goToPrompt = new Object[]{
                    "Position:", vectorField
            };

            int option = JOptionPane.showConfirmDialog(window, goToPrompt, "Go to where?", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                WorldEngine.findEverywhereByName("player").get(0)
                        .setPositionOffset(vectorField.getValue());
            }
        }

        if (key == KEY_V) {
            JFormattedTextField vInput = new JFormattedTextField(0.0f);
            Object[] goToPrompt = new Object[]{
                    "New velocity: ", vInput
            };

            int option = JOptionPane.showConfirmDialog(window, goToPrompt, "Set velocity", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                player.setSpeed(Float.parseFloat(vInput.getText()));
            }
        }

        if (key == KEY_LEFT_CONTROL && MouseController.isButtonDown(MOUSE_BUTTON_LEFT)) {
            player.setUsingMouse(true);
            WindowController.getWindow().setCursorLock(true);
        }

        if (key == KEY_MINUS) {
            player.setSpeed(Math.max(0, player.getSpeed() - 4f));
        }

        if (key == KEY_EQUAL) {
            player.setSpeed(Math.max(0, player.getSpeed() + 4f));
        }

        if (key == KEY_F12 || key == KEY_BACKSLASH) {
            TCSHookManager.panel.loadCurrentMap();
        }
    }

    @Override
    public void keyReleased(int key) {
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {

    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent nativeEvent) {
        if (nativeEvent.getKeyCode() == NativeKeyEvent.VC_CONTROL) {
            nativeCtrlPressed = true;
        }

        if (nativeEvent.getKeyCode() == NativeKeyEvent.VC_T && TCSHookManager.currentHook != null) {
            if (nativeCtrlPressed) {
                TCSHookManager.currentHook.teleportToPosition(2, ingamePosition);
            } else {
                TCSHookManager.currentHook.teleportToPosition(1, ingamePosition);
            }
        }

        if ((nativeEvent.getKeyCode() == NativeKeyEvent.VC_F12 || nativeEvent.getKeyCode() == NativeKeyEvent.VC_BACK_SLASH) && TCSHookManager.currentHook != null) {
            TCSHookManager.panel.loadCurrentMap();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
        if (nativeEvent.getKeyCode() == NativeKeyEvent.VC_CONTROL) {
            nativeCtrlPressed = false;
        }
    }

    @Override
    public void onButtonPress(int button) {
        lastLeftClick = System.currentTimeMillis();

        if (button == MouseButton.MIDDLE || button == MouseButton.RIGHT || (button == MouseButton.LEFT && EditorState.CURRENT.selectionMode == MapInterface.SelectionMode.PAN)) {
            player.setUsingMouse(true);
            WindowController.getWindow().setCursorLock(true);
        } else if (button == MouseButton.LEFT) {
            var ray = MouseController.getRay();
        }
    }

    @Override
    public void onButtonRelease(int button) {
        if (player.isUsingMouse()) {
            player.setUsingMouse(false);
            WindowController.getWindow().setCursorLock(false);
        }

        if (button == MouseButton.LEFT) {
            if (System.currentTimeMillis() - lastLeftClick < 200) {
                var ray = MouseController.getRay();

                var oldAngleQuat = Quaternionf.createYXZ(ray.dir());
                var newAngleQuat = new Quaternionf(oldAngleQuat.w, oldAngleQuat.x, -oldAngleQuat.y, -oldAngleQuat.z);

                var adjustedRay = new PhysicsRay(newAngleQuat.toEuler(), ray.pos().multiply(-1, 1, 1), ray.length());

                window.onMouseRelease(adjustedRay, true);
            }
        }
    }


    private class MapLoadFileHandler extends TransferHandler {
        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }

        public boolean canImport(TransferSupport ts) {
            return ts.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        public boolean importData(TransferSupport ts) {
            try {
                @SuppressWarnings("rawtypes")
                List data = (List) ts.getTransferable().getTransferData(
                        DataFlavor.javaFileListFlavor);
                if (data.size() < 1) {
                    return false;
                }

                OpenGG.asyncExec(() -> loadNewProject(((File) data.get(0)).toPath()));

                return true;
            } catch (IOException | UnsupportedFlavorException e) {
                return false;
            }
        }
    }
}
