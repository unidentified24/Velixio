# Verification record

Before the current redesign, 26 host contract checks, 14 backend/episode tests and six simulated Android lifecycle tests passed. Three Android 11 emulator tests exercised decoding a public player fixture, seeking, picture-in-picture and lifecycle release. Those results do not cover the current native startup or UI changes.

A test-device report for version 0.2 identified a streaming-service startup error before title playback. Version 0.3 removes unnecessary protection-library loading on one connection and reports safe startup stages. Runtime confirmation remains pending.

The current interface uses RecyclerView for poster collections. Live catalog content is retained; no public fixture is substituted into Home or Search.

Release checks must distinguish compilation, automated player tests and actual ARM-device streaming. Both configured connections, Original/Sub variants and available quality levels require real media checks. A complete content-parity claim cannot be based on a small sample.

## Version 0.3 checkpoint — October 26, 2026

- Debug APK assembled successfully.
- 26 host contract checks and 20 JVM/Android lifecycle tests passed (46 total).
- Android lint completed with no errors; 18 warnings concern dependency versions and localization.
- No Android device was connected. Live streaming, ARM engine startup and visual/performance acceptance remain pending the owner-installed APK check.

These results support packaging and regression confidence, not a claim that all content streams successfully.

## Version 0.4 checkpoint — October 26, 2026
- Debug build, 20 automated JVM/lifecycle tests and lint completed successfully.
- Lint has 22 warnings, including dependency/localization notices, custom-view notices and a dynamic-library-load warning. The dynamic load is restricted to packaged libraries copied into app-private storage, not downloaded code.
- Physical-device reports for 0.3 were S07 and S09. The initializer's path arguments and loaded basenames have changed; successful real-media playback is not established.
- Cold-start timing, cached-start timing and visual acceptance remain unmeasured on a phone.
