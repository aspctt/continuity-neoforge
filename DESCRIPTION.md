# <p align=center> Continuity </p>

![Version](https://img.shields.io/badge/Available_for-1.21.1-blue)
![Mod Loader](https://img.shields.io/badge/Mod_Loader-NeoForge-orange)
![Requires](https://img.shields.io/badge/Requires-Nothing-brightgreen)
![License](https://img.shields.io/badge/License-LGPL--3.0--only-blue)

Continuity makes resource packs that use the OptiFine connected textures, emissive textures, and custom block layers formats work without OptiFine. It is client side only, and this build is native NeoForge: no Sinytra Connector, no Fabric API, nothing else to install.

### Resource packs

A pack is read straight from its `optifine/ctm` directory as plain `.properties` files, the same layout OptiFine and Continuity on Fabric already use. Nothing needs converting.

Formally this implements the Continuity connected textures, emissive textures, and custom block layers specifications, each an extension of its OptiFine counterpart. They are documented on the [Continuity wiki](https://github.com/PepperCode1/Continuity/wiki), which remains the reference for pack authors.

Malformed files are reported rather than fatal. A properties file naming a block that is not installed, or a texture the pack forgot to ship, is logged with the file named and skipped, and the rest of the pack still loads.

### Built-in packs

Two packs ship with the mod, both off by default and enabled like any other:

**Default Connected Textures** --> connected glass, sandstone, and bookshelves, matching what OptiFine builds in

**Glass Pane Culling Fix** --> culls the faces between vertically stacked glass panes so they read as seamless

### A native NeoForge port

This is a port of [PepperCode1's Continuity](https://github.com/PepperCode1/Continuity), which is developed as a Fabric mod. Upstream releases marked for NeoForge are that Fabric build running under Connector with Forgified Fabric API beneath it. This one targets the NeoForge APIs directly, so install it instead of the upstream build rather than alongside it.

The goal is behavioural parity, not reinterpretation. Existing packs are the specification, so anything written for Continuity on Fabric is meant to look identical here.

Reaching that meant rebuilding the parts of the mod that leaned on the Fabric Rendering API, which NeoForge has no equivalent of:

Connection state --> resolved through the model data pipeline, which hands models the level and position once per block per chunk rebuild

Per-quad blend modes --> render types, declared per pass

Diffuse shading and ambient occlusion --> the flags vanilla quads already carry

Emissive quads --> forced to full brightness at draw time, covering both vanilla lighting and NeoForge's experimental light pipeline

Everything downstream of that, including the whole property format and every connection method, is upstream's work.

### Requirements

Minecraft 1.21.1 and NeoForge 21.1.0 or newer. Nothing else.

Runs in a large modpack alongside Sodium, Iris, ModernFix, Distant Horizons, C2ME, and Connector with Forgified Fabric API present. Connected textures have not yet been confirmed visually, so treat the rendering as untested until someone has looked at it.

### License

Continuity is licensed LGPL-3.0-only, the same as upstream, and the full terms are in [LICENSE](LICENSE).

This is a derivative work. It keeps the original licence, package names, and copyright, and is redistributed under the terms the original was published under. If you distribute the JAR, you must make the source available to whoever you distribute it to.
