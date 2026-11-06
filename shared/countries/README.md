# Country search dictionary

The web, Android, and iOS pickers use the same UTF-8 JSON file. Entries use ISO 3166-1 alpha-2 and alpha-3 codes from the iso-codes dataset and English region names from Node's Intl.DisplayNames. Currency assignments come from Unicode CLDR 49 supplemental currency data, selecting tender currencies active on November 6, 2026. Search aliases are maintained here. The pickers intersect search results with their existing asset catalogs.

- [ISO codes dataset](https://salsa.debian.org/iso-codes-team/iso-codes)
- [Unicode CLDR currency data](https://github.com/unicode-org/cldr-json/tree/49.0.0/cldr-json/cldr-core/supplemental)
- [Unicode license](CLDR-LICENSE)
