// Root plugin pinning: every plugin version is registered exactly once here
// (apply false). Subprojects apply by id WITHOUT versions, so cross-module
// resolution can never see the same Kotlin plugin twice with a missing
// version ("already on the classpath with an unknown version").
// Keep :app's existing alias() requests untouched — same versions, no conflict.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.secrets) apply false
    alias(libs.plugins.google.services) apply false
}
