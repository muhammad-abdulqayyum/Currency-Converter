# Version 4.0.3 — crypto units and recognition icons

The release adds SAT (Bitcoin satoshis) and DROP (XRP drops) to both independent Crypto pickers on Android and iOS. There are now 167 selectable assets: 156 world currencies, four metals, and seven crypto choices comprising five coins and two denominations.

## Denomination handling

- 1 BTC = 100,000,000 SAT; 1 XRP = 1,000,000 DROP.
- BTC ↔ SAT and XRP ↔ DROP use fixed ratios without a network lookup. Same-asset identity preserves the original amount.
- Market conversions derive units-per-USD from the parent BTC/XRP quote. Only the parent's snapshot is cached. Swiss ESTV provenance and date validation apply to the parent before deriving a rate.
- Saved/offline flags, source and date are retained. Differing-date comparisons remain explicitly labeled.
- Converted SAT/DROP results round half-up to whole units; positive results below the smallest displayable unit are labeled `< 1`. The converter is a reference calculator, not a transaction builder; fractional mathematical inputs remain accepted within the existing input rules.

Sources: https://bitcoin.org/en/vocabulary and https://xrpl.org/docs/references/protocol/data-types/basic-data-types . SAT and DROP are app unit identifiers, not new upstream API quote symbols.

## Icons and platform behavior

The ETH diamond, blue Litecoin Ł coin and curved community XRP X mark use shared local SVG sources in `artwork/crypto/` and matching bundled PNGs on both platforms. SAT and DROP retain their parent's visual identity with small unit badges. No new runtime library, icon service or rate API is added. Existing amount limits, privacy behavior, world flags, metal units, compact centered swap and heritage footer remain.

Android version code 43 uses the existing 4.x preview certificate and should update over 4.0.2 without uninstalling. iOS version/build is 4.0.3/43. Minimum deployment targets remain Android 6/API 23 and iOS/iPadOS 15. iOS distribution still requires Apple signing and account setup; no signed IPA has been published.

## Checks

Android: **passed** (46 core tests, lint/build, signed APK upgrade, all device flows). iOS: **14 core tests and Debug/Release compilation passed**; all five main iPhone 17 interaction tests passed in the first workflow. Full small-phone/iPad revalidation is **incomplete**, as detailed below.

- Android source tests, lint and assembly: https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/runs/40970162409
- Signed APK upgrade and device checks: https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/runs/40971150766
- iOS core tests, Debug/Release compilation and simulator checks: https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/runs/40972616476

The added core checks exercise exact denomination ratios in both directions, single-unit precision, cross-subunit market conversion, whole-unit rounding and parent-cache derivation. Device/simulator flows exercise both new selections, swaps, cross-subunit results, source/date disclosure and existing layouts. Android uses a dedicated API 23 emulator with synthetic local fixtures; iOS Debug uses injected fixtures compiled out of Release. No synthetic conversion prices are used in published initial-screen screenshots. An iOS 15 physical device is not part of the CI simulator matrix.

The new multi-step iOS unit flow has a 180-second allowance for slower small-phone simulator interaction. All conversion logic is unchanged by this test timing adjustment. The full phone suite is supplemented by layout, large-text/landscape and new-unit flows on the smaller phone and iPad.

## iOS verification limit

The application-source workflow https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/runs/40970162515 passed all five iPhone 17 tests, including new denominations, and produced the real main-phone screenshot. On iPhone SE, the multi-step unit flow reached its original two-minute execution allowance; the two layout/accessibility checks passed. The test's allowance was raised to 180 seconds without changing app code.

The adjusted workflow https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/runs/40972616476 and its automatic job retry both passed core tests and simulator/device compilation but stopped before interface tests because `simctl bootstatus` exceeded the 240-second startup limit. These are simulator-startup failures, not observed app crashes. The adjusted small-phone unit flow and the iPad matrix therefore remain unverified for this patch. No complete three-device passing result is claimed. Apple signing and physical iOS 15 validation remain separate release steps.
