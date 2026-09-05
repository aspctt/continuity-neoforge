# <p align=center> Continuity (NeoForge) </p>

<div align="center">

![Version](https://img.shields.io/badge/Available_for-1.21.1-blue)
![Requires](https://img.shields.io/badge/Requires-Nothing-brightgreen)
![License](https://img.shields.io/badge/License-LGPL--3.0--only-red)

![NeoForge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/neoforge_vector.svg)
![Forge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/unsupported/forge_vector.svg)

[![GitHub](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/github_vector.svg)](https://github.com/aspctt/continuity-neoforge)
[![Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/modrinth_vector.svg)](https://modrinth.com/mod/continuity-neoforged)
[![CurseForge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/curseforge_vector.svg)](https://www.curseforge.com/minecraft/mc-mods/continuity-neoforged)

</div>

Continuity makes resource packs that use the OptiFine connected textures, emissive textures, and custom block layers formats work without OptiFine. It is client side only, and this build is native NeoForge: no Sinytra Connector, no Fabric API, nothing else to install.

### Resource packs

Packs are read straight from their `optifine/ctm` directory, the same layout OptiFine and Continuity on Fabric already use, so nothing needs converting. A file naming a block or texture that is not there is logged and skipped rather than breaking the rest of the pack.

The formats are documented on the [Continuity wiki](https://github.com/PepperCode1/Continuity/wiki).

### Built-in packs

Two packs ship with the mod, both off by default:

**Default Connected Textures** --> connected glass, sandstone, and bookshelves, matching what OptiFine builds in

**Glass Pane Culling Fix** --> culls the faces between stacked glass panes so they read as seamless

### A native NeoForge port

This is a port of [PepperCode1's Continuity](https://github.com/PepperCode1/Continuity), a Fabric mod whose NeoForge releases run under Connector with Forgified Fabric API beneath them. This build needs neither, so install it instead of the upstream one rather than alongside it.

Packs written for Continuity on Fabric are meant to look identical here. Connected textures are not yet visually confirmed on this build.

### Requirements

Minecraft 1.21.1 and NeoForge 21.1.0 or newer.

### License

LGPL-3.0-only, the same as upstream, with the full terms in [LICENSE](https://github.com/aspctt/continuity-neoforge/blob/main/LICENSE). This is a derivative work: if you distribute the JAR, you must make the source available to whoever you distribute it to.
