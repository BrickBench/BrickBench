package com.opengg.loader;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

/**
 * A ClassLoader used to handle dark SVGs. 
 */
public class DarkSvgClassLoader extends ClassLoader{
    public DarkSvgClassLoader(){

    }
    @Override
    public URL getResource(String name) {
        if(name.endsWith("_dark.svg")) return null;
        try {
            return Paths.get(name).toUri().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
