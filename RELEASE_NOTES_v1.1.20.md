# CMReborn v1.1.20

Release date: 2026-07-31

## Summary
Evidence-mapped maintenance update for Google Messages `317307063` (`messages.android_20260714_00_RC00.phone_dynamic`) from production baseline `v1.1.19`.

## Changelog
- Updated proven-drifted hook candidates for profile archived-entry hiding, zero-state search cleanup, search category suppression, query-stage search filtering, result adapters, contact filtering and tap handling, attachment result filtering, archived selection mode, archive preserve, notification policy, and archive account intent propagation.
- Added the current Messages immediate-future helper used by category suppression.
- Kept historical candidates behind current verified mappings for supported prior builds.

## Validation
- Pulled the installed Google Messages APK with ADB and compared the decompiled JADX output against CMReborn hook functionality.
- Verified debug-only CMReborn hook logs on-device for the updated profile, search, contact, attachment, archived selection, archive preserve, and notification policy hooks.
- Built and installed the production APK, then force-stopped Google Messages.
- Release APK is non-debuggable; runtime debug logs remain gated by `BuildConfig.DEBUG`.

## APK
- SHA-256: `83806A2C35F595184796738F93333DB3D39071DD708C0AD853778CBEB98A652F`
