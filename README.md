# CM Reborn

LSPosed/Xposed module for Google Messages (`com.google.android.apps.messaging`) that turns the archive surface into a covert/private-thread workflow.

## Feature List

- Archived menu entry concealment (overflow + profile)
- Hidden archive trigger (`helloworld`)
- Archive back-navigation reroute to inbox
- Search zero-state cleanup
- Search archived-thread exclusion
- Search archived-only contact identity exclusion
- Contact tap/selection guard for archived-only identities
- Attachment/media archived-thread exclusion
- Archive-state preservation (`KEEP_ARCHIVED` path)
- Archived-folder unarchive action enforcement
- Manual unarchive signaling path
- Conversation notification toggle on archive/unarchive
- Conversation-channel migration fallback for notification restoration

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

## Backend Function Map

| Functionality | Backend implementation (high level) |
|---|---|
| Archived menu concealment | Hooks overflow handler + profile action builder and disables archived actions before execution/render. |
| Hidden archive trigger | Watches search-box input and opens `ArchivedActivity` when the trigger token matches exactly. |
| Back-navigation reroute | Marks trigger-opened archive sessions and reroutes `onBackPressed` to inbox activity. |
| Search zero-state cleanup | Hides search suggestion containers and suppresses category suggestions in zero-state. |
| Archived-thread search exclusion | Injects archive exclusion clauses and filters conversation result sets before display. |
| Archived-only identity exclusion | Resolves participant/contact identity keys and removes rows whose linked threads are archived-only. |
| Contact interaction guard | Adds fallback guards in contact selection/tap dispatch to block archived-only identity openings if UI filters miss. |
| Attachment/media exclusion | Tracks attachment query builders and applies archive exclusion clauses to media/link queries. |
| Archive-state preservation | Intercepts metadata refresh paths and rewrites `UNARCHIVED -> KEEP_ARCHIVED` when the action was not an explicit user unarchive. |
| Archived action-mode enforcement | Forces visible `Unarchive` and hides conflicting archive action in archived-folder selection mode. |
| Manual unarchive signaling | Marks explicit user unarchive action scope to bypass archive-preserve rewrite logic. |
| Conversation notification policy | On archive/unarchive hooks, updates conversation notification flag and conversation-channel importance state. |
| Channel restoration fallback | If channel importance cannot be restored on reused channel IDs, migrates to a fresh conversation channel ID and rebinds conversation metadata. |
| Runtime stability hardening | Uses bounded caches and safe context handling to reduce long-uptime drift and memory-retention risk. |

## Build

Debug:

```powershell
./gradlew.bat :app:assembleDebug
```

Release:

```powershell
./gradlew.bat :app:assembleRelease
```

## Install

```powershell
adb install -r app/build/outputs/apk/release/app-release.apk
adb reboot
```
