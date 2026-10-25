# Verification record — 2026-10-25

## Passed
- Native Android debug APK compilation and Android lint (zero errors; remaining warnings include English-only UI strings and newer dependency versions).
- 26 host-side stream/PiP contract checks: URL/header preservation, source immutability, invalid URL/header rejection, PiP auto-entry state and bounded video aspect ratios.
- 8 JVM unit checks: nested catalog parsing, duplicate editions, provider/content-kind identity, numeric IDs, oversized imports and rejection of imported loopback sources.
- Live HTTPS session initialization and catalog calls to both original backends; newer search and short-drama catalog/detail calls. Normal session tokens were issued; no rewards or purchases were submitted.

## Not passed / not established
- No video has been decoded or played by Veyra on an Android device in this environment.
- The hardware emulator could not start because Windows Hypervisor Platform acceleration was unavailable. A separate, headless software-emulation attempt exited before Android boot completed. Windows features were not changed and no user preview was opened.
- Native initialization, movie-detail verification signatures, Original/Sub playback, quality parity, PiP controls/transitions, seeking, background behavior and process-death restoration therefore remain unverified at runtime.
- One direct short-drama CDN sample request returned 403; the original native-adapter path has not yet been exercised. Do not infer a credit lock or successful playback from that result.
- The current private candidate uses the newer native engine for both provider configurations; older-engine compatibility and concurrent-provider behavior need testing.
- No complete catalog crawl, DRM, sidecar subtitle, casting or download parity verification has been performed.

## Required device matrix before semi-final delivery
Use ARM Android with a working decoder and both private profiles. Check a movie, multi-episode series, anime and short drama; both library editions where available; each real Original/Sub option; advertised renditions; subtitle/audio tracks; start/seek/source change/expiry; rotation; Home-to-PiP/manual PiP/play-pause/expand/close; process death; network interruption; and a genuine authorization denial. Compare the same item and source against its reference app.

The owner requires no preview or user testing request before semi-final acceptance. This integration candidate has not reached that checkpoint.
