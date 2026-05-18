# CMReborn v1.1.3

Patch release.

## Implemented Functionality
- Fixed notification policy drift where archived conversations could fail to restore notifications after unarchive
- Hardened conversation-channel restoration by cleaning stale migrated per-conversation channels before re-enable
- Stabilized archived-contact and archived-thread identity lookups across newer Google Messages builds

## Validation Summary
- Archive action disables notifications
- Unarchive action restores notifications
- Archived profile entry remains hidden
- Search zero-state suggestion chips remain suppressed
- Archived selection action-mode still exposes functional `Unarchive`

## Build Artifact
- `cmreborn-v1.1.3-release.apk`
