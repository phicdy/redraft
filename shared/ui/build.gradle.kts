plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "com.phicdy.redraft.shared.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.model)
            implementation(projects.shared.llm)
            implementation(projects.shared.data)

            api(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui.tooling.preview)

            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.kotlinx.coroutines.core)

            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.koin.compose.viewmodel)
        }
        androidMain.dependencies {
            // System back handling (the non-deprecated replacement for BackHandler).
            implementation(libs.jetbrains.navigationevent.compose)
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.compose.ui.test)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

// Preview screenshots rendered by PreviewScreenshotTest. Declared as a test output so that
// a build-cache hit restores the images instead of skipping them.
val screenshotDir: Provider<Directory> = layout.buildDirectory.dir("screenshots")
tasks.withType<Test>().configureEach {
    val dir = screenshotDir.get().asFile
    systemProperty("redraft.screenshotDir", dir.absolutePath)
    outputs.dir(dir)
}
