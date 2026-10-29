# Currency Converter

Muhammad Abdul-Qayyum’s Java Android currency converter, updated from fixed 2017 rates to a live HTTPS API.

## Version 2
- Convert USD to GBP, EUR, JPY, or BRL using Frankfurter's latest available daily reference rate.
- Shows the rate and its observation date. These are daily reference rates, not intraday quotes or a bank's final price.
- Decimal-safe calculations with currency-specific rounding (JPY uses zero decimal places).
- Input validation, background networking, loading indicator, connection/read timeouts, and retryable errors. No API key required and no hardcoded fallback rates.
- Responsive scrolling layout; Android 6.0+; target/compile SDK 35.

## Build and install
Open `project/` in Android Studio with JDK 17 and Android SDK 35, then build/run the app. The Gradle wrapper uses Gradle 8.9.

```
cd project
bash gradlew testDebugUnitTest assembleDebug
```

GitHub Actions also builds and tests on every push. Download `Currency-Converter-debug` from a successful [Android build](../../actions/workflows/android.yml) run to try the debug APK. Debug APKs are for testing, not Play Store distribution.

The APK under `android-installer-file/` is the historical version 1 build and does **not** include the API update. Use a new build from source or Actions.

## API
[Frankfurter documentation](https://frankfurter.dev/)

`GET https://api.frankfurter.dev/v2/rate/USD/GBP`

The app validates the response currency pair, rate and observation date before conversion. Only the currency pair is sent; the entered amount stays on the device.

## Verification
Unit tests cover decimal multiplication, rounding, zero, malformed input, negative/oversized amounts and invalid rates. An Android device/emulator run is still needed to validate installation and UI behavior.
