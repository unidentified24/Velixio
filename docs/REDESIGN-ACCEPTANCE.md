# Redesign acceptance — 1.0.0-rc1

| Request | Implementation / acceptance boundary |
| --- | --- |
| Full interface work | Home feature and mixed-size shelves; title pages without browsing chrome; artwork resume cards; separate My list; dedicated Downloads; recent searches; appearance previews; custom player controls. Physical-device visual acceptance is pending. |
| Three-line menu and logo | Hamburger navigation; generated ribbon-Z launcher/header mark. |
| Offline downloads | Ready and Transfers views, progress, local artwork when cached, delete/cancel, cache-only playback. One transfer at a time; no interrupted-transfer auto-resume or offline DRM licensing. Real-service airplane-mode test is pending. |
| All screens | Width-adaptive poster/episode/download grids, bounded large-window content and scrollable playback actions. Automated width checks cover phone/tablet/wide windows. This is not certification for every Android device. |
| MuMu / LDPlayer | ARM runtime translation must work in the emulator. Neither emulator has been validated; no x86 runtime is available. |
| Original/Sub/Dub and servers | Actual server version labels remain intact; versions and connections can be changed inside the player. |
| Episodes | Named episode cards on title pages and an in-player episode picker. |
| Synopsis | Expand/collapse remains contained within the About section. |
| PiP / rotate | Dedicated, accessibility-labeled icons; controls hide with the video controller. |
| Fit / Fill / Stretch | Direct cycling control plus automated resize-mode coverage. |
| Duplicate audio menu | Stock settings menu removed by replacing the controller layout. Version selection remains. |
| Seeking spinner | Buffer indicator delayed 800 ms. Sustained buffering still shows feedback. |
| Startup | Cached Home is immediately reusable; a placeholder appears without cached data. Server connection latency itself is not eliminated. |
| Continue / My list | Separate navigation destinations, with artwork-based resume cards. |
| Recommendations | More shelf variation, title exclusions, rotating features and catalog rating filters. Ratings are service-provided, not independently verified. |
| Themes / backups | Four palettes with previews, compact/comfortable layout and .velixiodata export/restore retained. |

The GitHub source publication remains pending the generated-logo upload review limitation reported with 0.11.0. Local builds include the logo.

## Final review correction
Continue Watching on Home uses portrait, uncropped posters with resume, progress and remaining time. Compact and comfortable sizing and exact-episode resume pass automated regression coverage. The separate Continue Watching page remains independent of Home shelf styling.
