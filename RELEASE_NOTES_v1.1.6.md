# CMReborn v1.1.6

Release date: 2026-05-26

## Summary
Production offset update for the latest Google Messages build with full core functionality restoration.

## Changelog
- Updated hook mappings for Google Messages `309541063` (`messages.android_20260508_02_RC03.phone_dynamic`).
- Replaced stale obfuscated targets with evidence-matched runtime targets for:
  - Profile archived entry hiding
  - Search zero-state cleanup
  - Search category suppression
  - Search conversation/starred/contact filtering
  - Contact tap archived-only guard
  - Attachment/search result archive filtering
  - Archived selection-mode unarchive visibility and action signaling
  - Archive preserve + notification policy hooks
  - Archive intent account helper
- Updated resource ID constants for current build (`action_show_archived`, `action_unarchive`).
- Removed hard failures from legacy class probes; legacy misses now resolve as non-fatal unavailable candidates.

## Validation
- Built and installed production APK.
- Verified hook activation and runtime behavior through ADB/runtime logs on-device.
- Confirmed notification toggle behavior (archive mute / unarchive restore) with user validation.
