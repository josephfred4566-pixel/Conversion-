# Conversion

Conversion is a small, offline Android unit-converter app. It currently converts common units for length, mass, temperature, and volume. Values are calculated on-device; the app does not require an account or network access.

## Included conversions

- **Length:** meters, kilometers, centimeters, millimeters, miles, feet, and inches
- **Mass:** kilograms, grams, milligrams, pounds, and ounces
- **Temperature:** Celsius, Fahrenheit, and Kelvin
- **Volume:** liters, milliliters, US gallons, US cups, and US fluid ounces

Choose a category and source/target units, enter a value, and tap **Convert**. Use **Swap units** to reverse the direction. The initial app is a starter implementation; additional categories and units can be added in `MainActivity.kt`.

## Build

The project uses Kotlin and the Android Gradle Plugin. Open the repository in Android Studio and run the `app` configuration, or build from a machine with JDK 17 and Gradle 8.9 installed:

```bash
gradle assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

A GitHub Actions workflow builds the debug APK when Android/Gradle source files are pushed to `main` or included in a pull request targeting `main`. It uploads the APK as the `conversion-debug-apk` artifact for seven days. The workflow can also be started manually from the Actions tab.

## Project layout

- `app/src/main/java/com/example/conversion/MainActivity.kt` — app UI and conversion definitions
- `app/src/main/AndroidManifest.xml` — launcher activity and app metadata
- `app/build.gradle.kts` — Android module configuration
- `.github/workflows/build-android-apk.yml` — CI build and artifact upload

This starter app is intended for general-purpose unit conversion; its results are not a substitute for domain-specific engineering, scientific, or regulated calculations.
