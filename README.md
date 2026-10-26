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
Version 0.3 is in device testing. The APK builds, all 46 automated checks pass, and Android lint reports no errors. Catalog access has been exercised, but live streaming is **not yet verified**. A device report identified a failure while starting the native streaming service. The startup sequence has been corrected to load the protection library only for the connection that requires it; safe stage codes distinguish remaining errors. This change still requires ARM-device verification.

The interface now recycles poster views instead of creating every card at once. Playback menus are compact and browsing categories belong to Veyra. Artwork and titles come from the configured catalog; no substitute media library is bundled.

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
