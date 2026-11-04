import Foundation

public struct RatePayload: Codable {
    public struct Provider: Codable { public let key: String; public let excluded: Bool? }
    public let base: String, quote: String, date: String
    public let rate: Decimal
    public let providers: [Provider]?
    enum CodingKeys: String, CodingKey { case base, quote, date, rate, providers }
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        base = try c.decode(String.self, forKey: .base); quote = try c.decode(String.self, forKey: .quote)
        date = try c.decode(String.self, forKey: .date); providers = try c.decodeIfPresent([Provider].self, forKey: .providers)
        if let text = try? c.decode(String.self, forKey: .rate) {
            guard let d = Decimal(string: text, locale: Conversion.posix) else { throw ConverterError.rate }; rate = d
        } else { rate = try c.decode(Decimal.self, forKey: .rate) }
    }
}
public struct Quote {
    public let rate: Decimal, date: String, source: String
    public let saved: Bool, offline: Bool
    public let age: Int
    public func details(code: String) -> String {
        if code == "USD" { return "USD · common reference base" }
        return "\(code) · \(date)" + (age > 7 ? " · Older rate. The current rate may differ." : "") + "\n" +
            (offline ? "Saved rate · refresh unavailable" : saved ? "Saved rate · reused locally" : "Reference rate retrieved") + "\n" + source
    }
}
public enum RatePolicy {
    public static let ttl: TimeInterval = 6 * 3600
    public static func age(_ date: String, now: Date) throws -> Int {
        let f = DateFormatter(); f.locale = Conversion.posix; f.timeZone = TimeZone(secondsFromGMT: 0)
        f.dateFormat = "yyyy-MM-dd"; f.isLenient = false
        guard date.range(of: "^[0-9]{4}-[0-9]{2}-[0-9]{2}$", options: .regularExpression) != nil,
              let d = f.date(from: date), f.string(from: d) == date else { throw ConverterError.date }
        let age = Int(floor(now.timeIntervalSince1970 / 86400) - floor(d.timeIntervalSince1970 / 86400))
        guard age >= 0 else { throw ConverterError.date }; return age
    }
    public static func quote(_ data: Data, for asset: Asset, now: Date, saved: Bool = false, offline: Bool = false) throws -> Quote {
        guard data.count <= 65536 else { throw ConverterError.rate }
        let p = try JSONDecoder().decode(RatePayload.self, from: data)
        guard p.base == "USD", p.quote == asset.code else { throw ConverterError.pair }
        try Conversion.validateRate(p.rate)
        let age = try self.age(p.date, now: now)
        let providers = (p.providers ?? []).filter { $0.excluded != true && $0.key.range(of: "^[A-Z0-9_]{1,20}$", options: .regularExpression) != nil }.map(\.key)
        if asset.category == .crypto && !providers.contains("ESTV") { throw ConverterError.provenance }
        let source = asset.category == .crypto ? "Swiss Federal Tax Administration (ESTV) via Frankfurter" :
            "Frankfurter · official-source reference rates" + (providers.isEmpty ? "" : "\nProviders: " + providers.prefix(3).joined(separator: ", ") + (providers.count > 3 ? " + \(providers.count - 3) more" : ""))
        return Quote(rate: p.rate, date: p.date, source: source, saved: saved, offline: offline, age: age)
    }
}
public struct Snapshot: Codable {
    public let data: Data, fetchedAt: Date
    public init(data: Data, fetchedAt: Date) { self.data = data; self.fetchedAt = fetchedAt }
}
public protocol SnapshotStore {
    func read(_ code: String) -> Snapshot?
    func write(_ snapshot: Snapshot, code: String)
}
public final class FileSnapshotStore: SnapshotStore {
    private let directory: URL, legacyDirectory: URL
    public init() {
        let manager = FileManager.default
        directory = manager.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0].appendingPathComponent("ReferenceRates", isDirectory: true)
        legacyDirectory = manager.urls(for: .cachesDirectory, in: .userDomainMask)[0].appendingPathComponent("ReferenceRates", isDirectory: true)
    }
    public func read(_ code: String) -> Snapshot? {
        let name = code + ".json"
        for location in [directory, legacyDirectory] {
            guard let data = try? Data(contentsOf: location.appendingPathComponent(name)), data.count < 100000,
                  let snapshot = try? JSONDecoder().decode(Snapshot.self, from: data) else { continue }
            if location == legacyDirectory { write(snapshot, code: code) }
            return snapshot
        }
        return nil
    }
    public func write(_ snapshot: Snapshot, code: String) {
        do {
            var folder = directory
            try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
            var values = URLResourceValues()
            values.isExcludedFromBackup = true
            try folder.setResourceValues(values)
            try JSONEncoder().encode(snapshot).write(to: folder.appendingPathComponent(code + ".json"), options: .atomic)
        } catch { /* A storage failure should not prevent the current conversion. */ }
    }
}
private final class NoRedirects: NSObject, URLSessionTaskDelegate {
    func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse,
                    newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) { completionHandler(nil) }
}
public actor RateRepository {
    public typealias Download = (Asset) async throws -> Data
    public typealias History = (Asset, Date) async throws -> Data
    private let store: SnapshotStore
    private let latest: (Asset) async throws -> (Data, Date?)
    private let history: History
    public init(store: SnapshotStore = FileSnapshotStore(), download: Download? = nil, history: History? = nil) {
        self.store = store
        if let download {
            latest = { (try await download($0), nil) }
            self.history = history ?? { _, _ in throw ConverterError.unavailable }
        } else {
            latest = { try await RateRepository.request($0) }
            self.history = history ?? { asset, now in
                let formatter = DateFormatter()
                formatter.locale = Conversion.posix
                formatter.timeZone = TimeZone(secondsFromGMT: 0)
                formatter.dateFormat = "yyyy-MM-dd"
                let from = formatter.string(from: now.addingTimeInterval(-7 * 86400))
                let to = formatter.string(from: now)
                return try await RateRepository.request(asset, range: "&from=\(from)&to=\(to)").0
            }
        }
    }
    public func get(_ asset: Asset, now: Date = Date()) async throws -> Quote {
        if asset.isSubunit {
            let parent = Asset(asset.rateCode, asset.rateCode, asset.symbol, .crypto)
            let quote = try await get(parent, now: now)
            return Quote(rate: quote.rate * asset.unitsPerCoin, date: quote.date,
                         source: quote.source + "\nDerived from " + asset.rateCode + " denomination",
                         saved: quote.saved, offline: quote.offline, age: quote.age)
        }
        if asset.code == "USD" { return Quote(rate: 1, date: "base", source: "USD reference base", saved: false, offline: false, age: 0) }
        let stored = store.read(asset.code)
        if let s = stored, now >= s.fetchedAt, now.timeIntervalSince(s.fetchedAt) < RatePolicy.ttl,
           let q = try? RatePolicy.quote(s.data, for: asset, now: now, saved: true) { return q }
        do {
            let (latestData, serverDate) = try await latest(asset)
            let referenceTime = serverDate ?? now
            let data: Data
            let quote: Quote
            if let valid = try? RatePolicy.quote(latestData, for: asset, now: referenceTime) {
                (data, quote) = (latestData, valid)
            } else {
                let response = try await history(asset, referenceTime)
                let entries = try JSONSerialization.jsonObject(with: response) as? [[String: Any]] ?? []
                let newest = entries.compactMap { entry -> (Data, Quote)? in
                    guard let data = try? JSONSerialization.data(withJSONObject: entry),
                          let quote = try? RatePolicy.quote(data, for: asset, now: referenceTime) else { return nil }
                    return (data, quote)
                }.max { $0.1.date < $1.1.date }
                guard let newest else { throw ConverterError.unavailable }
                (data, quote) = newest
            }
            if let current = store.read(asset.code),
               let saved = try? RatePolicy.quote(current.data, for: asset, now: referenceTime, saved: true),
               saved.date > quote.date { return saved }
            store.write(Snapshot(data: data, fetchedAt: referenceTime), code: asset.code)
            return quote
        } catch {
            if let s = store.read(asset.code), let quote = try? RatePolicy.quote(s.data, for: asset, now: max(now, s.fetchedAt), saved: true, offline: true) { return quote }
            throw ConverterError.unavailable
        }
    }
    private static func request(_ asset: Asset, range: String = "") async throws -> (Data, Date?) {
        guard asset.code.range(of: "^[A-Z]{3}$", options: .regularExpression) != nil else { throw ConverterError.pair }
        let path = range.isEmpty ? "/v2/rate/USD/\(asset.code)?expand=providers"
            : "/v2/rates?base=USD&quotes=\(asset.code)&expand=providers" + range
        let endpoint = "https://api.frankfurter.dev" + path + (asset.category == .crypto ? "&providers=ESTV" : "")
        let config = URLSessionConfiguration.ephemeral; config.timeoutIntervalForRequest = 10; config.timeoutIntervalForResource = 20; config.urlCache = nil
        let session = URLSession(configuration: config, delegate: NoRedirects(), delegateQueue: nil)
        defer { session.invalidateAndCancel() }
        var request = URLRequest(url: URL(string: endpoint)!); request.setValue("application/json", forHTTPHeaderField: "Accept")
        let (bytes, response) = try await session.bytes(for: request)
        guard let response = response as? HTTPURLResponse, response.statusCode == 200,
              response.expectedContentLength <= 65536 else { throw ConverterError.unavailable }
        var data = Data()
        for try await byte in bytes {
            guard data.count < 65536 else { throw ConverterError.rate }; data.append(byte)
        }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "EEE, dd MMM yyyy HH:mm:ss 'GMT'"
        let serverDate = response.value(forHTTPHeaderField: "Date").flatMap { formatter.date(from: $0) }
        return (data, serverDate)
    }
}
