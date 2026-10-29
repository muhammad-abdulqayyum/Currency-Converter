# Version 4.0.2 validation

The swap control moves from the From heading to the horizontal center between the source and destination cards. A locally drawn 30 dp circle and opposing horizontal arrows avoid font-dependent arrow glyphs. The native Button retains a 48 × 48 dp touch target, content description, keyboard focus, pressed/focused feedback and loading-state disabling.

The amount, conversion logic, independent category pickers, navy/cream palette, serif typography, heritage flag and author credit remain as in 4.0.1. The major version label remains VERSION 4; About reports 4.0.2. Version code 42 and the existing 4.x preview signing certificate allow an in-place upgrade.

Source build: https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/runs/40945182491 — JVM tests, Android lint and APK assembly passed.

Packaged APK device check: **PASSED** — https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/runs/40945780933

The dedicated Android 6/API 23 emulator checks upgrade from 4.0.1, default 420 × 900 one-screen fit, centered swap bounds and minimum touch target, preserved amount during swap, cross-category conversions with synthetic device-only fixtures, date disclosures, category search, same-asset conversion, About version, and the reachable heritage footer at 360 × 800 with 1.3 text scaling. Published initial-screen screenshots contain no synthetic conversion quotes.
