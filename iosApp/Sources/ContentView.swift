import SwiftUI
import VncntMusicCore

// Shell v1: proves SwiftUI ↔ shared Kotlin interop end to end using only
// zero-argument shared APIs (no model constructors needed yet).
struct ContentView: View {
    private let playlists = PlaylistBuilder.shared.emptyLibraryPlaylists()

    var body: some View {
        NavigationStack {
            List(playlists, id: \.id) { playlist in
                VStack(alignment: .leading) {
                    Text(playlist.title).font(.headline)
                    Text(playlist.subtitle).font(.subheadline).foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Vncnt Music")
        }
    }
}
