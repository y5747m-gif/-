package com.vocalplayer.app.ui.components

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vocalplayer.app.data.BackgroundStyle
import com.vocalplayer.app.ui.theme.*

@Composable
fun AppBackground(
    backgroundStyle: BackgroundStyle,
    customBackgroundUri: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (backgroundStyle) {
            BackgroundStyle.DEFAULT -> DefaultBackground()
            BackgroundStyle.GRADIENT_PURPLE -> GradientPurpleBackground()
            BackgroundStyle.GRADIENT_OCEAN -> GradientOceanBackground()
            BackgroundStyle.GRADIENT_SUNSET -> GradientSunsetBackground()
            BackgroundStyle.GRADIENT_FOREST -> GradientForestBackground()
            BackgroundStyle.DARK_NEBULA -> DarkNebulaBackground()
            BackgroundStyle.STARRY_NIGHT -> StarryNightBackground()
            BackgroundStyle.CUSTOM_IMAGE -> CustomImageBackground(customBackgroundUri)
        }
    }
}

@Composable
private fun DefaultBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        GradientMid.copy(alpha = 0.5f),
                        DarkBackground
                    )
                )
            )
    )
}

@Composable
private fun GradientPurpleBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "purple")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "purpleOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A0033),
                        Color(0xFF3D1C6E),
                        Color(0xFF6B3FA0),
                        Color(0xFF1A0033)
                    ),
                    start = Offset(offset, 0f),
                    end = Offset(offset + 500f, 1000f)
                )
            )
    )
}

@Composable
private fun GradientOceanBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "ocean")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "oceanOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF000428),
                        Color(0xFF004E92),
                        Color(0xFF000428)
                    ),
                    start = Offset(0f, offset),
                    end = Offset(500f, offset + 800f)
                )
            )
    )
}

@Composable
private fun GradientSunsetBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "sunset")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunsetOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141E30),
                        Color(0xFF243B55),
                        Color(0xFFCB2D3E).copy(alpha = 0.4f),
                        Color(0xFF141E30)
                    ),
                    startY = offset,
                    endY = offset + 1200f
                )
            )
    )
}

@Composable
private fun GradientForestBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B3D0B).copy(alpha = 0.8f),
                        Color(0xFF1A1A2E),
                        Color(0xFF0D2818).copy(alpha = 0.6f)
                    )
                )
            )
    )
}

@Composable
private fun DarkNebulaBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "nebula")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nebulaRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A0A2E),
                        Color(0xFF16213E),
                        Color(0xFF0D0D1A)
                    ),
                    center = Offset.Unspecified,
                    radius = 800f
                )
            )
    ) {
        // Nebula glow effects
        Box(
            modifier = Modifier
                .fillMaxSize(0.7f)
                .blur(60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonPurple.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun StarryNightBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF000011),
                        Color(0xFF0A0A2E),
                        Color(0xFF000022)
                    )
                )
            )
    )
    // Stars would be rendered here with Canvas
}

@Composable
private fun CustomImageBackground(uri: String?) {
    val context = LocalContext.current

    if (uri != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(uri))
                .crossfade(true)
                .build(),
            contentDescription = "Custom Background",
            modifier = Modifier
                .fillMaxSize()
                .blur(2.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.4f
        )
        // Overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )
    } else {
        DefaultBackground()
    }
}
