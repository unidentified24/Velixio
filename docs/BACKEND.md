# Streaming connection contract

`CatalogBackend` implements catalog access and stream resolution; `CombinedCatalog` builds Veyra's navigation and combines results while preserving source identities. The UI never uses raw server category names as navigation labels. Search covers every configured catalog, including titles outside the Home filters.

Connections have separate in-memory sessions and installation IDs. Requests use form-encoded POST bodies, HTTPS, signed headers and the service's response envelope. The existing wire format is an interoperability requirement, not a recommended design for a new service.

Movie details require a verification signature from the local streaming engine before the detail request. Configuration is fetched from the service and passed unchanged into that engine. Exact source URLs are encoded and handed to its resource endpoint without rewriting their content hosts, signed queries or quality. Short-form titles normalize their distinct catalog fields into the same playback model.

Original/Sub are server source labels, not a promise that an embedded caption track exists. Episode identities and order are retained. Quality, audio and captions depend on the selected source.

## Private inputs
Profiles are ignored JSON assets named `backend.json` and `backend-classic.json`. Their field names are `baseUrl`, `appId`, `packageName`, `version`, `channel`, `signingSecret`, `responseKey`, `responseIv`, `nativeKey`, `nativePackage`, `nativeVersion`. Values, sessions and captures must never appear in Git. The historical profile filenames are retained solely for local build compatibility.

ARM libraries use connection-specific filenames and separate non-exported service processes. These processes share the app UID. Each owns its storage and local port; the app invalidates ports when bindings die and stops idle services. The protection library loads only for the connection that requires it.

Safe startup codes: S01 configuration validation; S02 profile read; S03 protection-library load; S04 streaming-library load; S05 local storage; S06 native initialization; S07 invalid returned port; S08 timeout; S09 service connection failure or process exit. Errors exclude credentials, native exception messages and stream URLs.

## Acceptance
Validate native startup, verification signatures, first decoded frame, source switching, episode progression, quality, captions and PiP on ARM Android. A successful catalog response or APK build does not establish playback compatibility. Server authorization remains authoritative; no reward, purchase or entitlement is forged.

## Runtime startup and catalog loading
The native initializer receives the external storage root followed by the app-scoped external-files path. No broad storage permission is requested. Installed, packaged native libraries are staged in app-private code cache using their ABI basenames and marked read-only before loading; each connection retains its non-exported process. No network-downloaded executable code is loaded. Native compatibility remains unverified.

Catalog requests run concurrently across connections. Home metadata is restricted to title identifiers, artwork, rating, year and connection identity, with a 100-title / 512-KiB bound and seven-day display lifetime. It contains no stream URLs or session tokens. Connection failure triggers the next matching edition automatically; server source variants remain explicit.
