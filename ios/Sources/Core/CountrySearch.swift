import Foundation

enum CountrySearch {
    private struct Country: Decodable {
        let alpha3: String
        let name: String
        let aliases: [String]
        let currencies: [String]
    }

    private static let dictionary: [String: Country] = {
        guard let url = Bundle.main.url(forResource: "country-currencies", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let countries = try? JSONDecoder().decode([String: Country].self, from: data) else { return [:] }
        return countries
    }()

    static func normalize(_ text: String) -> String {
        text.trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
            .decomposedStringWithCanonicalMapping
            .replacingOccurrences(of: "\\p{M}", with: "", options: .regularExpression)
    }

    static func currencies(for query: String) -> Set<String> {
        guard !query.isEmpty else { return [] }
        if let exact = dictionary.first(where: { $0.key.lowercased() == query || $0.value.alpha3.lowercased() == query }) {
            return Set(exact.value.currencies)
        }
        return Set(dictionary.values.flatMap { country -> [String] in
            let matches = ([country.name] + country.aliases).contains { name in
                    let normalized = normalize(name)
                    return normalized == query || (query.count >= 3 && normalized.contains(query))
                }
            return matches ? country.currencies : []
        })
    }
}
