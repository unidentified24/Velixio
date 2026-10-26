# Verification

## Current checkpoint: 0.9.0
- Debug APK assembly and Android device-test APK compilation passed.
- All 45 JVM/lifecycle tests passed.
- Android lint reported zero errors and 18 warnings.
- Packaged APK signature, version, native identity bindings, companion asset and native-library integrity checks passed.
- Streaming adapter and runtime-input hashes match the verified baseline.

Tests cover catalog contracts, episode selection, native declarations, playback lifecycle and fullscreen controls, backup validation and round-trips, poster save actions, discovery selection, settings navigation, history-clear confirmation, export intents and narrow-screen page restoration, artwork cache retention, bounded work queues and shelf adapter reuse.

## Device acceptance
Device instrumentation was compiled but not executed for this checkpoint. Physical-device visual, accessibility and frame-performance acceptance remains outstanding.

Before release, verify first-frame playback for each configured connection, Original/Sub variants, available quality levels, episode transitions, captions, seeking, rotation, PiP, network loss and process recreation. Review browsing, saving, backup/restore and settings with enlarged fonts and TalkBack.

Automated player fixtures establish player behavior; they do not establish live-service or complete content compatibility.
