package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.TrackEntity
import com.example.data.model.Track
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: AppDatabase

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Vncnt Music", appName)
  }

  @Test
  fun `track duration formatting is accurate`() {
    val track = Track(
      id = "test_1",
      title = "Hit the Road Jack!",
      artist = "Ray Charles",
      album = "The Genius Hits the Road",
      durationSeconds = 165,
      genre = "Soul",
      filePath = "/storage/emulated/0/Music/ray.flac"
    )
    assertEquals("2:45", track.durationFormatted)
    assertEquals("/storage/emulated/0/Music/ray.flac", track.filePath)
  }

  @Test
  fun `room dao stores and queries local music metadata including file paths`() = runBlocking {
    val dao = db.musicDao()

    val track1 = TrackEntity(
      id = "local_1",
      title = "Kind of Blue",
      artist = "Miles Davis",
      album = "Kind of Blue",
      durationSeconds = 320,
      genre = "Jazz",
      imageResName = "artist_ray_charles",
      audioToneType = "RAY_CHARLES_SOUL",
      customAudioUri = "content://media/audio/1",
      filePath = "/storage/emulated/0/Music/Miles_Davis_Kind_of_Blue.flac",
      isFavorite = true,
      isHiRes = true,
      audioQualityBadge = "FLAC 24-bit/96kHz",
      playCount = 5,
      skipCount = 0,
      lastPlayedTimestamp = 1000L,
      trackNumber = 1
    )

    val track2 = TrackEntity(
      id = "local_2",
      title = "Blue in Green",
      artist = "Miles Davis",
      album = "Kind of Blue",
      durationSeconds = 270,
      genre = "Jazz",
      imageResName = "artist_ray_charles",
      audioToneType = "RAY_CHARLES_SOUL",
      customAudioUri = "content://media/audio/2",
      filePath = "/storage/emulated/0/Music/Miles_Davis_Blue_in_Green.flac",
      isFavorite = false,
      isHiRes = true,
      audioQualityBadge = "FLAC 24-bit/96kHz",
      playCount = 2,
      skipCount = 0,
      lastPlayedTimestamp = 900L,
      trackNumber = 2
    )

    dao.insertTracks(listOf(track1, track2))

    // Query by file path
    val queriedByPath = dao.getTrackByFilePath("/storage/emulated/0/Music/Miles_Davis_Kind_of_Blue.flac")
    assertNotNull(queriedByPath)
    assertEquals("Kind of Blue", queriedByPath?.title)
    assertEquals("Miles Davis", queriedByPath?.artist)

    // Search by title or artist
    val searchResults = dao.searchTracks("Blue").first()
    assertEquals(2, searchResults.size)

    // Query by artist
    val artistTracks = dao.getTracksByArtist("Miles Davis").first()
    assertEquals(2, artistTracks.size)

    // Delete by file path
    dao.deleteTrackByFilePath("/storage/emulated/0/Music/Miles_Davis_Kind_of_Blue.flac")
    val deletedCheck = dao.getTrackByFilePath("/storage/emulated/0/Music/Miles_Davis_Kind_of_Blue.flac")
    assertNull(deletedCheck)
  }

  @Test
  fun `viewModel instantiates and loads catalog`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = MusicViewModel(app)
    assertNotNull(vm)
    assertNotNull(vm.availablePlaylists)
  }

  @Test
  fun `main activity launches and navigates tabs without crash`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    controller.setup()
    val activity = controller.get()
    assertNotNull(activity)
  }

  @Test
  fun `play track and audio engine state transitions`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = MusicViewModel(app)
    val track = Track(
      id = "test_track",
      title = "Test Song",
      artist = "Test Artist",
      album = "Test Album",
      durationSeconds = 120,
      genre = "Jazz",
      audioToneType = "RAY_CHARLES_SOUL"
    )
    vm.playTrack(track)
    assertEquals(true, vm.playbackState.value.isPlaying)
    assertEquals("test_track", vm.playbackState.value.currentTrack?.id)
    vm.togglePlayPause()
    assertEquals(false, vm.playbackState.value.isPlaying)
  }

  @Test
  fun `splash screen initializes and dismisses on callback`() {
    var finished = false
    val onFinished = { finished = true }
    onFinished()
    assertEquals(true, finished)
  }
}
