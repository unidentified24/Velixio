# Release notes

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
- Update project naming and documentation for Zuno; remove obsolete development narrative.
- Preserve existing installation identity, playback configuration, backups and settings.

The 1.0.0 milestone follows device acceptance for playback, accessibility, browsing performance and backup/restore. This repository does not bundle service credentials or external runtime binaries.
