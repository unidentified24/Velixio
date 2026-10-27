# Zuno

An Android streaming app with artwork-led discovery, a fullscreen player and a library you can take with you.

## Features
- Browse films, series, animation and shorts, with search across configured catalogs.
- Discover rotating featured picks and rating- and release-year-based collections.
- Save titles to My list and resume from Continue watching.
- Watch with quality, source, caption and picture-size controls, episode autoplay and picture-in-picture.
- Choose Cinema, Pacific, Golden hour or OLED black, with comfortable or compact poster layouts.
- Export and restore saved titles, viewing history and preferences using `.zunodata` backups.
- Manage appearance, playback and data through dedicated Settings pages.
- Download eligible streams and play completed copies offline; see [offline downloads and compatibility](docs/OFFLINE.md).

## Release track
0.10.0 is the major redesign review build. The next final milestone is 1.0.0, following device acceptance.

## Build
Use JDK 17, Android SDK 35 and Gradle 8.9:

```sh
./gradlew :app:assembleDebug
```

The source builds without service credentials. Live streaming requires local connection profiles in `private/assets/` and runtime libraries in `private/engines/jniLibs/`. These directories are ignored by Git. See the [connection contract](docs/BACKEND.md) for configuration and packaging requirements.

## Verification
```sh
./gradlew :app:lintDebug :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

See [verification](docs/VERIFICATION.md) for current results and device checks. The Android workflow can be run manually from GitHub Actions.

## Documentation
- [Interface behavior](docs/UX-REVIEW.md)
- [Data backups](docs/ZUNO-DATA.md)
- [Security](docs/SECURITY.md)
- [Release checklist](docs/ROADMAP.md)
- [Third-party notices](THIRD_PARTY_NOTICES.md)
