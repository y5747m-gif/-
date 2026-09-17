package com.vocalplayer.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.vocalplayer.app.ui.theme.NeonCyan
import com.vocalplayer.app.ui.theme.NeonPurple
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

@Composable
fun WaveformVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 40
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val safeBarCount = barCount.coerceAtLeast(1)

    val phases = remember(safeBarCount) {
        List(safeBarCount) { it * 0.15f }
    }

    val amplitudes = phases.mapIndexed { index, phase ->
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = if (isPlaying) {
                0.3f + (kotlin.math.sin(index * 0.5f) * 0.4f + 0.4f) * 0.5f
            } else 0.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (600 + phase * 300).toInt(),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "amp$index"
        )
    }

    val primaryColor = NeonCyan
    val secondaryColor = NeonPurple

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        val barWidth = size.width / (safeBarCount * 2)
        val centerY = size.height / 2

        amplitudes.forEachIndexed { index, amp ->
            val x = size.width * (index + 0.5f) / safeBarCount
            val barHeight = size.height * amp.value

            val gradient = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.9f),
                    secondaryColor.copy(alpha = 0.6f)
                ),
                startY = centerY - barHeight / 2,
                endY = centerY + barHeight / 2
            )

            drawLine(
                brush = gradient,
                start = Offset(x, centerY - barHeight / 2),
                end = Offset(x, centerY + barHeight / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )

            // Mirror effect
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = 0.3f),
                        primaryColor.copy(alpha = 0.1f)
                    ),
                    startY = centerY + barHeight / 2,
                    endY = centerY + barHeight
                ),
                start = Offset(x, centerY + barHeight / 2),
                end = Offset(x, centerY + barHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun CircularVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    ringCount: Int = 3
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circular")

    val rotations = (0 until ringCount).map { ring ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (4000 + ring * 1500),
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation$ring"
        )
    }

    val scales = (0 until ringCount).map { ring ->
        infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (2000 + ring * 500),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale$ring"
        )
    }

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val baseRadius = minOf(size.width, size.height) / 2 * 0.6f

        (0 until ringCount).forEach { ring ->
            val radius = baseRadius + ring * 15f
            val rotation = rotations[ring].value
            val scale = if (isPlaying) scales[ring].value else 1f

            val points = 60
            val path = Path()

            for (i in 0..points) {
                val angle = (i * 2 * PI / points) + Math.toRadians(rotation.toDouble())
                val waveOffset = if (isPlaying) {
                    (sin(angle * 4) * 5.0 * scale).toFloat()
                } else 0f
                val r = radius * scale + waveOffset

                val x = centerX + (r * cos(angle)).toFloat()
                val y = centerY + (r * sin(angle)).toFloat()

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            val alpha = (1f - ring * 0.2f) * if (isPlaying) 0.8f else 0.3f

            drawPath(
                path = path,
                color = if (ring == 0) NeonCyan.copy(alpha = alpha)
                else NeonPurple.copy(alpha = alpha * 0.6f),
                style = Stroke(
                    width = 2f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun SpectrumVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spectrum")
    val barCount = 20

    val heights = (0 until barCount).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = if (isPlaying) {
                val normalized = index.toFloat() / barCount
                val peak = sin(normalized * PI).toFloat()
                0.2f + peak * 0.7f
            } else 0.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 400 + (index * 30),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 50)
            ),
            label = "height$index"
        )
    }

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 1.5f)
        val spacing = barWidth * 0.5f

        heights.forEachIndexed { index, height ->
            val x = index * (barWidth + spacing) + spacing
            val barHeight = size.height * height.value

            val gradient = Brush.verticalGradient(
                colors = listOf(
                    NeonCyan,
                    NeonPurple,
                    NeonCyan.copy(alpha = 0.3f)
                ),
                startY = size.height - barHeight,
                endY = size.height
            )

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
            )
        }
    }
}
