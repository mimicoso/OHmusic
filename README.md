# OHMusic

OHMusic is a lightweight, offline-first Android music player built with Kotlin
and Jetpack Compose. It scans music already stored on the device and uses Kyant
Backdrop for its liquid-glass interface.

## Features

- Local-only MediaStore scanning; audio must be longer than two minutes
- Media3 `MediaLibraryService` background playback
- System media controls for notifications, lock screens, headsets and Bluetooth
- Sequential, repeat-one and shuffle playback modes
- Favorites, queue and playback-position recovery
- Embedded album artwork with a bundled fallback cover
- No analytics, ads, account, custom splash screen or network permission

## Privacy

OHMusic reads audio metadata and files only after Android grants music access.
All music metadata, favorites and playback state remain on the device. The app
does not include analytics and does not request network access. See
[PRIVACY.md](PRIVACY.md) for details.

## Requirements

- Android Studio with JDK 17 or newer
- Android SDK 37
- Android 10 / API 29 or newer device

If the SDK is not detected automatically, create an untracked
`local.properties` file:

```properties
sdk.dir=/path/to/Android/Sdk
```

## Build and test

On Linux or macOS:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

On Windows:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Its
application ID is `com.ohmusic.app.debug`, so it can coexist with a release
installation.

For release verification without signing credentials:

```bash
./gradlew lintRelease assembleRelease bundleRelease
```

Release signing is configured exclusively through environment variables. See
[CONTRIBUTING.md](CONTRIBUTING.md#release-signing) and never commit a keystore.

## Architecture

```text
Compose UI -> ViewModel -> MusicPlayerManager -> MediaController
                                          \-> MediaLibraryService -> ExoPlayer
Compose UI -> ViewModel -> MusicRepository -> Room / MediaStore
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for lifecycle and data-flow
details. Device-specific behavior is tracked in
[docs/DEVICE_COMPATIBILITY.md](docs/DEVICE_COMPATIBILITY.md).

## Contributing

Issues and pull requests are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md)
and report security problems according to [SECURITY.md](SECURITY.md).

## License

OHMusic is licensed under the [Apache License 2.0](LICENSE). Third-party
attributions are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
