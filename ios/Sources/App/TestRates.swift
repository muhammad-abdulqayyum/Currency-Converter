// Deterministic rates are accessible only to Debug UI tests; excluded from Release.
#if DEBUG
import Foundation
final class TestSnapshotStore: SnapshotStore {
    private var values: [String: Snapshot] = [:]
    func read(_ code: String) -> Snapshot? { values[code] }
    func write(_ snapshot: Snapshot, code: String) { values[code] = snapshot }
}
enum TestRates {
    static func download(_ asset: Asset) async throws -> Data {
        let rates = ["GBP": "0.8", "BRL": "5", "PHP": "60", "XAU": "0.00025", "XAG": "0.025", "BTC": "0.00001"]
        guard let rate = rates[asset.code] else { throw ConverterError.unavailable }
        let f = DateFormatter(); f.timeZone = TimeZone(secondsFromGMT: 0); f.locale = Conversion.posix; f.dateFormat = "yyyy-MM-dd"
        let date = f.string(from: Date().addingTimeInterval(asset.code == "BTC" ? -86400 : 0))
        return Data("{\"base\":\"USD\",\"quote\":\"\(asset.code)\",\"date\":\"\(date)\",\"rate\":\(rate),\"providers\":[{\"key\":\"\(asset.code == "BTC" ? "ESTV" : "ECB")\"}]}".utf8)
    }
}
#endif
