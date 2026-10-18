package com.vncnt.music.core.audio

/**
 * Bridges engine callbacks to Swift through plain function types. Protocol
 * conformances and primitive-only function types are the two interop shapes
 * this shell has already shipped green; arbitrary listener protocols are not.
 * Swift assigns `onTick`/`onAmplitude`/... closures; this class owns the
 * AudioEngineListener wiring.
 */
class EngineEvents(engine: AudioEngine) : AudioEngineListener {

    init {
        engine.listener = this
    }

    var onTick: ((Int, Float) -> Unit)? = null
    var onAmplitude: ((Float) -> Unit)? = null
    var onCompleted: (() -> Unit)? = null
    var onFailure: ((String) -> Unit)? = null
    var onTickMs: ((Long, Float) -> Unit)? = null

    override fun onPosition(positionSeconds: Int, progress: Float) {
        onTick?.invoke(positionSeconds, progress)
    }

    override fun onPositionMs(positionMs: Long, positionSeconds: Int, progress: Float) {
        // Swift binds onTickMs for ms-resolution display; onTick stays for
        // seconds-resolution consumers.
        onTick?.invoke(positionSeconds, progress)
        onTickMs?.invoke(positionMs, progress)
    }

    override fun onAmplitudes(amplitudes: List<Float>) {
        amplitudes.forEach { onAmplitude?.invoke(it) }
    }

    override fun onTrackCompleted() {
        onCompleted?.invoke()
    }

    override fun onError(message: String) {
        onFailure?.invoke(message)
    }
}
