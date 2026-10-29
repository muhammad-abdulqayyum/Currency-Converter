# Version 4.0.1 layout and upgrade validation

## Changes

- Compact header and spacing; Swap moves beside the From label.
- A subtle divider introduces the heritage footer: flag on the left, full author credit beside it, About on the right when width and text scale permit.
- Flexible spare space places the footer at the bottom when the content fits. Longer results, narrow screens and larger text remain scrollable; About moves below the credit when needed.
- All main controls keep at least 48 dp touch targets. Picker rows and conversion arithmetic remain unchanged.

## Build and signing

All 36 unit tests passed and lint completed without errors. Source build: https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/runs/40932401090

The packaged preview is signed with the same privately backed-up debug key as Version 4.0. Certificate and APK hashes are recorded in `version-4.0.1-checks.json`. Version code 41 permits an in-place upgrade from version code 40.

This is still a debug-signed preview. Publishing to an app store needs the separate production preparation described in the main README.

## Device checks — passed

The exact packaged APK passed on Android 6/API 23: https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/runs/40932842237

- In-place install over the signed Version 4.0 preview.
- Initial 420×900 layout: full About touch target and heritage flag visible, with bottom padding, before any scrolling.
- Existing conversion/search/swap scenarios, including different reference dates and same-asset conversion.
- 360×800 layout at 1.3× font scaling: picker usable, full heritage footer and About reachable by scrolling; About showed Version 4.0.1.
- No fatal app exception observed.

The one-screen assertion concerns the default USD → GBP view. Long asset names, larger text, extra unit notices and detailed results can require scrolling. Synthetic fixture rates are used only in the emulator, not bundled into the APK.
