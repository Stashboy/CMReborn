# CMReborn v1.1.27

Release date: 2026-09-20

## Summary
Evidence-mapped maintenance update for Google Messages `322070063` (`messages.android_20260827_01_RC07.phone_dynamic`) from production baseline `v1.1.26`.

## Changelog
- Updated proven-drifted hook candidates for the hidden Archived profile action, zero-state search cleanup, category suppression, query-stage filtering, result adapters, contact filtering and tap handling, attachment filtering, archive preservation, and account-intent propagation.
- Updated current search-row archive status/conversation ID accessors and media, link, and location attachment identity paths while retaining prior-build fallbacks.
- Adapted archived-folder Unarchive visibility to the current action-provider path, which replaced the prior `ActionMode.Callback` controller.
- Updated current immutable collection, archive status/reason, metadata-operation, immediate-future, and inspected resource-ID mappings while retaining historical candidates.
- Preserved the narrowly scoped background-work notification suppression for notification ID `174344743` and channel `bugle_broadcast_receiver_channel`; incoming-message notifications and the foreground service remain unaffected.

## Validation
- Pulled the installed Google Messages APK with ADB and compared its JADX output against CMReborn hook functionality.
- Verified current hook mappings through debug-only CMReborn runtime logs on-device.
- Verified the dedicated background-work channel remains blocked without redirecting incoming-message channels.
- Built and installed the production APK, then force-stopped Google Messages.
- Release APK is non-debuggable; runtime debug logs remain gated by `BuildConfig.DEBUG`.

## APK
- SHA-256: `7F8203A416279369AF50422B13E3DB5AF47B2A71ABCCA19B3C76D7F15CD5B7A6`
