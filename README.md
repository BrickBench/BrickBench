# BrickBench
BrickBench is a map viewer for TCS that currently loads most of the files that have map data. The current files
that are loaded are GSC, TER, GIZ, and AI2.

## Instructions
There are two options for loading. 
* Load a specific file by selecting the file in the file explorer popup
* Load all relevant map files for a map by selecting the file directory in the file explorer popup. For AI2 files to load with the
directory option, they should be included in a subdirectory called AI in the map directory.

Once a map is loaded, information about the various subcomponents of the map such as triggers, locators for scripts, and splines can
be viewed on the left.

You can adjust various settings such as sensitivity and your default home directory in the "Settings" menu under File.

## Controls
* WASD to move
* Mouse to rotate the camera
* Enter to toggle locking the mouse cursor for camera movement
* Hold down left mouse to temporarily use 
* 1,2,3,4,5,6,7,8,9,0 to toggle rendering for the top menu
* Ctrl-G to go to a specific position

## Current Bugs
* Some triangles missing on collision mesh and display mesh.
* Material and shading pipeline improperly emulated resulting in mistexturing and other oddities.
* Mouse sometime snaps out of place.
* Infinite walls are missing pieces.
* Some collision meshes some do not have the correct rotation applied.
* Intel integrated GPUs have alot of issues and may not run correctly.

To report bugs, add an issue to this repository in the top of the window.
