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
        // The versions carried forward. 1.21.2 through 1.21.10 were built and released once, at 3.0.2, and
        // are not maintained past it, so they are no longer declared here. One target per model API band
        // remains, which is what keeps all three code paths compiled and checked.
        versions("1.21.1", "1.21.11", "26.1", "26.2")
        vcsVersion = "1.21.1"
    }
}

rootProject.name = "continuity"
