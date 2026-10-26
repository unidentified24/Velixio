# Zuno data backups

Open Settings, then Your data, to export or import a `.zunodata` file using Android's file picker. No storage permission is required.

Version 1 is UTF-8 JSON with a `zunodata` format marker and version number. It contains saved-title metadata, up to 100 history records, up to 200 playback positions and supported appearance/playback preferences. Lists support up to 1,000 saved titles. Video files, stream URLs, authentication data, service configuration and runtime binaries are excluded. Poster links with query parameters are omitted from exports.

Imports are limited to 2 MiB with bounded nesting, record counts and field lengths. Only known fields and preferences survive validation. Validation finishes before the merge summary appears or preferences are written. Saved titles are merged by provider/kind/title identity; newer history is retained, with the most recent 100 entries kept. Imported playback positions and supported preferences replace matching values. Invalid and future-version backups are rejected. Backups are not encrypted and contain personal viewing metadata.

The app keeps its existing installation identity so updating to Zuno retains local data and working streaming initialization. The public app name, wordmark, launcher icon and default theme are Zuno.
