package com.vncnt.music.core.audio

import com.vncnt.music.core.model.SoundPreset

/**
 * Enum-safe facade for the Swift shell: Kotlin-side code names SoundPreset
 * entries so Swift never has to spell Kotlin/Native-exported enum member
 * names (a binding shape this shell has not proven). Swift passes enum
 * values through opaquely between calls.
 */
object ShellPresets {

    fun initial(): SoundPreset = SoundPreset.STUDIO_FLAT

    fun label(preset: SoundPreset): String = preset.label

    fun next(current: SoundPreset): SoundPreset {
        val all = SoundPreset.entries
        val index = all.indexOf(current)
        return all[(index + 1) % all.size]
    }
}
