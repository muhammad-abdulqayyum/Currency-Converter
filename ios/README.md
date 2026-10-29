# Universal Currency Converter for iPhone and iPad

Native Swift/SwiftUI version 4.0.3 by Muhammad Hafiz Abdul-Qayyum, from Clinton Hill, Brooklyn, New York City, USA. Built for humanity, with care.

Minimum deployment target: **iOS/iPadOS 15.0**. Universal iPhone/iPad interface; portrait and landscape; safe-area insets; Dynamic Type and VoiceOver labels; scrolls on small screens and with larger text. Uses only Apple frameworks, with no third-party runtime dependencies. Keep the Android APK for Android; it cannot install on iOS.

## Features

The same 167 assets as Android: 156 world currencies, four metals and five cryptocurrencies plus Bitcoin satoshis and XRP drops. Independent searchable From/To pickers, centered swap, decimal conversion, six-hour local reference cache, explicitly labeled offline fallback up to seven rate-date days, source/date disclosure, ESTV validation for crypto and troy-ounce metal units. Cream/navy serif design, original emblem and Black American Heritage Flag footer.

Only rate snapshots are written into the OS-purgeable app cache. Amounts and history are not saved. HTTPS only, no redirects, bounded 64 KiB responses, connection/resource timeouts, no ads or analytics. Fiat and metal availability depends on the upstream provider. Crypto is dated Swiss reference data, not live market prices.

BTC ↔ SAT and XRP ↔ DROP are fixed denomination conversions and work without a rate lookup. Other pairs reuse the parent coin quote, provenance, date and cache. SAT/DROP outputs round to whole units. The ETH/LTC/XRP icons are bundled locally; no image network calls are added.

## Open and run on a Mac

Install Xcode 26 or newer and its iOS simulator runtime, plus [XcodeGen](https://github.com/yonaskolb/XcodeGen) (build-time project generator only):

```sh
brew install xcodegen
cd ios
xcodegen generate
open UniversalCurrencyConverter.xcodeproj
```

Select an iPhone or iPad simulator and Run. The generated project is reproducible from `project.yml`; no packages are downloaded into the application. To run on your own iPhone, select your Apple development team under Signing & Capabilities, choose a unique bundle ID if necessary, then select your connected phone. Do not commit certificates or provisioning profiles.

## Tests and build

```sh
swift test --package-path ios
python3 scripts/ios_simulator_check.py
```

Run from the repository root after generating the Xcode project. The iOS GitHub Actions workflow runs core tests, builds the real simulator application, runs UI tests on available phone/small-phone/iPad simulators, captures real screenshots, and compiles an unsigned device Release build. Read the workflow result for actual tested OS versions: the iOS 15 deployment target is not a claim that an iOS 15 runtime was exercised. Apple-hosted CI generally provides newer simulator runtimes. Physical-device testing, particularly an older iOS 15 device, remains a release validation step.

Debug UI tests inject synthetic rates using the `-ui-testing` launch argument; the fixtures and launch-argument override are compiled out of Release. Screenshots show the initial screen without synthetic quotes. Decimal arithmetic checks calculation errors and reports overflow rather than displaying an invalid result; final output rounds to currency/asset precision.

## Sharing with people

An unsigned device build or simulator `.app` is **not an installable iPhone download**. Normal public beta distribution uses TestFlight; public release uses the App Store. Both require your Apple Developer Program membership, signing/provisioning, an App Store Connect app record and the applicable Apple review. This repository does not include your Apple account, paid membership or signing credentials. Free personal-device development through Xcode is a separate option with Apple's provisioning limitations.

Use Xcode Product → Archive with your team, then Distribute App → App Store Connect to upload a signed build. Complete privacy/app information and add TestFlight testers. The bundled privacy manifest declares no tracking or collected data; review the provider's network processing and your final App Store privacy answers before submitting. Ordinary API access necessarily sends network metadata to the rate provider.

Apple references: [Xcode deployment support](https://developer.apple.com/xcode/system-requirements/), [upload builds](https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds/), [Developer Program](https://developer.apple.com/programs/enroll/).

Artwork credits remain in the repository's `THIRD_PARTY_NOTICES.md`. No Apple developer account was created and nothing was submitted to Apple by this change.
