# Veyra
A modern Android streaming app with a redesigned interface and player, designed to preserve the existing content, servers, quality options, and Original/Sub choices.

**Status: initial player foundation. The production catalog/backend is not connected yet.**
Repository URL: `unidentified24/Verya`. Product name: **Veyra**.

## Included
- Native Android starter with a dark violet interface.
- Media3 ExoPlayer for HTTPS HLS, DASH and MP4.
- Backend-neutral source contract preserving exact signed URLs, request headers and opaque source IDs.
- Source selector for Original/Sub (or any labels returned by the backend).
- Embedded subtitle selection, normal player controls and playback speed controls.
- Position and play/pause state retained when switching sources, rotating or temporarily leaving the activity.
- A playback-file importer for testing real streams before catalog integration.
- APK comparison, security findings and migration plan.

## Build
Use JDK 17, Android SDK Platform 35 and Gradle 8.9:
```
gradle :app:assembleDebug :app:lintDebug
```
The Gradle wrapper is included; use `./gradlew` on macOS/Linux or `gradlew.bat` on Windows.
Android Studio can open this folder. It generates the machine-specific `local.properties` SDK path.
The initial build pins AGP 8.7.3 / Media3 1.8.1. This is a development baseline, not a current Play Store release configuration.
Upgrade and test the target SDK and current Media3 release before distribution.

## Internal developer playback harness
Copy `examples/playback.example.json` to a private file and replace its reserved `.invalid` URLs with fresh, authorized backend-resolved HTTPS streams.
Keep all signed query parameters and required headers exactly as issued.
Transfer the JSON file to the test device, open Veyra and choose **Open playback file**.
The example is documentation and is intentionally not playable.
Playback files may contain temporary credentials; never commit real ones.

## Test the backend contract
With JDK 17:
```
javac -d build/contract-tests app/src/main/java/app/veyra/model/*.java tests/ContractTest.java
java -cp build/contract-tests ContractTest
```
Tests cover exact URL/header preservation, immutable source lists, source identity and invalid inputs.

## Delivery checkpoint
The owner does not want previews until semi-final testing. Internal builds and automated tests continue; the playback-file importer is a development harness, not the intended final catalog experience.
Veyra will have no viewing-credit wallet, spending or reward-ad unlock requirement. Authentication and signed stream URLs remain necessary.

## Next
See [roadmap](docs/ROADMAP.md), [backend contract](docs/BACKEND.md) and [APK analysis](docs/APK-analysis-and-comparison.md).
Actual playback against the old servers, Original/Sub equivalence and quality parity are not verified yet.
No transcoding is introduced; adaptive selection still depends on the manifest, network and device.

## License
Currently private; no project-wide open-source license has been granted.
Third-party components retain their own licenses. See [third-party notes](THIRD_PARTY_NOTICES.md).
