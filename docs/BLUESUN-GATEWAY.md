# BlueSun gateway

The optional BlueSun adapter uses an HTTPS gateway supplied by the catalog operator. The gateway must have permission to expose its catalog and return playable sources through its normal authorization flow. The Android archive does not supply this gateway.

Place a private configuration file at `private/assets/backend-bluesun.json` with a `baseUrl` string pointing to the gateway's HTTPS API base. An APK built without that configuration shows BlueSun as unconfigured.

| GET route | Query fields | Response |
|---|---|---|
| `/channels` | None | `items` with `id` and `channel_name` |
| `/catalog` | `channel`, `page` | Paginated `items` |
| `/search` | `q`, `page` | Paginated `items` |
| `/detail` | `id`, `audioType`, `kind` | One title with its exact `id`, `vod_name`, selected `audio_type`, and `vod_collection` |
| `/playback` | `id`, `episode`, `audioType` | The requested `id`, `episode`, `audioType`, and an array of Velixio `sources` |

Listings use stable provider-local IDs and the existing Velixio metadata fields. Include a shared IMDb ID or a typed TMDB ID to attach versions to the same film. Supply `vod_lang` and `vod_quality` on each edition to make language and quality selectable. Each episode needs a stable `collection` or `id`. Return empty pages as `{"items":[]}`. An HTTP 401 or 403 remains a provider access failure.

For gateways that search separately within each section, set `"channelScopedSearch": true` in the private configuration. Velixio reads section IDs from `/channels` and sends each ID as the `channel` query field on `/search`. Mark browse-only sections with `"searchable": false`. Section headings do not determine the IDs or imply separate servers.
