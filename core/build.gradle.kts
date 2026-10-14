// KMP shared module. commonMain holds portable logic (models, LRC parser,
// playlists, queue, LRCLIB networking, Room database) with zero platform
// imports — compilable for Android, JVM, and iOS from these exact files.
// Platform-owned pieces stay out: HTTP engines are per-target
// (OkHttp/Darwin, MockEngine in tests), Room codegen runs here via KSP, and
// the Android Context-bound singleton factory lives in androidMain.
plugins {
    // Versionless: versions are pinned once in the root build file (apply false).
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    // AGP 9+: unified Android+KMP plugin (replaces com.android.library,
    // which is incompatible with kotlin.multiplatform since AGP 9.0).
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    // Opt into Beta expect/actual classes (KT-61573): silences the per-file
    // warning on the shared AppDatabaseConstructor pattern, all targets.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // Local JVM: runs commonTest via `jvmTest`, no emulator needed.
    jvm()

    // Android consumer (:app). Unified AGP/KMP library block (replaces
    // androidTarget + the standalone android {} block).
    // NOTE: 'androidLibrary' renamed to 'android' per AGP 9 deprecation.
    android {
        namespace = "com.vncnt.music.core"
        compileSdk = 36
        minSdk = 24
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    // iOS targets (compile-only until Xcode framework linking is wired).
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.room.runtime)
            // Bundled SQLite driver for non-Android targets (JVM tests, iOS).
            implementation(libs.androidx.sqlite.bundled)
            // SQLite API (SupportSQLiteDatabase, SQLiteConnection) for shared code.
            implementation(libs.androidx.sqlite)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// Room codegen (KSP) for the shared @Database in commonMain, plus the
// platform compilations that instantiate it at runtime.
dependencies {
    add("kspCommonMainMetadata", libs.androidx.room.compiler)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
}
