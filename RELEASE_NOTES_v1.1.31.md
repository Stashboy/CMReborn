# CMReborn v1.1.31

## Google Messages 324451063 offset refresh

Restores three hook groups that stopped resolving after the Google Messages
`324451063` update, which reformatted several diagnostic strings from internal
slash paths to short `Class#method` labels and moved the search account off the
fragment peer. The automatic resolver still handled the other 29 class-role
targets with no literal offset list; only these fingerprints drifted. The
resolver cache schema was bumped to 4.

- Hidden-archive `helloworld` trigger: the `ZeroStateSearchFragmentPeer` log
  string moved onto the fragment while the account field stayed on the peer, so
  the old string-based peer match found nothing and the trigger could not open
  the archive. The peer is now matched structurally as the only class holding
  both the resolved account type and a `ZeroStateSearchBox` field; the trigger
  peer and account fields are still selected by unique type.
- Query-stage archived search filter: the search-result collector lost its
  `IcingSearchApiImpl$1` label. It is now pinned by the unique error string
  `cannot get the id for result.`, with the success/failure callbacks resolved
  by shape as before.
- Archive notification policy: the archive API lost its slash-path string. It is
  now pinned by `ArchiveApiImpl#setStatusOnConversations` plus the set-status
  method shape.

## Verified target and evidence

- Google Messages: `324451063`,
  `messages.android_20261002_00_RC00.phone_dynamic`.
- Messages base APK SHA-256:
  `2F666C55F129DA84E1606D06E183EE08FAF2B560D5A53CB44835007B4A97B159`.
- Debug discovery-only scan (known-name fallbacks disabled) resolved all 37
  class-role targets: 717 ms cold scan, 91 ms warm cache hit on the test device.
  The previous build left discovery incomplete at 29 targets on this update.
- JVM unit tests passed (ImmediateFutureFactory, ResolutionPolicy, TriggerFields
  including the moved-account/non-null-old-field and ambiguous-binding cases).
- Debug logs confirmed 28 hooks installed with zero hook failures and zero
  unavailable targets, including the query-stage search filter, the archive
  notification toggle policy, and the restored trigger peer/account binding
  (`ZeroStateSearchBox.f` -> peer -> account field). The two "disabled" lines
  (exact-APK attachment query fallbacks and the overflow handler probe, which
  defers to the installed Activity guard) match the healthy prior baseline.
- New fingerprint targets were confirmed against the decompiled APK before use;
  each new string/shape resolves to exactly one class.
- Verification was log-only. No automated phone taps, typing, calls, or message
  sends were used. Full content-search coverage and live incoming-message/banner
  delivery were not re-exercised; hook installation is not a substitute for those
  behavior tests.

## Production APK

- Package: `dev.cmreborn`
- Version: `1.1.31` (33)
- Asset: `CMReborn-v1.1.31.apk`
- SHA-256: `CFAD5AA8B99ACAD0EEAA352AB369DCEFE5B709ABCA5DA0E8C86536CB639A8915`

Production is non-debuggable with verbose runtime proof logs disabled.
The signed APK was installed, its on-device digest matched the hash above, and
Messages launched without a new crash or runtime proof log. Messages was
force-stopped afterward.
