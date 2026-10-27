# CoordKit

A tiny client-side Fabric mod for **Minecraft 26.2** ("Chaos Cubed").

It adds a compact info panel to the HUD, on-screen waypoint markers, saved waypoints,
a waypoint manager, a Nether/Overworld coordinate converter, a ruler tool, and a
settings screen — all without touching the server.

See [CHANGELOG.md](CHANGELOG.md) for release history.

## Features

### HUD
- X/Y/Z (2 decimals), block position, chunk position and dimension id.
- Facing (compass point + yaw), biome, in-game time and weather, separate sky and
  block light levels, movement speed and held-item durability.
- FPS and a real-world clock (both individually toggleable).
- Every HUD line can be switched off independently from **F8 → HUD** (the
  other-dimension line lives under **F8 → Navigation**), so the panel can show
  only coordinates, only FPS and coordinates, or any other combination.
- Nearby waypoints below the info lines: coloured dot, name and distance, sorted by
  distance and only shown for the dimension you are in.

### Nether ↔ Overworld conversion
- Optional extra HUD line with the matching position in the other dimension:
  Overworld → Nether divides X and Z by 8; Nether → Overworld multiplies them by 8.
  Y is unchanged.
- Only the Overworld and the Nether convert. The End and custom dimensions hide the
  line instead of showing a misleading value.
- The waypoint editor shows the counterpart position while you edit and can copy a
  dimension-aware `/execute in <dimension> run tp @s ...` command. A plain `/tp`
  does not change dimension, so the copy action uses `/execute in` deliberately.
- Off by default; enable it from **F8 → Navigation**.

### Measure tool
- Press **B** once to set point A, aim and press **B** again to set point B, and press
  **B** a third time to clear. The key is rebindable in *Controls*.
- Points snap to the centre of the block under the crosshair (up to 128 blocks). If
  no block is in range, the point is placed at your feet.
- While only A is set, a live ruler preview follows your crosshair.
- Shows straight-line distance, horizontal distance, ΔX/ΔY/ΔZ and the midpoint in a
  compact top-right panel, with a perspective-correct dotted line between the
  points. It also shows how many blocks are between the two points (counting full
  blocks along each axis).
- When B is set, chat offers clickable **Copy A**, **Copy B** and **Copy midpoint**
  actions.
- The measurement is transient: it is not saved and clears when you change dimension
  or leave the world.
- The ruler and its panel stay visible when the main HUD panel is disabled.

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
- **Share** copies the waypoint as a single-line `CK1` share code.
- **Group** waypoints with a free-text label (up to 24 characters); **Suggest**
  cycles through groups that already exist.
- Toggle a waypoint on/off with its **ON / OFF** button.
- **Del** asks for confirmation on the second click.
- Coordinates and names are editable; colours come from an 8-colour palette.
- New waypoints are scoped to the current world or server by default, so bases
  from one world do not appear in another. Old waypoints without a scope remain
  global; the editor can switch any waypoint between **World: This** and
  **World: All**.
- A death waypoint is created automatically when you die. The latest death in
  each world and dimension is updated in place; the setting can be turned off.

### Sharing and organising
- **Export…** offers three sets: **All waypoints**, **Visible here** (including
  global waypoints) and **Visible only** (visible in any world).
- Export as JSON to a timestamped file in `config/coordkit/exports/` and the
  clipboard, or as one `CK1` share code per line. **Open folder** shows the
  export location.
- Share codes over 240 characters are still copied but flagged as too long for
  one Minecraft chat message.
- **Import…** reads the clipboard or the latest export, previews valid,
  duplicate and invalid entries, and only then adds them.
- Imports are additive: duplicates are skipped, existing waypoints are never
  overwritten, and imported death waypoints become normal waypoints. JSON
  imports are all-or-nothing.
- The import scope policy can keep the original world, move waypoints to the
  current world, or make them global.
- Search matches names and groups. Scope, group and sort cycle buttons cover all
  worlds/this world, all/ungrouped/specific groups, and name/group/distance
  order.
- Select rows (or **Select all** matching the filters across pages) and use
  **Show**, **Hide** or **Delete**. Bulk delete confirms with the count on the
  second click.
- **Groups…** lists every group with its waypoint count. Rename updates all
  members; delete only clears the label and keeps the waypoints.


### Screens
- **F8** opens the settings screen, which has three tabs:
  - **HUD**: panel on/off, coordinates, FPS, clock, chunk line, dimension,
    facing, biome, time, sky light, block light, speed, durability, waypoint
    list, markers and edge arrows.
  - **Waypoints**: max waypoints on the HUD, marker range, waypoint list / marker
    toggles, and a shortcut to the manager.
  - **Navigation**: the other-dimension coordinate toggle and a reminder of how
    the measure tool works.
- The waypoint manager also opens the export, import and group screens.
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
| `B` | Measure point A → B → clear |

All six keybinds can be changed in *Options → Controls → Key Binds*
(category: CoordKit).

## Config file

`config/coordkit.json` is created on first save:

```json
{
  "configVersion": 2,
  "showHud": true,
  "showCoordinates": true,
  "showConvertedCoords": false,
  "showDimension": true,
  "showFps": true,
  "showClock": true,
  "showChunk": true,
  "showWaypoints": true,
  "maxWaypoints": 5,
  "showFacing": true,
  "showBiome": true,
  "showGameTime": true,
  "showSkyLight": false,
  "showBlockLight": false,
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
      "autoDeath": false,
      "group": "Home"
    }
  ]
}
```

A pre-1.4.0 file has no `configVersion` or `group`; it is upgraded on load and
saved with `configVersion: 2`. A single `showLight` value from 1.4.0 is split
across `showSkyLight` and `showBlockLight` on the next load. Configs created
before the converted-coordinate line load with `showConvertedCoords: false`, so
the extra line stays hidden until it is enabled. Older CoordKit versions can
still read a newer config, but they ignore fields they do not know (including
the split light toggles and the converted-coordinate toggle) and drop `group` if
they save it.

Deleting the file resets everything to defaults.

## Roadmap

- **Navigation**: dimension conversion and the measure tool are in the unreleased
  build. Tracked waypoints, a compass strip, arrival alerts, death history and
  routes are the next step.
- **HUD & marker customisation**: further layout, scale, marker style and display
  options. 1.5.0 added per-line HUD toggles and split sky/block light.

1.5.0 added finer HUD control: every info line can be toggled independently, and
sky and block light are shown as separate values. 1.4.0 shipped sharing and
organising: export/import, `CK1` share codes, groups, search/filter/sort and bulk
actions ([spec](docs/1.4.0-spec.md)).

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

The mod jar ends up in `build/libs/coordkit-1.5.1.jar`.

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
    ├── ScreenProjection.java          shared world-to-screen projection
    ├── CoordKitConfig.java            JSON config + waypoint list
    ├── Waypoint.java                  waypoint data model
    ├── DimensionMath.java             Overworld/Nether coordinate conversion
    ├── MeasurePoint.java              ruler point
    ├── MeasureMath.java               distance, delta and midpoint helpers
    ├── MeasureState.java              transient A → B → clear state
    ├── MeasureOverlay.java            ruler line, endpoint markers and panel
    ├── WaypointActions.java           shared create, quick-add and death logic
    ├── WaypointScope.java             world/server identity and visibility
    ├── WaypointQuery.java             search, filters, sorting and export sets
    ├── WaypointGroups.java            group listing, rename and clear rules
    ├── WaypointSharing.java           JSON/share-code codec + import planning
    ├── WaypointExports.java           export folder, file names and latest export
    ├── ClientFeedback.java            action-bar and chat helpers
    ├── WaypointStyle.java             colour palette
    ├── integration/
    │   └── ModMenuIntegration.java    optional Mod Menu config-screen hook
    └── ui/
        ├── CoordKitConfigScreen.java  tabbed settings screen
        ├── WaypointManagerScreen.java scrollable waypoint list + bulk actions
        ├── WaypointEditScreen.java    add/edit a waypoint
        ├── WaypointExportScreen.java  export set/format and file output
        ├── WaypointImportScreen.java  clipboard/export preview and import
        ├── WaypointGroupsScreen.java  group list with rename/delete
        └── GroupRenameScreen.java     rename one group
src/test/java/com/mackan/coordkit/client/
├── CoordKitConfigTest.java            config defaults + migrations
├── DimensionMathTest.java             Overworld/Nether conversion rules
├── MeasureMathTest.java               distance, delta and midpoint maths
├── MeasureStateTest.java              A → B → clear state machine
├── WaypointScopeTest.java             world-scope matching and visibility
├── WaypointSharingTest.java           JSON/share-code round-trips + validation
├── WaypointQueryTest.java             search, filters, sorting, export sets
├── WaypointExportsTest.java           export folder and file naming
└── WaypointGroupsTest.java            group listing, rename and clear
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
- Exports are written to `config/coordkit/exports/`; **Import latest export**
  reads the newest file there. Share codes carry no group label, so import JSON
  when groups must survive.
- Older clients (1.4.0 and earlier) can read a 1.5.0 config, but they ignore
  fields they do not know (the coordinates/dimension toggles and split light
  settings), and a 1.3.0 save drops the group labels. Update everyone before
  sharing a grouped list.
- Dimension conversion is display-only. Saved waypoints, share codes and configs
  keep their original dimension and coordinates; the counterpart line and copy
  action are derived when they are shown.
- The measure tool is transient. It has no config fields, is never written to
  `coordkit.json`, and clears when you change dimension or leave the world.
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
