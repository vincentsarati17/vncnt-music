package com.vncnt.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vncnt.music.data.model.LyricsData
import com.vncnt.music.data.model.PlaybackState
import com.vncnt.music.data.model.Track
import com.vncnt.music.ui.components.CircularAlbumArt
import com.vncnt.music.ui.components.LyricsView
import com.vncnt.music.ui.components.WaveformVisualizer
import com.vncnt.music.ui.theme.CoralRed
import com.vncnt.music.ui.theme.CoralRedGlow
import com.vncnt.music.ui.theme.CoralRedLight
import com.vncnt.music.ui.theme.DarkCardBorder
import com.vncnt.music.ui.theme.DarkSurface
import com.vncnt.music.ui.theme.DarkSurfaceVariant
import com.vncnt.music.ui.theme.FavoriteHeart
import com.vncnt.music.ui.theme.HairlineBorder
import com.vncnt.music.ui.theme.HiResGold
import com.vncnt.music.ui.theme.InactiveHeart
import com.vncnt.music.ui.theme.StudioMint
import com.vncnt.music.ui.theme.TextMuted
import com.vncnt.music.ui.theme.TextPrimary
import com.vncnt.music.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackState,
    nextTrackPreview: Track?,
    isLyricsMode: Boolean = false,
    lyrics: LyricsData? = null,
    isLyricsLoading: Boolean = false,
    onToggleLyricsMode: () -> Unit = {},
    onSeekToLyric: (Long) -> Unit = {},
    onFetchOnlineLyrics: () -> Unit = {},
    onOpenImportLyricsDialog: () -> Unit = {},
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenEqualizer: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(StudioMint)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MASTER AUDIO • 32-BIT DSP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.4.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .border(1.dp, HairlineBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("now_playing_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Library",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Live Lyrics Toggle Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isLyricsMode) CoralRed.copy(alpha = 0.2f) else DarkSurfaceVariant)
                            .border(1.dp, if (isLyricsMode) CoralRed else HairlineBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onToggleLyricsMode,
                            modifier = Modifier.testTag("now_playing_lyrics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Live Synchronized Lyrics",
                                tint = if (isLyricsMode) CoralRed else TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Audio Equalizer Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .border(1.dp, HairlineBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onOpenEqualizer,
                            modifier = Modifier.testTag("now_playing_eq_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Hi-Fi Equalizer",
                                tint = CoralRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Favorite Button
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .border(1.dp, HairlineBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { track?.let { onToggleFavorite(it) } },
                            modifier = Modifier.testTag("now_playing_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (track?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (track?.isFavorite == true) FavoriteHeart else InactiveHeart,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .testTag("now_playing_content"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // View Mode Switcher Pill (Artwork vs Live Lyrics)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (!isLyricsMode) CoralRed else Color.Transparent)
                        .clickable { if (isLyricsMode) onToggleLyricsMode() }
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                        .testTag("tab_album_art"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Artwork",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (!isLyricsMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isLyricsMode) Color.White else TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isLyricsMode) CoralRed else Color.Transparent)
                        .clickable { if (!isLyricsMode) onToggleLyricsMode() }
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                        .testTag("tab_live_lyrics"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isLyricsMode) Color.White else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Live Lyrics",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isLyricsMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (isLyricsMode) Color.White else TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 1. Central Area: Circular Album Art OR Synced Lyrics View
            if (!isLyricsMode) {
                Box(
                    modifier = Modifier
                        .size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularAlbumArt(
                        track = track,
                        progress = playbackState.currentPositionProgress,
                        isPlaying = playbackState.isPlaying,
                        discSize = 230.dp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    LyricsView(
                        track = track,
                        lyrics = lyrics,
                        currentPositionMs = playbackState.currentPositionMs,
                        isLoading = isLyricsLoading,
                        onSeekToLyric = onSeekToLyric,
                        onFetchOnline = onFetchOnlineLyrics,
                        onOpenImportDialog = onOpenImportLyricsDialog
                    )
                }
            }

            // 2. High-Resolution Audio Spec Badge
            if (track != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenEqualizer)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(HiResGold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = track.audioQualityBadge.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = HiResGold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // 3. Track Title & Artist Metadata
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = track?.title ?: "No track selected",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${track?.artist ?: "Unknown Artist"} • ${track?.album ?: "Offline Library"}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 4. Waveform Scrubber Deck
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Interactive Waveform visualizer
                WaveformVisualizer(
                    amplitudes = playbackState.amplitudes,
                    isPlaying = playbackState.isPlaying,
                    height = 54.dp
                )

                // Precision Seek Slider
                Slider(
                    value = playbackState.currentPositionProgress,
                    onValueChange = { onSeek(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .testTag("now_playing_seek_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = CoralRed,
                        activeTrackColor = CoralRed,
                        inactiveTrackColor = Color(0x28FFFFFF)
                    )
                )

                // Monospace Time Labels: Elapsed & Remaining
                val currentSec = playbackState.currentPositionSeconds
                val totalSec = track?.durationSeconds?.coerceAtLeast(1) ?: 1
                val remainingSec = (totalSec - currentSec).coerceAtLeast(0)

                val elapsedFormatted = "%d:%02d".format(currentSec / 60, currentSec % 60)
                val remainingFormatted = "-%d:%02d".format(remainingSec / 60, remainingSec % 60)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = elapsedFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = remainingFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                }
            }

            // 5. Master Transport Playback Controls Deck
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(28.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle button with LED dot indicator
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.testTag("control_shuffle")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.isShuffle) CoralRed else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        if (playbackState.isShuffle) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(CoralRed)
                            )
                        }
                    }
                }

                // Previous Track
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.testTag("control_prev")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Master Play / Pause Button with dual-ring halo
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CoralRedLight, CoralRed)
                            )
                        )
                        .clickable(onClick = onPlayPause)
                        .testTag("control_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Next Track
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.testTag("control_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Repeat Mode Toggle
                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier.testTag("control_repeat")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (playbackState.isRepeat) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (playbackState.isRepeat) CoralRed else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        if (playbackState.isRepeat) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(CoralRed)
                            )
                        }
                    }
                }
            }

            // 6. Up Next Drawer Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("more_songs_section")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "UP NEXT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = nextTrackPreview?.title ?: "Queue complete",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (nextTrackPreview != null) {
                        Text(
                            text = nextTrackPreview.durationFormatted,
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }
                }
            }
        }
    }
}
