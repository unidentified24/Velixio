# Veyra

An Android streaming app built around a quiet, artwork-led interface and a capable video player.

## Experience
- Home with a featured title and a recycled poster grid; dedicated Search and My Library navigation.
- Films, series, animation and shorts, with independent catalog sessions and clear partial-result errors.
- Saved titles, continue watching, seasons, episode selection and server-provided Original/Sub choices.
- Media3 playback with quality, audio, captions, speed, resume and optional next-episode autoplay.
- Android picture-in-picture, including playback controls and video aspect-ratio preservation.
- No wallet, viewing-credit countdown, reward-ad requirement or balance deduction.

## Development status
Version 0.4.2 is in device testing. It builds successfully; 24 backend, episode, native-interface and Android lifecycle tests pass, and lint reports no errors. Live playback is **not yet verified**.

Device reports isolated an initialization return of -1 on one connection and protection-library startup failure on the other. Native inspection found an app-identity mismatch; the private configurations now match Veyra's own package and signing certificate. A missing companion protection asset has also been restored to private build inputs. These are verified packaging/configuration corrections, but actual video playback remains **unverified** until the updated build runs on an ARM device.

Home now presents highly rated titles, recent releases and discovery rows from movie and series metadata. This is metadata-based ranking, not a personalized recommendation claim. Catalog connections load concurrently; a bounded app-private metadata preview appears on repeat launches while fresh results load. Title taps resolve a connection automatically and try the next matching edition after a failure. Original/Sub choices remain available separately.

The header uses a custom vector wordmark and bottom navigation has distinct icons and selection states. No substitute media library is bundled.

## Build
Use JDK 17, Android SDK 35 and Gradle 8.9:
```
./gradlew :app:assembleDebug
```
The repository builds without credentials. Live access requires locally supplied profiles in ignored `private/assets/` and runtime libraries in ignored `private/engines/jniLibs/`. Never commit these inputs. See [connection contract](docs/BACKEND.md).

## Verification
```
./gradlew :app:lintDebug :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```
Device tests use a public player fixture and do not establish live-service compatibility. See [verification](docs/VERIFICATION.md) and [release work](docs/ROADMAP.md). CI is manually triggered during development.

This repository is private. No project-wide open-source license has been granted. Dependency and private runtime terms remain applicable; see [third-party notices](THIRD_PARTY_NOTICES.md).

The 0.4.1 patch restores a native method declaration required by the integration ABI and the protection interface's mutable static map. Startup errors now include a sanitized failure category and preserve both connection attempts. A Copy details action includes Android version, supported ABIs and memory page size, without native exception text, credentials, stream URLs or private paths. The reported S03 failure still needs device verification; this is not a confirmed playback fix.
