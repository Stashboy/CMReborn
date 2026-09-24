# CMReborn v1.1.28

Release date: 2026-09-23

## Summary
Evidence-mapped maintenance update for Google Messages `322660063` (`messages.android_20260903_04_RC04.phone_dynamic`) from production baseline `v1.1.27`.

## Changelog
- Updated proven-drifted hook candidates for the hidden Archived profile action, zero-state search cleanup, category suppression, query-stage filtering, result adapters, contact filtering and tap handling, attachment filtering, archive preservation, and account-intent propagation.
- Restored the in-conversation **Remove from archive** action by mapping the current `KEEP_ARCHIVED` action provider and the current archived-selection controller.
- Updated current immutable collection, archive status/reason, metadata-operation, immediate-future, and inspected resource-ID mappings while retaining historical candidates.
- Preserved the narrowly scoped background-work notification suppression for notification ID `174344743` and channel `bugle_broadcast_receiver_channel`; incoming-message notifications and the foreground service remain unaffected.

## Validation
- Pulled the installed Google Messages APK with ADB and compared its JADX output against CMReborn hook functionality.
- Verified current hook mappings through debug-only CMReborn runtime logs on-device.
- Verified the in-conversation and archived-selection Unarchive visibility paths against the current Google Messages implementation.
- Verified the dedicated background-work channel remains blocked without redirecting incoming-message channels.
- Built and installed the production APK, then force-stopped Google Messages.
- Release APK is non-debuggable; runtime debug logs remain gated by `BuildConfig.DEBUG`.

## APK
- SHA-256: `EA314AEF85BE464B76C86BFFC7DE585F6D3182F9893618AB77DE0089A78F22BA`
