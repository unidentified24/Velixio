# Security

Application controls:
- Default system certificate and hostname validation; no trust-all TLS or user-CA trust override.
- INTERNET is the only requested permission. Player activity is not exported; PiP uses the Android API rather than overlay permission.
- HTTP is limited to the active local native adapter. Imports cannot supply local-proxy entries; external media requires HTTPS through the Java player transport.
- Media request headers are removed when a redirect or segment request changes origin. TLS downgrades are disabled. Hop-by-hop/Host overrides, control characters and excessive header data are rejected.
- API responses, playback imports, poster downloads/decoded sizes and catalog recursion are bounded. API redirects are rejected; artwork uses HTTPS without cookies and refuses redirects.
- The streaming SDKs are confined to separate non-exported app service processes. Their loopback ports are invalidated when bindings die; idle UI/PiP cleanup stops their dedicated processes. These processes share the app UID and are crash/lifecycle boundaries, not a separate security sandbox.
- Tokens are memory-only. Resume/title/history metadata and installation IDs are app-private; cloud backup and device transfer are excluded. Source URLs are not saved in watch history.
- No rewards, purchases, balance-debits or forged entitlements are submitted. App code does not log credentials or complete signed URLs.
- Credentials, real response captures and private SDK binaries remain outside the repository.

Material limits:
- The existing API's shared signing key and CBC envelope can be extracted from a distributed client. Keeping them out of Git does not make embedded secrets unextractable.
- The native streaming component is opaque and ARM-only. Its network destinations, TLS behavior, logs, background activity and shutdown behavior have not been audited or verified in the app. Java network policy does not constrain native socket code.
- Each connection loads its own SDK. JVM tests cannot exclude device-native crashes.
- Dependency versions/target SDK remain a development baseline. This is not a penetration test, full vulnerability scan, license clearance or production security certification.
- Existing server authorization and content availability remain authoritative. No authentication or credit-denial bypass is implemented.

Before release: complete ARM-device playback/lifecycle tests, audit or isolate the native component, check current dependency advisories, verify package identity support, and generate distribution notices.

The app stages only installed native libraries in app-private code cache, marks them read-only and loads their absolute paths. Lint flags the dynamic load for review. No remotely fetched executable is accepted. Catalog preview storage excludes streaming URLs and authentication tokens.
