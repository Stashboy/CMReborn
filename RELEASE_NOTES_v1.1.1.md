# CMReborn v1.1.1

Patch release.

## Implemented Functionality
- Archived menu entry concealment (overflow + profile surfaces)
- Hidden archive trigger (`helloworld`)
- Archive back-navigation reroute to inbox
- Search zero-state cleanup
- Search archived-thread exclusion
- Search archived-only contact identity exclusion
- Contact tap/selection guard for archived-only identities
- Attachment/media archived-thread exclusion
- Archive-state preservation (`KEEP_ARCHIVED` rewrite path)
- Archived-folder action-mode unarchive enforcement
- Explicit manual-unarchive signaling path
- Conversation notification toggle on archive/unarchive
- Conversation-channel migration fallback for notification restoration
- Conversation-channel disable fallback for forced notification-off state
- Runtime stability hardening (cache bounds + safe context handling)

## Simple Usage Instructions
- Install CMReborn v1.1.1 APK
- Enable module in LSPosed/Vector
- Hook to only Google Messages
- Open Google Messages > Tap Search > Type helloworld (that's your covert message box)
- Archiving any message thread puts it there, with muted notifications
- Unarchiving puts the thread back in Main inbox with notifications re-enabled
- Archive Tab hidden from Profile page
- Removed Search bar Suggestion Boxes
- Searching Archived message threads/contacts does not show up in Search

## Build Artifact
- `cmreborn-v1.1.1-release.apk`
- SHA-256: `15B247069A1CE999DC90A3A33629D1B04A4BF6AE5313AD7018F1E2CE8218B75E`
