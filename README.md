# <p align=center> Continuity </p>

<div align="center">

![Version](https://img.shields.io/badge/Available_for-1.21.1_--_26.2-blue)
![Requires](https://img.shields.io/badge/Requires-Nothing-brightgreen)
![License](https://img.shields.io/badge/License-LGPL--3.0--only-red)

![NeoForge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/neoforge_vector.svg)
![Forge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/unsupported/forge_vector.svg)

[![GitHub](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/github_vector.svg)](https://github.com/aspctt/continuity-neoforge)
[![Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/modrinth_vector.svg)](https://modrinth.com/mod/continuity-neoforged)
[![CurseForge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/curseforge_vector.svg)](https://www.curseforge.com/minecraft/mc-mods/continuity-neoforged)

</div>

## Description

Continuity makes resource packs that use the OptiFine connected textures, emissive textures, and custom block layers formats work without OptiFine. It is client-side only.

This is a **native NeoForge port** of [PepperCode1's Continuity](https://github.com/PepperCode1/Continuity). The upstream project is a Fabric mod, and its NeoForge build runs through Sinytra Connector with Fabric API underneath. This fork removes that chain: it targets NeoForge directly, with no Connector and no Fabric API, and is built with ModDevGradle against official mappings.

The target is behavioural parity with upstream, not a reinterpretation. Existing packs are the specification. A pack written for Continuity on Fabric should look identical here, down to how ambiguous or malformed properties files are tolerated, so the same `optifine/ctm` directories keep working unchanged.

Two resource packs ship built in and are off by default. **Default Connected Textures** provides connected glass, sandstone, and bookshelves, matching what OptiFine builds in. **Glass Pane Culling Fix** culls the faces between vertically stacked glass panes so they read as seamless.

Formally the mod implements the Continuity connected textures, emissive textures, and custom block layers specifications, each an extension of its OptiFine counterpart. Those are documented on the [upstream wiki](https://github.com/PepperCode1/Continuity/wiki), which remains the reference for pack authors.

## Port status

**Builds and runs. Connected textures confirmed on 1.21.1.** Every declared target loads: all mixins apply, both built-in resource packs register, and the Default Connected Textures pack parses into the same 42 quad processors with no errors of Continuity's own. On 1.21.1 that has been carried the last step, and glass in the world does read as connected, with NeoForge's experimental light pipeline both off and on. On the other targets the final visual check has not been done.

Sodium fits by design rather than by luck, and the parts that can be checked without looking at a screen have been. It resolves block models through the same NeoForge entry points this port implements: `getModelData`, `getRenderTypes` and `getQuads` below 1.21.5, `collectParts` above it. So connected textures reach it the way they reach vanilla. Emissive quads carry a `lightEmission` of 15, which Sodium reads back on every version from 1.21.2. On 1.21.1 that field does not exist, only the marker type does, and Sodium replaces the renderer that reads it, so emissive textures stay unlit under Sodium on that one version. Continuity's options appear as their own page in Sodium's video settings, which has been seen working. Everything but the config page is read off Sodium's source rather than observed, so the rendering itself is still unverified.

Embeddium is untested and unexamined.

The interesting part of the work is the renderer. Upstream is built on the Fabric Rendering API, whose mesh and quad-emitter model NeoForge has no equivalent of. Rather than reimplement that API wholesale, the processing pipeline here runs on a small quad abstraction backed directly by vanilla `BakedQuad` vertex arrays, and results are handed to the game the NeoForge way:

* Connection state is resolved in `BakedModel#getModelData`, which NeoForge calls once per block per chunk rebuild with the level and position available. No thread-local hack around `getQuads`.
* Per-quad blend modes become render types, surfaced through `getRenderTypes` and returned per pass from `getQuads`.
* Diffuse shading and ambient occlusion map onto the flags vanilla quads already carry.
* Emissive quads are tagged with a marker type and forced to full brightness at draw time, since NeoForge has no per-quad light override. Both lighting paths are covered: vanilla's, and the one NeoForge substitutes when its experimental light pipeline is enabled.

That is the 1.21.1 baseline. From 1.21.5 the quad carries a `lightEmission` field that stands in for the marker type, and from 26.1 the renderer folds emission into the lightmap on its own, so neither mixin is needed there. The sections below track the rest.

Two upstream mixins are gone rather than ported. Continuity on Fabric explicitly disables itself while falling blocks and piston-moved blocks render; on NeoForge both of those paths pass empty model data, so the same thing happens on its own.

## Building

The build is organised with [Stonecutter](https://stonecutter.kikugie.dev/), which compiles one source tree
against several Minecraft versions. Each target is a subproject under `versions/`, declared in
`settings.gradle.kts` and configured by its own `gradle.properties`; the shared build script is
`build.gradle.kts` at the root.

```
./gradlew build                 # build every declared version
./gradlew :1.21.1:build         # build one
./gradlew :1.21.1:runClient     # run one, sharing the root run/ directory
```

Declared targets and where they stand:

| Target | Covers | NeoForge | Model API | Builds | Loads and reads packs | Rendering confirmed |
|---|---|---|---|---|---|---|
| 1.21.1 | 1.21.1 | 21.1.249 | baked model | yes | yes | connected textures confirmed |
| 1.21.11 | 1.21.11 | 21.11.45 | block state model | yes | yes | not yet |
| 26.1 | 26.1, 26.1.1, 26.1.2 | 26.1.2.103 | block state model | yes | yes | not yet |
| 26.2 | 26.2 | 26.2.0.76 | block state model | yes | yes | not yet |

Every target starts: all mixins apply, both built-in packs register, and the Default Connected Textures pack
parses into the same 42 quad processors with no errors of Continuity's own.

Connected textures have only been confirmed rendering correctly on 1.21.1. The rest are unverified in game.

One target per model API band is kept, which is what holds all three code paths compiled and checked. The
directives in the source still name the versions in between, because that is where the changes they guard
actually landed, and a target could be added back without rewriting them.

1.21.2 through 1.21.10 were built and released once, at 3.0.2, and are not carried forward: no fixes, no
updates. Their targets were removed after that release rather than left building. Commit `bc6f405` is the last
one that declares all twelve, so an archived version can still be rebuilt from there if it ever has to be.

The one target covering more than a single version is 26.1, which serves 26.1 and 26.1.1 as well as 26.1.2.
Nothing in this mod tells those apart: all three compile to byte identical classes, every injection point
resolves against each, and they share a resource pack format.

Emissive textures are linked on the block atlas only, on every version, which is what upstream does too. From
1.21.11 items are stitched onto an atlas of their own, so a texture that lives only there has no emissive
counterpart. Block models drawn as items, which is what emissive packs mostly target, are unaffected.

Custom block layers are not supported from 26.1. The feature worked by answering the per block layer question
the game used to ask, and 26.1 stopped asking it: the layer is baked onto each quad instead. Upstream dropped
the feature on the same version for the same reason. The setting is left out of the config screen there rather
than left showing and doing nothing.

### Checking injection points

Mixin targets are annotation strings, so javac never sees them: a descriptor that drifted between versions
compiles cleanly and fails at runtime. Every `method` and `@At` target is therefore checked against the
bytecode of each target's own jars with `javap`, following supertypes so that NeoForge's interface extensions
count. That check found two injections that compiled but would not have applied: `SpriteSourceList.list` gained
a second argument in 1.21.10 and the one-argument overload became a delegate with no injection point left in
it, and `SpriteLoader.loadAndStitch` narrowed its last argument from a collection to a set at the same version.

```
./gradlew :1.21.11:build
python tools/check-mixin-targets.py 1.21.11
```

### What changed after 1.21.5

None of these was another architectural break on the scale of 1.21.5, but 1.21.11 came closer than the others.

| Version | What moved |
|---|---|
| 1.21.6 | chunk layers left `RenderType` for a `ChunkSectionLayer` enum of their own, and `RenderChunkRegion` became `RenderSectionRegion` |
| 1.21.8 | nothing beyond 1.21.6 |
| 1.21.9 | `AtlasSet` gave way to `SpriteLoader.Preparations`, reload listeners take a shared state rather than a resource manager, and pack metadata replaced `pack_format` with `min_format` and `max_format`. 1.21.10 changed nothing further |
| 1.21.11 | `ResourceLocation` became `Identifier`, `BakedQuad` stopped being backed by a vertex array, and blocks and items are stitched onto separate atlases |

The 1.21.11 quad change is the substantive one. Positions are now `Vector3fc`, texture coordinates are packed
into longs, and colours and normals moved into `BakedColors` and `BakedNormals`. The internal layout this port
keeps is unchanged; only the two methods that convert to and from a vanilla quad had to be rewritten, so the
processors and the model layer above them were untouched. Two smaller consequences: the mipped and unmipped
cutout chunk layers merged into one, and sprites are padded on the atlas instead of having their UVs shrunk.

### What band C needed

26.1 turned out to be less of a break than its error count suggested, once the errors were read rather than
counted. `collectParts` and `getQuads` kept their shape, so the band B model layer carried across with
directives and no third package was needed.

| Change | What it meant |
|---|---|
| The client model packages moved, and `BlockModelPart` became `BlockStateModelPart` | A replacement pass. This mod's own part was renamed to `ProcessedModelPart` first so the pattern could not catch it |
| `BakedQuad` gathered the sprite, layer, tint and shading into one `MaterialInfo` | The quad bridge was rebuilt around it. The layout this port keeps internally did not change, so the processors above it were untouched |
| The chunk layer moved from the part onto the quad | This simplified things. A quad knows its own layer, so the processed parts collapsed from one per layer to a single part covering all of them |
| The renderer folds a quad's light emission into the lightmap itself | Both mixins that forced the light of an emissive quad became unnecessary and are left out |
| `ItemBlockRenderTypes`, `BlockModelShaper` and `ItemRenderer` are gone | Two of the three had somewhere new to attach. The third took custom block layers with it |

Minecraft also moved to Java 25 on 26.1, so the toolchain and the mixin compatibility level follow the target.

### Where the version bands fall

Minecraft replaced `BakedModel` with `BlockStateModel` in 1.21.5, and with it the whole basis this port stands
on: `ModelData`, `ChunkRenderTypeSet` and `ModelProperty` are gone, so connection state can no longer be
resolved in `getModelData` and stashed for `getQuads` to read back. Upstream renamed its own model classes at
exactly the same version. That splits the range into three bands, not one gradient:

| Band | Versions | Model API | State |
|---|---|---|---|
| A | 1.21 - 1.21.4 | `BakedModel` and model data | working |
| B | 1.21.5 - 1.21.11 | `BlockStateModel` and block model parts | all targets start, none confirmed rendering |
| C | 26.1 - 26.2 | `BlockStateModel`, reworked again | both targets start, neither confirmed rendering |

Within a band the differences are small enough for `//?` directives. Across one they are not, so each band has
its own model layer under `client/model/`: `bakedmodel` for band A, `blockstatemodel` for band B. Only the
package matching the target is compiled. They sit under `src/main/java` rather than in separate source roots
because that is the only tree Stonecutter preprocesses.

Band B is the simpler of the two. NeoForge hands the level and position straight to `collectParts`, so results
no longer have to be routed through model data, and a render type belongs to a part rather than needing a set
declared up front.

Two version differences are worth knowing about when adding further targets. `BakedQuad` gained a light
emission argument in 1.21.2, and several methods the mixins target changed signature or became static across
1.21.x, so those injections match by name and capture only the argument they need rather than restating a
signature that will not survive. Minecraft also relaxed sprite path validation in 1.21.2, which let the
reserved-path machinery collapse from an index table and three supporting mixins down to a single rewrite.

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
