import XCTest
@testable import ConverterCore

final class MemoryStore: SnapshotStore {
    var values: [String: Snapshot] = [:]
    func read(_ code: String) -> Snapshot? { values[code] }
    func write(_ snapshot: Snapshot, code: String) { values[code] = snapshot }
}
final class ConverterCoreTests: XCTestCase {
    let gold = Asset("XAU", "Gold", "Au", .metals)
    let btc = Asset("BTC", "Bitcoin", "₿", .crypto)
    let gbp = Asset("GBP", "British Pound", "🇬🇧", .world)
    let now = Date(timeIntervalSince1970: 1793232000) // 2026-10-29 UTC
    func payload(code: String = "GBP", rate: String = "0.8", date: String = "2026-10-29", provider: String = "ECB", excluded: Bool = false) -> Data {
        Data("{\"base\":\"USD\",\"quote\":\"\(code)\",\"rate\":\(rate),\"date\":\"\(date)\",\"providers\":[{\"key\":\"\(provider)\",\"excluded\":\(excluded)}]}".utf8)
    }
    func testAmountBoundaries() throws {
        XCTAssertEqual(try Conversion.amount("1000000000000"), 1_000_000_000_000)
        XCTAssertEqual(try Conversion.amount("0.00000001"), Decimal(string: "0.00000001"))
        XCTAssertEqual(try Conversion.amount("0"), 0)
        for s in ["", "-1", "1e3", "1,000", "NaN", "1.000000001", "1000000000001", ".5"] { XCTAssertThrowsError(try Conversion.amount(s), s) }
    }
    func testCrossRatesAndPrecision() throws {
        XCTAssertEqual(try Conversion.cross(100, source: Decimal(string: "0.00025")!, target: 1, digits: 2), 400000)
        XCTAssertEqual(try Conversion.cross(100, source: 1, target: Decimal(string: "0.00025")!, digits: 8), Decimal(string: "0.025"))
        XCTAssertEqual(try Conversion.cross(100, source: Decimal(string: "0.00001")!, target: Decimal(string: "0.00025")!, digits: 8), 2500)
        XCTAssertEqual(try Conversion.cross(100, source: 5, target: 60, digits: 2), 1200)
        XCTAssertEqual(try Conversion.cross(1, source: 3, target: 1, digits: 8), Decimal(string: "0.33333333"))
        XCTAssertEqual(try Conversion.cross(1, source: 1, target: Decimal(string: "1.005")!, digits: 2), Decimal(string: "1.01"))
        XCTAssertThrowsError(try Conversion.cross(1, source: 0, target: 1, digits: 2))
        XCTAssertThrowsError(try Conversion.cross(-1, source: 1, target: 1, digits: 2))
    }
    func testUnitPrecisionAndTinyDisplay() {
        XCTAssertEqual(Conversion.digits(gold), 8); XCTAssertEqual(Conversion.digits(btc), 8)
        XCTAssertEqual(Conversion.digits(Asset("JPY", "Yen", "", .world)), 0)
        XCTAssertEqual(Conversion.digits(Asset("KWD", "Kuwaiti Dinar", "", .world)), 3)
        XCTAssertEqual(Conversion.digits(Asset("XRP", "XRP", "", .crypto)), 6)
        XCTAssertTrue(Conversion.display(0, asset: gold, positiveInput: true).hasPrefix("< "))
    }
    func testDatePolicy() throws {
        XCTAssertEqual(try RatePolicy.age("2026-10-29", now: now), 0)
        XCTAssertEqual(try RatePolicy.age("2026-10-22", now: now), 7)
        for s in ["2026-10-21", "2026-10-30", "2026-02-30", "2026-2-01", "garbage"] { XCTAssertThrowsError(try RatePolicy.age(s, now: now), s) }
    }
    func testResponseValidation() throws {
        XCTAssertEqual(try RatePolicy.quote(payload(), for: gbp, now: now).rate, Decimal(string: "0.8"))
        XCTAssertThrowsError(try RatePolicy.quote(payload(code: "JPY"), for: gbp, now: now))
        for rate in ["0", "-1", "1e31", "1e-31"] { XCTAssertThrowsError(try RatePolicy.quote(payload(rate: rate), for: gbp, now: now)) }
        XCTAssertThrowsError(try RatePolicy.quote(Data(repeating: 32, count: 65537), for: gbp, now: now))
    }
    func testCryptoRequiresIncludedSwissSource() throws {
        XCTAssertThrowsError(try RatePolicy.quote(payload(code: "BTC"), for: btc, now: now))
        XCTAssertThrowsError(try RatePolicy.quote(payload(code: "BTC", provider: "ESTV", excluded: true), for: btc, now: now))
        let q = try RatePolicy.quote(payload(code: "BTC", provider: "ESTV"), for: btc, now: now)
        XCTAssertTrue(q.source.contains("Swiss"))
    }
    func testFreshCacheAvoidsNetwork() async throws {
        let store = MemoryStore(); store.values["GBP"] = Snapshot(data: payload(), fetchedAt: now.addingTimeInterval(-100))
        let repo = RateRepository(store: store, download: { _ in XCTFail("Fresh cache should avoid download"); throw ConverterError.unavailable })
        let q = try await repo.get(gbp, now: now); XCTAssertTrue(q.saved); XCTAssertFalse(q.offline)
    }
    func testStaleCacheOfflineAndTooOldRefusal() async throws {
        let store = MemoryStore(); store.values["GBP"] = Snapshot(data: payload(date: "2026-10-25"), fetchedAt: now.addingTimeInterval(-30000))
        let repo = RateRepository(store: store, download: { _ in throw ConverterError.unavailable })
        let q = try await repo.get(gbp, now: now); XCTAssertTrue(q.offline); XCTAssertEqual(q.age, 4)
        store.values["GBP"] = Snapshot(data: payload(date: "2026-10-21"), fetchedAt: now.addingTimeInterval(-30000))
        do { _ = try await repo.get(gbp, now: now); XCTFail("Expired quote accepted") } catch { }
    }
    func testInvalidFreshCacheRefreshes() async throws {
        let store = MemoryStore(); store.values["GBP"] = Snapshot(data: payload(code: "JPY"), fetchedAt: now)
        let valid = payload(); let repo = RateRepository(store: store, download: { _ in valid })
        let q = try await repo.get(gbp, now: now); XCTAssertFalse(q.saved)
        XCTAssertEqual(store.values["GBP"]?.data, valid)
    }
    func testUSDIdentityAvoidsNetwork() async throws {
        let repo = RateRepository(store: MemoryStore(), download: { _ in XCTFail("USD lookup"); throw ConverterError.unavailable })
        let q = try await repo.get(Asset("USD", "US Dollar", "", .world), now: now); XCTAssertEqual(q.rate, 1)
    }
    func testCatalogMatchesAndroid() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        let text = try String(contentsOf: root.appendingPathComponent("Resources/currencies.tsv"), encoding: .utf8)
        let assets = try Catalog.load(text)
        XCTAssertEqual(assets.count, 167); XCTAssertEqual(Set(assets.map(\.code)).count, 167)
        XCTAssertEqual(assets.filter { $0.category == .world }.count, 156)
        XCTAssertEqual(assets.filter { $0.category == .metals }.count, 4)
        XCTAssertEqual(assets.filter { $0.category == .crypto }.count, 7)
        XCTAssertThrowsError(try Catalog.load("BAD")); XCTAssertThrowsError(try Catalog.load("USD\tUS Dollar\t🇺🇸"))
    }
    func testExactCryptoDenominations() throws {
        let sat = Asset("SAT", "Satoshi (Bitcoin)", "sat", .crypto), drop = Asset("DROP", "Drops (XRP)", "drop", .crypto)
        XCTAssertEqual(sat.rateCode, "BTC"); XCTAssertEqual(drop.rateCode, "XRP")
        XCTAssertEqual(Conversion.digits(sat), 0); XCTAssertEqual(Conversion.digits(drop), 0)
        XCTAssertEqual(try Conversion.cross(1, source: 1, target: sat.unitsPerCoin, digits: 0), 100_000_000)
        XCTAssertEqual(try Conversion.cross(1, source: sat.unitsPerCoin, target: 1, digits: 8), Decimal(string: "0.00000001"))
        XCTAssertEqual(try Conversion.cross(1, source: 1, target: drop.unitsPerCoin, digits: 0), 1_000_000)
        XCTAssertEqual(try Conversion.cross(1, source: drop.unitsPerCoin, target: 1, digits: 6), Decimal(string: "0.000001"))
        XCTAssertEqual(try Conversion.cross(100, source: 1000, target: 500000, digits: 0), 50000)
    }
    func testSatoshiUsesParentRateAndCache() async throws {
        let sat = Asset("SAT", "Satoshi (Bitcoin)", "sat", .crypto), store = MemoryStore()
        let body = payload(code: "BTC", rate: "0.00001", provider: "ESTV")
        let repo = RateRepository(store: store, download: { asset in XCTAssertEqual(asset.code, "BTC"); return body })
        let q = try await repo.get(sat, now: now)
        XCTAssertEqual(q.rate, 1000); XCTAssertEqual(q.date, "2026-10-29"); XCTAssertTrue(q.source.contains("ESTV"))
        XCTAssertNotNil(store.values["BTC"]); XCTAssertNil(store.values["SAT"])
        let again = try await repo.get(sat, now: now); XCTAssertTrue(again.saved)
    }
    func testDropsPreserveParentOfflineFlags() async throws {
        let drop = Asset("DROP", "Drops (XRP)", "drop", .crypto), store = MemoryStore()
        store.values["XRP"] = Snapshot(data: payload(code: "XRP", rate: "0.5", date: "2026-10-25", provider: "ESTV"), fetchedAt: now.addingTimeInterval(-30000))
        let repo = RateRepository(store: store, download: { asset in XCTAssertEqual(asset.code, "XRP"); throw ConverterError.unavailable })
        let q = try await repo.get(drop, now: now)
        XCTAssertEqual(q.rate, 500000); XCTAssertTrue(q.offline); XCTAssertTrue(q.saved); XCTAssertEqual(q.age, 4)
    }

}
