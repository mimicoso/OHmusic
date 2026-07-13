# Contributing to OHMusic

Thank you for helping improve OHMusic.

## Before opening a change

- Search existing issues first.
- Keep the app lightweight, local-first and free of analytics.
- Avoid private OEM APIs and undocumented third-party network endpoints.
- Preserve standard Media3 behavior across Android vendors.
- Do not replace the original glass launcher artwork without project approval.

## Development workflow

1. Create a focused branch.
2. Make the smallest complete change.
3. Add or update tests for behavior changes.
4. Run:

   ```bash
   ./gradlew testDebugUnitTest lintDebug assembleDebug
   ```

5. For changes to resources, Media3, Room or R8 configuration, also run:

   ```bash
   ./gradlew lintRelease assembleRelease
   ```

6. Describe user-visible behavior and device coverage in the pull request.

## Code style

- Use the official Kotlin style.
- Keep UI text in Android string resources.
- Give interactive custom composables complete accessibility semantics.
- Keep I/O on a background dispatcher.
- Do not log song titles, artist names or other user media metadata in release
  builds.

## Release signing

Release credentials are optional for normal source builds. Maintainers can sign
release artifacts by setting all of these environment variables:

- `OHMUSIC_KEYSTORE_FILE`
- `OHMUSIC_KEYSTORE_PASSWORD`
- `OHMUSIC_KEY_ALIAS`
- `OHMUSIC_KEY_PASSWORD`

Keystores and passwords must never be committed. A release tag should match the
app `versionName`, and published artifacts should include SHA-256 checksums.
