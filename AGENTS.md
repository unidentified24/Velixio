# Working on Zuno

- Use JDK 17, Android SDK 35 and Gradle 8.9.
- Keep catalog adapters separate from playback and interface code. Preserve exact source IDs, signed URLs, required headers, stream quality and authentication.
- Treat Original/Sub as service-defined source labels; they do not imply caption availability.
- Preserve distinct connection and short-form title identities. Do not merge titles solely by numeric ID or conceal partial failures.
- Keep credentials, signing keys, runtime binaries, real response captures and playback files out of Git. Local integration inputs belong under ignored `private/`.
- Keep system certificate and hostname validation. Do not add trust-all TLS, global cleartext access or unnecessary permissions.
- Preserve normal server authorization. Do not forge purchases, rewards or entitlements. The app has no viewing-credit wallet or balance deduction.
- Run relevant checks at the final implementation checkpoint. Report build, automated tests and device playback separately.
- Preserve the working streaming configuration during interface work. Compare runtime/input hashes and verify packaged signing bindings before delivery.
- Native initialization must match the installed package, signing certificate and SDK version. Package the required companion asset with its libraries; keep both outside Git.
- Each streaming engine runs in a non-exported service process. Local-proxy entries must match the active port and cannot be supplied by playback-file imports.
- Picture-in-picture retains playback across onPause, releases onStop, preserves aspect ratio and disables automatic entry when paused, ended or failed. Do not request overlay permission.
- Backup imports must be bounded and fully validated before applying changes. Export only supported viewing metadata and preferences, never sessions or media URLs.
