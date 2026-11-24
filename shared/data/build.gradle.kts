plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    android {
        namespace = "com.phicdy.redraft.shared.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    jvm("desktop")

    compilerOptions {
        // HistoryDatabaseConstructor is an expect object whose actuals Room generates.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.model)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// Room's compiler runs per target; the generated code includes the `actual` HistoryDatabaseConstructor.
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}

ksp {
    // Kotlin code generation is required for the expect/actual database constructor.
    arg("room.generateKotlin", "true")
}

// The exported schema is committed so future migrations can be generated and tested against it.
room {
    schemaDirectory("$projectDir/schemas")
}
