package com.vocalplayer.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vocalplayer.app.PlayerViewModel
import com.vocalplayer.app.data.AppSettings
import com.vocalplayer.app.data.BackgroundStyle
import com.vocalplayer.app.ui.components.AppBackground
import com.vocalplayer.app.ui.theme.*

@Composable
fun SettingsScreen(
    settings: AppSettings,
    viewModel: PlayerViewModel
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Text(
            text = "Customize your experience",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Background Section
        SettingsSection(title = "🎨 Background", icon = Icons.Default.Wallpaper) {
            BackgroundSelector(
                currentStyle = settings.backgroundStyle,
                customUri = settings.customBackgroundUri,
                onStyleSelected = { viewModel.updateBackgroundStyle(it) },
                onCustomUriSelected = { uri -> viewModel.updateCustomBackgroundUri(uri) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Audio Section
        SettingsSection(title = "🎵 Audio", icon = Icons.Default.MusicNote) {
            SettingsToggleItem(
                title = "Show Visualizer",
                subtitle = "Display audio waveform animation",
                icon = Icons.Default.GraphicEq,
                isChecked = settings.showVisualizer,
                onToggle = { viewModel.updateShowVisualizer(it) }
            )

            SettingsToggleItem(
                title = "Vocal Isolation Default",
                subtitle = "Enable vocal isolation on app start",
                icon = Icons.Default.Mic,
                isChecked = settings.vocalIsolationEnabled,
                onToggle = { viewModel.setVocalIsolationEnabled(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // UI Section
        SettingsSection(title = "✨ Interface", icon = Icons.Default.Palette) {
            SettingsToggleItem(
                title = "Button Animations",
                subtitle = "Enable pulse and bounce animations",
                icon = Icons.Default.Animation,
                isChecked = settings.buttonAnimations,
                onToggle = { viewModel.updateButtonAnimations(it) }
            )

            SettingsToggleItem(
                title = "Haptic Feedback",
                subtitle = "Vibrate on button press",
                icon = Icons.Default.Vibration,
                isChecked = settings.hapticFeedback,
                onToggle = { viewModel.updateHapticFeedback(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playback Section
        SettingsSection(title = "🎧 Playback", icon = Icons.Default.PlayCircle) {
            SettingsToggleItem(
                title = "Crossfade",
                subtitle = "Smooth transition between tracks",
                icon = Icons.Default.SwapHoriz,
                isChecked = settings.crossfadeEnabled,
                onToggle = { viewModel.updateCrossfadeEnabled(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Section
        SettingsSection(title = "ℹ️ About", icon = Icons.Default.Info) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Vocal MP3 Player",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version 1.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "An offline MP3 player with AI-powered vocal isolation. " +
                                "Remove music and keep only vocals/speech. " +
                                "Works completely offline with no internet required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground.copy(alpha = 0.7f)
            )
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggle(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isChecked) NeonCyan else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f)
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
private fun BackgroundSelector(
    currentStyle: BackgroundStyle,
    customUri: String?,
    onStyleSelected: (BackgroundStyle) -> Unit,
    onCustomUriSelected: (String?) -> Unit
) {
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onCustomUriSelected(it.toString()) }
    }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Choose Background",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Background grid
        val backgrounds = BackgroundStyle.entries.chunked(4)

        backgrounds.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { style ->
                    BackgroundThumbnail(
                        style = style,
                        isSelected = currentStyle == style,
                        onClick = {
                            when (style) {
                                BackgroundStyle.CUSTOM_IMAGE -> {
                                    imagePicker.launch("image/*")
                                }
                                else -> onStyleSelected(style)
                            }
                        }
                    )
                }
                // Fill empty spaces
                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.size(56.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Custom image preview
        if (currentStyle == BackgroundStyle.CUSTOM_IMAGE && customUri != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(Uri.parse(customUri))
                        .build(),
                    contentDescription = "Custom Background",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Custom image selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { onCustomUriSelected(null) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundThumbnail(
    style: BackgroundStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = when (style) {
        BackgroundStyle.DEFAULT -> "Default"
        BackgroundStyle.GRADIENT_PURPLE -> "Purple"
        BackgroundStyle.GRADIENT_OCEAN -> "Ocean"
        BackgroundStyle.GRADIENT_SUNSET -> "Sunset"
        BackgroundStyle.GRADIENT_FOREST -> "Forest"
        BackgroundStyle.DARK_NEBULA -> "Nebula"
        BackgroundStyle.STARRY_NIGHT -> "Stars"
        BackgroundStyle.CUSTOM_IMAGE -> "Custom"
    }

    val colors = when (style) {
        BackgroundStyle.DEFAULT -> listOf(DarkBackground, GradientMid)
        BackgroundStyle.GRADIENT_PURPLE -> listOf(Color(0xFF1A0033), Color(0xFF6B3FA0))
        BackgroundStyle.GRADIENT_OCEAN -> listOf(Color(0xFF000428), Color(0xFF004E92))
        BackgroundStyle.GRADIENT_SUNSET -> listOf(Color(0xFF141E30), Color(0xFFCB2D3E))
        BackgroundStyle.GRADIENT_FOREST -> listOf(Color(0xFF0B3D0B), Color(0xFF1A1A2E))
        BackgroundStyle.DARK_NEBULA -> listOf(Color(0xFF1A0A2E), Color(0xFF16213E))
        BackgroundStyle.STARRY_NIGHT -> listOf(Color(0xFF000011), Color(0xFF0A0A2E))
        BackgroundStyle.CUSTOM_IMAGE -> listOf(NeonPurple.copy(0.3f), NeonCyan.copy(0.3f))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(colors))
                .then(
                    if (isSelected) Modifier.border(
                        width = 2.dp,
                        color = NeonCyan,
                        shape = RoundedCornerShape(12.dp)
                    ) else Modifier
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (style == BackgroundStyle.CUSTOM_IMAGE) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Custom",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = NeonCyan,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.4f)
        )
    }
}
