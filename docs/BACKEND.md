# Backend reconstruction contract

## Recovered and exercised
Both APKs use form-encoded POST requests with app/package/version, installation ID, time, session token and a request signature. The normal init response supplies the session token. Responses use an AES-CBC envelope; success is `code=10000`, with payload in `result`. The APK's existing wire cryptography is retained for interoperability, not used as a new security design. Standard HTTPS verification remains enabled.

- Both: `/api/public/init`, `/api/channel/get_list`, `/api/channel/get_info` returned successful live responses.
- Newer: `/api/search/result` returned 20 search records in the check.
- Newer short dramas: `/api/playlet_channel/list`, `/api/playlet_channel/info`, `/api/playlet/screen`, `/api/playlet/info` returned successful responses. One detail response contained 39 episodes. This is catalog evidence, not playback evidence.

Catalogs differ: the original has Reality-TV where the newer has Filipino; the sampled movie pages contained 33 and 31 unique IDs, with 22 common, 11 original-only and 9 newer-only. These are changing samples, not a full inventory.

## Client boundary
`StreamingBackend` separates catalog/detail and playback resolution from UI. `LegacyBackend` keeps independent provider sessions; `CombinedCatalog` combines browse/search results while preserving provider editions, short-drama identity and incomplete-result warnings. Session tokens remain in memory. Random installation IDs, saved titles and resume positions remain in app-private storage.

`audio_type_option` contains backend source choices (`type`, `type_name`); `audio_type` is the selected value. Preserve these labels rather than assuming Sub is an embedded subtitle toggle. Movie detail includes `vod_collection`; series entries use `vod_id` and `series`. Short-drama records are normalized from `title`, `cover`, `blurb`, and `collection[].video_url` without modifying the source URL.

## Native authorization and media adapter
The movie detail path requires a second signature obtained from the APK's local native service: `/control?msg=verify&device_id=<installation ID plus content ID>&ts=<time>`. That value is sent with `cur_time`, `vod_id` and `audio_type` to `/api/vod/info_new`. It is separate from the HTTP request signature.

The SDK receives `p2p_config` from `/api/public/get_sys_conf` and starts a local port. The original application encodes the exact backend URL as Base64 and passes it to `/resource.m3u8?src=...` or `/resource.mp4?src=...`. The candidate follows this interface; it does not strip `nt-` prefixes, rewrite signed queries, substitute another media host or transcode content.

Only active, registered loopback adapter ports are accepted internally. Playback-file imports cannot request loopback mode. Java network configuration permits cleartext only for `127.0.0.1`; native SDK networking is outside Java's network-security controls and still requires auditing.

## Private inputs
Ignored asset profiles contain `baseUrl`, `appId`, `packageName`, `version`, `channel`, `signingSecret`, `responseKey`, `responseIv`, `nativeKey`, `nativePackage`, `nativeVersion`. Do not include a session token or a fixed device ID in a build profile. Never commit the values. The explicit owner authorization covers local extraction and ordinary compatibility calls, not publishing credentials or forging rewards/purchases.

The current implementation packages each APK's original ARM binary under a provider-specific filename in ignored `private/engines/jniLibs/<abi>/`. Each is loaded in a separate non-exported service process, so their shared ELF names and JNI class names cannot overwrite one another. The original ELF SONAMEs are preserved. Minimal original JNI declarations include the older protection-library interface. Idle app/PiP lifecycle cleanup unbinds services and terminates only their dedicated native processes. Runtime compatibility remains unverified; the Java ABI/ELF analysis is not proof of successful native loading.

## Unresolved acceptance checks
- Native load, package/signature binding, verify signature, HTTP/native resource playback and provider switching on ARM Android.
- Movies, TV, anime and short dramas across both providers; actual Original/Sub variants and rendition/audio/caption equivalence.
- Expiry, session refresh, lifecycle restoration and native service cleanup.
- Runtime equivalence of embedded/HTTPS sidecar captions, DRM and formats beyond currently supported Media3/native behavior. HTTP-only sidecar captions are not upgraded or fetched through an invented proxy route.
- No server credit denial may be hidden. The client has no credit gates; backend enforcement has not been disabled or proven absent for all content.

A direct bounded request to one returned short-drama HTTP media URL returned 403. The cause is unverified; this was not treated as evidence of a credit lock or worked around. The original native adapter path remains the required playback test.
