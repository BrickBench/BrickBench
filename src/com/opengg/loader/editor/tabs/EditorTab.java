package com.opengg.loader.editor.tabs;

import com.opengg.loader.Project;
import com.opengg.loader.editor.MapInterface;

import java.util.List;

/**
 * Represents a moveable/pop-out-able tab that saves position state between BrickBench instances.
 */
public interface EditorTab {

    /**
     * Returns the user-visible tab name
     */
    String getTabName();

    /**
     * Returns the internal tab ID
     */
    String getTabID();

    /**
     * Return the space in the UI this tab prefers to be in if one isn't already defined by the user.
     */
    MapInterface.InterfaceArea getPreferredArea();

    /**
     * Return the list of engine versions this tab is applicable to (all by default)
     */
    default List<Project.EngineVersion> getAllowedEngineVersions() { return List.of(Project.EngineVersion.values()); }

    /**
     * Return if this tab should exist by default in the UI.
     */
    boolean getDefaultActive();
}
