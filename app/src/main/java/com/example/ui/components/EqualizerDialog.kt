package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PlaybackState
import com.example.data.model.SoundPreset
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.HiResGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.glassBackgroundBrush
import com.example.ui.theme.glassBorderBrush
import com.example.ui.theme.glassCoralBorderBrush
import com.example.ui.theme.glassmorphic

@Composable
fun EqualizerDialog(
    playbackState: PlaybackState,
    onSelectPreset: (SoundPreset) -> Unit,
    onToggleBassBoost: () -> Unit,
    onToggleVirtualizer: () -> Unit,
    onToggleHiRes: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .glassmorphic(
                    shape = RoundedCornerShape(26.dp),
                    borderBrush = glassBorderBrush(highlightAlpha = 0.45f, shadowAlpha = 0.12f),
                    backgroundBrush = glassBackgroundBrush(startAlpha = 0.9f, endAlpha = 0.75f)
                )
                .testTag("equalizer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .glassmorphic(shape = CircleShape, borderBrush = glassCoralBorderBrush())
                                .background(Color(0xFF2A1C20), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = CoralRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Hi-Fi Audio Master",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "DSP Equalizer & Acoustic Profile",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .glassmorphic(shape = CircleShape, borderBrush = glassBorderBrush(highlightAlpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_equalizer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hi-Res Badge Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF383325))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "HI-RES AUDIO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = HiResGold,
                                        fontSize = 10.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "24-bit / 96kHz",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                            Text(
                                text = "Uncompressed direct master output with ultra-low jitter",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Switch(
                            checked = playbackState.isHiResModeEnabled,
                            onCheckedChange = { onToggleHiRes() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CoralRed
                            ),
                            modifier = Modifier.testTag("hi_res_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ACOUSTIC PRESETS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                SoundPreset.values().forEach { preset ->
                    val isSelected = playbackState.activePreset == preset
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelectPreset(preset) }
                            .testTag("preset_${preset.name}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF281F23) else DarkSurfaceVariant
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) CoralRed else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) CoralRed else TextPrimary
                                    )
                                )
                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(CoralRed)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DSP ENHANCEMENTS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bass Boost Toggle Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "Bass Boost (+12dB)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "Reinforces low-end resonance for deep acoustic bass & drums",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Switch(
                            checked = playbackState.bassBoostEnabled,
                            onCheckedChange = { onToggleBassBoost() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CoralRed
                            ),
                            modifier = Modifier.testTag("bass_boost_switch")
                        )
                    }
                }

                // 3D Virtualizer Toggle Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "3D Virtualizer Soundstage",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "Expands stereo field width to simulate binaural headphones",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Switch(
                            checked = playbackState.virtualizerEnabled,
                            onCheckedChange = { onToggleVirtualizer() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CoralRed
                            ),
                            modifier = Modifier.testTag("virtualizer_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Playback speed
                Text(
                    text = "PLAYBACK SPEED",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { speed ->
                        val isSpeedSelected = playbackState.playbackSpeed == speed
                        FilterChip(
                            selected = isSpeedSelected,
                            onClick = { onSetSpeed(speed) },
                            label = { Text("${speed}x") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CoralRed,
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("speed_${speed}x")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("done_equalizer_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text(
                        text = "Apply Settings",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}
