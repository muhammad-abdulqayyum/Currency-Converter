# Universal Currency Converter

Convert between world currencies, precious metals, and cryptocurrencies on the web, Android, and iPhone or iPad. Pick any two supported assets, enter an amount, and use **Swap** to reverse the pair.

## Try it

| Platform | Get started |
| --- | --- |
| Web | [Open Universal Currency Converter](https://universalconverter.app/) in your browser. |
| Android 6.0+ | Open the latest successful [Android build](https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/workflows/android.yml), sign in to GitHub, and download and extract the APK artifact. Open `app-debug.apk` on your device to install the debug-signed preview. |
| iPhone and iPad | Follow the [iOS build instructions](ios/README.md) to run the SwiftUI app in Xcode. Installing on a physical device requires a signed build. |

## Features

- Search the **World**, **Metals**, and **Crypto** pickers by name or code.
- Convert across categories, such as Brazilian reais to Philippine pesos or Bitcoin to gold. Metals are measured in troy ounces. The crypto picker includes Bitcoin satoshis and XRP drops.
- See the source and date of each reference rate. Quotes come from [Frankfurter v2](https://frankfurter.dev/) and are intended for reference. They exclude trading fees and spreads.
- Reuse saved quotes offline on Android and iOS. Both apps show the quote date and warn when a saved quote is more than seven days old.

## Development

| Platform | Documentation |
| --- | --- |
| Web | [Angular and Cloudflare Workers setup](docs/cloudflare-web.md) |
| Android | [Android CI](https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/workflows/android.yml) and [conversion validation](docs/version-4-validation.md) |
| iOS | [Xcode setup](ios/README.md) and [iOS CI](https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/workflows/ios.yml) |

The latest passing [iOS CI run](https://github.com/muhammad-abdulqayyum/Currency-Converter/actions/runs/43220254756) is on the simulator fix branch. A signed install on a physical iPhone is still unverified.

[Version 4 history](docs/version-4.0-history.md) · [Version 3 history](docs/version-3-history.md) · [Illustration credits](THIRD_PARTY_NOTICES.md)

## About

Built by **Muhammad Hafiz Abdul-Qayyum** in Clinton Hill, Brooklyn, New York City. The original project was uploaded in 2017.

For humanity. If Allah wills.
