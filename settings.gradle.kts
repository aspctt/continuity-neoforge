pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // Only the version the port is actually known to work on. Others get added as they are ported,
        // rather than declared up front and left broken.
        versions("1.21.1")
        vcsVersion = "1.21.1"
    }
}

rootProject.name = "continuity"
