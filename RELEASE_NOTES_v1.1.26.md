# CMReborn v1.1.26

Release date: 2026-09-07

## Summary
Evidence-mapped maintenance update for Google Messages `321562063` (`messages.android_20260820_02_RC05.phone_dynamic`) from production baseline `v1.1.25`.

## Changelog
- Updated proven-drifted hook candidates for the hidden Archived profile action, zero-state search cleanup, category suppression, query-stage filtering, result adapters, contact filtering and tap handling, attachment filtering, archived selection mode, archive preservation, and account-intent propagation.
- Restored archived-only contact exclusion in normal Messages search by mapping the current `ContactsAdapter#updateResults` path; contact identity checks remain fail-closed and retain mixed visible/archived identities.
- Updated the current immutable collection, archive status/reason, metadata-operation, and immediate-future mappings while retaining historical candidates for prior supported builds.
- Preserved the narrowly scoped background-work notification suppression for notification ID `174344743` and channel `bugle_broadcast_receiver_channel`; incoming-message notifications and the foreground service remain unaffected.

## Validation
- Pulled the installed Google Messages APK with ADB and compared its JADX output against CMReborn hook functionality.
- Verified all current hook mappings through debug-only CMReborn runtime logs on-device.
- Verified the archived-only contact filter executes on the normal search contact-results path without exposing contact data in logs.
- Verified the dedicated background-work channel on-device at importance `0`, with banner, sound, vibration, badge, and lock-screen display disabled.
- Built and installed the production APK, then force-stopped Google Messages.
- Release APK is non-debuggable; runtime debug logs remain gated by `BuildConfig.DEBUG`.

## APK
- SHA-256: `ABA8B2D1ADAC84EA368CC16337FD24BCBA3624445BBE54541F68755F0D07F12E`
