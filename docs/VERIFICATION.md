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

## Version 0.4.1
A device reported S03 during protection-library startup. Static comparison found a missing JNI `exec` declaration and a field-modifier mismatch; both are corrected. Four regression checks cover the native declarations and sanitized failure classification. These checks do not load the private native libraries or establish successful playback. Runtime confirmation remains pending.

## Version 0.4.2
Native inspection identified an installed-app identity check before media initialization. Its package comparison rejects the previous private native profile; the profile was bound to a different package and certificate. The expected binding calculation was independently verified against both supplied SDK configurations before preparing Veyra-specific values locally. Server credentials and native binaries were not modified.

A companion protection data asset was missing and is now included only in private build assets. Verification on the final APK confirmed both native identity bindings against its actual signing certificate and package, exact companion-asset bytes, and unchanged native libraries for both ABIs.

Debug build, 24 automated tests and lint passed (no lint errors; 22 warnings). Physical-device decoding, authorization and full content parity remain unverified. The companion asset's role in resolving JNI startup remains to be confirmed at runtime.

## Owner playback confirmation
The owner reported that streaming works with version 0.4.2. This is physical-device user evidence, not an automated full-library or both-source parity result.

## Version 0.5.0 interface pass
Controls have reset theme minimum widths, smaller inset surfaces and 48dp touch targets. Title details have one primary Play action, compact selectors and batched episode rows. Filters are restored when returning from My Library. The player header is a single compact row. Final debug build, 24 automated regression tests and lint passed (zero lint errors, 20 warnings). Hash comparison confirmed all 27 streaming adapter/private-input files unchanged. The signed APK has both correct native identity bindings, the companion asset and unchanged native libraries. Visual acceptance and scrolling measurements on the phone remain pending.
