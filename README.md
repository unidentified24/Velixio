# Veyra
A private Android rebuild with a combined catalog, credit-free interface and a modern Media3 player.

**Status: integration build, not a finished release.** Both original backends respond to normal session and catalog requests. Native playback authorization, actual video playback, PiP on a device and full quality/content parity remain unverified. No preview is being delivered before the semi-final checkpoint.

## Implemented
- Both original movie/TV catalog connections, combined browsing and search; overlapping titles retain their separate library editions.
- Newer app's separate short-drama browse, search and episode-detail adapter.
- Poster artwork, pagination, saved titles and clear partial-library failure states. Title/source state is restored after rotation, and superseded library requests are cancelled.
- Episode/season and backend-provided source choices; previous/next episode controls use server ordering and stable episode identities. Source refresh retains playback position.
- Media3 HLS, DASH and MP4 playback, track quality/audio/captions, speed controls and persistent resume positions.
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
The local suite currently passes 26 host contract checks, 14 backend/episode unit tests and 6 simulated Android lifecycle tests. Lifecycle coverage includes PiP controller hiding, pause/stop, saved position/speed/paused state and source refresh while visible or stopped. These checks do not decode original media or exercise the native engine.

On Windows use `gradlew.bat`. Android Studio can open this folder. Dependencies remain pinned to the current development baseline (AGP 8.7.3 / Media3 1.8.1); release compatibility/dependency upgrades are still pending.

## Private runtime inputs
The source checkout builds without secrets. Live integration builds additionally use ignored `private/assets/backend.json`, `private/assets/backend-classic.json`, and `private/jniLibs/<abi>/` supplied locally from the owner's APKs. These are already prepared in the owner's local workspace. They are not uploaded to GitHub. See [backend contract](docs/BACKEND.md).

The native engine is ARM-only. The current local candidate uses the newer APK's native engine with the recovered provider configurations. Compatibility with the older configuration, simultaneous provider use, package/signing identity and native lifecycle behavior require Android runtime testing. This must not be presented as proven parity.

An internal playback-file importer remains available in the menu. Example URLs are deliberately unplayable documentation; no sample provider replaces the original service.

## Verification and release
See [verification record](docs/VERIFICATION.md), [security review](docs/SECURITY.md), [credit-system trace](docs/CREDIT-SYSTEM.md) and [roadmap](docs/ROADMAP.md).
The repository remains private. No project-wide open-source license has been granted; third-party dependencies and the private native SDK retain their own licensing terms. See [third-party notes](THIRD_PARTY_NOTICES.md).
