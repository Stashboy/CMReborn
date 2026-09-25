# CMReborn v1.1.30

## Archive trigger fix

Fixes `helloworld` not opening the hidden archive after the Google Messages
323089063 update. The search peer's account field moved from `o` to `n`; `o`
became an unrelated, non-null provider. The old first-non-null lookup selected
that provider and could not populate the archive intent.

The trigger now discovers the account-intent writer, infers the actual account
type, and requires a unique compatible search-peer/account field binding. Its
text input is located by resource name, not an obfuscated field letter. Missing
or ambiguous bindings do not fall back to arbitrary objects. The resolver cache
schema was bumped to 3.

The existing automatic resolver handled the other 34 class-role targets in this
Messages update. No replacement literal offset list was needed.

## Verified target and evidence

- Google Messages: `323089063`,
  `messages.android_20260910_03_RC02.phone_dynamic`.
- Messages base APK SHA-256:
  `F82AF12415DEC1A2CBDB9E81881B37A864AAE1A9BFD206A1A2DC624AF25BE627`.
- All 37 class-role targets resolved with known-name hook fallbacks disabled:
  774 ms cold scan; 87 ms subsequent cache hit on the test device.
- Twelve JVM tests passed, including the moved-account/non-null-old-field
  regression, arbitrary field renaming, and missing/ambiguous binding rejection.
- Debug logs confirmed the named input watcher attached, account resolved by
  validated types, trigger detected, archive intent populated, and archive
  screen opened following a user-entered trigger.
- Other profile, search/contact, attachment, archive-preservation, Unarchive,
  and notification-policy hooks installed without reported hook failures.
  Profile concealment and search-category suppression fired in the logs.
- Background-work suppression channel remains at importance 0. This update does
  not change incoming-message notification or archive-state policies.
- No automated phone taps, typing, calls, or message sends were used. Full
  content-search coverage and live incoming-message/banner delivery were not
  re-exercised; hook installation is not a substitute for those behavior tests.

## Production APK

- Package: `dev.cmreborn`
- Version: `1.1.30` (32)
- Asset: `CMReborn-v1.1.30.apk`
- SHA-256: `D9AB91FC9C86E879B369CE010F2CC4A0618CB186F6C2F6F73B73CFDF6EFB8A7A`

Production is non-debuggable with verbose runtime proof logs disabled.
The signed APK was installed, its on-device digest matched the hash above, and
Messages launched without a new crash or runtime proof log. Messages was
force-stopped afterward. The user also confirmed that the trigger works again.
