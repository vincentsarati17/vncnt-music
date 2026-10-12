package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.playback.MusicPlaybackService
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.ImportLyricsDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.ScanAudioDialog
import com.example.ui.viewmodel.AppScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.InactiveHeart
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.AmbientGlassOrbsBackground
import com.example.ui.theme.glassBorderBrush
import com.example.ui.theme.glassmorphic
import com.example.ui.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    private val playbackActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MusicPlaybackService.ACTION_REQUEST_TOGGLE -> viewModel.togglePlayPause()
                MusicPlaybackService.ACTION_REQUEST_NEXT -> viewModel.nextTrack()
                MusicPlaybackService.ACTION_REQUEST_PREV -> viewModel.previousTrack()
                MusicPlaybackService.ACTION_REQUEST_TOGGLE_PAUSE,
                MusicPlaybackService.ACTION_REQUEST_STOP -> viewModel.pauseFromSystem("service")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerPlaybackReceiver()
        // Best-effort notification permission for Android 13+; foreground
        // service notification itself is exempt, but full lock-screen
        // controls need the grant. No-op if already granted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MyApplicationTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }

                Box(modifier = Modifier.fillMaxSize()) {
                    MainContent(viewModel = viewModel)

                    AnimatedVisibility(
                        visible = showSplash,
                        enter = fadeIn(),
                        exit = fadeOut(animationSpec = tween(450)) + scaleOut(targetScale = 1.08f)
                    ) {
                        SplashScreen(
                            onSplashFinished = { showSplash = false }
                        )
                    }
                }
            }
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Intentionally no-op: playback works without it, notification
            // extras are just less prominent.
        }

    private fun registerPlaybackReceiver() {
        val filter = IntentFilter().apply {
            addAction(MusicPlaybackService.ACTION_REQUEST_TOGGLE)
            addAction(MusicPlaybackService.ACTION_REQUEST_NEXT)
            addAction(MusicPlaybackService.ACTION_REQUEST_PREV)
            addAction(MusicPlaybackService.ACTION_REQUEST_TOGGLE_PAUSE)
            addAction(MusicPlaybackService.ACTION_REQUEST_STOP)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(playbackActionReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(playbackActionReceiver, filter)
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "register playback receiver failed", e)
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(playbackActionReceiver)
        } catch (e: Exception) {
            // Already unregistered / never registered.
        }
        super.onDestroy()
    }
}

@Composable
fun MainContent(viewModel: MusicViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val allTracks by viewModel.allTracks.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val availablePlaylists by viewModel.availablePlaylists.collectAsState()
    val activePlaylistTracks by viewModel.activePlaylistTracks.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val showEqualizer by viewModel.showEqualizer.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val showScanDialog by viewModel.showScanDialog.collectAsState()

    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val isLyricsLoading by viewModel.isLyricsLoading.collectAsState()
    val isLyricsMode by viewModel.isLyricsMode.collectAsState()
    val showImportLyricsDialog by viewModel.showImportLyricsDialog.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = androidx.compose.runtime.remember {
        androidx.compose.material3.SnackbarHostState()
    }
    androidx.compose.runtime.LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage!!)
            viewModel.consumeError()
        }
    }

    val context = LocalContext.current

    // SAF Document Picker for selecting real audio files from device storage
    val audioFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importAudioFiles(uris)
        }
    }

    // Permission launcher for scanning device audio files
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Proceed with scanner (scanner will query MediaStore or use local fallback safely)
        viewModel.openScanDialog()
        viewModel.startScanDeviceAudio()
    }

    val requestScanAudio: () -> Unit = {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.openScanDialog()
        if (isGranted) {
            viewModel.startScanDeviceAudio()
        } else {
            audioPermissionLauncher.launch(permission)
        }
    }

    // Predictive back handler
    BackHandler(enabled = currentScreen == AppScreen.NOW_PLAYING) {
        viewModel.closeNowPlaying()
    }
    BackHandler(enabled = currentScreen != AppScreen.HOME && currentScreen != AppScreen.NOW_PLAYING) {
        viewModel.selectTab(AppScreen.HOME)
    }

    // Calculate next track preview for Now Playing
    val nextTrack = rememberNextTrack(activePlaylistTracks, playbackState.currentTrack)

    AmbientGlassOrbsBackground {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("app_scaffold"),
            containerColor = Color.Transparent,
            snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
            bottomBar = {
                // Only show bottom navigation & mini-player when NOT in full Now Playing screen
                if (currentScreen != AppScreen.NOW_PLAYING) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        // Mini player bar
                        if (playbackState.currentTrack != null) {
                            MiniPlayerBar(
                                playbackState = playbackState,
                                onPlayPause = { viewModel.togglePlayPause() },
                                onNext = { viewModel.nextTrack() },
                                onExpand = { viewModel.openNowPlaying() }
                            )
                        }

                        // Floating Glassmorphic Bottom Navigation Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .glassmorphic(
                                    shape = RoundedCornerShape(26.dp),
                                    borderBrush = glassBorderBrush(highlightAlpha = 0.35f, shadowAlpha = 0.08f)
                                )
                        ) {
                            NavigationBar(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .testTag("bottom_nav_bar"),
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.HOME,
                                    onClick = { viewModel.selectTab(AppScreen.HOME) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = "Home",
                                            tint = if (currentScreen == AppScreen.HOME) CoralRed else InactiveHeart,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            "Home",
                                            fontSize = 11.sp,
                                            fontWeight = if (currentScreen == AppScreen.HOME) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.Transparent,
                                        selectedIconColor = CoralRed,
                                        unselectedIconColor = InactiveHeart,
                                        selectedTextColor = CoralRed,
                                        unselectedTextColor = InactiveHeart
                                    ),
                                    modifier = Modifier.testTag("nav_home")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.PLAYLIST,
                                    onClick = { viewModel.selectTab(AppScreen.PLAYLIST) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                            contentDescription = "Playlist",
                                            tint = if (currentScreen == AppScreen.PLAYLIST) CoralRed else InactiveHeart,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            "Playlist",
                                            fontSize = 11.sp,
                                            fontWeight = if (currentScreen == AppScreen.PLAYLIST) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.Transparent,
                                        selectedIconColor = CoralRed,
                                        unselectedIconColor = InactiveHeart,
                                        selectedTextColor = CoralRed,
                                        unselectedTextColor = InactiveHeart
                                    ),
                                    modifier = Modifier.testTag("nav_playlist")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.FAVORITES,
                                    onClick = { viewModel.selectTab(AppScreen.FAVORITES) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = "Favorites",
                                            tint = if (currentScreen == AppScreen.FAVORITES) CoralRed else InactiveHeart,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            "Favorites",
                                            fontSize = 11.sp,
                                            fontWeight = if (currentScreen == AppScreen.FAVORITES) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.Transparent,
                                        selectedIconColor = CoralRed,
                                        unselectedIconColor = InactiveHeart,
                                        selectedTextColor = CoralRed,
                                        unselectedTextColor = InactiveHeart
                                    ),
                                    modifier = Modifier.testTag("nav_favorites")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (currentScreen == AppScreen.NOW_PLAYING) 0.dp else innerPadding.calculateBottomPadding())
            ) {
                when (currentScreen) {
                    AppScreen.PLAYLIST -> {
                        PlaylistScreen(
                            playlist = selectedPlaylist,
                            tracks = activePlaylistTracks,
                            playbackState = playbackState,
                            onTrackClick = { viewModel.playTrack(it) },
                            onShufflePlay = {
                                viewModel.toggleShuffle()
                                viewModel.nextTrack()
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onOpenEqualizer = { viewModel.setShowEqualizer(true) },
                            onScanLocalAudio = requestScanAudio,
                            onNavigateBack = { viewModel.selectTab(AppScreen.HOME) }
                        )
                    }

                    AppScreen.NOW_PLAYING -> {
                        NowPlayingScreen(
                            playbackState = playbackState,
                            nextTrackPreview = nextTrack,
                            isLyricsMode = isLyricsMode,
                            lyrics = currentLyrics,
                            isLyricsLoading = isLyricsLoading,
                            onToggleLyricsMode = { viewModel.toggleLyricsMode() },
                            onSeekToLyric = { viewModel.seekToMs(it) },
                            onFetchOnlineLyrics = {
                                playbackState.currentTrack?.let {
                                    viewModel.fetchLyricsForTrack(it, forceOnline = true)
                                }
                            },
                            onOpenImportLyricsDialog = { viewModel.openImportLyricsDialog() },
                            onPlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.nextTrack() },
                            onPrevious = { viewModel.previousTrack() },
                            onSeek = { viewModel.seekTo(it) },
                            onToggleShuffle = { viewModel.toggleShuffle() },
                            onToggleRepeat = { viewModel.toggleRepeat() },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onOpenEqualizer = { viewModel.setShowEqualizer(true) },
                            onNavigateBack = { viewModel.closeNowPlaying() }
                        )
                    }

                    AppScreen.HOME -> {
                        HomeScreen(
                            playlists = availablePlaylists,
                            recentTracks = allTracks,
                            playbackState = playbackState,
                            onPlaylistClick = { viewModel.selectPlaylist(it) },
                            onTrackClick = { viewModel.playTrack(it) },
                            onShuffleAll = {
                                viewModel.toggleShuffle()
                                if (allTracks.isNotEmpty()) {
                                    viewModel.playTrack(allTracks.random())
                                }
                            },
                            onOpenEqualizer = { viewModel.setShowEqualizer(true) },
                            onScanLocalAudio = requestScanAudio
                        )
                    }

                    AppScreen.FAVORITES -> {
                        FavoritesScreen(
                            favoriteTracks = favoriteTracks,
                            playbackState = playbackState,
                            onTrackClick = { viewModel.playTrack(it) },
                            onShuffleFavorites = {
                                if (favoriteTracks.isNotEmpty()) {
                                    viewModel.toggleShuffle()
                                    viewModel.playTrack(favoriteTracks.random())
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showEqualizer) {
        EqualizerDialog(
            playbackState = playbackState,
            onSelectPreset = { viewModel.setSoundPreset(it) },
            onToggleBassBoost = { viewModel.toggleBassBoost() },
            onToggleVirtualizer = { viewModel.toggleVirtualizer() },
            onToggleHiRes = { viewModel.toggleHiResMode() },
            onSetSpeed = { viewModel.setSpeed(it) },
            onDismiss = { viewModel.setShowEqualizer(false) }
        )
    }

    if (showScanDialog) {
        ScanAudioDialog(
            isScanning = isScanning,
            scanResult = scanResult,
            onStartScan = { viewModel.startScanDeviceAudio() },
            onImportTracks = { viewModel.addScannedTracksToLibrary() },
            onPickAudioFiles = { audioFilePickerLauncher.launch(arrayOf("audio/*")) },
            onDismiss = { viewModel.dismissScanDialog() }
        )
    }

    if (showImportLyricsDialog) {
        ImportLyricsDialog(
            track = playbackState.currentTrack,
            onDismiss = { viewModel.closeImportLyricsDialog() },
            onSaveLyrics = { viewModel.saveCustomLyrics(it) }
        )
    }
}

@Composable
fun rememberNextTrack(tracks: List<com.example.data.model.Track>, current: com.example.data.model.Track?): com.example.data.model.Track? {
    // Up-next preview uses shared :core queue math (peekNext wraps at end).
    return com.vncnt.music.core.queue.QueueNavigator.peekNext(tracks, current?.id)
}
