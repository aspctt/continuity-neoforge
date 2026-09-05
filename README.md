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

| Target | NeoForge | Model API | Builds | Loads and reads packs | Rendering confirmed |
|---|---|---|---|---|---|
| 1.21.1 | 21.1.249 | baked model | yes | yes | connected textures confirmed |
| 1.21.3 | 21.3.97 | baked model | yes | yes | not yet |
| 1.21.4 | 21.4.157 | baked model | yes | yes | not yet |
| 1.21.5 | 21.5.98 | block state model | yes | yes | not yet |
| 1.21.6 | 21.6.20-beta | block state model | yes | yes | not yet |
| 1.21.8 | 21.8.54 | block state model | yes | yes | not yet |
| 1.21.10 | 21.10.63 | block state model | yes | yes | not yet |
| 1.21.11 | 21.11.45 | block state model | yes | yes | not yet |

Every target starts: all mixins apply, both built-in packs register, and the Default Connected Textures pack
parses into the same 42 quad processors with no errors of Continuity's own.

Connected textures have only been confirmed rendering correctly on 1.21.1. The rest are unverified in game.

The 1.21.6 target covers 1.21.7 and the 1.21.8 target covers 1.21.9, since NeoForge only ever published beta
builds for those two. That pairing assumes they are compatible, which has not been verified.

Emissive textures are linked on the block atlas only, on every version, which is what upstream does too. From
1.21.11 items are stitched onto an atlas of their own, so a texture that lives only there has no emissive
counterpart. Block models drawn as items, which is what emissive packs mostly target, are unaffected.

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

| Target | What moved |
|---|---|
| 1.21.6 | chunk layers left `RenderType` for a `ChunkSectionLayer` enum of their own, and `RenderChunkRegion` became `RenderSectionRegion` |
| 1.21.8 | nothing beyond 1.21.6 |
| 1.21.10 | `AtlasSet` gave way to `SpriteLoader.Preparations`, reload listeners take a shared state rather than a resource manager, and pack metadata replaced `pack_format` with `min_format` and `max_format` |
| 1.21.11 | `ResourceLocation` became `Identifier`, `BakedQuad` stopped being backed by a vertex array, and blocks and items are stitched onto separate atlases |

The 1.21.11 quad change is the substantive one. Positions are now `Vector3fc`, texture coordinates are packed
into longs, and colours and normals moved into `BakedColors` and `BakedNormals`. The internal layout this port
keeps is unchanged; only the two methods that convert to and from a vanilla quad had to be rewritten, so the
processors and the model layer above them were untouched. Two smaller consequences: the mipped and unmipped
cutout chunk layers merged into one, and sprites are padded on the atlas instead of having their UVs shrunk.

### What band C needs

26.1 and 26.2 have NeoForge builds (26.1.2.103 and 26.2.0.76) and a `versions/` directory each, and the
toolchain follows Minecraft to Java 25 there. They are not declared in `settings.gradle.kts`, because band C is
a third model layer rather than a set of renames. Compiling against 26.1 gives 100 errors that fall into three
groups:

| Group | What it is |
|---|---|
| Package moves | `BakedQuad`, `BlockStateModel`, `Material` and `BlockAndTintGetter` all moved, and `BlockModelPart` became `BlockStateModelPart`. Mechanical, and the shape of `collectParts` and `getQuads` is unchanged, so the band B model layer carries across |
| Chunk layers | a part no longer names a chunk layer at all. It carries `materialFlags()`, a bitmask on the quad, which is what the quad collection and the processed parts are built around today |
| Hooks with no target left | `ItemBlockRenderTypes`, `ItemRenderer`, `BlockModelShaper` and `LightTexture` are gone, and `ModelBlockRenderer.putQuadData` became `putQuadWithTint`, taking a `BlockQuadOutput` rather than a vertex consumer. Custom block layers, both emissive brightening paths and the model cache hook all need somewhere new to attach |

The first group is a replacement pass. The second reshapes the render package, and the third is four mixins
that have to be rewritten against a different rendering path, so this is closer in size to the original band B
port than to adding 1.21.11.

### Where the version bands fall

Minecraft replaced `BakedModel` with `BlockStateModel` in 1.21.5, and with it the whole basis this port stands
on: `ModelData`, `ChunkRenderTypeSet` and `ModelProperty` are gone, so connection state can no longer be
resolved in `getModelData` and stashed for `getQuads` to read back. Upstream renamed its own model classes at
exactly the same version. That splits the range into three bands, not one gradient:

| Band | Versions | Model API | State |
|---|---|---|---|
| A | 1.21 - 1.21.4 | `BakedModel` and model data | working |
| B | 1.21.5 - 1.21.11 | `BlockStateModel` and block model parts | all targets start, none confirmed rendering |
| C | 26.1 - 26.2 | `BlockStateModel`, reworked again | not started |

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
