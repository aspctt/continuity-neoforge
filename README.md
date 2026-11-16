# <p align=center style="text-align: center;"> Continuity (NeoForge) </p>

<p align="center" style="text-align: center;">
	<img alt="Available for" src="https://img.shields.io/badge/Available_for-1.21.1_--_26.3-blue">
	<img alt="Requires" src="https://img.shields.io/badge/Requires-Nothing-brightgreen">
	<img alt="License" src="https://img.shields.io/badge/License-LGPL--3.0--only-red">
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

Continuity makes resource packs that use OptiFine's connected textures, emissive textures and custom block layers
work without OptiFine. This is a port of [PepperCode1's Continuity](https://github.com/PepperCode1/Continuity) that
runs on NeoForge directly, without the Sinytra Connector and Fabric API that upstream's NeoForge files run under. It
is client side only, built with ModDevGradle against official mappings, and covers 1.21.1, 1.21.11 and 26.1 to 26.3.

A pack describes each connected texture in a `.properties` file under its `optifine/ctm` folder. The built-in Default
Connected Textures pack joins black stained glass with this one:

```properties
method=ctm
matchTiles=black_stained_glass
matchBlocks=black_stained_glass
tiles=0-46
connect=block
resourceCondition=textures/block/black_stained_glass.png
```

`tiles=0-46` names the 47 pieces in the same folder, and `resourceCondition` turns the file off when another pack
retextures black stained glass. Packs written for OptiFine or for Continuity on Fabric use the same layout and should
look identical here, down to how loosely a broken or ambiguous file is read. The formats are documented on the
[upstream wiki](https://github.com/PepperCode1/Continuity/wiki), which stays the reference for pack authors.

## Installation

Put the jar for your Minecraft version in the `mods` folder, in place of upstream Continuity rather than next to it.
There are no other dependencies.

| Jar suffix | Minecraft | NeoForge |
|---|---|---|
| `+1.21.1` | 1.21.1 | 21.1.0 or newer |
| `+1.21.11` | 1.21.11 | 21.11.0 or newer |
| `+26.1.2` | 26.1, 26.1.1 and 26.1.2 | 26.1.0 or newer |
| `+26.2` | 26.2 | 26.2.0 or newer |
| `+26.3` | 26.3 | 26.3.0 or newer |

1.21.2 to 1.21.10 had one release, 3.0.2, and stay on it.

Two resource packs come built in, both off by default. Default Connected Textures connects glass, glass panes,
sandstone and bookshelves the way OptiFine does, with a matching set for Programmer Art, and Glass Pane Culling Fix
hides the faces between vertically stacked panes. Turn them on in Options, then Resource Packs. The mod's own settings
are under Continuity: NeoForged in the NeoForge mod list and on a page of their own in Sodium's video settings, and
they are saved to `config/continuity.json`.

## Compatibility and known issues

Sodium resolves block models through the same NeoForge methods this port implements, `getModelData`,
`getRenderTypes` and `getQuads` on 1.21.1 and `collectParts` from 1.21.11, so connected textures reach it the way
they reach vanilla, and they have been seen working under it. Emissive textures are another matter on 1.21.1. There
an emissive quad is marked with a subclass that Continuity's mixins into vanilla's and NeoForge's block renderers look
for, and Sodium replaces both, so emissive textures stay unlit while Sodium is installed. From 1.21.11 the quad
carries a light emission of 15 instead, which Sodium reads on its own. That part comes from reading Sodium's source
and has not been checked in game.

ModernFix's dynamic resources option bakes a block model only when something first asks for it, and can drop it and
bake it again later, long after the baking event Continuity normally wraps models in. On 1.21.1 and 26.1, the two
targets ModernFix has NeoForge builds for, ModernFix tells Continuity through its integration entry point when the
option is on. Continuity then builds its processors before baking starts and wraps each model as it is baked, through
the same lambda the game and ModernFix both bake through. Connected and emissive textures have been seen working that
way on both targets.

Embeddium has not been tested or looked at.

Emissive textures cover block and item models only, as upstream, so entities and worn armor stay as they are.
Custom block layers are not supported from 26.1. The feature answered the per block layer question the game used to
ask, and 26.1 stopped asking it, since the layer is baked onto each quad instead. Upstream dropped the feature on the
same version for the same reason, and the setting is left out of the config screen there rather than shown doing
nothing.

## How the port works

Upstream is built on the Fabric Rendering API, whose mesh and quad emitter model NeoForge has no equivalent of.
Rather than reimplement that API wholesale, the processing pipeline here runs on a small quad abstraction backed
directly by vanilla `BakedQuad` vertex arrays, and the results are handed to the game the NeoForge way. On 1.21.1
that means:

- Connection state is resolved in `BakedModel#getModelData`, which NeoForge calls once per block per chunk rebuild
	with the level and position available, so no thread local is needed around `getQuads`.
- Per quad blend modes become render types, surfaced through `getRenderTypes` and returned per pass from `getQuads`.
- Diffuse shading and ambient occlusion map onto the flags vanilla quads already carry.
- Emissive quads are tagged with a marker type and forced to full brightness at draw time, since NeoForge has no per
	quad light override. Both lighting paths are covered, vanilla's and the one NeoForge substitutes when its
	experimental light pipeline is on, and connected textures have been seen working with the pipeline off and on.

From 1.21.5 the quad carries a light emission value that stands in for the marker type, and from 26.1 the renderer
folds emission into the lightmap itself, so neither lighting mixin is needed there. The version bands under Building
cover the rest.

Two upstream mixins are gone rather than ported. Continuity on Fabric disables itself while falling blocks and blocks
moved by pistons render, and on NeoForge both of those paths pass empty model data, so the same thing happens without
them.

## Building

The build uses [Stonecutter](https://stonecutter.kikugie.dev/) to compile one source tree against several Minecraft
versions. Each target is a subproject under `versions/`, declared in `settings.gradle.kts` and configured by its own
`gradle.properties`, and the shared build script is `build.gradle.kts` at the root.

```
./gradlew build                 # build every target
./gradlew :1.21.1:build         # build one
./gradlew :1.21.1:runClient     # run one, sharing the root run/ directory
```

`runClient` puts Sodium on the classpath as well, so a run always tests Continuity under Sodium. It is left out of
everything that gets published.

| Target | Covers | Built against NeoForge | Model API |
|---|---|---|---|
| 1.21.1 | 1.21.1 | 21.1.249 | baked model |
| 1.21.11 | 1.21.11 | 21.11.45 | block state model |
| 26.1 | 26.1, 26.1.1 and 26.1.2 | 26.1.2.103 | block state model |
| 26.2 | 26.2 | 26.2.0.76 | block state model |
| 26.3 | 26.3 | 26.3.0.12-beta | block state model |

Every target starts with all mixins applied and both built-in packs registered, parses the Default Connected Textures
pack into the same 42 quad processors with no errors of Continuity's own, and has been seen rendering connected
textures in game. The 26.3 target is still built against a beta from before NeoForge 26.3 left beta at 26.3.0.76.

The 26.1 target is the only one covering more than a single version. Nothing in this mod tells 26.1, 26.1.1 and
26.1.2 apart. All three compile to byte identical classes, every injection point resolves against each, and they
share a resource pack format.

Bands A and B keep one target each, which is enough to keep all three code paths compiled and checked. The directives
in the source still name the versions in between, because that is where the changes they guard landed, so a target
could be added back without rewriting them. 1.21.2 to 1.21.10 were built and released once, at 3.0.2, and their
targets were removed after that release rather than left building. Commit `bc6f405` is the last one that declares
all twelve, so an archived version can still be rebuilt from there.

### Checking injection points

Mixin targets are annotation strings, so javac never sees them, and a descriptor that drifted between versions
compiles cleanly and fails at runtime. `tools/check-mixin-targets.py` checks every `method` and `@At` target against
the bytecode of each target's own jars with `javap`, following supertypes so that NeoForge's interface extensions
count. It has caught two injections that compiled but would not have applied. `SpriteSourceList.list` gained a second
argument in 1.21.10 and the one-argument overload became a delegate with no injection point left in it, and
`SpriteLoader.loadAndStitch` narrowed its last argument from a collection to a set at the same version.

```
./gradlew :1.21.11:build
python tools/check-mixin-targets.py 1.21.11
```

### Checking the oldest NeoForge each jar accepts

Each jar is compiled once, against one NeoForge build, but its metadata accepts every build back to the start of its
line, and the 26.1 jar, built on 26.1.2, accepts 26.1 and 26.1.1 as well. `tools/check-linkage.py` reads every game
class, method and field the compiled jar refers to and checks that each exists in another build, and the mixin check
takes that build with `--neoforge`. CI runs both against the oldest build each range accepts, and the oldest of each
26.1 line. Every target passes there, so no range has needed raising.

```
./gradlew :26.1:writeCompileClasspath -Pneo_version=26.1.0.1-beta -Pminecraft_version=26.1
python tools/check-linkage.py 26.1 26.1.0.1-beta
python tools/check-mixin-targets.py 26.1 --neoforge 26.1.0.1-beta
```

### Where the version bands fall

Minecraft replaced `BakedModel` with `BlockStateModel` in 1.21.5, and with it the whole basis the 1.21.1 port stands
on. `ModelData`, `ChunkRenderTypeSet` and `ModelProperty` are gone, so connection state can no longer be resolved in
`getModelData` and stashed for `getQuads` to read back. Upstream renamed its own model classes at exactly the same
version. That splits the range into three bands rather than one gradient:

| Band | Versions | Model API | Targets |
|---|---|---|---|
| A | 1.21 to 1.21.4 | `BakedModel` and model data | 1.21.1 |
| B | 1.21.5 to 1.21.11 | `BlockStateModel` and block model parts | 1.21.11 |
| C | 26.1 to 26.3 | `BlockStateModel`, reworked again | 26.1, 26.2 and 26.3 |

Within a band the differences are small enough for `//?` directives, and across one they are not, so bands A and B
each have a model layer of their own under `client/model/`, `bakedmodel` and `blockstatemodel`, while band C reuses
band B's. Only the package matching the target is compiled. They sit under `src/main/java` rather than in separate
source roots because that is the only tree Stonecutter preprocesses. Band A's `CtmBakedModel` and
`EmissiveBakedModel` sit directly in `client/model/` instead, under the names the original Continuity gave them,
because other mods look them up by name.

Band B is the simpler of the two. NeoForge hands the level and position straight to `collectParts`, so results no
longer go through model data, and a render type belongs to a part rather than to a set declared up front.

Two version differences matter when adding further targets. `BakedQuad` gained a light emission argument in 1.21.2,
and several methods the mixins target changed signature or became static across 1.21.x, so those injections match by
name and capture only the argument they need rather than restating a signature that will not survive. Minecraft also
relaxed sprite path validation in 1.21.2, which let the reserved path machinery shrink from an index table and three
supporting mixins to a single rewrite.

### What changed after 1.21.5

None of these was another break on the scale of 1.21.5, though 1.21.11 came closest.

| Version | What moved |
|---|---|
| 1.21.6 | chunk layers left `RenderType` for a `ChunkSectionLayer` enum of their own, and `RenderChunkRegion` became `RenderSectionRegion` |
| 1.21.8 | nothing beyond 1.21.6 |
| 1.21.9 | `AtlasSet` gave way to `SpriteLoader.Preparations`, reload listeners take a shared state rather than a resource manager, and pack metadata replaced `pack_format` with `min_format` and `max_format`. 1.21.10 changed nothing further |
| 1.21.11 | `ResourceLocation` became `Identifier`, `BakedQuad` stopped being backed by a vertex array, and blocks and items are stitched onto separate atlases |

The 1.21.11 quad change is the substantive one. Positions are now `Vector3fc`, texture coordinates are packed into
longs, and colors and normals moved into `BakedColors` and `BakedNormals`. The internal layout this port keeps did not
change, and only the two methods that convert to and from a vanilla quad had to be rewritten, so the processors and
the model layer above them were untouched. The version had smaller consequences too. The mipped and unmipped cutout
chunk layers merged into one, sprites are padded on the atlas instead of having their UVs shrunk, and emissive
textures have to be paired on the new item atlas as well as the block atlas.

### What band C needed

26.1 turned out to be less of a break than its error count suggested, once the errors were read rather than counted.
`collectParts` and `getQuads` kept their shape, so the band B model layer carried across with directives and no third
package was needed.

| Change | What it meant |
|---|---|
| The client model packages moved, and `BlockModelPart` became `BlockStateModelPart` | A replacement pass. This mod's own part was renamed to `ProcessedModelPart` first so the pattern could not catch it |
| `BakedQuad` gathered the sprite, layer, tint and shading into one `MaterialInfo` | The quad bridge was rebuilt around it. The layout this port keeps internally did not change, so the processors above it were untouched |
| The chunk layer moved from the part onto the quad | This simplified things. A quad knows its own layer, so the processed parts collapsed from one per layer to a single part covering all of them |
| The renderer folds a quad's light emission into the lightmap itself | Both mixins that forced the light of an emissive quad became unnecessary and are left out |
| `ItemBlockRenderTypes`, `BlockModelShaper` and `ItemRenderer` are gone | Two of the three had somewhere new to attach. The third took custom block layers with it |

Minecraft also moved to Java 25 on 26.1, so the toolchain and the mixin compatibility level follow the target.

### Releasing

`./gradlew publishMods` uploads every target with its sources jar to Modrinth and CurseForge, and creates one GitHub
release for the version with the main jars attached, using
[mod-publish-plugin](https://modmuss50.github.io/mod-publish-plugin/). Each platform gets the targets one at a time,
from the oldest Minecraft version to the newest. The notes come from the version's section of `CHANGELOG.md`, and the
portals list each jar for the Minecraft versions in `release_minecraft_versions`.

Releases normally go out through the Release workflow in the Actions tab, started by hand with Run workflow. Push the
version's tag first, since the workflow refuses to run unless the tag exists and marks the commit it is building.
Ticking "dry run" shows what would be uploaded without sending anything. The workflow reads `MODRINTH_TOKEN`, a
Modrinth personal access token that can create, read and write versions, and `CURSEFORGE_TOKEN`, a CurseForge API
token, from the repository's Actions secrets, and uses its own token for GitHub.

To publish from a local machine instead, set those two tokens and a `GITHUB_TOKEN` that can write the repository's
contents as environment variables, then run `./gradlew publishMods`. With any of the three missing the run is a dry
run, which uploads nothing and writes what it would have sent under each project's `build/publishMods`.

## License

Continuity is licensed **LGPL-3.0-only**, the same as upstream, with the full terms in [LICENSE](./LICENSE). This port
keeps the original license, package names and copyright, and as a derivative work of PepperCode1's Continuity it is
published under the same terms.

## Credits

[PepperCode1](https://github.com/PepperCode1) wrote Continuity, its formats and effectively all of the logic this port
inherits. aspctt ported it to [NeoForge](https://neoforged.net/).
