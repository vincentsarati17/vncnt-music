package com.vncnt.music.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Glassmorphism Color Palette
val GlassSurfaceDark = Color(0x55161822)
val GlassSurfaceLight = Color(0x18FFFFFF)
val GlassSurfaceElevated = Color(0x701A1C26)
val GlassBorderSheen = Color(0x35FFFFFF)
val GlassBorderSubtle = Color(0x15FFFFFF)
val GlassCoralAccentGlow = Color(0x33FF3838)

fun glassBackgroundBrush(
    startAlpha: Float = 0.35f,
    endAlpha: Float = 0.15f
): Brush {
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFF282B38).copy(alpha = startAlpha),
            Color(0xFF14151D).copy(alpha = endAlpha)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
}

fun glassBorderBrush(
    highlightAlpha: Float = 0.35f,
    shadowAlpha: Float = 0.08f
): Brush {
    return Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = highlightAlpha),
            Color.White.copy(alpha = shadowAlpha),
            Color.White.copy(alpha = highlightAlpha * 0.6f)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
}

fun glassCoralBorderBrush(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            CoralRed.copy(alpha = 0.6f),
            Color.White.copy(alpha = 0.2f),
            CoralRed.copy(alpha = 0.4f)
        )
    )
}

fun Modifier.glassmorphic(
    shape: Shape = RoundedCornerShape(16.dp),
    borderWidth: Dp = 1.dp,
    borderBrush: Brush = glassBorderBrush(),
    backgroundBrush: Brush = glassBackgroundBrush()
): Modifier = this
    .clip(shape)
    .background(backgroundBrush, shape)
    .border(borderWidth, borderBrush, shape)

/**
 * Ambient glowing orbs that sit behind glassmorphic surfaces,
 * providing vibrant light refraction without continuous redraw overhead.
 */
@Composable
fun AmbientGlassOrbsBackground(
    modifier: Modifier = Modifier,
    accentGlow: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        // High-fidelity atmospheric glow blooms
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-right warm coral ambient bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x38FF3838),
                        Color(0x18FF3838),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.85f, height * 0.15f),
                    radius = width * 0.65f
                ),
                center = Offset(width * 0.85f, height * 0.15f),
                radius = width * 0.65f
            )

            // Middle-left deep violet/blue acoustic bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x303E2D68),
                        Color(0x153E2D68),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.1f, height * 0.45f),
                    radius = width * 0.7f
                ),
                center = Offset(width * 0.1f, height * 0.45f),
                radius = width * 0.7f
            )

            // Bottom-right amber-gold hi-fi resonance bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x22FFB703),
                        Color(0x0CFFB703),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.75f, height * 0.82f),
                    radius = width * 0.55f
                ),
                center = Offset(width * 0.75f, height * 0.82f),
                radius = width * 0.55f
            )
        }

        content()
    }
}
