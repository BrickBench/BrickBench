package com.opengg.loader.editor;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.opengg.core.engine.Resource;
import com.opengg.loader.DarkSvgClassLoader;

import java.util.Map;

public interface EditorIcons {
    int iconSmallDim = 16;
    DarkSvgClassLoader loader = new DarkSvgClassLoader();
    FlatSVGIcon longarrow = loadIcons("longarrow.svg",iconSmallDim,iconSmallDim-4);
    FlatSVGIcon add = loadIcons("plus.svg",iconSmallDim,iconSmallDim);
    FlatSVGIcon minus = loadIcons("minus.svg",iconSmallDim,iconSmallDim);
    FlatSVGIcon visibleEye = loadIcons("eye.svg");
    FlatSVGIcon invisibleEye = loadIcons("invisible.svg");
    FlatSVGIcon search = loadIcons("search.svg",20,20);

    //Button Row Icons
    int buttonRowIconSize = 20;
    FlatSVGIcon select = loadIcons("cursor.svg",buttonRowIconSize,buttonRowIconSize);
    FlatSVGIcon pan = loadIcons("move.svg",buttonRowIconSize,buttonRowIconSize);
    FlatSVGIcon paint = loadIcons("paintbrush.svg",buttonRowIconSize,buttonRowIconSize);
    FlatSVGIcon light = loadIcons("light.svg",buttonRowIconSize,buttonRowIconSize);
    FlatSVGIcon messages = loadIcons("text.svg",buttonRowIconSize,buttonRowIconSize);
    FlatSVGIcon highlight = loadIcons("highlight.svg",buttonRowIconSize,buttonRowIconSize);

    //Object Tree Icons
    int treeIconSize = 20;
    FlatSVGIcon gizmo = loadIcons("gizmo.svg",treeIconSize,treeIconSize);
    FlatSVGIcon terrain = loadIcons("terrain.svg",treeIconSize,treeIconSize);
    FlatSVGIcon locator = loadIcons("locator.svg",treeIconSize,treeIconSize);
    FlatSVGIcon locatorset = loadIcons("locatorset.svg",treeIconSize,treeIconSize);
    FlatSVGIcon creature = loadIcons("creature.svg",treeIconSize,treeIconSize);
    FlatSVGIcon spline = loadIcons("spline.svg",treeIconSize,treeIconSize);
    FlatSVGIcon wall = loadIcons("wall.svg",treeIconSize,treeIconSize);
    FlatSVGIcon ai = loadIcons("brain.svg",treeIconSize,treeIconSize);
    FlatSVGIcon door = loadIcons("door.svg",treeIconSize,treeIconSize);
    FlatSVGIcon model = loadIcons("model.svg",treeIconSize,treeIconSize);
    FlatSVGIcon specialobject = loadIcons("specialobject.svg",treeIconSize,treeIconSize);
    FlatSVGIcon staticmesh = loadIcons("staticmesh.svg",treeIconSize,treeIconSize);
    FlatSVGIcon triggers = loadIcons("trigger2.svg",treeIconSize,treeIconSize);
    FlatSVGIcon render = loadIcons("render.svg",treeIconSize,treeIconSize);
    FlatSVGIcon trash = loadIcons("trash.svg",20,20);

    //Project Tree Icons
    FlatSVGIcon areas = loadIcons("areas.svg",treeIconSize,treeIconSize);
    FlatSVGIcon maps = loadIcons("maps.svg",treeIconSize,treeIconSize);

    FlatSVGIcon video = loadIcons("video.svg",treeIconSize,treeIconSize);

    /**
     * Load the icon with the given name.
     */
    static FlatSVGIcon loadIcons(String name){
        FlatSVGIcon icon =  new FlatSVGIcon(Resource.getTexturePath(name), loader);
        icon.setColorFilter(EditorTheme.iconFilter);
        return icon;
    }

    /**
     * Load the icon with the given name, scaled to the given size.
     */
    static FlatSVGIcon loadIcons(String name,int dimX,int dimY){
        FlatSVGIcon icon =  new FlatSVGIcon(Resource.getTexturePath(name),dimX,dimY, loader);
        icon.setColorFilter(EditorTheme.iconFilter);
        return icon;
    }

    /**
     * Icons for the object tree.
     */
    Map<String, FlatSVGIcon> objectTreeIconMap = Map.ofEntries(
            Map.entry("Gizmo", gizmo),
            Map.entry("Pickups", gizmo),
            Map.entry("Panels", gizmo),
            Map.entry("Levers", gizmo),
            Map.entry("ZipUps", gizmo),
            Map.entry("HatMachines", gizmo),
            Map.entry("PushBlocks", gizmo),
            Map.entry("Tubes", gizmo),
            Map.entry("Terrain", terrain),
            Map.entry("Meshes", terrain),
            Map.entry("Locators", locator),
            Map.entry("LocatorSets", EditorIcons.locatorset),
            Map.entry("Creatures", creature),
            Map.entry("Splines", spline),
            Map.entry("Walls", wall),
            Map.entry("AI", ai),
            Map.entry("Doors", EditorIcons.door),
            Map.entry("Models", EditorIcons.model),
            Map.entry("SpecialObjects", EditorIcons.specialobject),
            Map.entry("StaticObjects", EditorIcons.staticmesh),
            Map.entry("DisplayLists", EditorIcons.staticmesh),
            Map.entry("Triggers", EditorIcons.triggers),
            Map.entry("Render", EditorIcons.render),
            Map.entry("Maps", maps),
            Map.entry("Areas", areas));
}
