# Tacita

Tacita sets the media volume when the phone screen turns off, unless media is playing. By default it leaves the volume alone whenever a Bluetooth audio device is connected.

## Build

Open this folder in Android Studio, allow it to install the Android SDK and Gradle dependencies, then run the `app` configuration on an Android 8.0 (API 26) or later device. The project compiles and targets Android 17 (API 37), which needs Android Gradle Plugin 9.1.1 and Gradle 9.3.1.

The first launch starts lock monitoring and requests notification permission on Android 13+. The ongoing notification is required by Android so the app can reliably receive screen-lock events in the background.

## Settings

- **Volume to drop to:** target media-stream volume, default `0`.
- **Include Bluetooth devices:** when enabled, apply the rule even with a Bluetooth audio output connected; default `false`.
