import Foundation

/// Generates a deterministic demo tone (stereo 16-bit PCM chord bed) on first
/// use so the shell exercises the real AVPlayer file path with zero bundled
/// assets and zero network.
enum ToneFile {
    static let durationSeconds = 24
    static let sampleRate = 22050

    static func url() throws -> URL {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("vncnt-demo-tone.wav")
        if !FileManager.default.fileExists(atPath: url.path) {
            try writeTone(to: url)
        }
        return url
    }

    private static func writeTone(to url: URL) throws {
        let channels = 2
        let bitsPerSample = 16
        let bytesPerSample = bitsPerSample / 8
        let frames = sampleRate * durationSeconds
        let dataBytes = frames * channels * bytesPerSample
        let byteRate = sampleRate * channels * bytesPerSample
        let blockAlign = channels * bytesPerSample

        var data = Data()
        data.reserveCapacity(44 + dataBytes)
        func appendU32(_ v: UInt32) {
            var x = v.littleEndian
            data.append(contentsOf: withUnsafeBytes(of: &x) { Array($0) })
        }
        func appendU16(_ v: UInt16) {
            var x = v.littleEndian
            data.append(contentsOf: withUnsafeBytes(of: &x) { Array($0) })
        }
        func appendString(_ s: String) { data.append(contentsOf: s.utf8) }

        appendString("RIFF"); appendU32(UInt32(36 + dataBytes)); appendString("WAVE")
        appendString("fmt "); appendU32(16); appendU16(1)
        appendU16(UInt16(channels)); appendU32(UInt32(sampleRate)); appendU32(UInt32(byteRate))
        appendU16(UInt16(blockAlign)); appendU16(UInt16(bitsPerSample))
        appendString("data"); appendU32(UInt32(dataBytes))

        // A-minor-ish bed: root+third+fifth, soft bass, gentle shimmer.
        let freqs = [220.0, 261.63, 329.63]
        let bass = 55.0
        for i in 0..<frames {
            let t = Double(i) / Double(sampleRate)
            var s = 0.0
            for f in freqs { s += sin(2.0 * .pi * f * t) / Double(freqs.count) }
            s += 0.5 * sin(2.0 * .pi * bass * t)
            s += 0.15 * sin(2.0 * .pi * 880.0 * t) * (0.5 + 0.5 * sin(2.0 * .pi * 0.5 * t))
            let clamped = max(-1.0, min(1.0, s * 0.45))
            let sample = Int16(clamped * Double(Int16.max))
            appendU16(UInt16(bitPattern: sample))
            appendU16(UInt16(bitPattern: sample))
        }
        try data.write(to: url)
    }
}
