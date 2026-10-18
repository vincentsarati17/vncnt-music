package com.vncnt.music.data.model

// Compatibility shims: canonical types live in :core
// (com.vncnt.music.core.model) for future KMP commonMain reuse.
// These aliases keep every existing import working with zero behavior change.
// TODO(KMP): migrate call sites to com.vncnt.music.core.model, then delete this file.
typealias Track = com.vncnt.music.core.model.Track
typealias SoundPreset = com.vncnt.music.core.model.SoundPreset
typealias PlaybackState = com.vncnt.music.core.model.PlaybackState
