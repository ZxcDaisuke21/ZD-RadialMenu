# ZD Radial Menu

A fully configurable radial menu for Minecraft (Fabric). A mod for adding a
circular menu to Minecraft — commands, key presses and submenus, opened with
a hotkey of your choice.

**Language / Язык:** English · [Русский](README_RU.md)

## Features

- Up to 10 slots in the main menu; each slot has a name, an icon (any item by
  ID) and an action:
  - **Command/Chat** — sends a command (starting with `/`) or a chat message;
  - **Key Binding** — triggers a selected game key binding (inventory, hotbar
    slots, etc.);
  - **Submenu** — a nested level with its own slots.
- **11 button styles:** Classic, Hexes, Custom, Ring, Bubbles, Diamonds,
  Triangles, Segments, Neon, Pixel, GTA.
- The **"Custom" style** is a full layout editor:
  - drag-and-drop cell placement with the mouse (click to select, click again
    to drag, click to release);
  - fine-tuning of XY position and size via button fields;
  - an editable palette of 12 colors plus manual ARGB input — the palette is
    saved to the config and persists between launches;
  - five cell shapes: rectangle, hexagon, circle, diamond, triangle;
  - shape-based hitboxes with a 3-pixel tolerance, so custom-drawn buttons
    are easy to hit;
  - add/remove cells and reset the layout to defaults.
- The **"Segments" style** is a ring menu: when you enter a submenu, it expands
  into the outer ring while the parent level stays as the inner ring
  (clicking it goes back).
- Three submenu display modes: overlay, ring, separate.
- All editor UI buttons share one style: translucent dark fill with a white
  outline.
- The config is saved automatically whenever an editor screen is closed.
- Full localization: English and Russian (follows the game's language).

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for your
   Minecraft version.
2. Put the following into your `mods` folder:
   - [Fabric API](https://modrinth.com/mod/fabric-api) matching your game
     version;
   - `zdradialmenu-<version>.jar` from the
     [Releases page](https://github.com/ZxcDaisuke21/ZD-RadialMenu/releases).

## Usage

- Open the menu with the key assigned in **Options → Controls**
  (the mod's category).
- **Left-click** a segment to run its action.
- **Right-click** a segment to open the editor for that slot.
- The **"Settings"** button in the bottom-right corner of the menu opens the
  full menu editor.
- In the editor, click a slot to configure it; use the **−/+** buttons to
  change the number of slots, and the **"Button style"** button to switch
  the appearance.

## Supported versions

- Minecraft 1.21.11 (Java Edition)
- Fabric Loader 0.19.5+
- Fabric API 0.141.6+1.21.11

Loader: Fabric. Client-side only.

## License

[MIT](https://github.com/ZxcDaisuke21/ZD-RadialMenu/blob/main/LICENSE)
