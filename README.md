# Tacita

Tacita sets the media volume when the phone screen turns off, unless media is playing. It leaves the volume alone whenever a Bluetooth audio device is connected by default. Monitoring is opt-in and is disabled on a fresh install.

## Build

Open this folder in Android Studio, allow it to install the Android SDK and Gradle dependencies, then run the `app` configuration on an Android 8.0 (API 26) or later device. The project compiles and targets Android 17 (API 37), which needs Android Gradle Plugin 9.1.1 and Gradle 9.3.1.

When you enable monitoring, Tacita requests notification permission on Android 13+. The ongoing notification is required by Android so the app can reliably receive screen-off events in the background.

## Release identity

- Version: `0.1` (version code `1`)

## Settings

- **Target volume:** target media-stream volume, default `5%`.
- **Include Bluetooth devices:** when enabled, apply the rule even with a Bluetooth audio output connected; default `false`.
