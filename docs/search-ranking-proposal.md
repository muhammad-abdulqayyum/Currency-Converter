# Universal Currency Converter search ranking proposal

**Status:** Proposed. Start with the Android picker.

Use the same ranking rules for currency, asset, and expandable country rows. Replace Palau’s fixed position with a ranking based on the text typed.

| Rank | Match |
|---|---|
| 1 | Exact currency or asset code |
| 2 | Exact country or asset name or recognized alias |
| 3 | Beginning of a name |
| 4 | Beginning of a later word in a name |
| 5 | Match inside a word, for searches of at least three characters |

Break ties alphabetically by the displayed name. Keep Palau and Palestine as expandable country rows. Palau opens to USD. Palestine opens to USD, ILS, and JOD. A direct USD search still shows the selectable currency.

## How to verify

- Type `p`. Palau appears among other results whose names start with P, in alphabetical order.
- Type `pal`. Palau and Palestine appear above Nepalese Rupee. USD appears inside the Palau group after tapping it.
- Type `pala`. Palau is the strongest match. Tap it and select USD.
- Type `USD`. The currency appears first and can be selected directly.

This proposal covers ranking in the existing picker. Making every country an expandable row would be a separate feature.
