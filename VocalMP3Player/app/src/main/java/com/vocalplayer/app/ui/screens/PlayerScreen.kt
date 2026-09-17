package com.vocalplayer.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocalplayer.app.PlayerViewModel
import com.vocalplayer.app.data.AppSettings
import com.vocalplayer.app.data.PlayerState
import com.vocalplayer.app.data.RepeatMode
import com.vocalplayer.app.ui.components.*
import com.vocalplayer.app.ui.theme.*

@Composable
fun PlayerScreen(
    playerState: PlayerState,
    settings: AppSettings,
    viewModel: PlayerViewModel
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header with vocal isolation toggle
        PlayerHeader(
            isVocalIsolationEnabled = playerState.isVocalIsolationEnabled,
            onToggleVocalIsolation = { viewModel.toggleVocalIsolation() },
            hapticEnabled = settings.hapticFeedback,
            animationsEnabled = settings.buttonAnimations
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Album Art
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AlbumArtView(
                albumArtUri = playerState.currentTrack?.albumArtUri,
                isPlaying = playerState.isPlaying,
                size = minOf(280.dp, maxWidth)
            )
            if (playerState.isBuffering) {
                CircularProgressIndicator(color = NeonCyan)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Track Info
        TrackInfo(
            title = playerState.currentTrack?.title ?: "No Track Selected",
            artist = playerState.currentTrack?.artist ?: "Select a song from library",
            album = playerState.currentTrack?.album ?: ""
        )

        playerState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visualizer
        if (settings.showVisualizer) {
            WaveformVisualizer(
                isPlaying = playerState.isPlaying,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Progress Bar
        ProgressBar(
            currentPosition = playerState.currentPosition,
            duration = playerState.duration,
            onSeek = { viewModel.seekTo(it) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Playback Controls
        PlaybackControls(
            isPlaying = playerState.isPlaying,
            isShuffleEnabled = playerState.isShuffleEnabled,
            repeatMode = playerState.repeatMode,
            onPlayPause = { viewModel.playPause() },
            onNext = { viewModel.playNext() },
            onPrevious = { viewModel.playPrevious() },
            onShuffle = { viewModel.toggleShuffle() },
            onRepeat = { viewModel.toggleRepeat() },
            buttonAnimations = settings.buttonAnimations,
            hapticEnabled = settings.hapticFeedback
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Vocal Isolation Controls
        if (playerState.isVocalIsolationEnabled) {
            VocalIsolationControls(
                level = playerState.vocalIsolationLevel,
                onLevelChange = { viewModel.setVocalIsolationLevel(it) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Playback Speed
        PlaybackSpeedControl(
            speed = playerState.playbackSpeed,
            onSpeedChange = { viewModel.setPlaybackSpeed(it) }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PlayerHeader(
    isVocalIsolationEnabled: Boolean,
    onToggleVocalIsolation: () -> Unit,
    hapticEnabled: Boolean,
    animationsEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Vocal Player",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                text = if (isVocalIsolationEnabled) "🎤 Vocal Focus ON" else "🎵 Normal Mode",
                style = MaterialTheme.typography.bodySmall,
                color = if (isVocalIsolationEnabled) NeonGreen else Color.White.copy(alpha = 0.5f)
            )
        }

        GlowButton(
            icon = if (isVocalIsolationEnabled) Icons.Default.Mic else Icons.Default.MusicNote,
            contentDescription = "Toggle Vocal Isolation",
            onClick = onToggleVocalIsolation,
            isActive = isVocalIsolationEnabled,
            activeColor = NeonGreen,
            hapticEnabled = hapticEnabled,
            animationsEnabled = animationsEnabled
        )
    }
}

@Composable
private fun TrackInfo(
    title: String,
    artist: String,
    album: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = artist,
            style = MaterialTheme.typography.bodyLarge,
            color = NeonCyan.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (album.isNotEmpty()) {
            Text(
                text = album,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProgressBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    Column {
        var sliderPosition by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }

        val progress = if (duration > 0) {
            (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
        } else 0f

        Slider(
            value = if (isDragging) sliderPosition else progress,
            onValueChange = { value ->
                isDragging = true
                sliderPosition = value
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek((sliderPosition * duration).toLong())
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentPosition),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    buttonAnimations: Boolean,
    hapticEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        GlowButton(
            icon = if (isShuffleEnabled) Icons.Default.Shuffle else Icons.Outlined.Shuffle,
            contentDescription = "Shuffle",
            onClick = onShuffle,
            isActive = isShuffleEnabled,
            size = 36.dp,
            iconSize = 18.dp,
            hapticEnabled = hapticEnabled,
            animationsEnabled = buttonAnimations
        )

        // Previous
        PulseButton(
            icon = Icons.Default.SkipPrevious,
            contentDescription = "Previous",
            onClick = onPrevious,
            size = 48.dp,
            iconSize = 28.dp,
            gradient = Brush.linearGradient(listOf(
                Color.White.copy(alpha = 0.1f),
                Color.White.copy(alpha = 0.2f)
            )),
            hapticEnabled = hapticEnabled,
            animationsEnabled = buttonAnimations
        )

        // Play/Pause - Main button
        PulseButton(
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            onClick = onPlayPause,
            size = 72.dp,
            iconSize = 36.dp,
            gradient = Brush.linearGradient(listOf(NeonCyan, NeonPurple)),
            hapticEnabled = hapticEnabled,
            animationsEnabled = buttonAnimations
        )

        // Next
        PulseButton(
            icon = Icons.Default.SkipNext,
            contentDescription = "Next",
            onClick = onNext,
            size = 48.dp,
            iconSize = 28.dp,
            gradient = Brush.linearGradient(listOf(
                Color.White.copy(alpha = 0.1f),
                Color.White.copy(alpha = 0.2f)
            )),
            hapticEnabled = hapticEnabled,
            animationsEnabled = buttonAnimations
        )

        // Repeat
        GlowButton(
            icon = when (repeatMode) {
                RepeatMode.ONE -> Icons.Default.RepeatOne
                else -> if (repeatMode == RepeatMode.ALL) Icons.Default.Repeat else Icons.Outlined.Repeat
            },
            contentDescription = "Repeat",
            onClick = onRepeat,
            isActive = repeatMode != RepeatMode.OFF,
            size = 36.dp,
            iconSize = 18.dp,
            hapticEnabled = hapticEnabled,
            animationsEnabled = buttonAnimations
        )
    }
}

@Composable
private fun VocalIsolationControls(
    level: Float,
    onLevelChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NeonGreen.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎤 Vocal Focus",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonGreen
                )
                Text(
                    text = "${(level * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonGreen.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = level,
                onValueChange = onLevelChange,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = NeonGreen,
                    activeTrackColor = NeonGreen,
                    inactiveTrackColor = NeonGreen.copy(alpha = 0.2f)
                )
            )

            Text(
                text = "Boosts the vocal range while reducing bass and high frequencies.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun PlaybackSpeedControl(
    speed: Float,
    onSpeedChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ Playback Speed",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonOrange
                )
                Text(
                    text = "${speed}x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonOrange.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Speed presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { preset ->
                    val isActive = speed == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isActive) NeonOrange.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSpeedChange(preset) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${preset}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isActive) NeonOrange else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = speed,
                onValueChange = onSpeedChange,
                valueRange = 0.5f..2.0f,
                steps = 11,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = NeonOrange,
                    activeTrackColor = NeonOrange,
                    inactiveTrackColor = NeonOrange.copy(alpha = 0.2f)
                )
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
