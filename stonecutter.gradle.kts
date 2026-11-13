import me.modmuss50.mpp.PublishModTask

plugins {
    id("dev.kikugie.stonecutter")
    // Versioned here and applied bare in build.gradle.kts, so every subproject shares this one copy of the
    // plugin. Each subproject hands the root's GitHub task to parent(), which casts it to the plugin's own
    // task class, and that cast fails against a second copy loaded separately.
    id("me.modmuss50.mod-publish-plugin") version "2.2.1"
}

stonecutter active "1.21.1"

stonecutter parameters {
    // Available to source files as `//$ minecraft` swaps and in `//? if` conditions.
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    // Pure renames only. Anything that changes arity, arguments or semantics is handled with an inline
    // `//? if` directive instead, so the difference is visible where it matters.
    replacements {
        string(current.parsed >= "1.21.2") {
            // Registry lookups returning the value directly moved to getValue; get now returns a Holder.
            replace("BuiltInRegistries.BLOCK.get(", "BuiltInRegistries.BLOCK.getValue(")
            replace("biomeRegistry.get(", "biomeRegistry.getValue(")
            replace(".registryOrThrow(", ".lookupOrThrow(")
        }

        string(current.parsed >= "1.21.5") {
            // TriState moved out of NeoForge and into vanilla.
            replace("import net.neoforged.neoforge.common.util.TriState;", "import net.minecraft.util.TriState;")
            // BakedQuad became a record in 1.21.5, but its accessor names collide with this mod's own quad
            // API, so those call sites use directives rather than a replacement that would rewrite both.
        }

        string(current.parsed >= "1.21.6") {
            // Chunk layers left RenderType for an enum of their own. The layers themselves did not change, so
            // this is a rename, but RenderType still exists for everything that is not a chunk layer. The
            // patterns below only match the type in a declaration or a factory call; identifiers such as
            // getRenderType, ChunkRenderTypeSet and ItemBlockRenderTypes are deliberately left alone.
            replace("import net.minecraft.client.renderer.RenderType;", "import net.minecraft.client.renderer.chunk.ChunkSectionLayer;")
            replace("RenderType.solid()", "ChunkSectionLayer.SOLID")
            replace("RenderType.cutoutMipped()", "ChunkSectionLayer.CUTOUT_MIPPED")
            replace("RenderType.cutout()", "ChunkSectionLayer.CUTOUT")
            replace("RenderType.translucent()", "ChunkSectionLayer.TRANSLUCENT")
            replace("CallbackInfoReturnable<RenderType>", "CallbackInfoReturnable<ChunkSectionLayer>")
            replace("RenderType ", "ChunkSectionLayer ")
            replace("RenderType,", "ChunkSectionLayer,")

            // The chunk render region was renamed alongside them.
            replace("RenderChunkRegion", "RenderSectionRegion")
        }

        string(current.parsed >= "1.21.9") {
            // The metadata section argument of loadAndStitch narrowed from a collection to a set, which changes
            // the descriptor the sprite loader injections match on.
            replace("ILjava/util/concurrent/Executor;Ljava/util/Collection;)", "ILjava/util/concurrent/Executor;Ljava/util/Set;)")
        }

        string(current.parsed >= "1.21.11") {
            // ResourceLocation became Identifier. The mod has its own InvalidIdentifierStateHolder, but no bare
            // Identifier of its own, so these patterns cannot collide with it in either direction.
            replace("import net.minecraft.resources.ResourceLocation;", "import net.minecraft.resources.Identifier;")
            replace("import net.minecraft.ResourceLocationException;", "import net.minecraft.IdentifierException;")
            replace("Lnet/minecraft/resources/ResourceLocation;", "Lnet/minecraft/resources/Identifier;")
            replace("ResourceLocationException ", "IdentifierException ")
            replace("ResourceLocation ", "Identifier ")
            replace("ResourceLocation,", "Identifier,")
            replace("ResourceLocation>", "Identifier>")
            replace("ResourceLocation.", "Identifier.")

            // The sprite suppliers gained names of their own rather than being spelled out as functions.
            replace("SpriteSource.SpriteSupplier", "SpriteSource.DiscardableLoader")
            replace("Function<SpriteResourceLoader, SpriteContents>", "SpriteSource.Loader")

            // The mipped and unmipped cutout chunk layers merged into a single mipped one.
            replace("ChunkSectionLayer.CUTOUT_MIPPED", "ChunkSectionLayer.CUTOUT")

            // RenderType moved down a package. Only ItemBlockRenderTypesMixin names it, once as a fully
            // qualified type and once inside an injection descriptor; every other file reaches the type through
            // an import that the 1.21.6 block rewrites to the chunk layer.
            replace("net.minecraft.client.renderer.RenderType", "net.minecraft.client.renderer.rendertype.RenderType")
            replace("Lnet/minecraft/client/renderer/RenderType;", "Lnet/minecraft/client/renderer/rendertype/RenderType;")
        }

        string(current.parsed >= "26.1") {
            // 26.1 reorganised the client model packages. The types keep their shape, so these are import
            // moves rather than API changes; only the model part was renamed as well.
            replace("import net.minecraft.client.renderer.block.model.BakedQuad;", "import net.minecraft.client.resources.model.geometry.BakedQuad;")
            replace("import net.minecraft.client.renderer.block.model.BlockStateModel;", "import net.minecraft.client.renderer.block.dispatch.BlockStateModel;")
            replace("import net.minecraft.client.renderer.block.model.BlockModelPart;", "import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;")
            replace("import net.minecraft.client.resources.model.Material;", "import net.minecraft.client.resources.model.sprite.Material;")
            replace("import net.minecraft.world.level.BlockAndTintGetter;", "import net.minecraft.client.renderer.block.BlockAndTintGetter;")

            // BlockModelPart became BlockStateModelPart. This mod's own part is named ProcessedModelPart so
            // that these patterns cannot catch it.
            replace("BlockModelPart ", "BlockStateModelPart ")
            replace("BlockModelPart>", "BlockStateModelPart>")
        }
    }
}

// Releasing. `./gradlew publishMods` puts every target on Modrinth and CurseForge and attaches each jar to
// one GitHub release for the version. The per-target half lives in build.gradle.kts.

val releaseVersion: String = providers.gradleProperty("mod_version").get()

// Uploads only happen with all three tokens set. Missing any one makes the whole run a dry run, which
// writes what would have been sent under each project's build/publishMods and contacts nothing, so a
// forgotten token can never leave a release up on some platforms and not others.
// A provider rather than a value, so only a build that publishes ever reads the tokens.
val releaseTokens = listOf("MODRINTH_TOKEN", "CURSEFORGE_TOKEN", "GITHUB_TOKEN")
val isReleaseDryRun: Provider<Boolean> = releaseTokens
    .map { name -> providers.environmentVariable(name).map { it.isBlank() }.orElse(true) }
    .reduce { isMissing, isOtherMissing -> isMissing.zip(isOtherMissing) { first, second -> first || second } }

// The configuration cache would store the tokens the publish tasks read, encrypted, under .gradle in this
// folder. A build that publishes skips the cache instead, so the tokens are never written to disk.
allprojects {
    tasks.withType<PublishModTask>().configureEach {
        notCompatibleWithConfigurationCache("Keeps the publishing tokens out of the configuration cache.")
    }
}

// A link reference at the foot of CHANGELOG.md, such as "[3.0.10]: https://...", which ends the last section
val changelogLinkReference = Regex("""^\[[^\]]+]: .*""")

/**
 * One version's notes from CHANGELOG.md: everything under its "## [x.y.z]" heading, up to the next
 * heading or the link references. Called when a publish task runs rather than while configuring, so a
 * version without a section yet stops only a release, never a build.
 */
fun changelogSection(version: String): String {
    val lines = file("CHANGELOG.md").readLines()
    val start = lines.indexOfFirst { it.startsWith("## [$version]") }
    check(start >= 0) { "CHANGELOG.md has no section for $version, so there is nothing to publish as its changelog." }

    return lines.drop(start + 1)
        .takeWhile { !it.startsWith("## ") && !changelogLinkReference.matches(it) }
        .joinToString("\n")
        .trim()
}

publishMods {
    // GitHub's release notes, without the version heading its title already shows. The subprojects build
    // the portal changelogs from this, so CHANGELOG.md is the only place the notes are written.
    changelog = providers.provider { changelogSection(releaseVersion) }
    type = STABLE
    version = releaseVersion
    displayName = releaseVersion
    dryRun = isReleaseDryRun

    // The release itself, created empty. Each subproject then uploads its jar into it through parent().
    github {
        accessToken = providers.environmentVariable("GITHUB_TOKEN")
        repository = "aspctt/continuity-neoforge"
        // Only used when the tag does not exist yet, in which case GitHub creates it at the tip of main.
        // Push the tag first so it marks the commit the jars were built from.
        commitish = "main"
        tagName = releaseVersion
        allowEmptyFiles = true
    }
}

// On each platform the targets upload one at a time, oldest Minecraft version first, so the GitHub release
// lists its jars in that order and the newest version ends up on top of each portal's file list. Left
// alone, parallel execution uploads them in whatever order the workers finish. The platforms still run
// alongside each other. Wired once every target has been evaluated, since each one creates its own tasks.
gradle.projectsEvaluated {
    for (taskName in listOf("publishGithub", "publishModrinth", "publishCurseforge")) {
        val uploads = stonecutter.versions.map { project(":${it.project}").tasks.named(taskName) }
        uploads.zipWithNext { earlier, later ->
            later.configure { mustRunAfter(earlier) }
        }
    }
}
