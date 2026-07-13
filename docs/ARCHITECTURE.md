# Architecture

## Playback

`MusicPlayerService` owns the single `ExoPlayer` and exposes it through a Media3
`MediaLibrarySession`. `MusicPlayerManager` is the UI-facing controller facade.
Screens never own or release the player, so playback can continue after the
activity leaves the foreground.

The current queue, current media ID, play mode and position are persisted in
local preferences. They are restored only when the corresponding songs are still
present in Room after a MediaStore scan.

## Library data

`MusicRepository` queries MediaStore on `Dispatchers.IO`. Only entries marked as
music and longer than 120,000 milliseconds are accepted. The complete scan result
replaces the Room table in one transaction while preserving favorite IDs.

## UI

The app has two Compose destinations: player and library. Only the selected tab
is composed. Each screen has a Hilt ViewModel and state is exposed through
`StateFlow`. Backdrop effects are scoped locally to avoid vendor GPU issues from
cross-page render sources.

## Startup

There is no custom splash activity. Permission requests and the MediaController
connection run after the first frame so the startup path stays small.

## Media interoperability

The service advertises both the Media3 and platform media-browser actions. This
lets Android System UI, Bluetooth devices, Android Auto-compatible clients and
vendor media panels discover the standard session. Private OEM notification APIs
are intentionally not used.
