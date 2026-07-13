# Changelog

All notable changes to OHMusic will be documented here. The project follows
semantic versioning after its first public release.

## Unreleased

## 1.0.1 - 2026-07-13

- Prevented page-switch flashes by prewarming retained screens and showing
  default artwork from the first loading frame.

## 1.0.0 - 2026-07-13

- Prepared the project for public open-source development.
- Standardized the product name as OHMusic.
- Limited local scans to songs longer than two minutes.
- Improved startup behavior, default artwork and adaptive launcher icons.
- Migrated playback to Media3 `MediaLibraryService` with five notification
  actions and multi-vendor system media controls.
- Removed third-party online cover fetching to keep playback offline and private.
