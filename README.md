# Hitbox Studio

A client-side Fabric mod for Minecraft **1.21.11** that replaces the vanilla F3+B hitboxes with a fully customizable version.
All settings live in the **Mod Menu** config screen. There's no ClickGUI, no keybind menu and no commands.

## Install

Put these in your `.minecraft/mods` folder (Fabric Loader 0.19.5+ for 1.21.11):

1. `hitbox-studio-1.0.0.jar` (this mod, from `build/libs/`)
2. [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.11
3. [Mod Menu](https://modrinth.com/mod/modmenu) 17.x for 1.21.11

Then: **Mods** button on the title screen (or pause menu) → Hitbox Studio → config icon.

## Build it yourself (Windows)

Needs a JDK 21 or newer.

```bash
gradlew.bat build
```

The jar ends up at `build/libs/hitbox-studio-1.0.0.jar`.

## What you can change

The menus are built from vanilla Minecraft widgets, laid out like the vanilla Options screens.

**Main screen**: *Hitboxes: ON/OFF* and *Show: With F3+B / Always* (Always still respects servers' reduced debug info), then buttons for each page:

- **General...**: hide with F1, max distance, global line width and opacity. The footer has *Reset All*.
- **Default Style...**: the look every section uses unless it has its own.
- **Highlights...**: recolor on crosshair target, in reach (your real interaction range) and hurt flash. *Replace* or *Mix*, optionally tinting the fill too.
- **Hidden Entities...**: any entity ID, modded too, that never gets a hitbox.
- **End Crystals, Players, Self, Hostile Mobs, Passive Mobs, Dropped Items, XP Orbs, Projectiles, Vehicles, Armor Stands, Decorations, Other**: each page has *Hitboxes: ON/OFF* and *Custom Style: ON/OFF*. Projectiles have a switch per type: arrows, tridents, pearls, snowballs/eggs, potions, fireballs, wind charges, fireworks, fishing bobbers, other. Crystals have their own target / in-reach colors.

**Every hitbox part is its own section** with show/hide, color, width (0.1–10 px), opacity and rainbow mode:

| Part | Vanilla color |
| --- | --- |
| Box Outline (+ visual size offset) | white |
| Fill (translucent faces) | not in vanilla, off by default |
| Eye Height Line | red |
| Look Vector (+ length, arrow head on/off) | blue |
| Position Dot | white |
| Riding Seat Box | yellow |
| Multipart Boxes (Ender Dragon) | green |

**Color wheel**: hue/saturation wheel and brightness bar, hex box (`#RRGGBB` / `#AARRGGBB`), opacity slider, old/new swatches, 8 recent colors, 16 presets, rainbow with speed.

**Live preview**: a spinning hitbox drawn with the current page's settings, beside the list. It only shows when the window is wide enough, so at GUI scale 4 on 1080p it's hidden.

Right-click any slider or color to reset it. Done saves, Cancel undoes everything since you opened the screen.

Defaults look exactly like vanilla, except XP orbs are hidden and crystals skip the useless eye line and look arrow.

## Fair play

Hitboxes are drawn through vanilla's own gizmo renderer, at the same point and with the same depth test as F3+B.
Nothing renders through walls, invisible entities stay hidden (same as vanilla), and *Always on* switches itself off on servers that enable reduced debug info.

## Config file

`config/hitboxstudio.json`: pretty-printed, colors stored as `#AARRGGBB`. If the file gets corrupted, it's backed up to `hitboxstudio.json.broken` and defaults are used. New options added in later versions get their defaults automatically.

## How it hooks into the game

| Target | Injection | Why |
| --- | --- | --- |
| `EntityHitboxDebugRenderer#emitGizmos` | `HEAD`, cancellable | Turns off vanilla's F3+B drawing while the mod is enabled. |
| `DebugRenderer#emitGizmos` | `TAIL` | Runs every frame inside the level's gizmo collector, so this is where Hitbox Studio emits its own hitboxes, with or without F3+B (Always on mode). |

In 1.21.11 each gizmo line carries its own width (the `rendertype_lines` shader reads a per-vertex `LineWidth`), so line thickness is a real pixel width. Widths under 1 px are drawn at 1 px and faded instead, because sub-pixel lines break into dots on most GPUs.

## Automated in-game test

`src/gametest` holds a Fabric client game test. It clicks through the config screen and color wheel with real mouse input, builds a world with crystals and mobs, and screenshots each render mode.
It is not included in the mod jar.
Pass `-Dhitboxstudio.test.monitor=X,Y` to the test JVM (the top-left corner of a monitor, e.g. `1920,0`) to open the test window on that monitor without it taking focus.

```bash
gradlew.bat runClientGameTest
```
