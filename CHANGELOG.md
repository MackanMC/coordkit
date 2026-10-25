# Changelog

All notable changes to CoordKit are documented here. This project follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
  save folder instead of the game/instance directory.
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

[Unreleased]: https://github.com/MackanMC/coordkit/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/MackanMC/coordkit/releases/tag/v1.2.0
