plugins {
    id("java-library")
    id("maven-publish")
    id("net.neoforged.moddev") version "2.0.137"
    id("idea")
}

// This script is the central build for every versioned subproject, so anything that reads a file has to
// reach for the root rather than the version directory it is being evaluated in.
val modId: String by project
val modArchivesName: String = property("mod_archives_name") as String
val modVersion: String = property("mod_version") as String
val minecraftVersion: String = property("minecraft_version") as String

fun prop(name: String): String = property(name) as String

fun extraBuildMetadata(): String {
    val buildNumber = System.getenv("GITHUB_RUN_NUMBER") ?: return ""
    return ".build.$buildNumber"
}

version = "$modVersion+$minecraftVersion" + extraBuildMetadata()
group = prop("mod_group_id")

base {
    archivesName = modArchivesName
}

repositories {
    mavenCentral()
    // Sodium's config API, published on its own so a mod can compile against it without pulling in Sodium.
    maven("https://maven.caffeinemc.net/releases") {
        content { includeGroup("net.caffeinemc") }
    }
    // The Sodium mod jar itself, added to the development run only so the config page can be looked at.
    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") }
    }
}

// Sodium's config API arrived in 0.8, which only some of its builds carry: the 0.6 and 0.7 lines that serve
// 1.21.3 through 1.21.10 have no such API at all. A target declares the version it can compile against, and
// one that declares none neither compiles the entry point nor points the mod metadata at it.
val sodiumApiVersion: String? = findProperty("sodium_api_version") as String?

// Mojang ships Java 21 to end users through 1.21.11, and Java 25 from 26.1.
java.toolchain.languageVersion = JavaLanguageVersion.of(if (versionAtLeast("26.1")) 25 else 21)

neoForge {
    version = prop("neo_version")

    // Parchment lags new Minecraft releases, so a target without it still builds; only the parameter names
    // and javadoc are missing.
    if (project.hasProperty("parchment_mappings_version")) {
        parchment {
            mappingsVersion = prop("parchment_mappings_version")
            minecraftVersion = prop("parchment_minecraft_version")
        }
    }

    // Access Transformers are automatically detected at
    // src/main/resources/META-INF/accesstransformer.cfg

    runs {
        create("client") {
            client()
            logLevel = org.slf4j.event.Level.DEBUG
            // One run directory shared by every version, so worlds and options survive switching targets.
            gameDirectory = rootProject.file("run")
        }
    }

    mods {
        create(prop("mod_id")) {
            sourceSet(sourceSets.main.get())
        }
    }
}

// The model layer is written twice, because 1.21.5 replaced BakedModel with BlockStateModel and dropped the
// model data the older one carries its results on. The two share nothing but the processors they drive, so
// each lives in its own package and only the one matching this version is compiled. They stay under
// src/main/java because that is the only tree Stonecutter preprocesses.
// Compared numerically, because 1.21.10 sorts before 1.21.5 as a string.
fun versionAtLeast(target: String): Boolean {
    fun parts(version: String) = version.split('.').map { it.toIntOrNull() ?: 0 }
    val current = parts(stonecutter.current.version)
    val other = parts(target)
    for (i in 0 until maxOf(current.size, other.size)) {
        val a = current.getOrElse(i) { 0 }
        val b = other.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return true
}

val usesBlockStateModel = versionAtLeast("1.21.5")
sourceSets.main.get().java.exclude(
    if (usesBlockStateModel) "**/model/bakedmodel/**" else "**/model/blockstatemodel/**"
)

// From 26.1 the renderer folds a quad's light emission into the lightmap itself, so nothing has to force the
// light of an emissive quad any more.
val emissionIsNative = versionAtLeast("26.1")

// Every mixin this version applies. A mixin left out here is also left out of the compile, so a target class
// that no longer exists never has to be worked around in the source.
val clientMixins = buildList {
    add("FallbackResourceManagerMixin")
    add("ModelManagerMixin")
    add("MultiPackResourceManagerMixin")
    add("ReloadableResourceManagerAccessor")
    add("ResourceLocationMixin")
    add("SpriteLoaderMixin")
    add("SpriteSourceListMixin")
    add("TextureAtlasSpriteMixin")
    add("BlockModelShaperMixin")
    add("RenderRegionAccessor")
    if (!emissionIsNative) {
        add("ModelBlockRendererMixin")
        add("ItemRendererMixin")
        // Custom block layers need a per block layer query to intercept, and 26.1 bakes the layer onto each
        // quad instead. Upstream dropped the feature there for the same reason.
        add("ItemBlockRenderTypesMixin")
    } else {
        // Nothing reads the parsed layers on this band, so the file is not parsed either.
        sourceSets.main.get().java.exclude("**/resource/CustomBlockLayers.java")
    }
    if (usesBlockStateModel) {
        add("ItemModelWrapperMixin")
    } else {
        // NeoForge dropped its experimental light pipeline along with the model data it fed.
        add("QuadLighterMixin")
    }
}.sorted()

val allMixins = listOf(
    "BlockModelShaperMixin", "FallbackResourceManagerMixin", "ItemBlockRenderTypesMixin",
    "ItemModelWrapperMixin", "ItemRendererMixin", "ModelBlockRendererMixin", "ModelManagerMixin",
    "MultiPackResourceManagerMixin", "QuadLighterMixin", "ReloadableResourceManagerAccessor",
    "RenderRegionAccessor", "ResourceLocationMixin", "SpriteLoaderMixin", "SpriteSourceListMixin",
    "TextureAtlasSpriteMixin",
)
for (mixin in allMixins - clientMixins.toSet()) {
    sourceSets.main.get().java.exclude("**/mixin/$mixin.java")
}
// 1.21.9 replaced the single pack format number with a supported range, so the field itself differs and not
// just its value.
val packFormatField = if (versionAtLeast("1.21.9")) {
    val major = prop("resource_pack_format")
    val minor = prop("resource_pack_format_minor")
    "\"min_format\": $major," + System.lineSeparator() + "    \"max_format\": [$major, $minor],"
} else {
    "\"pack_format\": " + prop("resource_pack_format") + ","
}

if (sodiumApiVersion == null) {
    sourceSets.main.get().java.exclude("**/config/SodiumConfigImpl.java")
}

// Sodium reads this out of the mod metadata and instantiates the class itself, so nothing in Continuity ever
// names it and the class stays unloaded when Sodium is not installed.
val sodiumEntryPoint = if (sodiumApiVersion == null) {
    ""
} else {
    "[modproperties.${prop("mod_id")}]" + System.lineSeparator() +
            "\"sodium:config_api_user\" = \"me.pepperbell.continuity.client.config.SodiumConfigImpl\""
}

val javaVersion = if (versionAtLeast("26.1")) 25 else 21
val clientMixinList = clientMixins.joinToString(",\n    ") { "\"$it\"" }

// Sodium is wanted on the classpath a run resolves and nowhere else. Extending runtimeClasspath alone puts
// it there without touching runtimeElements, which is what the published metadata is built from, so nobody
// depending on Continuity is asked to bring Sodium along.
val sodiumRuntime = configurations.create("sodiumRuntime")
configurations.runtimeClasspath.get().extendsFrom(sodiumRuntime)

dependencies {
    if (sodiumApiVersion != null) {
        // Continuity never calls into Sodium; it implements an interface Sodium looks for, so this is a
        // compile time contract and nothing more.
        compileOnly("net.caffeinemc:sodium-neoforge-api:$sodiumApiVersion")
        sodiumRuntime("maven.modrinth:sodium:${prop("sodium_version")}")
    }
}

// Expand the declared properties into the mod metadata template.
val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to prop("minecraft_version"),
        "minecraft_version_range" to prop("minecraft_version_range"),
        "neo_version" to prop("neo_version"),
        "neo_version_range" to prop("neo_version_range"),
        "loader_version_range" to prop("loader_version_range"),
        "mod_id" to prop("mod_id"),
        "mod_name" to prop("mod_name"),
        "mod_license" to prop("mod_license"),
        "mod_version" to project.version.toString(),
        "mod_authors" to prop("mod_authors"),
        "mod_description" to prop("mod_description"),
        "pack_format_field" to packFormatField,
        "client_mixins" to clientMixinList,
        "java_version" to javaVersion.toString(),
        "sodium_entry_point" to sodiumEntryPoint,
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from(rootProject.file("src/main/templates"))
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets.main.get().resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

tasks.jar {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_$modArchivesName" }
    }
}

java {
    withSourcesJar()
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = modArchivesName
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = rootProject.file("repo").toURI()
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
