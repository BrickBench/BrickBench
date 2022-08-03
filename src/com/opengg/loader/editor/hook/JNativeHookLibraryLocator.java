package com.opengg.loader.editor.hook;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeLibraryLocator;
import com.github.kwhat.jnativehook.NativeSystem;
import com.opengg.core.console.GGConsole;
import com.opengg.core.engine.Resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class JNativeHookLibraryLocator implements NativeLibraryLocator {
    /**
     * Perform default procedures to interface with the native library. These procedures include
     * unpacking and loading the library into the Java Virtual Machine.
     */
    public static void setJNativeHookLocator() {
        System.setProperty("jnativehook.lib.locator", JNativeHookLibraryLocator.class.getCanonicalName());
    }

    public Iterator<File> getLibraries() {
        List<File> libraries = new ArrayList<File>(1);

        String libName = System.getProperty("jnativehook.lib.name", "JNativeHook");

        String basePackage = GlobalScreen.class.getPackage().getName().replace('.', '/');

        String libNativeArch = NativeSystem.getArchitecture().toString().toLowerCase();
        String libNativeName = System
                .mapLibraryName(libName) // Get what the system "thinks" the library name should be.
                .replaceAll("\\.jnilib$", "\\.dylib"); 

        // Resource path for the native library.
        String libResourcePath = "/" + basePackage + "/lib/" +
                NativeSystem.getFamily().toString().toLowerCase() +
                '/' + libNativeArch + '/' + libNativeName;

        URL classLocation = GlobalScreen.class.getProtectionDomain().getCodeSource().getLocation();

        File libFile = null;
        GGConsole.log("Got JNativeHook at " + classLocation.getPath());
        if (classLocation.getProtocol().equals("jrt")) {
            libResourcePath = "lib/" +
                    NativeSystem.getFamily().toString().toLowerCase() +
                    '/' + libNativeArch + '/' + libNativeName;

            FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
            var dllPath = fs.getPath("modules", "com.github.kwhat.jnativehook","com","github","kwhat","jnativehook",libResourcePath);

            String version = GlobalScreen.class.getPackage().getImplementationVersion();
            if (version != null) {
                version = '-' + version;
            } else {
                version = "";
            }

            libFile = new File(
                    Resource.getUserDataPath().resolve(Path.of("lib")).toAbsolutePath().toString(),
                    libNativeName.replaceAll("^(.*)\\.(.*)$", "$1" + version + '.' + libNativeArch + ".$2")
            );

            if (!libFile.exists()) {
                try {
                    // Check and see if a copy of the native lib already exists.
                    if(!Files.exists(libFile.toPath().getParent())){
                        Files.createDirectory(libFile.toPath().getParent());
                    }
                    FileOutputStream libOutputStream = new FileOutputStream(libFile);
                    byte[] dll = Files.readAllBytes(dllPath);
                    libOutputStream.write(dll,0,dll.length);
                    libOutputStream.close();
                } catch (IOException e) {
                    GGConsole.exception(e);
                    throw new RuntimeException(e.getMessage(), e);
                }
                GGConsole.log("Extracted JNativeHook");
            }
        } else {
            File classFile = null;
            try {
                classFile = new File(classLocation.toURI());
            } catch (URISyntaxException e) {
                GGConsole.exception(e);
                classFile = new File(classLocation.getPath());
            }

            if (classFile.isFile()) {
                String libPath = System.getProperty("jnativehook.lib.path", classFile.getParentFile().getPath());
                InputStream resourceInputStream = GlobalScreen.class.getResourceAsStream("../../../"+libResourcePath);
                if (resourceInputStream == null) {
                    throw new RuntimeException("Unable to extract the native library " + libResourcePath + "!\n");
                }

                String version = GlobalScreen.class.getPackage().getImplementationVersion();
                if (version != null) {
                    version = '-' + version;
                } else {
                    version = "";
                }

                libFile = new File(
                        libPath,
                        libNativeName.replaceAll("^(.*)\\.(.*)$", "$1" + version + '.' + libNativeArch + ".$2")
                );

                if (!libFile.exists()) {
                    try {
                        FileOutputStream libOutputStream = new FileOutputStream(libFile);

                        int size;
                        byte[] buffer = new byte[4 * 1024];
                        while ((size = resourceInputStream.read(buffer)) != -1) {
                            libOutputStream.write(buffer, 0, size);
                        }

                        resourceInputStream.close();
                        libOutputStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
            } else {
                libFile = Paths.get(classFile.getAbsolutePath(), libResourcePath.toString()).toFile();
            }

            if (!libFile.exists()) {
                throw new RuntimeException("Unable to locate JNI library at " + libFile.getPath() + "!\n");
            }
        }

        libraries.add(libFile);
        return libraries.iterator();
    }
}

