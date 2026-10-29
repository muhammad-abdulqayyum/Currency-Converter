# Universal Currency Converter · Version 3

A lightweight Android reference converter by **Muhammad Hafiz Abdul-Qayyum**, Clinton Hill, Brooklyn, NYC. Built for humanity, with care.

Enter a USD amount, choose **World Currencies**, **Metals**, or **Crypto**, select an asset from the dropdown, and tap **Convert USD**.

**[Download the Version 3 preview APK](https://github.com/muhammad-abdulqayyum/Currency-Converter/raw/refs/heads/master/android-installer-file/Currency-Converter.apk)** · Android 6.0+ · debug-signed preview. See signing notes below if upgrading from a previous debug build.

## Version 3

- **155 world currencies:** current national, shared and territorial currencies in the verified bundled catalog, with flag/region symbols, codes and names.
- **Four precious metals:** gold (XAU), silver (XAG), platinum (XPT) and palladium (XPD), in **troy ounces** (31.1034768 grams).
- **Five cryptocurrencies:** Bitcoin, Ethereum, XRP, Litecoin and Bitcoin Cash. Swiss Federal Tax Administration (ESTV) reference values, not live trading prices.
- Cream background, navy text, restrained red and green accents, and an original sun/crescent/star emblem.
- One API, no account, ads, analytics SDKs, private API keys, or application backend.
- **Android 6.0+ (API 23)**; target/compile SDK 35. A Play submission needs a separately prepared production release and current target-SDK review.

## Preview

![World currencies screen](docs/screenshots/world.png)

## Validation

- 21 JVM unit tests passed; Android lint completed without errors.
- Debug APK measured 68,707 bytes (about 67 KiB).
- Installed and launched on an Android 6.0/API 23 emulator. Exercised GBP, gold and BTC conversion screens with validated live-feed snapshots seeded into the local cache, and checked a narrow screen with enlarged text. No runtime crash was observed.
- The separate live-feed check validated all 164 USD pairs. The emulator smoke check does not establish end-to-end network behavior on every supported phone.

## Rates and coverage

All data comes from [Frankfurter v2](https://frankfurter.dev/). The 164 offered USD pairs were checked against its live feed during development. Supported pairs can become unavailable or change later. A catalog entry is not a guarantee of an executable exchange rate.

The catalog includes shared currencies and territorial currencies for broad coverage. Euro-area countries share EUR; XAF, XOF, XCD and XPF serve multiple countries/territories and use neutral regional symbols. It excludes USD as an output (USD is the input), historical ANG/SVC, offshore CNH (CNY is available), and accounting units CMD/XDR. XCG replaces ANG. Flags help identify currency areas and are not statements about sovereignty. Flag glyph appearance depends on Android's emoji font; names and codes remain visible.

A single-pair request uses `/v2/rate/USD/{CODE}?expand=providers`; crypto adds `providers=ESTV`. Responses must have the requested pair, positive bounded numeric rates, valid dates and, for crypto, the ESTV source. The app rejects dates in the future or more than seven UTC calendar days old. Reference dates and sources are displayed. Values older than three days carry an additional age label.

### Lightweight local cache

Only validated rate snapshots are saved locally, one per asset. Snapshots are reused for six hours; calculations with those rates happen on-device. If refreshing fails, a valid saved rate up to seven days old can be used with an explicit saved/connection-unavailable label. Older data is rejected. Amounts and conversion history are not persisted. This cache does not make daily data live.

The app checks HTTPS certificates normally, does not follow redirects, sets connection/read timeouts, limits response size and prevents duplicate in-flight conversions. No insecure certificate bypass or HTTP fallback is used.

## Build and checks

Requires JDK 17, Gradle 8.9 and Android SDK 35/build tools 35.0.0:

```sh
gradle -p project testDebugUnitTest lintDebug assembleDebug
python3 scripts/validate_rates.py
```

The live check is read-only and needs an internet connection. It checks catalog coverage, positive rates, freshness and Swiss crypto provenance. Unit tests cover currency rounding, crypto/metal precision, input boundaries, strict rate dates and cache expiration. GitHub Actions builds and uploads the APK.

Debug build: `project/app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions artifacts use debug signing; signatures from different build environments may differ, requiring uninstalling an earlier debug build. Production publishing requires a stable private signing/upload key; never commit private keys.

## About

“From your Brother Muhammad Hafiz Abdul-Qayyum in the Wilderness of North America.”

For humanity. If Allah wills.

Original project uploaded in 2017. Version 2 modernized the build and exchange-rate integration; Version 3 expands worldwide reference coverage while keeping the interaction simple. Land, advertising, paid upgrades and live market-price feeds are outside Version 3.
