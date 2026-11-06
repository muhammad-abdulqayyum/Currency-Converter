import Foundation

enum CountrySearch {
    private struct Country: Decodable {
        let alpha3: String
        let name: String
        let aliases: [String]
        let exactAliases: [String]?
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

    static func currencies(for query: String, excluding excludedCountry: String? = nil) -> Set<String> {
        guard !query.isEmpty else { return [] }
        let countries = dictionary.filter { $0.key != excludedCountry }
        if let exact = countries.first(where: { $0.key.lowercased() == query || $0.value.alpha3.lowercased() == query ||
            ($0.value.exactAliases?.contains { normalize($0) == query } ?? false) }) {
            return Set(exact.value.currencies)
        }
        return Set(countries.values.flatMap { country -> [String] in
            let matches = ([country.name] + country.aliases).contains { name in
                    let normalized = normalize(name)
                    return normalized == query || (query.count >= 3 && normalized.contains(query))
                }
            return matches ? country.currencies : []
        })
    }
}
