open module com.opengg.loader {
    requires com.opengg.core;
    requires com.opengg.system;
    requires com.opengg.math;
    requires com.opengg.console;
    requires com.opengg.base;

    requires java.desktop;
    requires java.logging;
    requires com.formdev.flatlaf.extras;
    requires jdk.crypto.cryptoki;

    requires org.apache.commons.io;
    requires org.apache.commons.lang3;

    requires com.github.kwhat.jnativehook;
    requires dds;
    requires github.api;

    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.lwjgl.nfd;
    requires org.lwjgl.meshoptimizer;

    requires com.formdev.flatlaf;
    requires com.formdev.flatlaf.intellijthemes;

    requires com.github.benmanes.caffeine;

    requires miglayout.core;
    requires miglayout.swing;

    requires vcdiff.core;
    requires discord.rpc;

    requires net.sourceforge.argparse4j;
    requires jnrepl;
    requires reflections;

    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.module.paramnames;
    requires com.fasterxml.jackson.datatype.jdk8;
    requires com.fasterxml.jackson.dataformat.xml;
    requires com.fasterxml.aalto;

    exports com.opengg.loader;
    exports com.opengg.loader.loading;
    exports com.opengg.loader.game.nu2;
    exports com.opengg.loader.game.nu2.scene;
    exports com.opengg.loader.game.nu2.gizmo;
    exports com.opengg.loader.game.nu2.rtl;
    exports com.opengg.loader.components;
}
