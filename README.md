# BrickBench
BrickBench is a map viewer for TCS (and LIJ1/Batman experimentally) that currently loads most of the files that have map data. The current files
that are loaded are GSC, TER, GIZ, and AI2.

## Instructions
There are two options for loading. 
* Load a specific file by selecting the file in the file explorer popup or dragging it into the window.
* Load all relevant map files for a map by selecting the file directory in the file explorer popup, or dragging a directory into the window.
  * For AI2 files to load with the
directory option, they should be included in a subdirectory called AI in the map directory.

Once a map is loaded, information about the various subcomponents of the map such as triggers, locators for scripts, and splines can
be viewed on the left.

You can adjust various settings such as sensitivity and your default home directory in the "Settings" menu under File.

## Current Bugs
* Material and shading pipeline improperly emulated resulting in mistexturing and other oddities.
* Intel integrated GPUs have alot of issues and may not run correctly.

To report bugs, add an issue to this repository in the top of the window.
