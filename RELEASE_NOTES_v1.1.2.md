# CMReborn v1.1.2

Patch release.

## Implemented Functionality
- Added support for latest Google Messages update (offset/class remap refresh)
- Restored attachment/media archived-thread exclusion query-context hook path
- Expanded query builder fallback compatibility for newer internal SQL builder types
- Preserved prior behavior for archived menu concealment, hidden trigger, search/contact filtering, archive-preserve logic, and notification policy

## Compatibility Changes (Latest Google Messages)
- Search/attachment ops candidates expanded to include `clek`/`cldj`
- Search filter candidates expanded to include `clcp`
- Query builder base fallback expanded to include `eiok`
- Query clause base fallback expanded to include `eiol`
- Numeric where-clause candidate expanded to include `eihs`

## Simple Usage Instructions
- Install CMReborn v1.1.2 APK
- Enable module in LSPosed/Vector
- Hook to only Google Messages
- Open Google Messages > Tap Search > Type helloworld (that's your covert message box)
- Archiving any message thread puts it there, with muted notifications
- Unarchiving puts the thread back in Main inbox with notifications re-enabled
- Archive Tab hidden from Profile page
- Removed Search bar Suggestion Boxes
- Searching Archived message threads/contacts does not show up in Search

## Build Artifact
- `cmreborn-v1.1.2-release.apk`
- SHA-256: `299A9B409884B9D7A7176AC54B5675D74C4B819E8B6E65DBD3A2010CB265026C`
