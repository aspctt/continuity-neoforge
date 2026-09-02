# <p align=center> Continuity </p>

![Version](https://img.shields.io/badge/Available_for-1.21.1-blue)
![Mod Loader](https://img.shields.io/badge/Mod_Loader-NeoForge-orange)
![Requires](https://img.shields.io/badge/Requires-Nothing-brightgreen)
![License](https://img.shields.io/badge/License-LGPL--3.0--only-blue)

## Description

Continuity makes resource packs that use the OptiFine connected textures, emissive textures, and custom block layers formats work without OptiFine. It is client-side only.

This is a **native NeoForge port** of [PepperCode1's Continuity](https://github.com/PepperCode1/Continuity). The upstream project is a Fabric mod, and its NeoForge build runs through Sinytra Connector with Fabric API underneath. This fork removes that chain: it targets NeoForge directly, with no Connector and no Fabric API, and is built with ModDevGradle against official mappings.

The target is behavioural parity with upstream, not a reinterpretation. Existing packs are the specification. A pack written for Continuity on Fabric should look identical here, down to how ambiguous or malformed properties files are tolerated, so the same `optifine/ctm` directories keep working unchanged.

Two resource packs ship built in and are off by default. **Default Connected Textures** provides connected glass, sandstone, and bookshelves, matching what OptiFine builds in. **Glass Pane Culling Fix** culls the faces between vertically stacked glass panes so they read as seamless.

Formally the mod implements the Continuity connected textures, emissive textures, and custom block layers specifications, each an extension of its OptiFine counterpart. Those are documented on the [upstream wiki](https://github.com/PepperCode1/Continuity/wiki), which remains the reference for pack authors.

## Port status

**Builds and runs. Not yet visually verified.** The mod loads on NeoForge 21.1.249 for 1.21.1, every mixin applies, both built-in resource packs register, and the Default Connected Textures pack parses into its full set of quad processors with a world rendering and no errors. That holds with NeoForge's experimental light pipeline both off and on. What has not been confirmed is the last step: that glass in the world actually reads as connected. Treat it as untested until someone has looked at it.

Also untested: Sodium and Embeddium. The design should suit them, since connection state is resolved through the NeoForge model pipeline rather than a hook into vanilla chunk rendering, but that is an expectation and not a result.

The interesting part of the work is the renderer. Upstream is built on the Fabric Rendering API, whose mesh and quad-emitter model NeoForge has no equivalent of. Rather than reimplement that API wholesale, the processing pipeline here runs on a small quad abstraction backed directly by vanilla `BakedQuad` vertex arrays, and results are handed to the game the NeoForge way:

* Connection state is resolved in `BakedModel#getModelData`, which NeoForge calls once per block per chunk rebuild with the level and position available. No thread-local hack around `getQuads`.
* Per-quad blend modes become render types, surfaced through `getRenderTypes` and returned per pass from `getQuads`.
* Diffuse shading and ambient occlusion map onto the flags vanilla quads already carry.
* Emissive quads are tagged with a marker type and forced to full brightness at draw time, since NeoForge has no per-quad light override. Both lighting paths are covered: vanilla's, and the one NeoForge substitutes when its experimental light pipeline is enabled.

Two upstream mixins are gone rather than ported. Continuity on Fabric explicitly disables itself while falling blocks and piston-moved blocks render; on NeoForge both of those paths pass empty model data, so the same thing happens on its own.

## Installation

Place the JAR in your `mods` folder. There are no dependencies.

Resource packs are enabled the usual way, in Options then Resource Packs. Mod settings live in the NeoForge mod list, under Continuity.

## Dependencies

* Minecraft 1.21.1
* NeoForge 21.1.0 or newer

Neither Fabric API nor Sinytra Connector is required, or supported. Install the upstream build if you want the Connector route.

## Licensing

Continuity is licensed **LGPL-3.0-only**, the same as upstream. The full terms are in [LICENSE](./LICENSE).

This fork keeps the original licence, package names, and copyright. It is a derivative work of PepperCode1's Continuity and is redistributed under the terms that work was published under.

## Credits

### Upstream

* [PepperCode1](https://github.com/PepperCode1) - Continuity, its specifications, and effectively all of the logic this fork inherits

### This fork

* aspctt - NeoForge port

### Built on

* [NeoForge](https://neoforged.net/) - mod loader
