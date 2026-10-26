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
The owner confirmed playback works in version 0.4.2 on their Android phone. This confirms the reported playback issue is resolved for their test; it is not a complete content, quality or source-parity audit.

Version 0.6.0 makes playback edge-to-edge with tap-to-show controls that hide together, landscape movie playback, a compact options menu, and optional fit/fill sizing. Search has a dedicated entry screen; Library has its own filters, and returning from a title restores the originating list position. Compact controls retain 48dp touch targets. See [interface review](docs/UX-REVIEW.md).

The working streaming adapters and private runtime inputs are preserved byte-for-byte. Private initialization values match Veyra's signing identity, and the companion runtime asset remains packaged locally. Native binaries and credentials remain outside Git.

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

