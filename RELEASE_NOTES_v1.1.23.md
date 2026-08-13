# CMReborn v1.1.23

Release date: 2026-08-13

## Summary
Evidence-mapped maintenance update for Google Messages `318930063` (`messages.android_20260731_00_RC00.phone_dynamic`) from production baseline `v1.1.22`.

## Changelog
- Updated proven-drifted hook candidates for profile archived-entry hiding, zero-state search cleanup, search category suppression, query-stage search filtering, result adapters, contact filtering and tap handling, attachment result filtering, archived selection mode, archive preservation, notification policy, and archive account-intent propagation.
- Updated the current immutable collection, archive status/reason, metadata-operation, and immediate-future mappings while retaining historical candidates for prior supported builds.
- Kept the narrowly scoped background-work notification suppression mapped to notification ID `174344743` and channel `bugle_broadcast_receiver_channel`; incoming-message notifications and the foreground service itself remain unaffected.

## Validation
- Pulled the installed Google Messages APK with ADB and compared the decompiled JADX output against CMReborn hook functionality.
- Verified all current hook mappings through debug-only CMReborn runtime logs on-device.
- Exercised the exact Messages internal foreground-service path with a debug-only probe and verified that the background-work notification was redirected to the blocked channel with no fatal exception; removed the probe before the production build.
- Verified the dedicated channel on-device at importance `0`, with banner, sound, vibration, badge, and lock-screen display disabled.
- Built and installed the production APK, then force-stopped Google Messages.
- Release APK is non-debuggable; runtime debug logs remain gated by `BuildConfig.DEBUG`.

## APK
- SHA-256: `C41975D0E25E8CC0B17DA755AFF70DB09311F6EB6B765D185B1F51354BFE79FE`
