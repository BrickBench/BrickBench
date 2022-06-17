package com.opengg.loader;

import  com.github.austinc.jnrepl.Jnrepl;

public class ClojureDebugger {
    public static void startDebugRepl() {
        Jnrepl.startRepl();

        var hook = new Thread(Jnrepl::shutdownRepl);
        Runtime.getRuntime().addShutdownHook(hook);
    }
}
