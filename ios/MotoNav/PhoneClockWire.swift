import Foundation

/// UTC epoch seconds and explicit phone offset, independent of language/locale.
/// Mirrors android PhoneClockWire so the round display's home clock can sync (#TIME).
enum PhoneClockWire {
    static func encode(wallMillis: Int64, offsetSeconds: Int) -> String? {
        let seconds = wallMillis / 1000
        guard wallMillis >= 1_577_836_800_000, seconds < 4_102_444_800,
              offsetSeconds >= -50_400, offsetSeconds <= 50_400 else { return nil }
        return "#TIME,\(seconds),\(offsetSeconds)"
    }
}
