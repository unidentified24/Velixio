# Release notes

## 1.0.1-rc2
- Add Sunset (aubergine and peach) and Midnight gold (ink blue and champagne).
- Preserve existing theme selections and include both new palettes in backup and restore.
- Increase version code to 18 for in-app update installation.


## 1.0.1-rc1
- Rename the app, repository, resources and backup exports to Velixio; retain installation identity, stored collections and earlier backup imports.
- Introduce a generated V/play logo with an adaptive launcher icon and a wordmark sized for the longer name.
- Add automatic daily release checks and verified in-app update downloads with Android-confirmed installation.
- Add daily folder backups with seven-copy retention, read-back validation and failure recovery.
- Expand to eight themes, coordinate player and browsing colors, and smooth download progress updates.
- Preserve GPL v3 and original-author attribution.


## 1.0.0-rc1
- Restore portrait cards for Continue Watching on Home. Show the complete poster without cropping it into a landscape frame.
- Add a dedicated resume control, progress bar and remaining-time label.
- Reserve enough shelf height for controls and text in comfortable and compact poster layouts.
- Preserve player, downloads, streaming configuration, backups and installation identity.

58 automated tests pass; lint reports zero errors and 39 warnings. Packaged signing/native checks pass. This remains a test-signed release candidate; physical-device and real-service offline acceptance are pending before 1.0.0.

## 0.12.0
- Replace the stock video controller with a custom seek/playback layout and direct version, quality, sizing, captions and episode actions. Remove the duplicate stock settings/audio menu.
- Select individual episodes inside playback while retaining the current stream if resolution fails.
- Add a Downloads navigation destination with Ready/Transfers views, artwork saved from the browsing cache, progress, storage totals, offline play and deletion.
- Introduce wide discovery cards, numbered highly rated cards and artwork-based Continue Watching cards. Keep excluded titles out even when the remaining discovery set is empty.
- Hide browsing chrome on title pages; add recent searches and visual theme previews.
- Preserve streaming inputs, normal authorization, cache-only offline playback and backup behavior.

57 automated tests pass. Lint reports zero errors and 39 warnings. Live offline playback, physical-device appearance/performance and emulator native playback still require acceptance.

## 0.11.0
- Replace the compact side-by-side feature with large cinematic artwork, gradient-backed titles and integrated actions.
- Rebuild title-page artwork into a full-width composition.
- Introduce a generated ribbon-Z logo in the launcher, header and library empty state.
- Add a midnight/coral visual identity, quiet category tabs, a rounded navigation dock and revised poster spacing.
- Restyle settings navigation and sheet controls.
- Preserve playback transport, downloads, library and backup behavior.

53 automated tests pass; lint has zero errors and 24 warnings. Physical-device visual acceptance remains outstanding.

## 0.10.0
- Redesign the wordmark, launcher icon and Cinema, Pacific and Golden hour themes.
- Add three-line navigation and styled action sheets, contained synopsis expansion and responsive episode cards.
- Separate Continue watching and My list navigation; add recommendation hiding and startup placeholders.
- Keep connection/version switching inside the player; add explicit rotate and picture-in-picture icons, Fit/Fill/Stretch and delayed buffering feedback.
- Add foreground downloads and cache-only offline playback with progress, cancellation and deletion.
- Adapt grids and reading widths to phone, tablet and wide windows.

This is a review build before 1.0.0. Automated checks pass; real-title offline playback and device/emulator acceptance remain outstanding. Downloads do not yet resume automatically after interruption or acquire offline DRM licenses.

## 0.9.0
- Prepare the final review milestone before 1.0.0.
- Reuse poster memory across screen recreation and avoid clearing unchanged artwork.
- Bound queued artwork requests to prioritize visible browsing work.
- Reuse Home shelf adapters and preserve horizontal positions when recycled.
- Reopen recent catalog views from a bounded cache while preserving incomplete-result notices.
- Update project naming and documentation for Velixio; remove obsolete development narrative.
- Preserve existing installation identity, playback configuration, backups and settings.

The 1.0.0 milestone follows device acceptance for playback, accessibility, browsing performance and backup/restore. This repository does not bundle service credentials or external runtime binaries.
