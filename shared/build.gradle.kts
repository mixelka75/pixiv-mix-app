plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(17)

    androidTarget()
    jvm("desktop")

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs("web") {
        browser()
        binaries.executable()
    }

    sourceSets.all {
        languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3Api")
        languageSettings.optIn("androidx.compose.foundation.ExperimentalFoundationApi")
        languageSettings.optIn("com.arkivanov.decompose.DelicateDecomposeApi")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            api(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            api(libs.coil.compose)
            implementation(libs.coil.network.ktor3)

            api(libs.decompose)
            api(libs.decompose.compose)

            api(libs.koin.core)
            implementation(libs.koin.compose)

            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            api(libs.koin.android)
            implementation(libs.androidx.security.crypto)
            implementation(libs.androidx.webkit)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.commons.compress)
            implementation(libs.telephoto.zoomable.image.coil3)
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.swing)
        }

        val webMain by getting {
            kotlin.srcDir(layout.buildDirectory.dir("generated/web-config/kotlin"))
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}

// Bake build-time defaults for the web target. The hosted Pages deploy needs a
// proxy URL/token preconfigured (browsers can't talk to pixiv directly — CORS).
// Forks pass their own values via -Ppixmix.web.proxyBaseUrl / -Ppixmix.web.proxyToken;
// empty values mean "no default", and the user can fill it in Settings manually.
val generateWebBuildDefaults = tasks.register("generateWebBuildDefaults") {
    val baseUrl = providers.gradleProperty("pixmix.web.proxyBaseUrl").orElse("")
    val token = providers.gradleProperty("pixmix.web.proxyToken").orElse("")
    val outDir = layout.buildDirectory.dir("generated/web-config/kotlin/wtf/mxl/pixmix/shared/config")
    inputs.property("baseUrl", baseUrl)
    inputs.property("token", token)
    outputs.dir(outDir)
    doLast {
        val dir = outDir.get().asFile.also { it.mkdirs() }
        val esc = { s: String -> s.replace("\\", "\\\\").replace("\"", "\\\"") }
        dir.resolve("WebBuildDefaults.kt").writeText(
            """
            package wtf.mxl.pixmix.shared.config

            object WebBuildDefaults {
                const val proxyBaseUrl: String = "${esc(baseUrl.get())}"
                const val proxyToken: String = "${esc(token.get())}"
            }
            """.trimIndent() + "\n"
        )
    }
}

tasks.matching {
    it.name.startsWith("compileKotlinWeb") || it.name.startsWith("compileWeb") || it.name == "kspKotlinWeb"
}.configureEach { dependsOn(generateWebBuildDefaults) }
tasks.matching { it.name == "sourcesJar" || it.name.endsWith("SourcesJar") }
    .configureEach { dependsOn(generateWebBuildDefaults) }

android {
    namespace = "wtf.mxl.pixmix.shared"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
