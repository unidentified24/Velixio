# Backend compatibility contract

## Non-negotiable behavior
Preserve catalog/content IDs, episode IDs, source IDs, complete signed media URLs, required headers and server-provided renditions. Do not substitute another content provider, proxy or transcode streams to achieve the redesign.

Original/Sub must be mapped from actual backend records. They could represent alternate source lists, different audio/video editions, burned-in subtitles or separate subtitle tracks. The user-visible labels alone do not establish which. Keep source switching separate from Media3 text-track selection.

## Initial boundary
`StreamingBackend.resolve(contentId, episodeId)` returns `PlaybackRequest`.
It is an interface, not a working adapter. The JSON importer supplies the same model for player development.
There is no live catalog API call or embedded app credential in the starter.

Current fields:
- schemaVersion: 1
- contentId: stable episode/content identity
- title: display title
- sources: unique id, original label, exact HTTPS URL, explicit MIME type and required string headers

Do not decode/re-encode signed URLs. Keep ordering and escaping. Headers apply to the selected Media3 HTTP data-source factory, including requests for manifests/segments; confirm per-host/header scope before production, especially cross-host redirects.
Never put reusable credentials or captured playback responses into Git.

## Work remaining
1. Trace the original app's catalog, detail, episode and source-resolution calls.
2. Establish authentication/session issuance, signing, expiry and any app-signature binding from owner-provided source or observed authorized traffic.
3. Capture redacted fixtures and map Original/Sub to response fields.
4. Determine whether legacy `nt-http`, local HLS proxy or native `pp_hls` formats need a documented adapter. Do not pass unknown schemes to ExoPlayer.
5. Handle expiring URLs by resolving again, rather than repeatedly retrying an expired URL.
6. Add sidecar subtitle metadata and DRM/license configuration if actual streams require them.
7. Test manifest and segment hosts, redirects, cookies, content types, range requests and seeking.
8. Match every advertised rendition and source against the old apps.

The initial player intentionally accepts HTTPS only. If the existing backend requires HTTP, identify exact domains and agree a scoped compatibility configuration; do not globally disable TLS verification or cleartext protection.
It does not yet support DRM, sidecar captions, downloads, casting or native proprietary stream formats.

## Parity acceptance matrix
For each representative movie/episode, record: original content ID; backend source ID; Original/Sub label; fresh stream URL fingerprint (not token); manifest renditions; selected codec/resolution/bitrate; audio/subtitle availability; startup, seeking and expiry behavior.
Exercise both source choices, multiple seasons/episodes and network recovery. A successful APK build is not proof of streaming parity.
