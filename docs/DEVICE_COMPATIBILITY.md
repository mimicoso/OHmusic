# Device compatibility

The following configurations have received manual regression testing during
development. This list is informational and does not imply vendor endorsement.

| Vendor | Android / skin | Covered behavior |
|---|---|---|
| realme | Android 16 | startup, scanning, playback, process recovery, media notification |
| vivo | Android 14 / Funtouch OS 14 | native MediaStyle notification and core controls |
| vivo | Android 16 / Funtouch OS 16 | large media card, artwork colors and five actions |

OEM-only status-bar islands may require signature permissions or a vendor
partnership. OHMusic uses public Media3 and Android APIs and does not emulate
private vendor protocols.

When reporting a compatibility problem, include the device model, Android build,
whether battery restrictions are enabled and a screenshot with personal song
metadata hidden.
