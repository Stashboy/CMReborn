# CMReborn v1.1.24

Release date: 2026-08-25

## Summary
Evidence-mapped maintenance update for Google Messages `319227063` (`messages.android_20260803_03_RC02.phone_dynamic`) from production baseline `v1.1.23`.

## Changelog
- Updated proven-drifted hook candidates for the hidden Archived profile action, zero-state search cleanup, category suppression, query-stage filtering, result adapters, contact filtering and tap handling, attachment filtering, archived selection mode, archive preservation, and account-intent propagation.
- Updated the current immutable collection, archive status/reason, metadata-operation, and immediate-future mappings while retaining historical candidates for prior supported builds.
- Preserved the narrowly scoped background-work notification suppression for notification ID `174344743` and channel `bugle_broadcast_receiver_channel`; incoming-message notifications and the foreground service remain unaffected.

## Validation
- Pulled the installed Google Messages APK with ADB and compared its JADX output against CMReborn hook functionality.
- Verified all current hook mappings through debug-only CMReborn runtime logs on-device.
- Verified the dedicated background-work channel on-device at importance `0`, with banner, sound, vibration, badge, and lock-screen display disabled.
- Built and installed the production APK, then force-stopped Google Messages.
- Release APK is non-debuggable; runtime debug logs remain gated by `BuildConfig.DEBUG`.

## APK
- SHA-256: `86B8BB2370F03E9C20BEE8113A7C87F1080D2ADCCAD1B2607A51782202926246`
