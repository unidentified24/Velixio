# Third-party software
This repository currently has no project-wide open-source license.

New dependencies:
- AndroidX Media3 (ExoPlayer, HLS, DASH and UI), Apache License 2.0. Source: https://github.com/androidx/media
- Android Gradle Plugin and Android SDK tooling: retain their applicable upstream notices.
- Gradle wrapper/tooling: Apache License 2.0. Source: https://github.com/gradle/gradle

Before distributing binaries, generate an inventory of resolved direct and transitive dependencies and include all required notices and license texts.
This file is a development inventory, not a complete distribution notice bundle.

The original APKs contain additional commercial SDKs. Their analysis does not grant redistribution rights for those SDKs. No original DEX/native SDK binaries are part of this source repository.

Integration additions: Media3's OkHttp datasource, OkHttp and Okio retain their upstream Apache-2.0 licenses; JUnit 4 (EPL-1.0) and JSON-java (upstream license) are test-only dependencies. Resolve actual transitive versions and bundle upstream texts before distribution.

Private local integration builds include an owner-supplied ARM streaming engine recovered from the APK. It is excluded from Git and has not received an open-source redistribution grant through this project. The small `com.pp.hls` Java declaration is an ABI interface, not a copy of native implementation code.

Robolectric 4.16 and its test-only dependencies are used for simulated Android lifecycle regression tests; they are not packaged in the app. Robolectric is distributed under the MIT license. See https://github.com/robolectric/robolectric/blob/master/LICENSE.
