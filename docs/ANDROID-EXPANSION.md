# Android expansion

This is the first development increment for the requested feature set, not completion of the entire roadmap. Cross-device accounts/sync and the iPhone website are deferred at the owner's request. Source changes are being prepared on a development branch; existing public releases are not replaced automatically.

## Implemented in the first increment

| Request | Current behavior / limits |
| --- | --- |
| Update popup | Version, bounded release notes, asset size, Update and Remind me tomorrow. Automatic checks remain once daily while the app is used. |
| What's new | Dedicated screen shown once per new installed version, also available in Settings. |
| Storage manager | Separately measures offline videos, other app files, temporary cache, runtime files, external files and update packages. Cleanup removes only expired updater APKs. Completed downloads and native runtime files are protected. |
| Wi-Fi / download limit | Optional Wi-Fi requirement and 2–32 GB total download budget, checked before and during transfers. A transfer stops when the policy is exceeded; this is not a resumable queue yet. |
| Backup status | Existing status/failure page retained. Backups now bind to the profile selected during folder setup. Manual exports use the current profile. This does not back up every profile at once. |
| Personal profiles | Up to eight local profiles. Separate saved titles, history, resume positions, searches, collections, feedback and Home layout. Default profile retains original storage. Playback stays pinned to the profile that started it. Appearance and downloaded videos remain shared. Profiles are not authenticated accounts or a privacy lock. |
| Recommendations | Explicit Not interested removes a title from Home discovery. Existing rating/year selection remains; a learned genre recommendation model is still pending. |
| Collections | Create named collections, add titles from their detail page, open titles and remove entries. Personal collections, ratings and notes are validated and included in metadata backups. |
| Player gestures | Double tap left/right to seek 10 seconds; vertical swipe left for window brightness or right for media volume. Gestures operate while controls are hidden. |
| Control lock | Touch lock with an accessible unlock button; hidden in PiP. |
| Sleep timer | 15/30/60 minutes or end of episode; timer survives Activity recreation. |
| Episode countdown | Ten-second countdown with Play now / Cancel, only when metadata confirms a next episode. Cancels when the player stops. |
| Captions | Standard/large/extra-large and white/yellow on black. Timing offsets and more styles remain pending. |
| Home layout | Per-profile show/hide and shelf ordering, plus featured visibility. |
| Playback recovery | Direct retry and version switching on player errors, with controls kept visible. |
| Notifications | Update-prompt preference, private download title notices and link to Android notification controls. Foreground download notification remains required by Android. Episode reminder controls await the reminder feature. |
| Accessibility | Larger app labels, higher contrast secondary text, reduced launch motion, existing Android font scaling and control labels. Physical TalkBack/large-font acceptance remains necessary. |
| Offline entry | When no Internet-capable network is present and completed downloads exist, launch Downloads directly. Existing cache-only playback remains. |
| Search | Debounced title results while typing, with submitted searches retained. Dedicated genre/year/rating/language filters remain pending. |
| Watched controls | Mark watched/unwatched, clear matching resume entries, remove individual Continue Watching cards. |
| Personal ratings | Store 0–10 rating and a note up to 500 characters per title/profile. |
| Launcher shortcuts | Search and Downloads shortcuts. |
| Viewing statistics | Saved progress minutes, retained completed history, manually watched count, saved titles and collections. Does not claim to measure total watch time. |
| Privacy | Pause history/resume recording per profile and hide titles in download notifications. |
| Feedback | Reviewable text report, optional displayed app/Android/device details, then Android's chooser. No background upload. |

## Still to build

1. Durable download queue with pause, resume, retry, reordering and fresh stream resolution. Signed/local proxy URLs must not be treated as permanent queue identities.
2. Full search filters and improved preference-driven recommendations.
3. More subtitle styles and timing offsets.
4. New-episode following/reminders, subject to reliable catalog availability data.
5. Licensed trailers and compatible TV casting. The current localhost-only streaming proxy cannot be sent directly to a TV.
6. Android TV layout, remote focus navigation, tablet/foldable-specific layouts and device verification.
7. Home-screen widget.
8. Travel verification of complete, readable offline cache resources.
9. Audio dialogue/normalization controls after decoder/device compatibility checks.
10. All-profile backup/restore, broader notification controls and refined profile management.

The website will require a browser-compatible streaming endpoint or authorized gateway; the existing native Android proxy cannot run in Safari. No credentials or streaming runtime binaries will be placed in web source.

## Acceptance

Automated checks and package integrity are recorded in VERIFICATION.md. Real-device playback, gesture feel, TV/casting, Wi-Fi transitions, storage measurements and document-provider behavior must be reported separately. No claim is made that every requested feature is finished or that Play Protect accepts the build.
