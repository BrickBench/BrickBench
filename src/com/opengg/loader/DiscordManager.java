package com.opengg.loader;

import com.opengg.core.Configuration;
import com.opengg.loader.editor.EditorState;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;

public class DiscordManager {
    private static boolean started;

    public static void startDiscord() {
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().setReadyEventHandler((user) -> {}).build();

        DiscordRPC.discordInitialize("887583828158328874", handlers, true);
        DiscordRPC.discordRegister("887583828158328874", "");

        var thread = new Thread(DiscordRPC::discordShutdown);
        Runtime.getRuntime().addShutdownHook(thread);

        started = true;
    }

    public static void update() {
        if (!started) return;

        DiscordRichPresence rich;

        if (Configuration.getBoolean("discord-integration-show-project")) {
            if (EditorState.getProject() != null) {
                if (EditorState.getProject().isProject()) {
                    if (EditorState.getActiveMap() != null) {
                        rich = new DiscordRichPresence.Builder("On " + EditorState.getProject().projectName()).setDetails("Editing " + EditorState.getActiveMap().levelData().name()).setBigImage("ai_upscale", "Editing").build();
                    } else {
                        rich = new DiscordRichPresence.Builder("On " + EditorState.getProject().projectName()).setBigImage("ai_upscale", "Editing").build();
                    }
                } else {
                    rich = new DiscordRichPresence.Builder("Viewing " + EditorState.getProject().projectName()).setBigImage("ai_upscale", "Editing").build();
                }
            } else {
                rich = new DiscordRichPresence.Builder("Idling").setBigImage("ai_upscale", "Idling").build();
            }
        } else {
            rich = new DiscordRichPresence.Builder("In BrickBench").setBigImage("ai_upscale", "Editing").build();
        }

        DiscordRPC.discordUpdatePresence(rich);
        DiscordRPC.discordRunCallbacks();
    }
}
