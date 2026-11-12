import countries from '../../shared/countries/country-currencies.json' with { type: 'json' };

export function normalizeSearch(value: string): string {
  return value.trim().toLowerCase().normalize('NFD').replace(/\p{M}/gu, '');
}

export function countryCurrencies(query: string, excludedCountry?: string, secondExcludedCountry?: string): Set<string> {
  const matches = new Set<string>();
  if (!query) return matches;
  for (const [code, country] of Object.entries(countries)) {
    if (code === excludedCountry || code === secondExcludedCountry) continue;
    if (code.toLowerCase() === query || country.alpha3.toLowerCase() === query ||
        ('exactAliases' in country && country.exactAliases.some(name => normalizeSearch(name) === query))) return new Set(country.currencies);
    if ([country.name, ...country.aliases].some(name => {
          const normalized = normalizeSearch(name);
          return normalized === query || (query.length >= 3 && normalized.includes(query));
        })) {
      country.currencies.forEach(currency => matches.add(currency));
    }
  }
  return matches;
}
