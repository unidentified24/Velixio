# Veyra implementation roadmap

## Implemented and checked in code
- [x] APK comparison and client credit-system trace.
- [x] Both movie/TV catalog protocols, separate provider sessions, combined browse/search and editions.
- [x] Newer short-drama catalog, search and episode-detail mapping.
- [x] Media3 player, source refresh, resume, quality/audio/embedded-caption controls and identity-safe previous/next episode navigation.
- [x] Android PiP mini-player and media-session play/pause controls.
- [x] Poster-grid discovery, Continue watching, saved titles, pagination, partial-backend errors and bounded input handling.
- [x] Optional next-episode autoplay, remembered speed, default quality and HTTPS sidecar-caption wiring.
- [x] Separate per-provider native engine services, original binaries and idle cleanup (awaiting final verification).
- [x] Private native SDK wrapper/resource candidate; no secrets or SDK binaries committed.
- [x] Debug build, lint, contract/backend/episode checks and six simulated Android lifecycle regressions.

## Required before semi-final acceptance
- [ ] Working ARM Android runtime: current hardware emulator cannot start, software emulation exited before boot, and this Windows launcher rejects the downloaded ARM64 image.
- [ ] Confirm native SDK load/verify/resource paths with Veyra's package/signing identity.
- [ ] Verify both original engines, provider switching, service reconnection, native background activity and cleanup.
- [ ] Play representative movies, TV episodes, anime and short dramas from both libraries.
- [ ] Verify actual Original/Sub choices, exact streams/renditions, audio and embedded/sidecar captions.
- [ ] Device test PiP entry/exit/play-pause/close, seeking, rotation, process death, expiry and network recovery.
- [ ] Exercise zero-credit/expired legacy states without client gates; surface genuine server authorization failures.
- [ ] Resolve any server/native compatibility failure. No assumed success from catalog URLs or APK compilation.

## Release
- [ ] Review/update target SDK and dependencies; complete accessibility and device compatibility checks.
- [ ] Generate complete dependency/license inventory and obtain native redistribution clearance.
- [ ] Select license for owned source and prepare private release signing.
- [ ] Publish or change repository visibility only on explicit owner request.

Only the two APKs are reconstruction inputs; do not request backend source or secrets. No user previews or testing requests before semi-final acceptance. Full streaming parity is not yet established.
