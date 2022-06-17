package com.opengg.loader.internal;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * For multithreading in X/XWayland windows.
 */
public interface X11 extends Library {
    X11 Lib = Native.load("X11", X11.class);

    int XInitThreads();
}
