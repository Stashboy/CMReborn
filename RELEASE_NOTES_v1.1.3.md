# CMReborn v1.1.3

Production release for latest Google Messages compatibility + search UX stability.

## Changelog
- Updated core hook mapping and runtime attachment points for the latest Google Messages build.
- Fixed profile archived action removal at the data-model layer (not only action interception).
- Fixed zero-state category/suggestion suppression against the current `dqko` + `dqjq` pipeline.
- Added explicit Zero-State Search keyboard auto-focus/show enforcement on activity resume.
- Preserved prior core archive workflow paths:
  - covert `helloworld` trigger
  - archive/back-navigation routing
  - archive-preserve and explicit unarchive handling
  - archived-thread and archived-only identity filtering
  - archive notification policy hooks

## Validation Evidence
- Runtime hook install confirms all targeted core hooks are active for this build.
- Runtime log confirms category suppression, renderer suppression, and keyboard auto-show execution.
- Runtime log contains no CMReborn hook errors in verification run.

## Build Artifact
- `cmreborn-v1.1.3-release.apk`
- SHA-256: `5E070B1D0FEEED0374599878CDCD075A15167B65BA91CD99DEBB5275344B724F`
