package com.opengg.loader;

import com.opengg.core.Configuration;
import com.opengg.core.console.GGConsole;
import com.opengg.core.engine.Resource;
import com.opengg.loader.Project.GameVersion;

import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A class for managing base games.
 *
 * Base games are the games that are used by BrickBench for diff-compression, global files, and other uses.
 * If the user does not allow BrickBench to copy their game, they are responsible for ensuring that it isn't modified.
 * Developers can assume that the base games are always clean.
 */
public class GameBaseManager {
    static Path getGameBaseDirectory(String game){
        var root = Configuration.getConfigFile("editor.ini").getConfig(game + "-clean-game-root");
        if(root.isEmpty()){
            return Resource.getUserDataPath().resolve(Path.of("games", game));
        }else{
            return Path.of(root);
        }
    }
    
    /**
     * Returns a path corresponding to the root directory of the indicated game, prompting the user
     * if a directory has not yet been provided.
     * @param game The game identifier to get the base directory for (one of "lswtcs", "lij1", "batman1")
     * @return The root directory of the indicated game if it exists.
     */
    public static Optional<Path> getBaseDirectoryOrPromptForNew(Project.GameVersion game){
        var path = getGameBaseDirectory(game.SHORT_NAME);

        if(Files.exists(path)){
            return Optional.of(path);
        }else{
            var continueCopy = JOptionPane.showOptionDialog(BrickBench.CURRENT.window,
                    """
                    To edit files, BrickBench needs an unedited extracted copy of the game as reference.
                    Would you like to select an instance of %s to copy? This will copy your game into an internal folder, so you can edit or delete your clean copy afterwards without worry.
                    Optionally, you can also define a custom directory for BrickBench to look into for game files to save space.
                    Only do this if you are fully sure that that game copy will not be edited accidentally, as project files may corrupt if you edit a non-clean map.
                    """.formatted(game.NAME),
                    "Find game copy?",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{"Copy", "Set Custom Directory", "Cancel"}, "Copy");

            if(continueCopy == 0){
                var selectedFolder = FileUtil.openFileDialog(Configuration.get("home"), FileUtil.LoadType.DIRECTORY, "Clean copy of " + game.NAME);
                if(selectedFolder.isPresent()){

                    try (var exit = SwingUtil.showLoadingAlert("Copying...", "Copying game files, this may take a few minutes...", false)) {
                        FileUtils.copyDirectory(selectedFolder.get().toFile(), Resource.getUserDataPath().resolve(Path.of("games", game.SHORT_NAME)).toFile());

                        return Optional.of(Resource.getUserDataPath().resolve(Path.of("games", game.SHORT_NAME)));
                    } catch (IOException e) {
                        SwingUtil.showErrorAlert("Failed to copy game instance", e);
                        return Optional.empty();
                    }
                }else{
                    return Optional.empty();
                }
            }else if(continueCopy == 1) {
                var selectedFolder = FileUtil.openFileDialog(Configuration.get("home"), FileUtil.LoadType.DIRECTORY, "Clean copy of " + game.NAME);
                if(selectedFolder.isPresent()){
                    Configuration.getConfigFile("editor.ini").writeConfig(game.SHORT_NAME + "-clean-game-root", selectedFolder.get().toString());
                    Configuration.writeFile(Configuration.getConfigFile("editor.ini"));
                    return selectedFolder;
                }else{
                    return Optional.empty();
                }
            }else{
                return Optional.empty();
            }
        }
    }
}
