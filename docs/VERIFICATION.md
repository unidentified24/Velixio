# Verification record — 2026-10-25

## Passed
- Native Android debug APK compilation and Android lint (zero errors; remaining warnings include English-only UI strings and newer dependency versions).
- 26 host-side stream/PiP contract checks: URL/header preservation, source immutability, invalid URL/header rejection, PiP auto-entry state and bounded video aspect ratios.
- 8 backend JVM unit checks: nested catalog parsing, duplicate editions, provider/content-kind identity, numeric IDs, oversized imports and rejection of imported loopback sources.
- 6 episode-selection regression tests: reordered collections, ID fallback, source changes, ambiguous identities and first/last boundaries.
- 6 Robolectric Android lifecycle tests on simulated API 28: PiP controller hiding/pause retention, stop/release, position/speed/paused restoration, saved-state precedence, malformed imports and visible/stopped source resolution.
- Live HTTPS session initialization and catalog calls to both original backends; newer search and short-drama catalog/detail calls. Normal session tokens were issued; no rewards or purchases were submitted.

## Not passed / not established
- No original-provider video has been decoded or played by Veyra. Public-fixture playback on a GitHub-hosted Android 11 emulator passed; this is separate from native-backend compatibility.
- The hardware emulator could not start because Windows Hypervisor Platform acceleration was unavailable. Headless software-emulation attempts, including graphics-disabled diagnostics, exited before Android boot completed. An official API 27 ARM64 image was checksum-verified locally; the installed Windows launcher explicitly rejected ARM64, and a direct ARM engine attempt also exited before boot. Windows features were not changed and no user preview was opened.
- Native initialization, movie-detail verification signatures, Original/Sub playback, quality parity and real-device PiP/decoder behavior therefore remain unverified. Simulated lifecycle checks are not evidence of original stream playback or native process-death recovery.
- One direct short-drama CDN sample request returned 403; the original native-adapter path has not yet been exercised. Do not infer a credit lock or successful playback from that result.
- The current private candidate uses the newer native engine for both provider configurations; older-engine compatibility and concurrent-provider behavior need testing.
- No complete catalog crawl, DRM, sidecar subtitle, casting or download parity verification has been performed.

## Required device matrix before semi-final delivery
Use ARM Android with a working decoder and both private profiles. Check a movie, multi-episode series, anime and short drama; both library editions where available; each real Original/Sub option; advertised renditions; subtitle/audio tracks; start/seek/source change/expiry; rotation; Home-to-PiP/manual PiP/play-pause/expand/close; process death; network interruption; and a genuine authorization denial. Compare the same item and source against its reference app.

The owner requires no preview or user testing request before semi-final acceptance. This integration candidate has not reached that checkpoint.

## Repository checks
The earlier GitHub Actions runs failed before compilation because setup-android requested the removed SDK package `tools`. The workflow now requests `platform-tools` explicitly. Local assemble, lint and all 46 checks pass. GitHub Actions run 40218698812 also passed for commit b0276f67e1804a6e276075d85590ed1403115ef0 after this repair. Lint currently reports zero errors and 15 warnings (English UI strings and newer dependency versions).

## Android emulator playback tests
`PlayerDeviceTest` uses the public Media3 Big Buck Bunny MP4 fixture to check actual first-frame decoding, seeking, manual PiP, Home-to-PiP, media-session pause/play, close/release and paused Home behavior on API 30 and API 35. All three tests passed on the GitHub-hosted Android 11/API 30 emulator in run 40219088230, commit a3db1411e9397cbf19e9f9d469bbdfdf5b4aea45. API 35 coverage is being added. These tests never initialize either private provider and cannot establish original-server/native-SDK or Original/Sub parity. No private assets, native binaries, sample catalog or test clips are committed as product inputs.
