package com.vncnt.music.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vncnt.music.R
import com.vncnt.music.ui.theme.AmbientGlassOrbsBackground
import com.vncnt.music.ui.theme.CoralRed
import com.vncnt.music.ui.theme.CoralRedGlow
import com.vncnt.music.ui.theme.DarkSurfaceVariant
import com.vncnt.music.ui.theme.HiResGold
import com.vncnt.music.ui.theme.TextMuted
import com.vncnt.music.ui.theme.TextPrimary
import com.vncnt.music.ui.theme.TextSecondary
import com.vncnt.music.ui.theme.glassBorderBrush
import com.vncnt.music.ui.theme.glassmorphic
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Entrance animations
    val contentScale = remember { Animatable(0.85f) }
    val contentAlpha = remember { Animatable(0f) }

    // Subtle ambient icon pulse glow
    val infiniteTransition = rememberInfiniteTransition(label = "SplashIconPulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    var statusMessage by remember { mutableStateOf("Initializing Hi-Fi Audio Engine...") }

    LaunchedEffect(Unit) {
        // Entrance animation
        contentScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
        )

        // Progress text steps
        delay(650)
        statusMessage = "Loading Room Database..."
        delay(650)
        statusMessage = "Studio Acoustics Ready"
        delay(500)

        onSplashFinished()
    }

    AmbientGlassOrbsBackground(
        modifier = modifier
            .fillMaxSize()
            .testTag("splash_screen")
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Allow instant tap-to-continue
                onSplashFinished()
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(contentScale.value)
                .alpha(contentAlpha.value),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                // App Icon Container (ready for official app launcher icon)
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .testTag("splash_app_icon_container"),
                    contentAlignment = Alignment.Center
                ) {
                    // Soft atmospheric radial brand glow
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    CoralRed.copy(alpha = 0.35f * pulseGlow),
                                    CoralRedGlow.copy(alpha = 0.12f * pulseGlow),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension / 1.5f
                            ),
                            radius = size.minDimension / 1.5f
                        )
                    }

                    // App icon squircle card
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(DarkSurfaceVariant)
                            .border(
                                1.5.dp,
                                glassBorderBrush(highlightAlpha = 0.3f),
                                RoundedCornerShape(24.dp)
                            )
                            .testTag("splash_logo"),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_music_icon),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // App Title
                Text(
                    text = "VNCNT MUSIC",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tagline / Subtitle badge
                Box(
                    modifier = Modifier
                        .glassmorphic(
                            shape = RoundedCornerShape(20.dp),
                            borderBrush = glassBorderBrush(highlightAlpha = 0.3f),
                            backgroundBrush = Brush.horizontalGradient(
                                colors = listOf(
                                    DarkSurfaceVariant.copy(alpha = 0.6f),
                                    DarkSurfaceVariant.copy(alpha = 0.3f)
                                )
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(HiResGold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HIGH-FIDELITY OFFLINE AUDIO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.5.sp,
                                color = HiResGold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Status message
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Subtle tap-to-skip hint
                Text(
                    text = "Tap anywhere to skip",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
