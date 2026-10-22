package com.vncnt.music.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vncnt.music.core.model.PlaybackState
import com.vncnt.music.core.model.Playlist
import com.vncnt.music.core.model.Track
import com.vncnt.music.ui.components.rememberDrawableResId
import com.vncnt.music.ui.theme.AudiophileAmber
import com.vncnt.music.ui.theme.CoralRed
import com.vncnt.music.ui.theme.CoralRedGlow
import com.vncnt.music.ui.theme.CoralRedLight
import com.vncnt.music.ui.theme.DarkCardBorder
import com.vncnt.music.ui.theme.DarkSurface
import com.vncnt.music.ui.theme.DarkSurfaceVariant
import com.vncnt.music.ui.theme.HairlineBorder
import com.vncnt.music.ui.theme.HiResGold
import com.vncnt.music.ui.theme.TextMuted
import com.vncnt.music.ui.theme.TextPrimary
import com.vncnt.music.ui.theme.TextSecondary
import com.vncnt.music.ui.theme.glassBackgroundBrush
import com.vncnt.music.ui.theme.glassBorderBrush
import com.vncnt.music.ui.theme.glassCoralBorderBrush
import com.vncnt.music.ui.theme.glassmorphic

@Composable
fun HomeScreen(
    playlists: List<Playlist>,
    recentTracks: List<Track>,
    playbackState: PlaybackState,
    onPlaylistClick: (Playlist) -> Unit,
    onTrackClick: (Track) -> Unit,
    onShuffleAll: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onScanLocalAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // 1. Studio Masthead
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VNCNT",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "PURE OFFLINE HIGH-FIDELITY AUDIO",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextMuted,
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp
                            )
                        )
                    }

                    // Tactile Hardware Equalizer Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenEqualizer)
                            .testTag("home_eq_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Studio Equalizer",
                            tint = CoralRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // 2. Hardware Utility Bar (Shuffle All & Scan Device Storage)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Shuffle button with audio signal styling
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    DarkSurfaceVariant,
                                    DarkSurface
                                )
                            )
                        )
                        .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onShuffleAll)
                        .testTag("home_shuffle_all_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = CoralRed,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Shuffle All",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Scan Audio button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onScanLocalAudio)
                        .testTag("home_scan_audio_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = AudiophileAmber,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan Device",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        // 3. Featured Playlists & Curated Crates
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library Crates",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Text(
                    text = "${playlists.size} CRATES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    val bannerRes = rememberDrawableResId(context, playlist.bannerDrawableName)
                    Box(
                        modifier = Modifier
                            .width(210.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
                            .clickable { onPlaylistClick(playlist) }
                            .testTag("playlist_card_${playlist.id}")
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            ) {
                                if (bannerRes != 0) {
                                    Image(
                                        painter = painterResource(id = bannerRes),
                                        contentDescription = playlist.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color(0xFF2E1A22),
                                                        Color(0xFF161822)
                                                    )
                                                )
                                            )
                                    )
                                }

                                // Dark vignette gradient
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color(0xCC0E1015)
                                                )
                                            )
                                        )
                                )

                                // Micro category tag
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xCC0D0E13))
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = playlist.filterTag,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = HiResGold,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.8.sp
                                        )
                                    )
                                }

                                // Play icon badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp)
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(CoralRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = playlist.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${playlist.songCount} TRACKS • ${playlist.artistCount} ARTISTS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // 4. Offline Tracks Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library Tracks",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                if (recentTracks.isNotEmpty()) {
                    Text(
                        text = "${recentTracks.size} AUDIO FILES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (recentTracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(DarkSurface)
                                .border(1.dp, HairlineBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = CoralRed,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Offline Library Empty",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No audio files indexed yet. Scan your device storage or import FLAC/WAV/MP3 audio to begin.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onScanLocalAudio,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                        ) {
                            Icon(imageVector = Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Device Storage", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        } else {
            itemsIndexed(recentTracks, key = { _, track -> track.id }) { index, track ->
                val isCurrent = playbackState.currentTrack?.id == track.id
                val isPlayingCurrent = isCurrent && playbackState.isPlaying

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackClick(track) }
                        .background(if (isCurrent) DarkSurfaceVariant.copy(alpha = 0.5f) else Color.Transparent)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .testTag("home_track_${track.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Numbering or Animated Waveform indicator
                    Box(
                        modifier = Modifier.width(28.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (isPlayingCurrent) {
                            LiveEqualizerIcon()
                        } else {
                            Text(
                                text = "%02d".format(index + 1),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isCurrent) CoralRed else TextMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Track info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) CoralRed else TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Audio Quality badge
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
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Monospace duration
                    Text(
                        text = track.durationFormatted,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextMuted
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveEqualizerIcon() {
    val transition = rememberInfiniteTransition(label = "eq_bars")
    val h1 by transition.animateFloat(
        initialValue = 4f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by transition.animateFloat(
        initialValue = 12f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by transition.animateFloat(
        initialValue = 6f, targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3"
    )

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(16.dp)
    ) {
        Box(modifier = Modifier.width(2.5.dp).height(h1.dp).background(CoralRed, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(2.5.dp).height(h2.dp).background(CoralRed, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(2.5.dp).height(h3.dp).background(CoralRed, RoundedCornerShape(1.dp)))
    }
}
