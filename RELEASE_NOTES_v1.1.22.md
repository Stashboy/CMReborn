# CMReborn v1.1.22

Release date: 2026-08-13

## Summary
Evidence-mapped maintenance update for Google Messages `318719063` (`messages.android_20260727_03_RC00.phone_dynamic`) from production baseline `v1.1.21`.

## Changelog
- Updated proven-drifted hook candidates for profile archived-entry hiding, zero-state search cleanup, search category suppression, query-stage search filtering, result adapters, contact filtering and tap handling, attachment result filtering, archived selection mode, archive preservation, notification policy, and archive account-intent propagation.
- Updated the current immutable collection, archive status/reason, metadata-operation, and immediate-future mappings while retaining historical candidates for prior supported builds.
- Suppressed only Google Messages' transient `Messages is doing work in the background` foreground-service notification by redirecting notification ID `174344743` from `bugle_broadcast_receiver_channel` to a dedicated blocked channel. The foreground service itself is still started so background work is not interrupted.

## Validation
- Pulled the installed Google Messages APK with ADB and compared the decompiled JADX output against CMReborn hook functionality.
- Verified all current hook mappings through debug-only CMReborn runtime logs on-device.
- Exercised the exact Messages internal foreground-service path with a debug-only probe and verified that the notification was redirected to the blocked channel without a process crash; removed the probe before the production build.
- Verified the dedicated channel on-device at importance `0`, with banner, sound, vibration, badge, and lock-screen display disabled.
- Built and installed the production APK, then force-stopped Google Messages.
- Release APK is non-debuggable; runtime debug logs remain gated by `BuildConfig.DEBUG`.

## APK
- SHA-256: `17DE461A566DB48DB5B8B852A39A242CDA6515EB1A9F06AE70073533D6DDDD08`
