package com.example.data.lyrics

// Compatibility shim: parsing logic lives in :core and is unit-tested there.
// Existing call sites (LyricsRepository, tests) keep working unchanged.
// TODO(KMP): migrate call sites to com.vncnt.music.core.lyrics.LrcParser.
typealias LrcParser = com.vncnt.music.core.lyrics.LrcParser
