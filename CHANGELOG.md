# Changelog

All notable changes to CoordKit are documented here. This project follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
