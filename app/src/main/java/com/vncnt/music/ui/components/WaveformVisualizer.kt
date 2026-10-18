package com.vncnt.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vncnt.music.ui.theme.CoralRed
import com.vncnt.music.ui.theme.WaveformActive

@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
    barCount: Int = 32,
    activeColor: Color = WaveformActive,
    accentColor: Color = CoralRed
) {
    val phase = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            phase.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            phase.snapTo(0f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val totalBars = barCount.coerceAtLeast(16)
        val barSpacing = 4.dp.toPx()
        val totalSpacing = barSpacing * (totalBars - 1)
        val barWidth = ((canvasWidth - totalSpacing) / totalBars).coerceAtLeast(2.dp.toPx())

        for (i in 0 until totalBars) {
            val amp = amplitudes.getOrElse(i % amplitudes.size) { 0.2f }
            val dynamicMultiplier = if (isPlaying) {
                0.7f + 0.3f * kotlin.math.sin(phase.value * kotlin.math.PI * 2 + i * 0.2).toFloat()
            } else {
                0.25f
            }

            val normalizedHeight = (amp * dynamicMultiplier).coerceIn(0.08f, 1.0f) * canvasHeight
            val barHeight = normalizedHeight.coerceAtLeast(6.dp.toPx())
            val x = i * (barWidth + barSpacing)
            val y = (canvasHeight - barHeight) / 2f

            val isMiddleAccent = i in (totalBars / 3)..(2 * totalBars / 3) && isPlaying
            val barColor = if (isMiddleAccent && i % 4 == 0) accentColor else activeColor

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
