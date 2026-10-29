# Version 4.0 validation

## Build and math

- JDK 17, Gradle 8.9, Android compile SDK 35; minimum API 23.
- 36 JVM tests passed: 21 existing tests and 15 Version 4 tests.
- New cases cover USD/gold in both directions, gold/silver, BTC/gold, XRP/BTC, BRL/PHP, fractional coins, final-only rounding, zero, three-decimal currencies, invalid rates, excessive precision, exponent input and the amount limit.
- Android lint completed with zero errors. Remaining warnings include untranslated programmatic labels and legacy unused resources.
- The pushed source checkpoint also passed GitHub Actions: https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/runs/40873122965
- ZIP integrity and APK signing verification passed. Hash, size and certificate fingerprint are recorded in `version-4-checks.json`.

## Live feed

The read-only `scripts/validate_rates.py` check passed for all 164 API-backed entries: 155 world currencies, four metals and five cryptocurrencies. USD is additionally selectable locally as the reference base, giving 165 assets total. This checks current positive rates, dates and Swiss crypto provenance; providers can change their coverage later.

## Installation

This reconstruction uses a new debug certificate. A differently signed 3.x APK must be uninstalled first. The new certificate is for previews, not a production signing policy. The old APKs remain available.

## Limits

Crypto and metal values are reference quotes, not executable trading prices. Each leg may have a different reference date; the result labels that explicitly. This validation does not establish end-to-end network behavior on every Android phone or guarantee future API availability. Synthetic test rates used in the emulator are not bundled into the APK.

## Reproduce the device checks

On a dedicated, booted Android emulator with `ANDROID_HOME` set, run:

```sh
python3 scripts/android_smoke.py
```

The script installs the committed APK and places synthetic reference rates in the emulator cache. Test artifacts are written to `device-checks/`. The GitHub Android 6 device workflow runs the same script using hardware acceleration.

## Android 6 device check — passed

The exact committed APK passed the hardware-accelerated API 23 workflow:
https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/runs/40929864665

Verified installation and launch, World/Metals/Crypto selection, code search, gold → USD, USD → gold via Swap, Bitcoin → gold, Bitcoin → silver, BRL → PHP, and same-asset palladium conversion. Synthetic fixtures establish expected arithmetic independently of changing prices. The differing BTC reference date was explicitly labeled. About showed Version 4.0.

A 360×800 layout with 1.3× font scaling also passed picker selection and footer navigation. No app fatal exception was observed. Screenshots are in `docs/screenshots/v4-*.png`. This run validates the packaged APK, rather than relying only on a separately rebuilt CI artifact.
