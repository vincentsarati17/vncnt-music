package com.vncnt.music.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vncnt.music.core.model.SoundPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

/** Persisted DSP + queue prefs so relaunch restores the user's setup. */
data class UserPrefs(
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val preset: SoundPreset = SoundPreset.STUDIO_FLAT,
    val bassBoost: Boolean = true,
    val virtualizer: Boolean = true,
    val hiRes: Boolean = true,
    val speed: Float = 1.0f,
    val lastTrackId: String? = null
)

class UserPreferencesRepository(private val context: Context) {

    private val store = context.applicationContext.userPrefsDataStore

    val prefs: Flow<UserPrefs> = store.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { p ->
            UserPrefs(
                isShuffle = p[KEY_SHUFFLE] == true,
                isRepeat = p[KEY_REPEAT] == true,
                preset = runCatching { SoundPreset.valueOf(p[KEY_PRESET] ?: "") }
                    .getOrDefault(SoundPreset.STUDIO_FLAT),
                bassBoost = p[KEY_BASS] ?: true,
                virtualizer = p[KEY_VIRT] ?: true,
                hiRes = p[KEY_HIRES] ?: true,
                speed = p[KEY_SPEED] ?: 1.0f,
                lastTrackId = p[KEY_LAST_TRACK]
            )
        }

    suspend fun setShuffle(v: Boolean) = store.edit { it[KEY_SHUFFLE] = v }
    suspend fun setRepeat(v: Boolean) = store.edit { it[KEY_REPEAT] = v }
    suspend fun setPreset(v: SoundPreset) = store.edit { it[KEY_PRESET] = v.name }
    suspend fun setBass(v: Boolean) = store.edit { it[KEY_BASS] = v }
    suspend fun setVirtualizer(v: Boolean) = store.edit { it[KEY_VIRT] = v }
    suspend fun setHiRes(v: Boolean) = store.edit { it[KEY_HIRES] = v }
    suspend fun setSpeed(v: Float) = store.edit { it[KEY_SPEED] = v }
    suspend fun setLastTrack(id: String?) = store.edit {
        if (id == null) it.remove(KEY_LAST_TRACK) else it[KEY_LAST_TRACK] = id
    }

    companion object {
        private val KEY_SHUFFLE = booleanPreferencesKey("shuffle")
        private val KEY_REPEAT = booleanPreferencesKey("repeat")
        private val KEY_PRESET = stringPreferencesKey("preset")
        private val KEY_BASS = booleanPreferencesKey("bass")
        private val KEY_VIRT = booleanPreferencesKey("virtualizer")
        private val KEY_HIRES = booleanPreferencesKey("hires")
        private val KEY_SPEED = floatPreferencesKey("speed")
        private val KEY_LAST_TRACK = stringPreferencesKey("last_track_id")
    }
}
