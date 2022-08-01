package com.opengg.loader.loading;

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
import java.util.logging.Logger;

/**
 * Based on DefaultLibaryLoader
 */
public class MyDefaultLibraryLocator implements NativeLibraryLocator {
    private static final Logger log = Logger.getLogger(GlobalScreen.class.getPackage().getName());

    /**
     * Perform default procedures to interface with the native library. These procedures include
     * unpacking and loading the library into the Java Virtual Machine.
     */
    public static void setAaDefaultLocator() {
        System.setProperty("jnativehook.lib.locator", MyDefaultLibraryLocator.class.getCanonicalName());
    }
    public Iterator<File> getLibraries() {
        List<File> libraries = new ArrayList<File>(1);

        String libName = System.getProperty("jnativehook.lib.name", "JNativeHook");

        // Get the package name for the GlobalScreen.
        String basePackage = GlobalScreen.class.getPackage().getName().replace('.', '/');

        String libNativeArch = NativeSystem.getArchitecture().toString().toLowerCase();
        String libNativeName = System
                .mapLibraryName(libName) // Get what the system "thinks" the library name should be.
                .replaceAll("\\.jnilib$", "\\.dylib"); // Hack for OS X JRE 1.6 and earlier.

        // Resource path for the native library.
        String libResourcePath = "/" + basePackage + "/lib/" +
                NativeSystem.getFamily().toString().toLowerCase() +
                '/' + libNativeArch + '/' + libNativeName;

        URL classLocation = GlobalScreen.class.getProtectionDomain().getCodeSource().getLocation();

        File libFile = null;
        GGConsole.log(classLocation.getPath());
        System.out.println(classLocation.getProtocol());
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
            GGConsole.error(libFile.getAbsolutePath());
            GGConsole.error(String.valueOf(libFile.exists()));
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
                    GGConsole.error(String.valueOf(dll.length));
                } catch (IOException e) {
                    GGConsole.error("Exception: " + e.getMessage());
                    throw new RuntimeException(e.getMessage(), e);
                }
                GGConsole.log("Library Extract");
                log.fine("Extracted library: " + libFile.getPath() + ".\n");
            }
            GGConsole.log("out");
        } else {
            File classFile = null;
            try {
                classFile = new File(classLocation.toURI());
            } catch (URISyntaxException e) {
                log.warning(e.getMessage());
                classFile = new File(classLocation.getPath());
            }

            if (classFile.isFile()) {
                // Jar Archive
                String libPath = System.getProperty("jnativehook.lib.path", classFile.getParentFile().getPath());
                GGConsole.error(libResourcePath);
                System.out.println( "sdsd:" + GlobalScreen.class.getResource("/").getPath());
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
                        // Check and see if a copy of the native lib already exists.
                        FileOutputStream libOutputStream = new FileOutputStream(libFile);

                        // Read from the digest stream and write to the file steam.
                        int size;
                        byte[] buffer = new byte[4 * 1024];
                        while ((size = resourceInputStream.read(buffer)) != -1) {
                            libOutputStream.write(buffer, 0, size);
                        }

                        // Close all the streams.
                        resourceInputStream.close();
                        libOutputStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }

                    log.fine("Extracted library: " + libFile.getPath() + ".\n");
                }
            } else {
                // Loose Classes
                libFile = Paths.get(classFile.getAbsolutePath(), libResourcePath.toString()).toFile();
            }

            if (!libFile.exists()) {
                throw new RuntimeException("Unable to locate JNI library at " + libFile.getPath() + "!\n");
            }
        }

        log.fine("Loading library: " + libFile.getPath() + ".\n");
        libraries.add(libFile);

        return libraries.iterator();
    }
}

