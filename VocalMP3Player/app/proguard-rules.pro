# Proguard rules for Vocal MP3 Player

# Keep ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep data classes
-keep class com.vocalplayer.app.data.** { *; }

# Keep service
-keep class com.vocalplayer.app.service.** { *; }
