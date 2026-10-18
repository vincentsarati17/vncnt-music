import Foundation
import MediaPlayer
import VncntMusicCore

/// Observable playback state, driven by the shared engine on the main actor.
/// Swift only touches: closures, primitive args, and pre-built shared objects
/// — the two interop shapes already proven in shipped shell builds.
@MainActor
final class PlayerState: ObservableObject {
    // Typed as whatever the shared builder returns (proven list-interop type
    // from the shipped shell build) — never a Swift Array cast.
    @Published var playlists = PlaylistBuilder.shared.emptyLibraryPlaylists()
    @Published var currentTrack: Track? = nil
    @Published var isPlaying: Bool = false
    @Published var positionMs: Int64 = 0
    @Published var durationMs: Int64 = 1
    @Published var level: Float = 0.2
    @Published var errorMessage: String? = nil
    @Published var isReady: Bool = false
    @Published var speed: Float = 1.0

    let engine: DarwinAudioEngine = DarwinAudioEngine()
    let nowPlaying = NowPlayingPresenter()
    private var events: EngineEvents? = nil
    private var didBindRemoteCommands = false

    var progress: Double {
        guard durationMs > 0 else { return 0 }
        return Double(positionMs) / Double(durationMs)
    }

    func prepare() {
        let events = EngineEvents(engine: engine)
        events.onTickMs = { [weak self] ms, _ in self?.positionMs = ms.int64Value }
        events.onAmplitude = { [weak self] amp in self?.level = amp.floatValue }
        events.onCompleted = { [weak self] in
            self?.isPlaying = false
            self?.positionMs = 0
            self?.publishNowPlaying()
        }
        events.onFailure = { [weak self] message in
            self?.errorMessage = message
            self?.isPlaying = false
        }
        self.events = events

        do {
            let url = try ToneFile.url()
            let demo = DemoTracks.shared.tone(filePath: url.path)
            playlists = PlaylistBuilder.shared.buildPlaylists(
                tracks: DemoTracks.shared.library(filePath: url.path)
            )
            currentTrack = demo
            durationMs = Int64(demo.durationSeconds) * 1000
            isReady = true
            bindRemoteCommands()
            publishNowPlaying()
        } catch {
            errorMessage = "Could not prepare demo audio."
        }
    }

    func toggle() {
        guard let track = currentTrack else { return }
        if isPlaying {
            engine.pause()
            isPlaying = false
        } else {
            errorMessage = nil
            engine.playTrack(track: track, startPositionSeconds: Int32(positionMs / 1000))
            isPlaying = true
        }
        publishNowPlaying()
    }

    func seek(to progress: Double) {
        engine.seekToMs(positionMs: Int64(progress * Double(durationMs)))
        publishNowPlaying()
    }

    /// Lock-screen / Control Center transport. The Swift
    /// `addTarget(handler:)` API is documented and stable; each handler
    /// returns `.success` so the system accepts the command. Guarded because
    /// `prepare()` can re-run when the root view reappears and every
    /// registration would otherwise stack a duplicate handler.
    private func bindRemoteCommands() {
        guard !didBindRemoteCommands else { return }
        didBindRemoteCommands = true
        let handler: (MPRemoteCommandEvent) -> MPRemoteCommandHandlerStatus = { [weak self] _ in
            self?.toggle()
            return .success
        }
        _ = MPRemoteCommandCenter.shared().playCommand.addTarget(handler: handler)
        _ = MPRemoteCommandCenter.shared().pauseCommand.addTarget(handler: handler)
        _ = MPRemoteCommandCenter.shared().togglePlayPauseCommand.addTarget(handler: handler)
    }

    private func publishNowPlaying() {
        guard let track = currentTrack else {
            nowPlaying.clear()
            return
        }
        nowPlaying.update(
            title: track.title,
            artist: track.artist,
            durationMs: durationMs,
            positionMs: positionMs,
            isPlaying: isPlaying
        )
    }

    func setSpeed(_ value: Float) {
        speed = value
        engine.setSpeed(speed: value)
    }
}
