package com.vncnt.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vncnt.music.core.model.PlaybackState
import com.vncnt.music.ui.theme.CoralRed
import com.vncnt.music.ui.theme.DarkCardBorder
import com.vncnt.music.ui.theme.DarkSurfaceVariant
import com.vncnt.music.ui.theme.HairlineBorder
import com.vncnt.music.ui.theme.HiResGold
import com.vncnt.music.ui.theme.TextMuted
import com.vncnt.music.ui.theme.TextPrimary
import com.vncnt.music.ui.theme.TextSecondary

@Composable
fun MiniPlayerBar(
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    val context = LocalContext.current
    val resId = rememberDrawableResId(context, track.imageResName)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onExpand)
            .testTag("mini_player_bar")
    ) {
        Column {
            // Micro progress indicator along the top edge
            LinearProgressIndicator(
                progress = { playbackState.currentPositionProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = CoralRed,
                trackColor = Color(0x22FFFFFF)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album artwork thumbnail
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF22242D))
                        .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (resId != 0) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(42.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = CoralRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title, Artist and Codec badge
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (track.isHiRes) {
                            Text(
                                text = "HI-RES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = HiResGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = " • ",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                        }
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Play / Pause circular button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CoralRed)
                        .clickable(onClick = onPlayPause)
                        .testTag("mini_player_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Skip Next button
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
