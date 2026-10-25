# CoordKit

A tiny client-side Fabric mod for **Minecraft 26.2** ("Chaos Cubed").

It adds a compact info panel to the HUD, on-screen waypoint markers, saved waypoints,
a waypoint manager, and a settings screen — all without touching the server.

See [CHANGELOG.md](CHANGELOG.md) for release history.

## Features

### HUD
- X/Y/Z (2 decimals), block position, chunk position and dimension id.
- Facing (compass point + yaw), biome, in-game time and weather, light level,
  movement speed and held-item durability.
- FPS and a real-world clock (both individually toggleable).
- Nearby waypoints below the info lines: coloured dot, name and distance, sorted by
  distance and only shown for the dimension you are in.

### In-view waypoint markers
- Every visible waypoint is projected onto the screen from the camera, so you can
  simply look around to find it — no need to watch the top-left corner.
- Markers are drawn on the HUD layer, so they stay visible through terrain.
- Each visible waypoint is a small coloured diamond with its name in white and the
  distance in the waypoint's colour, slightly faded with distance.
- Waypoints outside your view collapse to a clean direction arrow at the screen edge —
  the label disappears until the waypoint is back in view.
- Labels automatically nudge apart when several waypoints are close together.
- Configurable marker range (64 m to 4096 m) and an optional edge-arrow toggle.
- Markers can be enabled independently of the info panel.

### Waypoints
- **F9** opens the waypoint manager.
- **F10** adds a waypoint at your current position immediately.
- **Add here** saves your current position (name, X/Y/Z, dimension, colour).
- **Add aim** saves the block you are looking at, up to 128 blocks away.
- Click a waypoint name to edit it; **TP** copies `/tp @s <x> <y> <z>`.
- Toggle a waypoint on/off with its **ON / OFF** button.
- **Del** asks for confirmation on the second click.
- Coordinates and names are editable; colours come from an 8-colour palette.
- New waypoints are scoped to the current world or server by default, so bases
  from one world do not appear in another. Old waypoints without a scope remain
  global; the editor can switch any waypoint between **World: This** and
  **World: All**.
- A death waypoint is created automatically when you die. The latest death in
  each world and dimension is updated in place; the setting can be turned off.

### Screens
- **F8** opens the settings screen, which has two tabs:
  - **HUD**: panel on/off, FPS, clock, chunk line, facing, biome, time, light,
    speed, durability, waypoint list, markers and edge arrows.
  - **Waypoints**: max waypoints on the HUD, marker range, waypoint list / marker
    toggles, and a shortcut to the manager.
- With [Mod Menu](https://modrinth.com/mod/modmenu) installed, the same screen is
  available from CoordKit's entry in the mod list.
- Settings are written to `config/coordkit.json` immediately.

## Controls

| Key | Action |
| --- | --- |
| `F6` | Toggle HUD |
| `F7` | Copy your position as `/tp @s <x> <y> <z>` |
| `F8` | Open settings |
| `F9` | Open waypoint manager |
| `F10` | Add a waypoint at your position |

All five keybinds can be changed in *Options → Controls → Key Binds*
(category: CoordKit).

## Config file

`config/coordkit.json` is created on first save:

```json
{
  "showHud": true,
  "showFps": true,
  "showClock": true,
  "showChunk": true,
  "showWaypoints": true,
  "maxWaypoints": 5,
  "showFacing": true,
  "showBiome": true,
  "showGameTime": true,
  "showLight": false,
  "showSpeed": false,
  "showDurability": true,
  "showMarkers": true,
  "markerOffscreenArrows": true,
  "markerMaxDistance": 512,
  "scopeNewWaypointsToWorld": true,
  "createDeathWaypoint": true,
  "waypoints": [
    {
      "name": "Base",
      "x": 128.5,
      "y": 70.0,
      "z": -256.5,
      "dimension": "minecraft:overworld",
      "color": 4632774,
      "visible": true,
      "world": "sp:my-world",
      "autoDeath": false
    }
  ]
}
```

Deleting the file resets everything to defaults.

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | 0.20.1+ |
| Fabric API | 0.161.0+26.2 (any `+26.2` build) |
| Java | 25+ |
| Mod Menu (optional) | 20.0.3+ |

## Build

The project uses the Gradle wrapper, so only a JDK 25 install is required.
Point `JAVA_HOME` at it (the `java` on your PATH may be older):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
.\gradlew.bat build
```

The mod jar ends up in `build/libs/coordkit-1.3.0.jar`.

## Run in a dev client

Mod Menu is on the dev runtime classpath, so the integration can be tested directly:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
.\gradlew.bat runClient
```

## Project layout

```
src/main/java/com/mackan/coordkit/
├── CoordKit.java                      shared constants (mod id, logger, id helper)
└── client/
    ├── CoordKitClient.java            entrypoint: keybinds, HUD and death handling
    ├── CoordKitHud.java               HUD panel + waypoint list rendering
    ├── WaypointMarkers.java           camera-to-screen waypoint markers
    ├── CoordKitConfig.java            JSON config + waypoint list
    ├── Waypoint.java                  waypoint data model
    ├── WaypointActions.java           shared create, quick-add and death logic
    ├── WaypointScope.java             world/server identity and visibility
    ├── ClientFeedback.java            action-bar and chat helpers
    ├── WaypointStyle.java             colour palette
    ├── integration/
    │   └── ModMenuIntegration.java    optional Mod Menu config-screen hook
    └── ui/
        ├── CoordKitConfigScreen.java  tabbed settings screen
        ├── WaypointManagerScreen.java scrollable waypoint list
        └── WaypointEditScreen.java    add/edit a waypoint
src/test/java/com/mackan/coordkit/client/
└── WaypointScopeTest.java             world-scope matching + config migration
src/main/resources/
├── fabric.mod.json
└── assets/coordkit/                   icon + en_us translations
```

## Notes

- The mod is `"environment": "client"`, so it does nothing on a dedicated server
  even if the jar is placed in the server's `mods` folder.
- Waypoints from configs created before 1.3.0 have no world scope and therefore
  remain visible in every world until you edit them. Open the waypoint and click
  its **World: All** button once to scope it to the current world.
- 26.2 is unobfuscated; Loom therefore needs no mappings configuration.
- 26.2 renamed most GUI APIs: screens render through
  `GuiGraphicsExtractor.extractRenderState(...)`, input arrives as `KeyEvent` /
  `MouseButtonEvent`, and screens are opened with `Minecraft.gui.setScreen(...)`.
- 26.2's Fabric API has no world-render event, so waypoint markers are projected
  to screen space and drawn on the HUD layer instead of as 3D beams.
- Build versions: Loom 1.19.3, Gradle 9.8.2, Java 25 toolchain.
- Mod Menu is optional: the `modmenu` entrypoint is only read when Mod Menu is
  installed, and the settings screen remains reachable with `F8`.

## License

MIT — see `LICENSE`.
