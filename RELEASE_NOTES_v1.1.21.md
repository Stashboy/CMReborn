# CMReborn v1.1.21

Release date: 2026-08-04

## Summary
Evidence-mapped maintenance update for Google Messages `317865063` (`messages.android_20260720_05_RC00.phone_dynamic`) from production baseline `v1.1.20`.

## Changelog
- Updated proven-drifted hook candidates for profile archived-entry hiding, zero-state search cleanup, search category suppression, query-stage search filtering, result adapters, contact filtering and tap handling, attachment result filtering, archived selection mode, archive preserve, notification policy, and archive account intent propagation.
- Updated the current Messages unarchive resource fallback, search account field, and immediate-future helper.
- Kept historical candidates behind current verified mappings for supported prior builds.

## Validation
- Pulled the installed Google Messages APK with ADB and compared the decompiled JADX output against CMReborn hook functionality.
- Verified debug-only CMReborn runtime logs on-device for hook installation, archived search-result removal, search suggestion suppression, trigger-only Archived launch, account propagation, and back rerouting.
- Built and installed the production APK, then force-stopped Google Messages.
- Release APK is non-debuggable; runtime debug logs remain gated by `BuildConfig.DEBUG`.

## APK
- SHA-256: `035E822AAE78348CE2C11E983BE9ED0F3E567016BCBAAA10A0093F614CFBA71E`
