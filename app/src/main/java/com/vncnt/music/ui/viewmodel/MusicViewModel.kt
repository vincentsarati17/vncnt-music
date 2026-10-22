package com.vncnt.music.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vncnt.music.audio.AudioFocusHelper
import com.vncnt.music.audio.HiFiAudioEngine
import com.vncnt.music.core.audio.AudioEngine
import com.vncnt.music.core.audio.AudioEngineListener
import com.vncnt.music.core.db.AppDatabase
// Companion extension from :core androidMain (see MusicRepository).
import com.vncnt.music.core.db.getDatabase
import com.vncnt.music.core.model.LyricsData
import com.vncnt.music.core.model.PlaybackState
import com.vncnt.music.core.model.Playlist
import com.vncnt.music.core.model.SoundPreset
import com.vncnt.music.core.model.Track
import com.vncnt.music.data.prefs.UserPreferencesRepository
import com.vncnt.music.data.repository.LyricsRepository
import com.vncnt.music.data.repository.MusicRepository
import com.vncnt.music.data.scanner.ScanResult
import com.vncnt.music.playback.MusicPlaybackService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    PLAYLIST,
    NOW_PLAYING,
    HOME,
    FAVORITES
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val repository = MusicRepository(application)
    private val lyricsRepository = LyricsRepository(application, AppDatabase.getDatabase(application).lyricsDao())
    private val prefsRepo = UserPreferencesRepository(application)
    // Programmed to the shared contract; Android injects HiFiAudioEngine,
    // iOS will inject its AVPlayer-backed implementation later.
    private val audioEngine: AudioEngine = HiFiAudioEngine(application)

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "ViewModel coroutine failed", e)
        _errorMessage.value = "Something went wrong. Please try again."
    }

    private val focusHelper = AudioFocusHelper(
        application,
        onPauseForLoss = { pauseFromSystem("focus-loss") },
        onResumeAfterGain = { /* manual resume only: avoids surprise playback */ },
        onDuck = { duck -> audioEngine.setDucked(duck) }
    )

    private var previousTab = AppScreen.HOME

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun consumeError() {
        _errorMessage.value = null
    }

    // Lyrics State
    private val _currentLyrics = MutableStateFlow<LyricsData?>(null)
    val currentLyrics: StateFlow<LyricsData?> = _currentLyrics.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

    private val _isLyricsMode = MutableStateFlow(false)
    val isLyricsMode: StateFlow<Boolean> = _isLyricsMode.asStateFlow()

    private val _showImportLyricsDialog = MutableStateFlow(false)
    val showImportLyricsDialog: StateFlow<Boolean> = _showImportLyricsDialog.asStateFlow()

    val allTracks: StateFlow<List<Track>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<Track>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availablePlaylists: StateFlow<List<Playlist>> = allTracks.map { tracks ->
        repository.buildPlaylists(tracks)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        repository.buildPlaylists(emptyList())
    )

    private val _selectedPlaylist = MutableStateFlow(repository.buildPlaylists(emptyList())[0])
    val selectedPlaylist: StateFlow<Playlist> = _selectedPlaylist.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showEqualizer = MutableStateFlow(false)
    val showEqualizer: StateFlow<Boolean> = _showEqualizer.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult.asStateFlow()

    private val _showScanDialog = MutableStateFlow(false)
    val showScanDialog: StateFlow<Boolean> = _showScanDialog.asStateFlow()

    // Playlist filtered tracks based on dynamic filter tags
    val activePlaylistTracks: StateFlow<List<Track>> = combine(
        allTracks,
        favoriteTracks,
        _selectedPlaylist,
        _searchQuery
    ) { all, favs, playlist, query ->
        val baseList = when (playlist.filterTag) {
            "ALL" -> all
            "FAVORITES" -> favs
            "TOP" -> all.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
            else -> all.filter { it.genre.equals(playlist.filterTag, ignoreCase = true) }
        }
        if (query.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(handler) {
            try {
                repository.initializePreloadedTracksIfEmpty()
            } catch (e: Exception) {
                Log.w(TAG, "init library failed", e)
            }
        }

        // Restore persisted DSP prefs before first playback.
        viewModelScope.launch(handler) {
            try {
                val p = prefsRepo.prefs.first()
                audioEngine.setSoundPreset(p.preset)
                audioEngine.setBassBoost(p.bassBoost)
                audioEngine.setVirtualizer(p.virtualizer)
                audioEngine.setHiRes(p.hiRes)
                audioEngine.setSpeed(p.speed)
                _playbackState.update {
                    it.copy(
                        isShuffle = p.isShuffle,
                        isRepeat = p.isRepeat,
                        activePreset = p.preset,
                        bassBoostEnabled = p.bassBoost,
                        virtualizerEnabled = p.virtualizer,
                        isHiResModeEnabled = p.hiRes,
                        playbackSpeed = p.speed
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "restore prefs failed", e)
            }
        }

        viewModelScope.launch(handler) {
            availablePlaylists.collect { list ->
                val current = _selectedPlaylist.value
                val matching = list.find { it.id == current.id }
                if (matching != null) {
                    _selectedPlaylist.value = matching
                } else if (list.isNotEmpty()) {
                    _selectedPlaylist.value = list.first()
                }
            }
        }

        // Connect audio engine callbacks via the shared contract.
        audioEngine.listener = object : AudioEngineListener {
            override fun onPosition(sec: Int, progress: Float) {
                _playbackState.update {
                    it.copy(
                        currentPositionSeconds = sec,
                        currentPositionProgress = progress
                    )
                }
            }

            override fun onPositionMs(ms: Long, sec: Int, progress: Float) {
                _playbackState.update {
                    it.copy(
                        currentPositionMs = ms,
                        currentPositionSeconds = sec,
                        currentPositionProgress = progress
                    )
                }
                // Keep the lock-screen widget's progress bar current.
                MusicPlaybackService.publishPosition(ms)
            }

            override fun onAmplitudes(amps: List<Float>) {
                _playbackState.update {
                    it.copy(amplitudes = amps)
                }
            }

            override fun onTrackCompleted() {
                onTrackFinished()
            }

            override fun onError(msg: String) {
                Log.w(TAG, "engine error: $msg")
                _errorMessage.value = msg
            }
        }
    }

    fun playTrack(track: Track) {
        val current = _playbackState.value.currentTrack
        if (current?.id == track.id && !_playbackState.value.isPlaying) {
            resumeWithFocus()
            return
        }

        // Production: never start audible playback without focus.
        focusHelper.requestFocus()

        _playbackState.update {
            it.copy(
                currentTrack = track,
                isPlaying = true,
                currentPositionSeconds = 0,
                currentPositionMs = 0L,
                currentPositionProgress = 0f
            )
        }
        try {
            audioEngine.playTrack(track, 0)
        } catch (e: Exception) {
            Log.e(TAG, "playTrack failed: ${track.id}", e)
            _errorMessage.value = "Couldn't play \"${track.title}\"."
            _playbackState.update { it.copy(isPlaying = false) }
            return
        }
        syncService(track, true)
        fetchLyricsForTrack(track)
        viewModelScope.launch(handler) {
            try {
                repository.recordTrackPlayed(track, 0, false)
                prefsRepo.setLastTrack(track.id)
            } catch (e: Exception) {
                Log.w(TAG, "record play failed", e)
            }
        }
    }

    fun togglePlayPause() {
        val current = _playbackState.value
        if (current.currentTrack == null) {
            val list = activePlaylistTracks.value
            if (list.isNotEmpty()) {
                playTrack(list.first())
            }
            return
        }

        if (current.isPlaying) {
            audioEngine.pause()
            focusHelper.abandonFocus()
            _playbackState.update { it.copy(isPlaying = false) }
            current.currentTrack?.let { syncService(it, false) }
        } else {
            resumeWithFocus()
        }
    }

    /** System-initiated pause (focus loss, headset unplug, notification). Never steals focus back. */
    fun pauseFromSystem(reason: String = "system") {
        if (!_playbackState.value.isPlaying) return
        Log.d(TAG, "pauseFromSystem: $reason")
        try {
            audioEngine.pause()
        } catch (e: Exception) {
            Log.w(TAG, "pause failed", e)
        }
        focusHelper.abandonFocus()
        _playbackState.update { it.copy(isPlaying = false) }
        _playbackState.value.currentTrack?.let { syncService(it, false) }
    }

    fun stopPlayback() {
        try {
            audioEngine.stop()
        } catch (e: Exception) {
            Log.w(TAG, "stop failed", e)
        }
        focusHelper.abandonFocus()
        _playbackState.update {
            it.copy(isPlaying = false, currentPositionProgress = 0f, currentPositionMs = 0L, currentPositionSeconds = 0)
        }
        try {
            MusicPlaybackService.stop(app)
        } catch (e: Exception) {
            Log.w(TAG, "stop service failed", e)
        }
    }

    private fun resume() {
        resumeWithFocus()
    }

    private fun resumeWithFocus() {
        focusHelper.requestFocus()
        try {
            audioEngine.resume()
        } catch (e: Exception) {
            Log.e(TAG, "resume failed", e)
            _errorMessage.value = "Couldn't resume playback."
            return
        }
        _playbackState.update { it.copy(isPlaying = true) }
        _playbackState.value.currentTrack?.let { syncService(it, true) }
    }

    private fun syncService(track: Track, isPlaying: Boolean) {
        val state = _playbackState.value
        val durationMs = track.durationSeconds * 1000L
        try {
            if (isPlaying) {
                MusicPlaybackService.start(
                    app, track.title, track.artist, true,
                    positionMs = state.currentPositionMs,
                    durationMs = durationMs,
                    artName = track.imageResName
                )
            } else {
                MusicPlaybackService.update(
                    app, track.title, track.artist, false,
                    positionMs = state.currentPositionMs,
                    durationMs = durationMs,
                    artName = track.imageResName
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "sync service failed", e)
        }
    }

    fun nextTrack() {
        val currentList = activePlaylistTracks.value
        if (currentList.isEmpty()) return
        val current = _playbackState.value.currentTrack

        // Shared queue math (unit-tested in :core); playback stays here.
        val nextIndex = com.vncnt.music.core.queue.QueueNavigator.nextIndex(
            currentList, current?.id, _playbackState.value.isShuffle
        ) ?: return
        playTrack(currentList[nextIndex])
    }

    fun previousTrack() {
        val currentList = activePlaylistTracks.value
        if (currentList.isEmpty()) return
        val current = _playbackState.value.currentTrack

        // If played more than 3 seconds, restart current track
        if (com.vncnt.music.core.queue.QueueNavigator.shouldRestartCurrent(
                _playbackState.value.currentPositionSeconds
            )
        ) {
            seekTo(0f)
            return
        }

        val prevIndex = com.vncnt.music.core.queue.QueueNavigator.previousIndex(
            currentList, current?.id, _playbackState.value.isShuffle
        ) ?: return
        playTrack(currentList[prevIndex])
    }

    fun seekTo(progress: Float) {
        val track = _playbackState.value.currentTrack ?: return
        val safe = progress.coerceIn(0f, 1f)
        val targetSec = (safe * track.durationSeconds).toInt()
        val targetMs = (safe * track.durationSeconds * 1000f).toLong()
        _playbackState.update {
            it.copy(
                currentPositionSeconds = targetSec,
                currentPositionMs = targetMs,
                currentPositionProgress = safe
            )
        }
        try {
            audioEngine.seekTo(targetSec)
        } catch (e: Exception) {
            Log.w(TAG, "seek failed", e)
        }
    }

    fun seekToMs(positionMs: Long) {
        val track = _playbackState.value.currentTrack ?: return
        val durMs = (track.durationSeconds * 1000f).coerceAtLeast(1000f)
        val progress = (positionMs.toFloat() / durMs).coerceIn(0f, 1f)
        _playbackState.update {
            it.copy(
                currentPositionMs = positionMs,
                currentPositionSeconds = (positionMs / 1000L).toInt(),
                currentPositionProgress = progress
            )
        }
        try {
            audioEngine.seekToMs(positionMs)
        } catch (e: Exception) {
            Log.w(TAG, "seekMs failed", e)
        }
    }

    fun toggleLyricsMode() {
        _isLyricsMode.value = !_isLyricsMode.value
        if (_isLyricsMode.value && _currentLyrics.value == null) {
            val track = _playbackState.value.currentTrack
            if (track != null) {
                fetchLyricsForTrack(track)
            }
        }
    }

    fun openImportLyricsDialog() {
        _showImportLyricsDialog.value = true
    }

    fun closeImportLyricsDialog() {
        _showImportLyricsDialog.value = false
    }

    fun fetchLyricsForTrack(track: Track, forceOnline: Boolean = false) {
        viewModelScope.launch(handler) {
            _isLyricsLoading.value = true
            try {
                val data = lyricsRepository.getOrFetchLyrics(track, forceRefreshOnline = forceOnline)
                _currentLyrics.value = data
            } catch (e: Exception) {
                Log.w(TAG, "lyrics fetch failed: ${track.id}", e)
            } finally {
                _isLyricsLoading.value = false
            }
        }
    }

    fun saveCustomLyrics(lrcContent: String) {
        val track = _playbackState.value.currentTrack ?: return
        if (lrcContent.isBlank()) {
            _errorMessage.value = "Lyrics are empty."
            return
        }
        viewModelScope.launch(handler) {
            try {
                val parsed = lyricsRepository.saveCustomLyrics(track.id, lrcContent)
                _currentLyrics.value = parsed
                closeImportLyricsDialog()
            } catch (e: Exception) {
                Log.w(TAG, "save lyrics failed", e)
                _errorMessage.value = "Couldn't save lyrics."
            }
        }
    }

    fun toggleShuffle() {
        val next = !_playbackState.value.isShuffle
        _playbackState.update { it.copy(isShuffle = next) }
        viewModelScope.launch(handler) { prefsRepo.setShuffle(next) }
    }

    fun toggleRepeat() {
        val next = !_playbackState.value.isRepeat
        _playbackState.update { it.copy(isRepeat = next) }
        viewModelScope.launch(handler) { prefsRepo.setRepeat(next) }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch(handler) {
            try {
                repository.toggleFavorite(track.id, track.isFavorite)
                if (_playbackState.value.currentTrack?.id == track.id) {
                    _playbackState.update {
                        it.copy(currentTrack = it.currentTrack?.copy(isFavorite = !track.isFavorite))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "favorite failed", e)
                _errorMessage.value = "Couldn't update favorites."
            }
        }
    }

    fun selectPlaylist(playlist: Playlist) {
        _selectedPlaylist.value = playlist
        _currentScreen.value = AppScreen.PLAYLIST
    }

    fun selectTab(screen: AppScreen) {
        if (screen != AppScreen.NOW_PLAYING) {
            previousTab = screen
        }
        _currentScreen.value = screen
    }

    fun openNowPlaying() {
        _currentScreen.value = AppScreen.NOW_PLAYING
    }

    fun closeNowPlaying() {
        _currentScreen.value = previousTab
    }

    fun setSoundPreset(preset: SoundPreset) {
        audioEngine.setSoundPreset(preset)
        _playbackState.update { it.copy(activePreset = preset) }
        viewModelScope.launch(handler) { prefsRepo.setPreset(preset) }
    }

    fun toggleBassBoost() {
        val next = !_playbackState.value.bassBoostEnabled
        audioEngine.setBassBoost(next)
        _playbackState.update { it.copy(bassBoostEnabled = next) }
        viewModelScope.launch(handler) { prefsRepo.setBass(next) }
    }

    fun toggleVirtualizer() {
        val next = !_playbackState.value.virtualizerEnabled
        audioEngine.setVirtualizer(next)
        _playbackState.update { it.copy(virtualizerEnabled = next) }
        viewModelScope.launch(handler) { prefsRepo.setVirtualizer(next) }
    }

    fun toggleHiResMode() {
        val next = !_playbackState.value.isHiResModeEnabled
        audioEngine.setHiRes(next)
        _playbackState.update { it.copy(isHiResModeEnabled = next) }
        viewModelScope.launch(handler) { prefsRepo.setHiRes(next) }
    }

    fun setSpeed(speed: Float) {
        val safe = speed.coerceIn(0.5f, 2.0f)
        audioEngine.setSpeed(safe)
        _playbackState.update { it.copy(playbackSpeed = safe) }
        viewModelScope.launch(handler) { prefsRepo.setSpeed(safe) }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query.take(120)
    }

    fun setShowEqualizer(show: Boolean) {
        _showEqualizer.value = show
    }

    fun openScanDialog() {
        _showScanDialog.value = true
    }

    fun dismissScanDialog() {
        _showScanDialog.value = false
    }

    fun startScanDeviceAudio() {
        viewModelScope.launch(handler) {
            _isScanning.value = true
            try {
                val result = repository.scanDeviceAudio()
                _scanResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "scan failed", e)
                _errorMessage.value = "Scan failed. Check storage permission."
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun addScannedTracksToLibrary() {
        viewModelScope.launch(handler) {
            try {
                val result = _scanResult.value
                if (result != null && result.tracksFound.isNotEmpty()) {
                    repository.addScannedTracks(result.tracksFound)
                }
            } catch (e: Exception) {
                Log.e(TAG, "add scanned failed", e)
                _errorMessage.value = "Couldn't add scanned tracks."
            } finally {
                _showScanDialog.value = false
            }
        }
    }

    fun importLocalAudio(title: String, artist: String, durationSec: Int, uri: String, filePath: String? = null) {
        viewModelScope.launch(handler) {
            try {
                repository.importLocalTrack(title, artist, durationSec, uri, filePath)
            } catch (e: Exception) {
                Log.e(TAG, "import failed", e)
                _errorMessage.value = "Couldn't import track."
            }
        }
    }

    fun importAudioFiles(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        if (uris.size > 50) {
            _errorMessage.value = "Pick up to 50 files at a time."
            return
        }
        viewModelScope.launch(handler) {
            try {
                repository.importLocalUris(uris)
            } catch (e: Exception) {
                Log.e(TAG, "import uris failed", e)
                _errorMessage.value = "Couldn't import files."
            } finally {
                _showScanDialog.value = false
            }
        }
    }

    private fun onTrackFinished() {
        val current = _playbackState.value.currentTrack
        if (current != null) {
            viewModelScope.launch(handler) {
                try {
                    repository.recordTrackPlayed(current, current.durationSeconds, true)
                } catch (e: Exception) {
                    Log.w(TAG, "record finished failed", e)
                }
            }
        }
        if (_playbackState.value.isRepeat) {
            current?.let { playTrack(it) }
        } else {
            nextTrack()
        }
    }

    override fun onCleared() {
        try {
            focusHelper.abandonFocus()
            audioEngine.release()
            // If nothing is playing, drop the foreground notification too.
            if (!_playbackState.value.isPlaying) {
                MusicPlaybackService.stop(app)
            }
        } catch (e: Exception) {
            Log.w(TAG, "onCleared cleanup failed", e)
        } finally {
            super.onCleared()
        }
    }

    companion object {
        private const val TAG = "MusicViewModel"
    }
}
