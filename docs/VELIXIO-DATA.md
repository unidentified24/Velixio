# Velixio data backups

Open Settings, then Your data, to export or import a `.velixiodata` file using Android's file picker. No storage permission is required.

Version 1 is UTF-8 JSON with a `velixiodata` format marker and version number. It contains saved-title metadata, up to 100 history records, up to 200 playback positions and supported appearance/playback preferences. Lists support up to 1,000 saved titles. Video files, stream URLs, authentication data, service configuration and runtime binaries are excluded. Poster links with query parameters are omitted from exports.

Imports are limited to 2 MiB with bounded nesting, record counts and field lengths. Only known fields and preferences survive validation. Validation finishes before the merge summary appears or preferences are written. Saved titles are merged by provider/kind/title identity; newer history is retained, with the most recent 100 entries kept. Imported playback positions and supported preferences replace matching values. Invalid and future-version backups are rejected. Backups are not encrypted and contain personal viewing metadata.

The Android expansion adds an optional allowlisted `personal` object for collections, explicit discovery/watched flags and ratings/notes. Collections hold sanitized title metadata; unknown personal fields and malformed records are rejected. Manual exports and imports apply to the current local profile. Scheduled backups use the profile recorded when the backup folder was chosen. Appearance is device-wide. This version does not export all profiles in a single file or export Home shelf ordering.

The app keeps its existing installation identity so updating to Velixio retains local data and working streaming initialization. The public app name, wordmark, launcher icon and default theme are Velixio.

The second Android increment extends personal metadata with followed-series records and tracked watch time. Runtime download jobs, signed URLs, request headers and video cache remain excluded. Backup restore keeps the greater of the current/imported tracked watch-time total. Home layout, search/discovery preferences and external-caption offsets remain local preferences outside this backup version.
