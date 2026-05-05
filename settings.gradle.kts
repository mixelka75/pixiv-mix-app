pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Auto-download missing JDK toolchains (e.g. JDK 17 on systems where the package
    // manager only ships JDK 21, like Debian trixie). Resolves any toolchain pinned
    // via `jvmToolchain(...)` from the Foojay disco API.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    // Kotlin/Wasm's binaryen + nodejs setup plugins register their own custom
    // download repositories at build time (release tarballs from github.com).
    // We leave the resolution mode at the Gradle default so those plugin-added
    // repositories aren't rejected.
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PixMix"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":composeApp")
include(":shared")
