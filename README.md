# Zuno

An Android streaming app with artwork-led discovery, a fullscreen player and a library you can take with you.

## Downloads
[Download APKs from GitHub Releases](https://github.com/unidentified24/Zuno/releases).

Choose the newest release candidate for testing; older APKs are retained as historical builds.

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


## License and Attribution

Zuno is an open-source Android application developed by [unidentified24](https://github.com/unidentified24).

Copyright © 2026 unidentified24.

Licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.

You may fork, modify, and redistribute Zuno under the terms of GPL v3.

When redistributing Zuno, you must preserve applicable copyright and license notices. Modified versions must also identify their changes as required by GPL v3.

Do not falsely claim authorship of the original Zuno source code.

Original project: https://github.com/unidentified24/Zuno
