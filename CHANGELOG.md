# Changelog

All notable changes to CoordKit are documented here. This project follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Author metadata and copyright now use the name **MackanMC**.

## [1.5.0] - 2026-10-26

### Added

- Individual HUD toggles for the coordinate line and the dimension line, so every
  info line can be shown or hidden on its own.
- Separate **Sky light** and **Block light** toggles and values, replacing the
  single combined light level. A 1.4.0 `showLight` setting is applied to both
  layers when the config is loaded.

### Changed

- The HUD settings tab has room for the new toggles and uses the full window
  height at the smallest supported GUI size.

## [1.4.0] - 2026-10-25

### Added

- **Export…** with three sets: **All waypoints**, **Visible here** (including
  global waypoints) and **Visible only**. JSON exports are written to
  `config/coordkit/exports/coordkit-<yyyyMMdd-HHmmss>.json`, copied to the
  clipboard and can be opened from the screen; share codes can be copied one per
  line.
- `CK1` share codes (name, coordinates, dimension, colour, world scope and
  visibility) with a per-row **Share** action. Codes over 240 characters are
  still copied but flagged as too long for one chat message.
- **Import…** from the clipboard or the latest export, with a preview of valid,
  duplicate and invalid entries and a scope policy: keep the original world,
  move to the current world, or make global.
- Additive, duplicate-safe imports. Existing waypoints are never overwritten,
  imported death waypoints become normal waypoints, and JSON imports are
  all-or-nothing.
- Flat waypoint groups: a 24-character group field in the editor with existing
  group suggestions, plus a **Groups…** screen to rename a group across all its
  waypoints or delete the label without deleting waypoints.
- Manager search over names and groups, plus scope, group and sort cycle
  buttons. Sorting supports name, group then name, and distance from the player.
- Per-row selection, **Select all** matching the filters across pages, and bulk
  **Show**, **Hide** and **Delete** actions. Bulk delete confirms with the count
  on the second click.
- `configVersion: 2` in the config, with automatic migration of pre-1.4.0 files
  and a `group` default.
- Tag-triggered GitHub release workflow with a build-only dry run, version/tag
  matching and a guard against overwriting an existing release.
- Unit tests for export sets, JSON round-trips, share codes, validation,
  duplicate detection, group rules and config migration.

### Changed

- Waypoint rows gained a selection button and a **Share** action. The bottom
  action row switches to bulk actions while a selection exists, so the manager
  keeps its height at small GUI scales.
- The waypoint editor gained a group field and a **Suggest** button.
- Configs are saved with `configVersion` and every waypoint's `group`, even when
  the group is empty.

### Notes

- 1.3.0 clients can still read a 1.4.0 config. They ignore `configVersion` and
  `group`, but if a 1.3.0 client saves the file it drops the group labels.
  Update every client before sharing a grouped list.
- Share codes do not carry a group label. Use a JSON export when groups must
  survive.
- Export files live in `config/coordkit/exports/`; **Import latest export** reads
  the newest JSON file in that folder.

## [1.3.0] - 2026-10-25

### Added

- Per-world/server waypoint scoping: new waypoints are only shown in the world or
  server where they were created. The editor can switch a waypoint between
  **World: This** and **World: All**.
- Automatic death waypoint, updating one waypoint per world and dimension. It can
  be disabled in settings, and editing it turns it into a normal waypoint.
- **F10** quick-add saves the player's current position immediately, with
  duplicate detection.
- **Add aim** in the waypoint manager saves the block under the crosshair, up to
  128 blocks away.
- Action-bar feedback for saving, deleting, toggling and quick-adding waypoints.
- GitHub Actions build workflow and unit tests for world-scope matching and
  config migration.

### Changed

- Waypoint coordinates are validated before saving or copying a teleport
  command; invalid input shows an error instead of silently using old values.
- Waypoint rows note when a waypoint belongs to another world.

### Fixed

- Waypoints no longer leak between singleplayer worlds: the scope now uses the
  level's save-folder name instead of the game/instance directory.
- Fixed the save-folder key including a trailing `.`, which made every
  singleplayer waypoint appear to belong to another world.
- Scoped waypoints stay hidden when the current world cannot be identified,
  instead of appearing in every world.
- Server scope keys are case-insensitive and ignore an explicit `:25565` port.

## [1.2.0] - 2026-10-25

### Added

- On-screen waypoint markers with a coloured diamond, name and distance.
- Markers stay visible through terrain and remain pinned to their world positions.
- Off-screen waypoints collapse to an edge arrow, with labels hidden until they are in view.
- Marker range setting (64–4096 m), optional edge arrows, and label collision handling.
- Two-tab settings screen at F8, plus optional Mod Menu integration.
- HUD lines for facing, biome, in-game time and weather, light level, speed and durability.
- Waypoint manager: edit names and coordinates, colour palette, visibility toggles, delete confirmation and TP command copying.
- Settings save immediately to `config/coordkit.json`.

### Changed

- Markers use the game's camera projection matrix, staying accurate with camera bobbing, roll and FOV changes.
- Marker art simplified to a shadowed diamond with a two-tone label.
- HUD panel and waypoint markers can be enabled independently.

### Fixed

- Markers no longer drift or sit on an offset when the camera moves.

[Unreleased]: https://github.com/MackanMC/coordkit/compare/v1.5.0...HEAD
[1.5.0]: https://github.com/MackanMC/coordkit/releases/tag/v1.5.0
[1.4.0]: https://github.com/MackanMC/coordkit/releases/tag/v1.4.0
[1.3.0]: https://github.com/MackanMC/coordkit/releases/tag/v1.3.0
[1.2.0]: https://github.com/MackanMC/coordkit/releases/tag/v1.2.0
