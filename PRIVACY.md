# Privacy

OHMusic is an offline local music player.

## Data accessed

- Audio files and MediaStore metadata, after the user grants Android's music
  access permission
- Notification permission, when required by the Android version, to show active
  playback controls

## Data stored

- A local Room database containing scanned song metadata and favorites
- Local preferences containing the playback mode, queue identifiers and last
  playback position

The app does not copy or upload audio files. It does not contain analytics,
advertising SDKs, account systems or network access.

## Backup

Android device-to-device transfer may preserve favorites and playback settings.
Scanned song metadata, cached artwork and transient playback state are excluded
from cloud backup because they can be recreated from local media.

## Deletion

Uninstalling OHMusic removes its local database and preferences. Android may
retain explicitly allowed backup data according to the user's system backup
settings.
