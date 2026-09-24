# CMReborn v1.1.29

## Automatic semantic hook discovery

- Adds DexKit-based runtime discovery for profile concealment, search/contact and
  attachment adapters, archive APIs and metadata paths, and Unarchive visibility.
- Resolves targets from diagnostic strings, live resource IDs, type signatures,
  constructor shapes, and method/field relationships instead of relying on a new
  literal class-name list for each Messages release.
- Requires unique matches. Exact-name fallback is limited to the verified
  Messages 322660063 base APK; ambiguous results never select the first match.
- Caches validated descriptors by resolver schema, module version, Messages
  version, and all installed APK digests. Stale/invalid caches are rescanned;
  partial scans are not persisted. No APK or conversation data is uploaded.
- Resolves conversation-row accessors by return type and handles the host's
  obfuscated future-listener method without a hardcoded helper name.
- Keeps the narrowly targeted background-work notification suppression and
  existing incoming-message notification policy.

This reduces maintenance for ordinary obfuscation changes; major Google UI,
database, payload, or behavioral changes can still require a module update.
An unresolved privacy hook does not guarantee concealment on an untested build.
See [HOOK_DISCOVERY.md](HOOK_DISCOVERY.md) for coverage and limitations.

## Verified target and evidence

- Google Messages: `322660063`,
  `messages.android_20260903_04_RC04.phone_dynamic`.
- Installed Messages base APK SHA-256:
  `30F8B15F2D27ECDADF23F10126ABD714E3BE857340DE17237E231A3AC32690E1`.
- Debug cold discovery resolved all 34 class-role targets with known-name hook
  fallbacks disabled: 770 ms. Subsequent validated cache hits: 83–94 ms on the
  test device. These timings are observations, not performance guarantees.
- Eight JVM tests passed, covering uniqueness, exact-APK fallback restrictions,
  ambiguity, cache-key invalidation, and renamed future-listener behavior.
- On-device logs confirmed current hook installation, profile concealment,
  search-category suppression, the archive trigger/account context/back route,
  in-chat KEEP_ARCHIVED action handling, and metadata archive preservation.
- Archived selection displayed an enabled Unarchive action with Archive hidden.
  No archive-state changes were deliberately made for testing.
- Background-work channel remains at importance 0. Release is non-debuggable,
  APK signature verified, installed successfully, and launched without a new
  crash or debug runtime log; Messages was force-stopped afterward.
- Full contact/attachment content searches and live incoming-message/banner
  delivery were not re-exercised in this migration run. Their target mapping and
  hook installation were checked; existing behavior is retained.

## APK

- Package: `dev.cmreborn`
- Version: `1.1.29` (31)
- Asset: `CMReborn-v1.1.29.apk`
- SHA-256: `0DC7B367083A934813539450B42277A38B8A81209F39C3E124C2B3295426098F`

DexKit 2.3.0 license notices and source links are included in the APK under
`assets/licenses/` and in this repository.
