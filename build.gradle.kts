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
