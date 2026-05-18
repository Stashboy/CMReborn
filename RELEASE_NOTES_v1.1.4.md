# CMReborn v1.1.4

Patch release.

## Implemented Functionality
- Fixed notification toggles so archive reliably disables notifications and unarchive reliably restores notifications
- Hardened conversation-channel re-enable by clearing stale migrated conversation channels before restore
- Kept archived visibility/search protections aligned with the latest supported Google Messages build

## Validation Summary
- Archive action sets notification policy to disabled
- Unarchive action restores notification policy
- Archived profile entry remains hidden
- Search zero-state suggestion chips remain suppressed
- Archived selection action-mode still exposes functional `Unarchive`

## Build Artifact
- `cmreborn-v1.1.4-release.apk`
