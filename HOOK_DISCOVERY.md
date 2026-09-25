# Semantic hook discovery

Starting with v1.1.29, CMReborn uses DexKit 2.3.0 to resolve hook targets from
the installed Google Messages APK during Application.attach. No network lookup
or APK upload occurs on the device.

## Resolution and safety

- Distinctive diagnostic strings identify the profile action, search collector,
  result adapters, contact handlers, archive API, and metadata operations.
- Current resource IDs, signatures, constructor shapes, and relationships between
  types narrow the search. The in-chat Unarchive provider must reference both
  action labels and invoke the discovered archive-status predicate.
- Every query must have one distinct result. Ambiguous matches are not chosen by
  position, and ambiguity disables literal-name fallback for the whole scan.
- Known-name fallbacks are restricted to the verified Messages version 322660063
  AND its exact base APK SHA-256. Historical names are not tried on unknown builds.
- Search view-data fields are mapped from adapter field reads; conversation row
  getters are resolved using conversation-ID and archive-status return types.
- Since v1.1.30, the archive trigger also discovers the account-intent writer,
  account type, search-fragment peer, and its unique account field. The input is
  found by its resource name. It never selects the first non-null field among
  historical letters: after Messages 323089063, the former account field holds
  an unrelated provider. Cache schema 3 includes these additional bindings.
- Android lifecycle hooks and background-work notification routing use Android
  API contracts and do not require obfuscated Messages class names.
- A completed-future adapter recognizes the host's listener signature even when
  its member name is obfuscated. Unknown future contracts are rejected before use.

## Cache

Resolved descriptors are saved atomically in Messages' private code cache.
The key includes the resolver schema, module version, Messages version code,
and SHA-256 of the base APK and every installed split. Cached classes, methods,
and fields are reflected against the current classloader before use.

An incomplete, stale, unreadable, or invalid cache triggers a new scan. Partial
scans are not persisted. The cache contains no conversations or contact data.

## Maintenance workflow

1. Verify the installed Messages version and APK digest.
2. Build debug with known-name hook fallbacks disabled:

   ```powershell
   ./gradlew.bat :app:testDebugUnitTest :app:assembleDebug '-Pcmreborn.discoveryOnly=true'
   ```

3. Install debug, restart Messages, and inspect resolver/hook logs. Confirm all
   semantic targets resolve, then restart again to check a warm-cache hit.
4. Exercise profile concealment, search exclusions, the hidden archive trigger,
   Unarchive visibility, archive preservation, and notification policy. Hook
   installation alone is not proof that all user-visible behavior works.
5. Decompile and update only the fingerprints or payload mappings that evidence
   shows have drifted. Bump the resolver schema when changing fingerprints.
6. Build/test/install production and publish using the normal evidence workflow.

Routine name reshuffling should no longer require a new release. This is not a
promise of universal future compatibility: Google can remove fingerprints,
change method semantics, remodel payloads, or change database/UI behavior. Some
contact/attachment payload helpers still retain historical accessors. A skipped
privacy hook may leave a Messages surface unfiltered; it is NOT a privacy-safe
fail-closed mode. Revalidate after substantive upstream changes. Production
compatibility failures are reported to the Xposed log without conversation data;
verbose runtime proof logs remain debug-only.

## Dependency notices

See `app/src/main/assets/licenses/NOTICE.txt` for DexKit source and license links.
License texts are included in both the repository and APK. No AADisplay code is
included.
