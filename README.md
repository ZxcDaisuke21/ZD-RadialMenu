\# ZD Radial Menu



A radial menu of commands and actions for Minecraft on Fabric. Opens with a hotkey of your choice,

and is configured entirely in-game — no manual file editing required (though the config is a plain

JSON file, so you can edit it by hand too).



\*\Language / Язык:\*\ \[Русский](README_RU.md) · English



\## Features



\- \*\*Up to 10 slots\*\* in the main menu; each slot has a name, an icon (any item by ID) and an action:

&#x20; - \*\*Command/Chat\*\* — sends a command (starting with `/`) or a chat message;

&#x20; - \*\*Key Binding\*\* — triggers a selected game key binding (inventory, hotbar slots, etc.);

&#x20; - \*\*Submenu\*\* — a nested level with its own slots.

\- \*\*Eight button styles\*\*: Classic, Hexes, Custom, Ring, Bubbles, Diamonds, Triangles, Segments.

\- \*\*The "Custom" style\*\* is a full layout editor:

&#x20; - drag-and-drop cell placement with the mouse;

&#x20; - fine-tuning of X/Y position and size via input fields;

&#x20; - a palette of 12 colors plus manual ARGB input;

&#x20; - five cell shapes: square, hexagon, circle, diamond, triangle;

&#x20; - add/remove cells and reset the layout to defaults.

\- \*\*The "Segments" style\*\* is a ring menu: when you enter a submenu, it expands into the outer ring

&#x20; while the parent level stays as the inner ring (clicking it goes back).

\- All editor UI buttons share one style: translucent dark fill with a white outline.

\- The config is saved automatically whenever an editor screen is closed.



\## Installation



1\. Install \[Fabric Loader](https://fabricmc.net/use/) for your Minecraft version.

2\. Put the following into your `mods` folder:

&#x20;  - \[Fabric API](https://modrinth.com/mod/fabric-api) matching your game version;

&#x20;  - `zdradialmenu-<version>.jar` from the \[Releases](вставить ссылку!) page.



\## Usage



\- Open the menu with the key assigned in \*\*Options → Controls\*\* (the mod's category).

\- \*\*Left-click\*\* a segment to run its action.

\- \*\*Right-click\*\* a segment to open the editor for that slot.

\- The \*\*"Settings"\*\* button in the bottom-right corner of the menu opens the full menu editor.

\- In the editor: click a slot to configure it, use the «−/＋» buttons to change the number of slots,

&#x20; and the «Button style» button to switch the appearance.



\## Config



File: `config/zdradialmenu.json`. Example:



```json

{

&#x20; "skin": "DONUT",

&#x20; "sectorCount": 4,

&#x20; "sectors": \[

&#x20;   {

&#x20;     "name": "Day",

&#x20;     "command": "/time set day",

&#x20;     "icon": "minecraft:clock",

&#x20;     "type": "command",

&#x20;     "keyBinding": "",

&#x20;     "children": \[]

&#x20;   },

&#x20;   {

&#x20;     "name": "Building",

&#x20;     "command": "",

&#x20;     "icon": "minecraft:brick\_block",

&#x20;     "type": "submenu",

&#x20;     "keyBinding": "",

&#x20;     "children": \[

&#x20;       {

&#x20;         "name": "Creative",

&#x20;         "command": "/gamemode creative",

&#x20;         "icon": "",

&#x20;         "type": "command",

&#x20;         "keyBinding": "",

&#x20;         "children": \[]

&#x20;       }

&#x20;     ]

&#x20;   }

&#x20; ],

&#x20; "customLayout": \[

&#x20;   { "dx": 0, "dy": -40, "w": 56, "h": 48, "color": 0x60808080, "shape": "hex" }

&#x20; ]

}

```



\- `type`: `command` | `key` | `submenu`

\- `icon`: an item ID, e.g. `minecraft:diamond`; leave empty to show the slot's name instead

\- `skin`: `CLASSIC` | `HEX` | `CUSTOM` | `RING` | `BUBBLES` | `ROMB` | `TRI` | `DONUT`

\- `color` in `customLayout`: ARGB, e.g. `0x60808080` — translucent gray



\## Building



```bash

./gradlew build

```



The resulting jar will be in `build/libs/`.



\## Compatibility



\- Minecraft: `<specify your version, e.g. 1.21.11>`

\- Fabric Loader: `0.20.1` and compatible

\- Fabric API: the version matching your Minecraft version



\## License



Distributed under the MIT license — see \[LICENSE](LICENSE).



