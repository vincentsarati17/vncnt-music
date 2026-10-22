# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Production: keep line numbers for Play crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room — keep entities/DAOs, DB impl is generated.
-keep class androidx.room.RoomDatabase { *; }
-keep class com.vncnt.music.core.db.** { *; }
-dontwarn androidx.room.**
-keepattributes *Annotation*

# Retrofit / OkHttp / Moshi — reflective JSON adapters.
-keep class com.squareup.moshi.** { *; }
-keep class com.vncnt.music.data.api.** { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonClass class *
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Coil — keep setters used via reflection.
-keep class coil.** { *; }
-dontwarn coil.**

# Compose / AndroidX — R8 handles most, keep test tags visible.
-keepattributes EnclosingMethod,InnerClasses,Signature

# DataStore / Preferences proto.
-keep class androidx.datastore.** { *; }

# Media notification style.
-keep class androidx.media.app.NotificationCompat$MediaStyle { *; }
