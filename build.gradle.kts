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
}

// Mojang ships Java 21 to end users in 1.21.1, so mods should target Java 21.
java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = prop("neo_version")

    parchment {
        mappingsVersion = prop("parchment_mappings_version")
        minecraftVersion = prop("parchment_minecraft_version")
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

// NeoForge dropped its experimental light pipeline along with the model data it fed, so the mixin that covered
// it has no target from 1.21.5 and is left out of both the compile and the mixin config.
if (usesBlockStateModel) {
    sourceSets.main.get().java.exclude("**/mixin/QuadLighterMixin.java")
}
val lightingMixin = if (usesBlockStateModel) "" else ",\n    \"QuadLighterMixin\""

dependencies {
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
        "resource_pack_format" to prop("resource_pack_format"),
        "lighting_mixin" to lightingMixin,
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
