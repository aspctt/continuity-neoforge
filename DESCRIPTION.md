# <p align="center" style="text-align: center;">Continuity (NeoForge)</p>

<p align="center" style="text-align: center;"><img alt="Available for" src="https://img.shields.io/badge/Available_for-1.21.1_--_26.3-blue"> <img alt="Requires" src="https://img.shields.io/badge/Requires-Nothing-brightgreen"> <img alt="License" src="https://img.shields.io/badge/License-LGPL--3.0--only-red"></p>

<p align="center" style="text-align: center;"><img alt="Available for NeoForge" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/neoforge_vector.svg"> <img alt="Won't support Forge" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/unsupported/forge_vector.svg"></p>

<p align="center" style="text-align: center;"><a href="https://buymeacoffee.com/aspctt"><img alt="Buy Me a Coffee" height="40" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/donate/buymeacoffee-singular_vector.svg"></a></p>

<p align="center" style="text-align: center;"><a href="https://github.com/aspctt/continuity-neoforge"><img alt="Available on GitHub" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/github_vector.svg"></a> <a href="https://modrinth.com/mod/continuity-neoforged"><img alt="Available on Modrinth" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/modrinth_vector.svg"></a> <a href="https://www.curseforge.com/minecraft/mc-mods/continuity-neoforged"><img alt="Available on CurseForge" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/curseforge_vector.svg"></a></p>

Continuity makes resource packs that use OptiFine's connected textures, emissive textures and custom block layers work without OptiFine. It runs on the client only and is built for NeoForge itself, so it needs no Sinytra Connector, no Fabric API and nothing else.

### Resource packs

A pack describes each connected texture in a short `.properties` file in its `optifine/ctm` folder. This one comes from the built-in pack and joins black stained glass into one clean sheet:

```properties
method=ctm
matchTiles=black_stained_glass
matchBlocks=black_stained_glass
tiles=0-46
connect=block
resourceCondition=textures/block/black_stained_glass.png
```

`tiles=0-46` points at the 47 pieces of glass sitting next to the file, and the last line turns the file off when another pack retextures black stained glass. OptiFine and Continuity on Fabric read the same layout, so packs made for either work here without converting anything. A file naming a block or texture the game does not have is logged and skipped, and the rest of the pack loads as usual.

The full format is documented on the [Continuity wiki](https://github.com/PepperCode1/Continuity/wiki).

### Built-in packs

Two packs ship with the mod, and both stay off until you turn them on in Options, then Resource Packs:

**Default Connected Textures** --> connected glass, glass panes, sandstone and bookshelves, as OptiFine has them built in, with a matching set for Programmer Art

**Glass Pane Culling Fix** --> hides the faces between vertically stacked glass panes, so a column of panes reads as one

### Versions

Each Minecraft version below has its own file, and each file needs NeoForge from the same line:

- **1.21.1** --> NeoForge 21.1.0 or newer
- **1.21.11** --> NeoForge 21.11.0 or newer
- **26.1, 26.1.1 and 26.1.2** --> one shared file, NeoForge 26.1.0 or newer
- **26.2** --> NeoForge 26.2.0 or newer
- **26.3** --> NeoForge 26.3.0 or newer

1.21.2 to 1.21.10 got a single release, 3.0.2, and stay on it.

### Compatibility

Sodium is supported, and Continuity's options get a page of their own in its video settings. Connected textures have been seen working under it. Emissive textures are another story on 1.21.1, where they stay unlit while Sodium is installed, since Sodium replaces the part of the renderer that lights them. From 1.21.11 Sodium should light them itself. That comes from reading its source, and nobody has checked it in game yet.

ModernFix's dynamic resources option works on 1.21.1 and 26.1, the two versions here that ModernFix is out for, and connected and emissive textures apply to the models it bakes on demand. Embeddium has not been tested.

Emissive textures cover block and item models only, as in the original Continuity, so entities and worn armor stay as they are. Custom block layers stop working from 26.1, since the game no longer asks which layer a block goes in. The original Continuity dropped them on the same version, and the setting is left off the config screen there.

### A native NeoForge port

This is a port of [PepperCode1's Continuity](https://github.com/PepperCode1/Continuity), a Fabric mod whose NeoForge files run under Sinytra Connector with Forgified Fabric API beneath them. This build needs neither, so install it instead of the upstream one, not alongside it. Packs made for Continuity on Fabric should look exactly the same here, and if one does not, please report it on [GitHub](https://github.com/aspctt/continuity-neoforge/issues).

### License

LGPL-3.0-only, the same as upstream, with the full terms in [LICENSE](https://github.com/aspctt/continuity-neoforge/blob/main/LICENSE). This is a derivative work, so if you pass the jar on, you have to make the source available to whoever you give it to.
