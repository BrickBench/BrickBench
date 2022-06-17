package com.opengg.loader;

import com.opengg.core.console.GGConsole;
import com.opengg.core.engine.Resource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.kohsuke.github.GitHub;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;

public class Updater {
    public static void checkForUpdates(){
        try {
            var gitHub = GitHub.connect();
            var repository = gitHub.getRepository("BrickBench/BrickBench");
            var latestRelease = repository.getLatestRelease();

            if(!(latestRelease.isPrerelease() || latestRelease.isDraft())
                    && new Version(latestRelease.getTagName()).compareTo(BrickBench.VERSION) > 0){
                GGConsole.log("BrickBench is not up to date, " + latestRelease.getTagName() + " is available");
                var doDownload = showOptionPane(latestRelease.getName(), latestRelease.getHtmlUrl().toString());
                if(!doDownload) return;

                for(var asset : latestRelease.listAssets()){
                    if(!asset.getName().endsWith(".zip")) continue;

                    var exit = SwingUtil.showLoadingAlert("Updating", "Updating BrickBench", false);
                    exit.setState("Downloading update");
                    var tempDownload = Files.createTempDirectory("BrickBenchDownloads");
                    var zipFile = tempDownload.resolve("BrickBench.zip");
                    var extractDir = tempDownload.resolve("BrickBench");

                    GGConsole.log("Downloading asset " + asset.getName());

                    var downloadLink = asset.getBrowserDownloadUrl();

                    URL website = new URL(downloadLink);

                    Files.createDirectories(zipFile.getParent());

                    FileOutputStream fos = new FileOutputStream(zipFile.toString());
                    fos.getChannel().transferFrom(Channels.newChannel(website.openStream()), 0, Long.MAX_VALUE);

                    exit.setState("Unpacking update");
                    GGConsole.log("Unpacking asset");

                    FileUtil.unzip(zipFile.toString(), extractDir.toString());

                    fos.close();
                    Files.delete(zipFile);
                    GGConsole.log("New version downloaded, restarting now...");

                    exit.close();

                    if (SystemUtils.IS_OS_WINDOWS) {
                        new ProcessBuilder()
                                .command("cmd.exe", "/c", extractDir.resolve("BrickBench").resolve("Updater.exe").toString(),
                                        Resource.getApplicationPath().toString(),
                                        extractDir.resolve("BrickBench").toString())
                                .directory(Resource.getApplicationPath().toFile())
                                .start();
                    } else if (SystemUtils.IS_OS_UNIX) {
                        FileUtils.copyDirectory(extractDir.toFile(), Resource.getApplicationPath().toFile());
                    }

                    System.exit(0);
                }
            }else{
                GGConsole.log("BrickBench is up to date");
            }
        } catch (Exception e) {
            GGConsole.warn("Failed to fetch latest release: " + e.getMessage());
        }
    }

    private static boolean showOptionPane(String version, String url){

        JEditorPane ep = new JEditorPane("text/html",
                ("<html>A new BrickBench version is available: %s <br>You can view the changelog <a href=\"%s\">here</a> " +
                        "<br>Would you like to update now? This will close the program.</html>")
                        .trim().formatted(version, url));

        ep.addHyperlinkListener(e -> {
            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (URISyntaxException | IOException uriSyntaxException) {
                    uriSyntaxException.printStackTrace();
                }
            }
        });
        ep.setEditable(false);

        var result = JOptionPane.showOptionDialog(BrickBench.CURRENT.window, ep, "Download update?",
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, new String[]{"Yes", "No"}, "Yes");

        return result == JOptionPane.YES_OPTION;
    }
}

