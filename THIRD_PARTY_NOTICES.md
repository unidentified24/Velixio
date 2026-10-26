# Third-party software
This repository currently has no project-wide open-source license.

Dependencies:
- AndroidX Media3 (ExoPlayer, HLS, DASH and UI), Apache License 2.0. Source: https://github.com/androidx/media
- Android Gradle Plugin and Android SDK tooling: retain their applicable upstream notices.
- Gradle wrapper/tooling: Apache License 2.0. Source: https://github.com/gradle/gradle

Before distributing binaries, generate an inventory of resolved direct and transitive dependencies and include all required notices and license texts.
This file is a development inventory, not a complete distribution notice bundle.

Additional dependencies: Media3's OkHttp datasource, OkHttp and Okio retain their upstream Apache-2.0 licenses; JUnit 4 (EPL-1.0) and JSON-java (upstream license) are test-only dependencies. Resolve actual transitive versions and bundle upstream texts before distribution.

Streaming builds use an external ARM runtime. Its binaries are excluded from Git; distribution remains subject to its applicable license. This project does not grant redistribution rights for that runtime. The small `com.pp.hls` Java declaration is an ABI interface, not a copy of native implementation code.

Robolectric 4.16 and its test-only dependencies are used for simulated Android lifecycle regression tests; they are not packaged in the app. Robolectric is distributed under the MIT license. See https://github.com/robolectric/robolectric/blob/master/LICENSE.

AndroidX Test Core/Runner/Ext JUnit and UI Automator are test-only Apache-2.0 dependencies. Device tests reference a public Media3 Big Buck Bunny fixture; neither the clip nor an alternative catalog is bundled with Zuno.

AndroidX RecyclerView 1.4.0 is distributed under Apache License 2.0. Source: https://android.googlesource.com/platform/frameworks/support/+/androidx-main/recyclerview/
