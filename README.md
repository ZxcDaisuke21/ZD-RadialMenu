# ZD Radial Menu

A radial menu of commands and actions for Minecraft on Fabric. Opens with a hotkey of your choice, and is configured entirely in-game — no manual file editing required (though the config is a plain JSON file, so you can edit it by hand too).

**Language / Язык:** [Русский](README_RU.md) · English

## Features

- **Up to 10 slots** in the main menu; each slot has a name, an icon (any item by ID) and an action:
  - **Command/Chat** — sends a command (starting with `/`) or a chat message;
  - **Key Binding** — triggers a selected game key binding (inventory, hotbar slots, etc.);
  - **Submenu** — a nested level with its own slots.
- **Eight button styles**: Classic, Hexes, Custom, Ring, Bubbles, Diamonds, Triangles, Segments.
- **The "Custom" style** is a full layout editor:
  - drag-and-drop cell placement with the mouse;
  - fine-tuning of X/Y position and size via input fields;
  - a palette of 12 colors plus manual ARGB input;
  - five cell shapes: square, hexagon, circle, diamond, triangle;
  - add/remove cells and reset the layout to defaults.
- **The "Segments" style** is a ring menu: when you enter a submenu, it expands into the outer ring while the parent level stays as the inner ring (clicking it goes back).
- All editor UI buttons share one style: translucent dark fill with a white outline.
- The config is saved automatically whenever an editor screen is closed.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for your Minecraft version.
2. Put the following into your `mods` folder:
   - [Fabric API](https://modrinth.com/mod/fabric-api) matching your game version;
   - `zdradialmenu-<version>.jar` from the [Releases](<ссылка на Releases>) page.

## Usage

- Open the menu with the key assigned in **Options → Controls** (the mod's category).
- **Left-click** a segment to run its action.
- **Right-click** a segment to open the editor for that slot.
- The **"Settings"** button in the bottom-right corner of the menu opens the full menu editor.
- In the editor: click a slot to configure it, use the «−/＋» buttons to change the number of slots, and the **"Button style"** button to switch the appearance.

## Config

File: `config/zdradialmenu.json`. Example:

```json
{
  "skin": "DONUT",
  "sectorCount": 4,
  "sectors": [
    {
      "name": "Day",
      "command": "/time set day",
      "icon": "minecraft:clock",
      "type": "command",
      "keyBinding": "",
      "children": []
    },
    {
      "name": "Building",
      "command": "",
      "icon": "minecraft:brick_block",
      "type": "submenu",
      "keyBinding": "",
      "children": [
        {
          "name": "Creative",
          "command": "/gamemode creative",
          "icon": "",
          "type": "command",
          "keyBinding": "",
          "children": []
        }
      ]
    }
  ],
  "customLayout": [
    { "dx": 0, "dy": -40, "w": 56, "h": 48, "color": 0x60808080, "shape": "hex" }
  ]
}
