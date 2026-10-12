package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.Track
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceVariant

@Composable
fun CircularAlbumArt(
    track: Track?,
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    discSize: Dp = 230.dp
) {
    val context = LocalContext.current

    // Subtle gentle vinyl rotation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val currentRotation = if (isPlaying) rotationAngle else 0f

    // Resolve drawable resource
    val resId = rememberDrawableResId(context, track?.imageResName)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(discSize)
    ) {
        // Outer concentric progress ring and vinyl bezel
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 5.dp.toPx()
            val canvasRadius = size.minDimension / 2f
            val arcRadius = canvasRadius - strokeWidth

            // Dark outer rim track
            drawCircle(
                color = Color(0xFF20222A),
                radius = arcRadius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Dynamic Coral-Red progress arc (starts at -90 degrees / 12 o'clock)
            val sweepAngle = (progress.coerceIn(0f, 1f) * 360f)
            if (sweepAngle > 0f) {
                drawArc(
                    color = CoralRed,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Concentric vinyl groove shadow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x00000000),
                        Color(0x55000000),
                        Color(0xDD0B0C0E)
                    ),
                    center = center,
                    radius = arcRadius
                ),
                radius = arcRadius - 4.dp.toPx()
            )
        }

        // Circular album image container inside the bezel
        Box(
            modifier = Modifier
                .size(discSize - 32.dp)
                .rotate(currentRotation)
                .clip(CircleShape)
                .background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (resId != 0) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = track?.title ?: "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Vinyl record fallback
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = Color(0xFF16171C))
                    // Subtle vinyl grooves
                    for (r in listOf(0.3f, 0.5f, 0.7f, 0.85f)) {
                        drawCircle(
                            color = Color(0xFF262832),
                            radius = (size.minDimension / 2f) * r,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                    drawCircle(
                        color = CoralRed,
                        radius = (size.minDimension / 2f) * 0.15f
                    )
                }
            }

            // Central vinyl spindle hole accent
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(DarkBackground)
            )
        }
    }
}

@Composable
fun rememberDrawableResId(context: android.content.Context, name: String?): Int {
    if (name.isNullOrBlank()) return 0
    return androidx.compose.runtime.remember(name) {
        context.resources.getIdentifier(name, "drawable", context.packageName)
    }
}
