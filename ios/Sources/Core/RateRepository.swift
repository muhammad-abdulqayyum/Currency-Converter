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
        return "\(code) · \(date)" + (age > 3 ? " · older reference (\(age) days)" : "") + "\n" +
            (offline ? "Saved rate · connection unavailable" : saved ? "Saved rate · reused locally" : "Reference rate retrieved") + "\n" + source
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
        guard (0...7).contains(age) else { throw ConverterError.date }; return age
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
    private let directory: URL
    public init() {
        directory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0].appendingPathComponent("ReferenceRates", isDirectory: true)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
    }
    public func read(_ code: String) -> Snapshot? {
        guard let data = try? Data(contentsOf: directory.appendingPathComponent(code + ".json")), data.count < 100000 else { return nil }
        return try? JSONDecoder().decode(Snapshot.self, from: data)
    }
    public func write(_ snapshot: Snapshot, code: String) {
        if let data = try? JSONEncoder().encode(snapshot) { try? data.write(to: directory.appendingPathComponent(code + ".json"), options: .atomic) }
    }
}
private final class NoRedirects: NSObject, URLSessionTaskDelegate {
    func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse,
                    newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) { completionHandler(nil) }
}
public actor RateRepository {
    public typealias Download = (Asset) async throws -> Data
    private let store: SnapshotStore, download: Download
    public init(store: SnapshotStore = FileSnapshotStore(), download: @escaping Download = RateRepository.download) {
        self.store = store; self.download = download
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
            let data = try await download(asset)
            let quote = try RatePolicy.quote(data, for: asset, now: now)
            store.write(Snapshot(data: data, fetchedAt: now), code: asset.code); return quote
        } catch {
            if let s = stored, let quote = try? RatePolicy.quote(s.data, for: asset, now: now, saved: true, offline: true) { return quote }
            throw ConverterError.unavailable
        }
    }
    public static func download(_ asset: Asset) async throws -> Data {
        guard asset.code.range(of: "^[A-Z]{3}$", options: .regularExpression) != nil else { throw ConverterError.pair }
        let endpoint = "https://api.frankfurter.dev/v2/rate/USD/\(asset.code)?expand=providers" + (asset.category == .crypto ? "&providers=ESTV" : "")
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
        return data
    }
}
