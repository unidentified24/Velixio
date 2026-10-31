# Verification

## Current development checkpoint: 1.1.0-rc1
- Debug APK assembly, device-test compilation and 84 JVM/lifecycle tests passed.
- Android debug lint: zero errors and 59 warnings.
- Packaged signature, installation package/version, native bindings, companion asset and all eight native-library integrity checks passed.
- Added tests for profile isolation, playback-profile pinning, private-history suppression, personal-record backup allowlisting/validation, Home ordering, protected cleanup, PiP touch-lock behavior, restored sleep timers and scheduled-backup profile binding.
- Physical-device playback, gestures, Wi-Fi transitions, notification behavior, document-provider behavior, large fonts/TalkBack and visual acceptance remain unverified. No Play Protect verdict is established.

This is the first increment of the Android expansion. See ANDROID-EXPANSION.md for feature limits and pending work.

## Previous checkpoint: 1.0.1-rc1
- Non-debuggable release APK assembly and device-test APK compilation passed. Device tests were not executed for this checkpoint.
- All 69 JVM/lifecycle tests passed.
- Android release lint reported zero errors and 45 warnings.
- Packaged APK signature, version, native identity bindings, companion asset and native-library integrity checks passed.
- Streaming adapter and runtime-input hashes match the verified baseline.

Additional coverage verifies update version ordering, repository/digest restrictions, stable-versus-candidate selection, daily backup scheduling, seven-copy retention, failed-write recovery, all eight theme exports and import of earlier backup formats. Future update installation, real folder-provider access and Play Protect acceptance remain phone checks.

Tests cover catalog contracts, episode selection, native declarations, playback lifecycle and fullscreen controls, backup validation and round-trips, poster save actions, discovery selection, settings navigation, history-clear confirmation, export intents and narrow-screen page restoration, artwork cache retention, bounded work queues and shelf adapter reuse.

## Device acceptance
Device instrumentation was compiled but not executed for this checkpoint. Physical-device visual, accessibility and frame-performance acceptance remains outstanding.

Before release, verify first-frame playback for each configured connection, Original/Sub variants, available quality levels, episode transitions, captions, seeking, rotation, PiP, network loss and process recreation. Review browsing, saving, backup/restore and settings with enlarged fonts and TalkBack.

Automated player fixtures establish player behavior; they do not establish live-service or complete content compatibility.

## Redesign and offline checks
Additional automated coverage verifies Fit/Fill/Stretch sizing, phone/tablet/wide-window column selection, private download record validation, cache-only reads and a synthetic HLS download with network-free manifest/segment reads. Real catalog downloads, airplane-mode playback, visual acceptance and MuMuPlayer/LDPlayer compatibility remain unverified. See OFFLINE.md for limitations.

New regressions cover completed-download navigation to cache-only playback, transfer-tab restoration, direct picture-size controls without stock settings, and recommendation exclusions without fallback resurrection.

The Continue Watching regression verifies portrait aspect ratio, FIT_CENTER artwork, visible progress, remaining time, exact episode resume and unclipped shelf height in comfortable and compact layouts.
