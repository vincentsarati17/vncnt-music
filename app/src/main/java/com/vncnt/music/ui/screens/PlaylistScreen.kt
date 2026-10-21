package com.vncnt.music.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vncnt.music.data.model.PlaybackState
import com.vncnt.music.data.model.Playlist
import com.vncnt.music.data.model.Track
import com.vncnt.music.ui.components.rememberDrawableResId
import com.vncnt.music.ui.theme.CoralRed
import com.vncnt.music.ui.theme.DarkBackground
import com.vncnt.music.ui.theme.DarkCardBorder
import com.vncnt.music.ui.theme.DarkSurface
import com.vncnt.music.ui.theme.DarkSurfaceVariant
import com.vncnt.music.ui.theme.FavoriteHeart
import com.vncnt.music.ui.theme.HiResGold
import com.vncnt.music.ui.theme.InactiveHeart
import com.vncnt.music.ui.theme.TextMuted
import com.vncnt.music.ui.theme.TextPrimary
import com.vncnt.music.ui.theme.TextSecondary
import com.vncnt.music.ui.theme.glassBackgroundBrush
import com.vncnt.music.ui.theme.glassBorderBrush
import com.vncnt.music.ui.theme.glassCoralBorderBrush
import com.vncnt.music.ui.theme.glassmorphic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlist: Playlist,
    tracks: List<Track>,
    playbackState: PlaybackState,
    onTrackClick: (Track) -> Unit,
    onShufflePlay: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenEqualizer: () -> Unit,
    onScanLocalAudio: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenNowPlaying: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    val bannerResId = rememberDrawableResId(context, playlist.bannerDrawableName)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Playlist",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(38.dp)
                            .glassmorphic(shape = CircleShape, borderBrush = glassBorderBrush(highlightAlpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("playlist_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(38.dp)
                            .glassmorphic(shape = CircleShape, borderBrush = glassBorderBrush(highlightAlpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("playlist_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier
                                .glassmorphic(
                                    shape = RoundedCornerShape(16.dp),
                                    borderBrush = glassBorderBrush()
                                )
                        ) {
                            DropdownMenuItem(
                                text = { Text("Hi-Fi Equalizer & DSP", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = CoralRed
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOpenEqualizer()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Scan Local Audio", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Radar,
                                        contentDescription = null,
                                        tint = CoralRed
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onScanLocalAudio()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("playlist_track_list"),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // Hero Header with headphones image & floating red shuffle FAB
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    // Header background image with gradient fade
                    if (bannerResId != 0) {
                        Image(
                            painter = painterResource(id = bannerResId),
                            contentDescription = playlist.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(DarkSurfaceVariant)
                        )
                    }

                    // Dark gradient scrim over the image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x33121316),
                                        Color(0xBB121316),
                                        DarkBackground
                                    )
                                )
                            )
                    )

                    // Floating circular coral-red Shuffle FAB (as seen in screenshot!)
                    FloatingActionButton(
                        onClick = onShufflePlay,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-10).dp)
                            .size(54.dp)
                            .testTag("shuffle_fab_button"),
                        shape = CircleShape,
                        containerColor = CoralRed,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle Playlist",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Playlist title and count metadata
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        if (playlist.description.isNotBlank()) {
                            Text(
                                text = playlist.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // High-Resolution Metadata Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        val distinctArtists = tracks.map { it.artist }.distinct().count { it.isNotBlank() }
                        val artistLabel = if (distinctArtists == 1) "1 ARTIST" else "$distinctArtists ARTISTS"
                        val songLabel = if (tracks.size == 1) "1 TRACK" else "${tracks.size} TRACKS"
                        Text(
                            text = "$songLabel • $artistLabel",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = HiResGold,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Empty state if no tracks in playlist
            if (tracks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF2C2F3A),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No tracks in this playlist",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Scan your device or import local audio files to populate this playlist",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )
                        Button(
                            onClick = onScanLocalAudio,
                            colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Device Storage")
                        }
                    }
                }
            }

            // Numbered track list
            itemsIndexed(
                items = tracks,
                key = { _, track -> track.id }
            ) { index, track ->
                val isCurrent = playbackState.currentTrack?.id == track.id
                PlaylistItemRow(
                    index = index + 1,
                    track = track,
                    isPlaying = isCurrent && playbackState.isPlaying,
                    isCurrent = isCurrent,
                    onClick = { onTrackClick(track) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onOpenNowPlaying = onOpenNowPlaying
                )
            }
        }
    }
}

@Composable
fun PlaylistItemRow(
    index: Int,
    track: Track,
    isPlaying: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenNowPlaying: () -> Unit = {}
) {
    val context = LocalContext.current
    val artistThumbResId = rememberDrawableResId(context, track.imageResName)
    var rowMenuExpanded by remember { mutableStateOf(false) }

    val rowModifier = if (isCurrent) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .glassmorphic(
                shape = RoundedCornerShape(16.dp),
                borderBrush = glassCoralBorderBrush(),
                backgroundBrush = glassBackgroundBrush(startAlpha = 0.45f, endAlpha = 0.2f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    }

    Row(
        modifier = rowModifier.testTag("track_row_${track.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number (e.g. "01", "02")
        Text(
            text = "%02d".format(index),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) CoralRed else TextMuted
            ),
            modifier = Modifier.width(28.dp)
        )

        // Squircle artwork thumbnail
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF22242D)),
            contentAlignment = Alignment.Center
        ) {
            if (artistThumbResId != 0) {
                Image(
                    painter = painterResource(id = artistThumbResId),
                    contentDescription = track.artist,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = CoralRed,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Subtle playing badge overlay
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x77000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Playing",
                        tint = CoralRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Artist • Duration
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrent) CoralRed else TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${track.artist}  •  ${track.durationFormatted}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (track.isHiRes) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hi-Res",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HiResGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Heart favorite button (Coral red filled if active, grey outline if not)
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .size(36.dp)
                .testTag("fav_button_${track.id}")
        ) {
            Icon(
                imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (track.isFavorite) "Liked" else "Like",
                tint = if (track.isFavorite) FavoriteHeart else InactiveHeart,
                modifier = Modifier.size(20.dp)
            )
        }

        // 3-dots action menu
        Box {
            IconButton(
                onClick = { rowMenuExpanded = true },
                modifier = Modifier
                    .size(32.dp)
                    .testTag("options_button_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = InactiveHeart,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = rowMenuExpanded,
                onDismissRequest = { rowMenuExpanded = false },
                modifier = Modifier.glassmorphic(
                    shape = RoundedCornerShape(16.dp),
                    borderBrush = glassBorderBrush()
                )
            ) {
                DropdownMenuItem(
                    text = { Text("Play", color = TextPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = CoralRed
                        )
                    },
                    onClick = {
                        rowMenuExpanded = false
                        onClick()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (track.isFavorite) "Remove from Favorites" else "Add to Favorites",
                            color = TextPrimary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (track.isFavorite) FavoriteHeart else InactiveHeart
                        )
                    },
                    onClick = {
                        rowMenuExpanded = false
                        onToggleFavorite()
                    }
                )
                if (isCurrent) {
                    DropdownMenuItem(
                        text = { Text("Open Now Playing", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = CoralRed
                            )
                        },
                        onClick = {
                            rowMenuExpanded = false
                            onOpenNowPlaying()
                        }
                    )
                }
            }
        }
    }
}
