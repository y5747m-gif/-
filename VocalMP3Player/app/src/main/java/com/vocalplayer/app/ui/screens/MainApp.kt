package com.vocalplayer.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vocalplayer.app.PlayerViewModel
import com.vocalplayer.app.Screen
import com.vocalplayer.app.ui.components.AppBackground
import com.vocalplayer.app.ui.theme.*

@Composable
fun MainApp(
    viewModel: PlayerViewModel = viewModel()
) {
    val playerState by viewModel.playerState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val audioTracks by viewModel.audioTracks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val haptic = LocalHapticFeedback.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic background
        AppBackground(
            backgroundStyle = settings.backgroundStyle,
            customBackgroundUri = settings.customBackgroundUri
        )

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                BottomNavigationBar(
                    currentScreen = currentScreen,
                    onNavigate = { screen ->
                        if (settings.hapticFeedback) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        viewModel.navigateTo(screen)
                    },
                    buttonAnimations = settings.buttonAnimations
                )
            }
        ) { paddingValues ->
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) + slideInVertically(
                        initialOffsetY = { it / 20 },
                        animationSpec = tween(300)
                    ) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "screenTransition"
            ) { screen ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    when (screen) {
                        is Screen.Player -> PlayerScreen(
                            playerState = playerState,
                            settings = settings,
                            viewModel = viewModel
                        )
                        is Screen.Library -> LibraryScreen(
                            tracks = audioTracks,
                            isLoading = isLoading,
                            onTrackClick = { track ->
                                viewModel.playTrack(track)
                                viewModel.navigateTo(Screen.Player)
                            },
                            onRefresh = { viewModel.refreshLibrary() }
                        )
                        is Screen.Settings -> SettingsScreen(
                            settings = settings,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // Mini player bar at bottom when not on player screen
        if (currentScreen !is Screen.Player && playerState.currentTrack != null) {
            MiniPlayerBar(
                playerState = playerState,
                onPlayPause = { viewModel.playPause() },
                onClick = { viewModel.navigateTo(Screen.Player) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            )
        }
    }
}

@Composable
private fun BottomNavigationBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    buttonAnimations: Boolean
) {
    NavigationBar(
        containerColor = Color(0xFF0D0D1A).copy(alpha = 0.95f),
        contentColor = NeonCyan,
        tonalElevation = 0.dp,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        val items = listOf(
            Triple(Screen.Player, Icons.Filled.PlayCircle, "Player"),
            Triple(Screen.Library, Icons.Filled.LibraryMusic, "Library"),
            Triple(Screen.Settings, Icons.Filled.Settings, "Settings")
        )

        items.forEach { (screen, icon, label) ->
            val isSelected = currentScreen::class == screen::class

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(screen) },
                icon = {
                    Box {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(24.dp)
                        )

                        // Selection indicator dot
                        if (isSelected) {
                            val infiniteTransition = rememberInfiniteTransition(label = "dot")
                            val dotAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dotAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .offset(y = (-2).dp)
                                    .align(Alignment.TopCenter)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = dotAlpha))
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NeonCyan,
                    selectedTextColor = NeonCyan,
                    unselectedIconColor = Color.White.copy(alpha = 0.5f),
                    unselectedTextColor = Color.White.copy(alpha = 0.5f),
                    indicatorColor = NeonCyan.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Composable
private fun MiniPlayerBar(
    playerState: com.vocalplayer.app.data.PlayerState,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: return

    Card(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini album art
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonPurple.copy(0.3f), DarkBackground)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }

            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Default.Pause
                    else Icons.Default.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                    tint = NeonCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Progress bar
        if (playerState.duration > 0) {
            LinearProgressIndicator(
                progress = (playerState.currentPosition.toFloat() / playerState.duration).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = NeonCyan,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}
