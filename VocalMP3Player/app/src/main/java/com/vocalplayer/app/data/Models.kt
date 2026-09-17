package com.vocalplayer.app.data

import android.net.Uri

data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: Uri,
    val albumArtUri: Uri? = null
)

data class PlayerState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val volume: Float = 1f,
    val isVocalIsolationEnabled: Boolean = false,
    val vocalIsolationLevel: Float = 1f,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1f,
    val playlist: List<AudioTrack> = emptyList(),
    val currentIndex: Int = -1
)

enum class RepeatMode {
    OFF, ONE, ALL
}

enum class BackgroundStyle {
    DEFAULT, GRADIENT_PURPLE, GRADIENT_OCEAN, GRADIENT_SUNSET,
    GRADIENT_FOREST, DARK_NEBULA, STARRY_NIGHT, CUSTOM_IMAGE
}

data class AppSettings(
    val isDarkTheme: Boolean = true,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.DEFAULT,
    val customBackgroundUri: String? = null,
    val vocalIsolationEnabled: Boolean = false,
    val vocalIsolationLevel: Float = 1f,
    val playbackSpeed: Float = 1f,
    val crossfadeEnabled: Boolean = false,
    val gaplessPlayback: Boolean = true,
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val showVisualizer: Boolean = true,
    val buttonAnimations: Boolean = true,
    val hapticFeedback: Boolean = true
)

enum class AudioQuality {
    LOW, MEDIUM, HIGH
}
