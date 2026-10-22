# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [3.0.9] - 2026-10-21

### Fixed

- 1.21.1: A crash when placing rotated blocks with Additional Placements installed

## [3.0.8] - 2026-10-16

### Fixed

- 1.21.1: A crash when Quark and Supplementaries are installed together
- 1.21.1: Items held in hand showing the wrong model or position when their model uses NeoForge's separate transforms, composite or layered item models
- 26.3: The game failing to load with NeoForge 26.3.0.43-beta or newer

## [3.0.7] - 2026-09-24

### Added

- 1.21.1 and 26.1: Support for ModernFix's dynamic resources, so connected and emissive textures apply to the models it bakes on demand without making it bake every model up front

### Fixed

- Resource packs failing to load, and being removed, with ModernFix's dynamic resources on, as in All The Mods 11
- Resource packs failing to load when a mod's block or model refers to another mod that is not installed
- 1.21.1: A resource reload failure when a mod's model has a face without a texture

## [3.0.6] - 2026-09-23

### Added

- Minecraft 26.3 support, on NeoForge 26.3.0.1-beta or newer

### Fixed

- 1.21.11, 26.1 and 26.2: Emissive textures not showing on items that use item textures, such as a diamond

## [3.0.5] - 2026-09-23

### Fixed

- 1.21.11, 26.1 and 26.2: A crash when EvilCraft's Box of Eternal Closure came into view
- Mods that look up their own block models breaking, since Continuity replaced those models even when no resource pack had connected or emissive textures for them

## [3.0.4] - 2026-09-19

### Fixed

- Scrambled connected textures on most methods, with blocks showing stretched pieces of other textures, the built-in glass and bookshelves included
- 26.1 and 26.2: A crash when joining a world, on every world without Sodium and with Sodium once a resource pack used biome conditions, such as Patrix
- 26.1 and 26.2: A crash when a block with overlay tiles came into view, reported with Patrix in All the Mods 11

## [3.0.3] - 2026-09-16

### Removed

- Builds for 1.21.2 to 1.21.10, which stay on 3.0.2. Updates now cover 1.21.1, 1.21.11, 26.1 and 26.2

### Fixed

- 1.21.11, 26.1 and 26.2: Connected textures not working at all, since their tiles were added to the block atlas after Minecraft had already stitched it
- 26.1 and 26.2: Thousands of warnings with full stack traces in the log on every launch, one for each tile missing from the atlas
- 26.2: A deprecation warning on startup about how the mod icon is declared

## [3.0.2] - 2026-09-09

### Added

- Minecraft 1.21.2 to 26.2 support
- 1.21.1, 1.21.11, 26.1 and 26.2: A Continuity page in Sodium's video settings, sharing its options with the mod's own config screen

### Changed

- The in-game name is now Continuity: NeoForged

### Removed

- 26.1 and newer: Custom block layers, matching upstream. The game bakes the render layer onto each quad there, so the hook they relied on is gone, while connected and emissive textures are unaffected

### Fixed

- CTM rules with a `biomes=` clause never applying under Sodium, since the biome lookup came back empty outside vanilla chunk rendering

## [3.0.1] - 2026-09-03

### Changed

- The mod credits the port author alongside the original authors

## [3.0.0] - 2026-09-03

First release, a port of Continuity 3.0.0 to NeoForge for Minecraft 1.21.1.

[Unreleased]: https://github.com/aspctt/continuity-neoforge/compare/3.0.9...HEAD
[3.0.9]: https://github.com/aspctt/continuity-neoforge/compare/3.0.8...3.0.9
[3.0.8]: https://github.com/aspctt/continuity-neoforge/compare/3.0.7...3.0.8
[3.0.7]: https://github.com/aspctt/continuity-neoforge/compare/3.0.6...3.0.7
[3.0.6]: https://github.com/aspctt/continuity-neoforge/compare/3.0.5...3.0.6
[3.0.5]: https://github.com/aspctt/continuity-neoforge/compare/3.0.4...3.0.5
[3.0.4]: https://github.com/aspctt/continuity-neoforge/compare/3.0.3...3.0.4
[3.0.3]: https://github.com/aspctt/continuity-neoforge/compare/3.0.2...3.0.3
[3.0.2]: https://github.com/aspctt/continuity-neoforge/compare/3.0.1...3.0.2
[3.0.1]: https://github.com/aspctt/continuity-neoforge/compare/3.0.0...3.0.1
[3.0.0]: https://github.com/aspctt/continuity-neoforge/releases/tag/3.0.0
