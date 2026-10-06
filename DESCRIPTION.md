# <p align=center style="text-align: center;"> Continuity (NeoForge) </p>

<p align="center" style="text-align: center;">
	<img alt="Available for" src="https://img.shields.io/badge/Available_for-1.21.1_--_26.3-blue">
	<img alt="Requires" src="https://img.shields.io/badge/Requires-Nothing-brightgreen">
	<img alt="License" src="https://img.shields.io/badge/License-LGPL--3.0--or--only-red">
</p>

<p align="center" style="text-align: center;">
	<img alt="Available for NeoForge" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/neoforge_vector.svg">
	<img alt="Won't support Forge" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/unsupported/forge_vector.svg">
</p>

<p align="center" style="text-align: center;">
	<a alt="Buy Me a Coffee" href="https://buymeacoffee.com/aspctt"><img alt="Buy Me a Coffee" height="40" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/donate/buymeacoffee-singular_vector.svg"></a>
</p>

<p align="center" style="text-align: center;">
	<a alt="Available on GitHub" href="https://github.com/aspctt/continuity-neoforge"><img alt="Available on GitHub" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/github_vector.svg"></a>
	<a alt="Available on Modrinth" href="https://modrinth.com/mod/continuity-neoforged"><img alt="Available on Modrinth" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/modrinth_vector.svg"></a>
	<a alt="Available on CurseForge" href="https://www.curseforge.com/minecraft/mc-mods/continuity-neoforged"><img alt="Available on CurseForge" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/curseforge_vector.svg"></a>
</p>

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

Packs written for Continuity on Fabric are meant to look identical here.

### Requirements

NeoForge, and nothing else. A separate file is built for every Minecraft version from 1.21.1 through 1.21.11, and for 26.1, 26.2 and 26.3. Download the one matching your Minecraft version; each needs the NeoForge line that goes with it, so the 1.21.8 build wants NeoForge 21.8.0 or newer and the 26.3 build wants 26.3.0 or newer.

### License

LGPL-3.0-only, the same as upstream, with the full terms in [LICENSE](https://github.com/aspctt/continuity-neoforge/blob/main/LICENSE). This is a derivative work: if you distribute the JAR, you must make the source available to whoever you distribute it to.
