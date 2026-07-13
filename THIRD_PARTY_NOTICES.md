# Third-party notices

OHMusic depends on open-source libraries distributed through Maven Central and
Google Maven. Their original license texts and copyright notices remain with
their respective projects and artifacts.

Major direct dependencies include:

- AndroidX, Jetpack Compose, Room and Media3 — Apache License 2.0
- Dagger and Hilt — Apache License 2.0
- Coil — Apache License 2.0
- Kyant Backdrop / AndroidLiquidGlass — Apache License 2.0
- Kyant Shapes — Apache License 2.0
- Kotlin and Kotlin Coroutines — Apache License 2.0
- JUnit 4 — Eclipse Public License 1.0 (test scope only)

Dependency versions are declared in `app/build.gradle.kts`. Before publishing a
binary release, regenerate and review the complete transitive dependency license
report so any upstream `NOTICE` requirements are preserved.
