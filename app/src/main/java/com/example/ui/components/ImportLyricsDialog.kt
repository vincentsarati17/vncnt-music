package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Track
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ImportLyricsDialog(
    track: Track?,
    onDismiss: () -> Unit,
    onSaveLyrics: (String) -> Unit
) {
    var lrcInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("import_lyrics_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Import Lyrics",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = track?.title ?: "Current Track",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Format explanation
                Text(
                    text = "Paste text in timestamped LRC format (e.g. [00:15.50] Lyric line) or plain text lyrics.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Text field
                OutlinedTextField(
                    value = lrcInput,
                    onValueChange = { lrcInput = it },
                    placeholder = {
                        Text(
                            text = "[00:00.00] Intro\n[00:12.50] First verse line\n[00:24.00] Chorus...",
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .testTag("lrc_text_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CoralRed,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick presets / sample helper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val duration = track?.durationSeconds ?: 180
                            val q1 = duration / 4
                            val q2 = duration / 2
                            val q3 = (duration * 3) / 4
                            lrcInput = buildString {
                                appendLine("[00:02.00] ♪ Instrumental Intro ♪")
                                appendLine("[%02d:%02d.00] When the music takes over the room".format(q1 / 60, q1 % 60))
                                appendLine("[%02d:%02d.00] Feel the rhythm flowing through your mind".format(q2 / 60, q2 % 60))
                                appendLine("[%02d:%02d.00] Pure high-fidelity acoustic sound".format(q3 / 60, q3 % 60))
                                appendLine("[%02d:%02d.00] Outro fade".format((duration - 5).coerceAtLeast(0) / 60, (duration - 5).coerceAtLeast(0) % 60))
                            }
                        }
                    ) {
                        Text("Fill Demo LRC Template", fontSize = 12.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (lrcInput.isNotBlank()) {
                                onSaveLyrics(lrcInput.trim())
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                        shape = RoundedCornerShape(12.dp),
                        enabled = lrcInput.isNotBlank(),
                        modifier = Modifier.testTag("save_lyrics_button")
                    ) {
                        Text("Save & Sync", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
