# Working on Veyra
- Preserve existing streaming/backend behavior. Do not silently substitute sample content or another provider.
- Original/Sub are backend source labels until response semantics are verified; do not equate them with subtitle on/off.
- Preserve exact signed URLs, source IDs and required headers. Never log credentials or complete signed URLs.
- Keep the backend adapter separate from playback/UI.
- Do not copy trust-all TLS code, global cleartext settings or unnecessary permissions from reference APKs.
- No signing keys, real playback files, raw APK/DEX/native SDK binaries or credentials in Git.
- The repository stays private until the owner explicitly requests publication.
- Report build, contract tests and real-device playback separately. Never claim backend integration from a successful build.
- Use JDK 17 / Gradle 8.9 for the current baseline.
