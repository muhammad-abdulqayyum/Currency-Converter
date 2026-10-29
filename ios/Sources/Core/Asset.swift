import Foundation

public enum Category: String, CaseIterable, Identifiable {
    case world = "World", metals = "Metals", crypto = "Crypto"
    public var id: String { rawValue }
}
public struct Asset: Identifiable, Equatable {
    public let code: String
    public let name: String
    public let symbol: String
    public let category: Category
    public var id: String { code }
    public var isSubunit: Bool { code == "SAT" || code == "DROP" }
    public var rateCode: String { code == "SAT" ? "BTC" : code == "DROP" ? "XRP" : code }
    public var unitsPerCoin: Decimal { code == "SAT" ? 100_000_000 : code == "DROP" ? 1_000_000 : 1 }
    public var subtitle: String { if isSubunit { return code + " · " + rateCode + " subunit" }; return code + (category == .metals ? " · troy oz" : category == .crypto ? " · coin units" : "") }
    public var unit: String { category == .metals ? "troy oz of \(name)" : code }
    public init(_ code: String, _ name: String, _ symbol: String, _ category: Category) {
        self.code = code; self.name = name; self.symbol = symbol; self.category = category
    }
}
public enum Catalog {
    public static func load(_ text: String) throws -> [Asset] {
        var world = try text.split(whereSeparator: \.isNewline).map { line -> Asset in
            let f = line.split(separator: "\t", omittingEmptySubsequences: false).map(String.init)
            guard f.count == 3, f[0].range(of: "^[A-Z]{3}$", options: .regularExpression) != nil,
                  !f[1].isEmpty, !f[2].isEmpty else { throw ConverterError.catalog }
            return Asset(f[0], f[1], f[2], .world)
        }
        guard !world.isEmpty, !world.contains(where: { $0.code == "USD" }),
              Set(world.map(\.code)).count == world.count else { throw ConverterError.catalog }
        world.append(Asset("USD", "US Dollar", "🇺🇸", .world))
        world.sort { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
        return world + [Asset("XAU", "Gold", "Au", .metals), Asset("XAG", "Silver", "Ag", .metals),
                        Asset("XPT", "Platinum", "Pt", .metals), Asset("XPD", "Palladium", "Pd", .metals),
                        Asset("BTC", "Bitcoin", "₿", .crypto), Asset("ETH", "Ethereum", "Ξ", .crypto),
                        Asset("XRP", "XRP", "X", .crypto), Asset("LTC", "Litecoin", "Ł", .crypto),
                        Asset("BCH", "Bitcoin Cash", "₿", .crypto), Asset("SAT", "Satoshi (Bitcoin)", "sat", .crypto),
                        Asset("DROP", "Drops (XRP)", "drop", .crypto)]
    }
}
public enum ConverterError: LocalizedError {
    case catalog, amount, rate, date, pair, provenance, unavailable, arithmetic
    public var errorDescription: String? {
        switch self {
        case .catalog: return "The asset catalog could not be loaded."
        case .amount: return "Enter 0 to 1 trillion, with up to 8 decimal places. Use a decimal point."
        case .unavailable: return "No recent reference rate is available. Check your connection and try again."
        case .arithmetic: return "This conversion exceeds the supported precision. Try a smaller amount."
        default: return "The service returned an invalid or outdated reference rate. Please try again later."
        }
    }
}
public enum Conversion {
    public static let posix = Locale(identifier: "en_US_POSIX")
    public static func amount(_ value: String) throws -> Decimal {
        let s = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard s.count <= 22, s.range(of: "^[0-9]+(\\.[0-9]{1,8})?$", options: .regularExpression) != nil,
              let d = Decimal(string: s, locale: posix), d <= Decimal(1_000_000_000_000) else { throw ConverterError.amount }
        return d
    }
    public static func digits(_ asset: Asset) -> Int {
        if asset.isSubunit { return 0 }
        if asset.code == "XRP" { return 6 }
        if asset.category != .world { return 8 }
        let f = NumberFormatter(); f.numberStyle = .currency; f.currencyCode = asset.code
        return max(0, f.maximumFractionDigits)
    }
    public static func cross(_ amount: Decimal, source: Decimal, target: Decimal, digits: Int) throws -> Decimal {
        guard amount >= 0 else { throw ConverterError.amount }
        try validateRate(source); try validateRate(target)
        var a = amount, t = target, s = source, product = Decimal(), result = Decimal(), rounded = Decimal()
        // Decimal has finite precision: fail explicitly rather than silently truncate intermediate values.
        guard NSDecimalMultiply(&product, &a, &t, .plain) == .noError else { throw ConverterError.arithmetic }
        let status = NSDecimalDivide(&result, &product, &s, .plain)
        guard status == .noError || status == .lossOfPrecision else { throw ConverterError.arithmetic }
        NSDecimalRound(&rounded, &result, digits, .plain)
        return rounded
    }
    public static func validateRate(_ rate: Decimal) throws {
        guard !rate.isNaN, rate > 0, rate >= Decimal(string: "1e-30")!, rate <= Decimal(string: "1e30")! else { throw ConverterError.rate }
    }
    public static func display(_ value: Decimal, asset: Asset, positiveInput: Bool, exact: Bool = false) -> String {
        let f = NumberFormatter(); f.numberStyle = .decimal
        f.minimumFractionDigits = asset.category == .world ? digits(asset) : 0
        f.maximumFractionDigits = exact ? max(8, digits(asset)) : digits(asset)
        let shown: String
        if value == 0 && positiveInput {
            let tiny = Decimal(sign: .plus, exponent: -digits(asset), significand: 1)
            shown = "< " + (f.string(from: tiny as NSDecimalNumber) ?? "0")
        } else { shown = f.string(from: value as NSDecimalNumber) ?? NSDecimalNumber(decimal: value).stringValue }
        return shown + " " + asset.unit
    }
}
