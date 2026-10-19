import SwiftUI
import VncntMusicCore

// Shell player UI: shared playlists on top, transport bound to the shared
// AVPlayer engine below. Demo audio is synthesized locally (see ToneFile).
struct ContentView: View {
    @StateObject private var player = PlayerState()
    private let speeds: [Float] = [0.75, 1.0, 1.25, 1.5]

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if !player.isReady {
                    Spacer()
                    ProgressView("Preparing demo audio…")
                    Spacer()
                } else {
                    List(player.playlists, id: \.id) { playlist in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(playlist.title).font(.headline)
                            Text(playlist.subtitle).font(.subheadline).foregroundStyle(.secondary)
                        }
                    }
                    .listStyle(.plain)
                    .frame(maxHeight: 240)

                    if player.usingDemoLibrary {
                        Button("Use my music library") { player.requestDeviceLibrary() }
                            .buttonStyle(.bordered)
                            .font(.footnote)
                            .frame(maxWidth: .infinity)
                    }

                    Divider()

                    VStack(spacing: 12) {
                        Text(player.currentTrack?.title ?? "No track")
                            .font(.title3).bold()
                        Text(player.currentTrack?.artist ?? "")
                            .font(.subheadline).foregroundStyle(.secondary)
                        LevelStrip(level: player.level)
                        Slider(
                            value: Binding(
                                get: { player.progress },
                                set: { player.seek(to: $0) }
                            ),
                            in: 0...1
                        )
                        HStack {
                            Text(fmt(player.positionMs))
                                .font(.caption).monospacedDigit()
                            Spacer()
                            Text("-\(fmt(max(Int64(0), player.durationMs - player.positionMs)))")
                                .font(.caption).foregroundStyle(.secondary).monospacedDigit()
                        }
                        HStack {
                            Spacer()
                            Button(action: { player.toggle() }) {
                                Image(systemName: player.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                                    .font(.system(size: 60))
                            }
                            .buttonStyle(.plain)
                            Spacer()
                        }
                        HStack(spacing: 8) {
                            ForEach(speeds, id: \.self) { s in
                                Button("\(s)x") { player.setSpeed(s) }
                                    .buttonStyle(.bordered)
                                    .foregroundStyle(player.speed == s ? Color.red : Color.primary)
                            }
                        }
                        HStack(spacing: 8) {
                            Button(player.presetLabel) { player.cyclePreset() }
                                .buttonStyle(.bordered)
                            Button("Bass") { player.toggleBass() }
                                .buttonStyle(.bordered)
                                .foregroundStyle(player.bassBoostOn ? Color.red : Color.primary)
                            Button("Wide") { player.toggleWide() }
                                .buttonStyle(.bordered)
                                .foregroundStyle(player.wideOn ? Color.red : Color.primary)
                        }
                        if let error = player.errorMessage {
                            Text(error).font(.footnote).foregroundStyle(.red)
                        }
                    }
                    .padding()
                }
            }
            .navigationTitle("Vncnt Music")
        }
        .onAppear {
            if !player.isReady {
                player.prepare()
            }
        }
    }

    private func fmt(_ ms: Int64) -> String {
        let total = ms / 1000
        return "\(total / 60):\(String(format: "%02d", total % 60))"
    }
}

private struct LevelStrip: View {
    let level: Float

    var body: some View {
        HStack(spacing: 3) {
            ForEach(0..<32, id: \.self) { i in
                let phase = sin(Double(i) * 0.55 + Double(level) * 3.0)
                let height = 8 + Double(level) * (18 + 14 * phase)
                RoundedRectangle(cornerRadius: 2)
                    .fill(.secondary)
                    .frame(width: 6, height: CGFloat(max(8, height)))
            }
        }
        .frame(height: 52)
    }
}
