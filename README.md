# Veyra
A private Android rebuild with a combined catalog, credit-free interface and a modern Media3 player.

**Status: integration build, not a finished release.** Both original backends respond to normal session and catalog requests. Public-fixture decoding, seeking and PiP passed on an Android 11 emulator. Native playback authorization and actual original-provider quality/content parity remain unverified. No preview is being delivered before the semi-final checkpoint.

## Implemented
- Both original movie/TV catalog connections, combined browsing and search; overlapping titles retain their separate library editions.
- Newer app's separate short-drama browse, search and episode-detail adapter.
- Poster-grid discovery, Continue watching, pagination, saved titles and clear partial-library failure states. Title/source state is restored after rotation, and superseded library requests are cancelled.
- Episode/season and backend-provided source choices; previous/next episode controls use server ordering and stable episode identities. Source refresh retains playback position.
- Media3 HLS, DASH and MP4 playback, quality/audio/embedded-caption controls, HTTPS sidecar captions, speed controls and persistent resume positions. Settings include default quality, remembered speed and optional next-episode autoplay.
- Android picture-in-picture mini player: manual button, automatic entry while playing on supported devices, media-session play/pause and aspect-ratio preservation.
- No viewing-credit wallet, debit, reward-ad prerequisite, earned-time countdown or invented VIP/purchase state.
- HTTPS certificate/hostname verification, bounded inputs and responses, origin-scoped media headers, private local state and backup exclusions.

## Build and checks
Use JDK 17, Android SDK 35 and the included Gradle 8.9 wrapper:
```
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
javac -d build/contract-tests app/src/main/java/app/veyra/model/*.java tests/ContractTest.java
java -cp build/contract-tests ContractTest
```
Before the latest feature-completion pass, the local suite passed 26 host contract checks, 14 backend/episode unit tests and 6 simulated Android lifecycle tests. Lifecycle coverage includes PiP controller hiding, pause/stop, saved position/speed/paused state and source refresh while visible or stopped. These checks do not decode original media or exercise the native engine.

The manually triggered GitHub workflow runs `:app:connectedDebugAndroidTest` on an Android emulator using a public Media3 test clip. This is an internal player check; the fixture is not part of the library or a replacement content provider.

On Windows use `gradlew.bat`. Android Studio can open this folder. Dependencies remain pinned to the current development baseline (AGP 8.7.3 / Media3 1.8.1); release compatibility/dependency upgrades are still pending.

## Private runtime inputs
The source checkout builds without secrets. Live integration builds additionally use ignored `private/assets/backend.json`, `private/assets/backend-classic.json`, and `private/engines/jniLibs/<abi>/` supplied locally from the owner's APKs. These are already prepared in the owner's local workspace. They are not uploaded to GitHub. See [backend contract](docs/BACKEND.md).

The native engine is ARM-only. Each provider now uses its own original binary in a separate non-exported service process, with independent storage and port ownership. Bindings close after 30 seconds without a visible app/PiP window. JNI loading, package/signing identity and native lifecycle behavior still require Android runtime verification. This must not be presented as proven parity.

The product menu exposes Continue watching, My list and playback settings. The internal playback parser remains available for integration tooling. Example URLs are deliberately unplayable documentation; no sample provider replaces the original service.

## Verification and release
See [verification record](docs/VERIFICATION.md), [security review](docs/SECURITY.md), [credit-system trace](docs/CREDIT-SYSTEM.md) and [roadmap](docs/ROADMAP.md).
The repository remains private. No project-wide open-source license has been granted; third-party dependencies and the private native SDK retain their own licensing terms. See [third-party notes](THIRD_PARTY_NOTICES.md).

The owner requested implementation before further testing. Automatic push/PR test triggers are disabled for now. The new engine isolation, history, settings, grid and sidecar-caption changes have not yet been included in a final test pass.

Latest implementation packaging: `:app:assembleDebug` succeeded. Final unit/device/live-backend verification is deferred; the earlier test results do not cover this new feature set.
