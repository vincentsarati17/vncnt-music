import Foundation
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
    private var events: EngineEvents? = nil

    var progress: Double {
        guard durationMs > 0 else { return 0 }
        return Double(positionMs) / Double(durationMs)
    }

    func prepare() {
        let events = EngineEvents(engine: engine)
        events.onTickMs = { [weak self] ms, _ in self?.positionMs = ms }
        events.onAmplitude = { [weak self] amp in self?.level = amp }
        events.onCompleted = { [weak self] in
            self?.isPlaying = false
            self?.positionMs = 0
        }
        events.onFailure = { [weak self] message in
            self?.errorMessage = message
            self?.isPlaying = false
        }
        self.events = events

        do {
            let url = try ToneFile.url()
            let demo = DemoTracks.tone(filePath: url.path)
            playlists = PlaylistBuilder.shared.buildPlaylists(tracks: [demo])
            currentTrack = demo
            durationMs = Int64(Int32(DemoTracks.DEMO_DURATION_SECONDS)) * 1000
            isReady = true
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
    }

    func seek(to progress: Double) {
        engine.seekToMs(positionMs: Int64(progress * Double(durationMs)))
    }

    func setSpeed(_ value: Float) {
        speed = value
        engine.setSpeed(speed: value)
    }
}
