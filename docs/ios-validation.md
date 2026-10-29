# iPhone and iPad version 4.0.2

Native Swift/SwiftUI port, deployment target iOS/iPadOS 15.0. Author: Muhammad Hafiz Abdul-Qayyum, from Clinton Hill, Brooklyn, New York City, USA.

## Scope

The port shares the Android world-currency catalog byte for byte: 156 world currencies including USD, four troy-ounce metals and five cryptocurrencies. Both asset pickers support category filtering and name/code search. Swap keeps the amount, changes source units and clears the previous result. Decimal arithmetic uses a common USD reference. Both quote dates and provider information are displayed, with differing dates labeled as indicative comparisons.

Reference snapshots are cached in the app's purgeable cache directory, reused for six hours. On refresh failure a valid quote dated within seven UTC days may be reused with an explicit saved/offline label. Future dates, incorrect pairs, invalid rates and crypto without included ESTV provenance are rejected. HTTPS downloads reject redirects and cap response bodies at 64 KiB. Amounts/history are not persisted. Debug UI fixtures and their launch-argument override are compiled out of Release.

The native interface keeps the cream/navy palette, serif typography, original emblem, compact centered swap control and Black American Heritage Flag footer. It supports safe areas, Dynamic Type, portrait/landscape and naturally scrollable content on smaller screens. The normal-size phone footer has a dedicated no-scroll test. Interactive targets use at least 44 points; Swap uses 48 points.

## Validation

Core tests and both compilation checks passed. Simulator interaction checks are still running; this document does not yet record a passing interaction result.

Workflow: https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/runs/40952205673

- Eleven deterministic core tests cover catalog integrity, valid/invalid amounts, cross rates and rounding, asset precision, UTC date limits, response validation, included Swiss crypto provenance, fresh-cache reuse, offline fallback, expiry refusal, invalid-cache refresh and USD identity.
- Simulator Debug and unsigned physical-device Release compilation use Xcode 26.3.
- UI checks exercise conversion flows, preserved amount after swap, source/date disclosure, country-code search, same-asset conversion, centered swap bounds, default footer fit, About/version/author credit, and large-text landscape footer access.

The deployment target is **not** proof of an iOS 15 runtime test. CI uses currently installed simulator runtimes. Physical-device testing, especially an older iOS 15 phone, remains a release validation step.

## Installation status

The source, project definition and checks are committed in the public repository. XcodeGen creates the Xcode project; see `ios/README.md`. CI provides a simulator app and check artifacts. The device Release build is unsigned. No installable signed IPA or TestFlight/App Store release has been produced: that requires the author's Apple team/account, signing/provisioning and distribution setup. No Apple account was created and no app was submitted to Apple.
