package com.opengg.loader.editor.hook;

import com.opengg.core.Configuration;
import com.opengg.core.math.Vector3f;
import com.opengg.core.util.SystemUtil;
import com.opengg.loader.BrickBench;
import com.opengg.loader.SwingUtil;
import com.opengg.loader.Project.GameVersion;
import com.opengg.loader.components.HookCharacterManager;
import com.opengg.loader.editor.EditorState;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TCSHookManager {
    public static TCSHookCommunicator currentHook;
    public static HookCharacterManager enemyManager;
    public static TCSHookPanel panel;

    public static List<HookCharacter> allCharacters = new ArrayList<>();
    public static HookCharacter playerOne;
    public static HookCharacter playerTwo;

    public static boolean isEnabled(){
        return currentHook != null;
    }

    public static void beginHook(GameExecutable executable){
        if (SystemUtil.IS_LINUX) {
            SwingUtil.showErrorAlert("This feature is currently not available on Linux");
            return;
        }

        var hook = new TCSHookCommunicator(executable);
        var success = hook.attemptHook();

        if (success) {
            currentHook = hook;
            panel.updateConnectionUIState();
            JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "Connected to process.");
        } else {
            JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "Failed to find a running game instance.");
        }
    }

    public static void endHook(){
        if (currentHook != null)
            currentHook.close();

        currentHook = null;
        panel.updateConnectionUIState();
        JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "Disconnected from process.");
    }

    public static void update(){
        if (currentHook != null) {
            allCharacters.clear();
            var active = currentHook.checkForValidityAndReaquirePointers();
            if (!active) {
                currentHook.close();
                currentHook = null;
                panel.updateConnectionUIState();
                JOptionPane.showMessageDialog(BrickBench.CURRENT.window, "Lost connection to process.");

            } else {
                playerOne = new HookCharacter(currentHook.readPlayerLocation(1), 180 - currentHook.readPlayerAngle(1), 0);
                playerTwo = new HookCharacter(currentHook.readPlayerLocation(2), 180 - currentHook.readPlayerAngle(2), 0);

                allCharacters = currentHook.getAllCharacters();

                if (Configuration.getBoolean("autoload-hook")) {
                    var currentHookMap = currentHook.getCurrentMap();
                    var currentMap = EditorState.getActiveMap();
                    try{
                        var newMap = Path.of(currentHook.getDirectory().toString(), currentHookMap);

                        if (Files.exists(newMap) &&
                                (currentMap == null || !currentMap.levelData().xmlData().mapFilesDirectory().toString().toLowerCase().contains(currentHookMap.toLowerCase()))) {
                            var success = BrickBench.CURRENT.loadNewProject(newMap, false);
                            if (!success) Configuration.set("autoload-hook", "false");
                            BrickBench.CURRENT.player.setPositionOffset(playerOne.pos().multiply(-1, 1, 1).add(new Vector3f(0, 0.2f, 0)));
                        }
                    } catch (InvalidPathException e) {
                        //no real map yet
                    }
                }
            }
        }
    }
    

    public enum GameExecutable {
        TCS_GOG(GameVersion.LSW_TCS, "gog", "GOG"),
        TCS_STEAM(GameVersion.LSW_TCS, "steam", "Steam"),
        LIJ1_GOG(GameVersion.LIJ1, "gog", "Steam");

        public GameVersion GAME;
        public String SHORT_NAME;
        public String NAME;

		private GameExecutable(GameVersion gAME, String shortName, String name) {
			GAME = gAME;
            SHORT_NAME = GAME.SHORT_NAME + "-" + shortName;
            NAME = GAME.NAME + " (" + name + ")";

		}
    }
}
