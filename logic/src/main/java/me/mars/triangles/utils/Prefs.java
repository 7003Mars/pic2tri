package me.mars.triangles.utils;

import arc.Core;
import arc.files.Fi;

public class Prefs {
    public static final String internalName = "triangles";

    public static String setting(String name) {
        return internalName + "." + name;
    }

    public static boolean debugMode = false;
    public static Fi genCacheDir = new Fi(Core.files.getCachePath()).child("pic2tri");
}
