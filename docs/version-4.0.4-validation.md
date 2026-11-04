# Version 4.0.4 develop preview

The dimensional metals and crypto artwork is shared between Android and iOS. Bitcoin Cash's white B now tilts counterclockwise/left, opposite BTC; it is not mirrored. Android About, iOS UI expectations and simulator reports now agree on 4.0.4. Both denomination badges and the existing cream/navy layout remain.

## Branches and installation

`develop` holds reviewable previews. `master` is unchanged by this cleanup and will receive approved changes through a pull request. Android version 4.0.4/code 44 retains the preview certificate used by 4.0.3. SHA-256 certificate fingerprint: `648d41550ab8f7b9b34618551fd3002199f0e2bc1bb4d792d4f20712d2f85c93`.

## Validation

- Android unit tests, lint and APK assembly: passed. https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/runs/40985569461
- Fresh-build Android 6 device flows and normal/large-text screenshots: running.
- Packaged APK upgrade from 4.0.3: pending.
- iOS core tests, simulator compilation and unsigned device Release: passed. Interface matrix running. https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/runs/40985569488

Device conversions use synthetic local fixtures solely for repeatable testing. Home screenshots show the initial screen. No synthetic rates are bundled in the Android preview APK. Apple signing/distribution is not part of this Android preview.
