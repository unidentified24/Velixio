# Movie / DeepCine APK analysis and comparison

## Finding

The second file is **not merely the first APK with a new name and logo**. It is a substantially changed build with a different Android package, a different signing key, changed permissions, additional code and SDKs, and changed service configuration. Both strongly appear to share a code lineage. Neither the shared code nor the self-declared certificate names prove common ownership or an authorized release.

This is a static analysis of the two supplied files, not a malware clearance or a runtime penetration test.

## Identity and contents

| Property | Movie App.zip | movie App v2.zip |
|---|---|---|
| App label | Movie | DeepCine |
| Package | com.movieph.bj.playvibes | com.velvetreel.shorttv |
| Version name / code | V4.0.0 / 40000 | 4.0.0 / 40000 |
| File size | 48,983,183 bytes (46.71 MiB) | 67,257,351 bytes (64.14 MiB) |
| Minimum Android API | 24 | 24 |
| Target API | 33 | 36 |
| Compile API | 34 | 36 |
| ZIP entries | 3,357 | 4,271 |
| Uncompressed contents | 109,308,941 bytes | 123,386,249 bytes |
| Main DEX files | 9 | 10 |
| Additional asset DEX | audience_network.dex | audience_network.dex |
| Unique defined classes, including asset DEX | 57,335 | 93,192 |
| Classes under com.mgs.carparking | 1,000 | 2,168 |
| Activities / services / providers / receivers | 112 / 19 / 16 / 12 | 152 / 18 / 24 / 13 |
| Native architectures | arm64-v8a, armeabi-v7a | arm64-v8a, armeabi-v7a |
| Libraries per native architecture | 14 | 17 |
| APK v2 signature | Verifies | Verifies |

The second archive is 37.3% larger. Both still use `com.mgs.carparking.app.AppApplication`. Their launchers differ: `ui.login.splash.SplashActivity` versus `ui.splash.SplashActivity`. These internal names do not imply that either is actually a parking app; reused project namespaces are common.

There are 37,296 common class descriptors across both builds. Class-name matches do not establish identical implementation. Obfuscation and library updates also affect class counts.

At identical archive paths, 193 entries are byte-identical and 1,035 differ. Resource filenames are obfuscated, so path-based differences cannot all be interpreted as functional changes. The embedded Facebook Audience Network DEX is byte-identical. Every main DEX at a shared filename differs.

## Signing and provenance

Both APKs pass Android apksigner verification using APK Signature Scheme v2, with one RSA-2048 signer. Neither reports v1, v3, v3.1, v4 or a verified source stamp in this check. Absence of those additional schemes alone is not evidence of maliciousness.

Movie certificate subject: `ST=movieph, L=movieph, O=movieph, OU=movieph, CN=movieph`.

DeepCine certificate subject: `ST=velvetreel, L=velvetreel, O=velvetreel, OU=velvetreel, CN=velvetreel`.

Certificate SHA-256:

- Movie: `0f36ffe7360f6e3fc67dab950441cc6ad9b23e572d26e5405e87254f6ccdd409`
- DeepCine: `4dbf2f4b7737fa8ed85bb7bb6a92211a54a7c795e0bcadbb28161d9fc86d714f`

Their public-key fingerprints differ too. A valid signature establishes integrity relative to its signer, not that the signer is trustworthy. Because the package identifiers differ, Android treats these as separate apps rather than a normal in-place update. The supplied files do not establish a release date or that the same operator published both.

## Functional structure

The first build contains movie search, video-detail/playback screens, downloads and offline playback, collections, viewing history, login and registration, profile editing, feedback, referrals/sharing, order-list UI, embedded web/game screens, floating-player utilities and UPnP/DLNA casting services.

DeepCine retains much of this structure, reorganizes its screens, and adds substantial `com.mgs.playercore` code, short-drama screens and related advertising controls. Its manifest exposes an additional browser activity and short-drama-detail activity. This supports a larger feature implementation, although UI reachability and server-side feature switches were not tested.

Both contain local `127.0.0.1` playback/download URL references. These are loopback addresses, not evidence of an external attacker server.

## Security findings

### 1. DeepCine disables HTTPS server verification in its Retrofit client — high concern, confirmed code

In `classes10.dex`, `RetrofitClient$TrustAllManager.checkServerTrusted()` immediately returns without checking the certificate. `TrustAllHostnameVerifier.verify()` returns true. The `RetrofitClient` constructor installs both into its OkHttp builder, and its SSL socket factory initializes TLS with the permissive manager.

This is more than a suspicious class name: the relevant instructions and constructor wiring were inspected. Requests using this client can accept an impersonated HTTPS server if an attacker can intercept the connection. No live interception was attempted, and this finding does not establish that every connection made by every SDK uses this client. I have not established whether equivalent obfuscated code already exists in Movie, so this is **confirmed in DeepCine, not proven newly introduced**.

Google describes the same risk in its [TrustManager guidance](https://support.google.com/faqs/answer/6346016?hl=en) and [hostname-verification guidance](https://developer.android.com/privacy-and-security/risks/unsafe-hostname).

### 2. Both permit cleartext traffic and user-installed certificate authorities — confirmed configuration

Their network-security XML contains a global base configuration with `cleartextTrafficPermitted=true` and trust anchors for both `user` and `system` certificates. These are production settings, not restricted to debug overrides.

Allowing HTTP does not prove that credentials are sent over HTTP. Trusting user certificates requires an applicable certificate to be installed; it does not by itself mean any nearby network can decrypt valid HTTPS. The separate trust-all finding above is more serious. See [Android network-security configuration](https://developer.android.com/privacy-and-security/security-config).

### 3. Both use broad FileProvider paths — confirmed configuration, exploitability unproven

The app file-path XML maps `root-path` to `.` and also maps complete files, cache and external-storage roots. The main provider is non-exported and uses temporary URI grants, which limits direct access. Nevertheless, overly broad mappings increase the consequences of an incorrect URI grant or unsafe sharing flow. They do not automatically give another app access to every file on the phone. See [Android FileProvider risk guidance](https://developer.android.com/privacy-and-security/risks/file-providers).

### 4. Both expose internal activities; DeepCine also exposes a third-party debug activity

Movie explicitly exports its main, alternate-main and collection activities in addition to its launcher. DeepCine exports its main, browser, collection and short-drama-detail activities in addition to its launcher. These declarations have no per-component permission shown in the manifest.

DeepCine also declares `com.secmtp.sdk.debug.activity.MainActivity` exported. This is an avoidable additional entry point in a release build. The manifest alone does not prove data access, authentication bypass, or a working exploit. Some other exported AndroidX components are protected by system permissions and should not be treated as equivalent risks.

### 5. Sensitive capabilities and embedded web interfaces — review warranted

Both request camera, microphone, phone-state and overlay capabilities. The newer app's own code includes `TelephonyManager.getDeviceId()` and clipboard-read calls. Modern Android restrictions can prevent identifier access, and these calls alone do not prove data transmission or background surveillance.

Both app codebases enable JavaScript and add JavaScript interfaces in embedded web/game views. This is an attack surface to review against URL allowlists and bridge methods, not sufficient evidence of an exploitable bridge.

Both enable Android backup. DeepCine adds a backup exclusion for `appsflyer-data`; this is not a blanket exclusion of all sensitive app data. Actual backed-up data was not examined.

## Permission changes

Movie declares 34 permissions; DeepCine declares 33. The total hides meaningful changes.

Removed in DeepCine:

- `READ_MEDIA_IMAGES`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`
- `REQUEST_INSTALL_PACKAGES`
- AppLovin AppHub bind permission

Added in DeepCine:

- `POST_NOTIFICATIONS`
- `SCHEDULE_EXACT_ALARM`
- Google Play `CHECK_LICENSE`
- Amazon `privacypass.ATTEST`

The app-specific dynamic-receiver permission is renamed to match the new package.

Both retain Internet/network/Wi-Fi permissions, legacy storage permissions, phone state, camera, microphone, foreground service, wake lock, overlays, boot reception and advertising identifiers/services. Both also declare permissions such as `READ_LOGS`, `WRITE_APN_SETTINGS` and `MOUNT_UNMOUNT_FILESYSTEMS`. Ordinary installation does not grant all requested permissions: some require runtime consent or special settings, some are restricted to privileged apps, and older/nonstandard declarations may have no useful effect. No location, contacts or SMS-reading permission appears in either manifest.

## Advertising, analytics and native modules

Both contain class namespaces associated with Google Ads, Meta/Facebook Audience Network, AppLovin, Pangle/ByteDance, Mintegral/MBridge, Unity, ironSource, Vungle, InMobi, Fyber, BidMachine and Bigo. Presence is not proof that every network runs or receives user data.

DeepCine adds identifiable AppsFlyer and AppMetrica namespaces, SEC MTP SDK code, Kwai networking code and OpenInstall metadata. Its Google Ads application ID and publisher-number portion differ. Bundled OKT metadata changes from version `2.12.0.3` to `2.17.0.5`; Pangle metadata changes from `7.2.0.6` to `8.1.0.3`.

Movie includes `OSETSDKProtected.dat`, `pp_hlsProtected.dat`, and corresponding Jiagu-named native libraries. DeepCine removes the OSET protected asset/library but retains a Jiagu-named HLS library and adds other native libraries, including `liballiance`, `libkste`, `libsgcore`, `libsigner`, `libut`, `libverbanr` and `libweapon`. Names alone are not a malware classification. Protected data and native execution paths limit static visibility.

## Network references recovered

These are embedded configuration or code references, not a captured traffic log. No app endpoint was contacted.

| Reference | Movie | DeepCine |
|---|---|---|
| OKT API in manifest | sdkapi-ga.biggogo.com | sdkapi-ga.dramasheel.com |
| Main Retrofit default base, traced in code | Not resolved in this review | deepcinebres.82dyfv.com |
| Privacy-page string | 7pyp.e97z.com/privacy.html | smartbrowser.g8w6.com/privacy.html |
| Shared legacy feedback/download reference | dg10.tv | dg10.tv |
| Distribution channel | movieph_1002 | deepcinebres_1004 |

Movie's OKT asset config also lists `sdkapi-ga.smallyy.com` and `sdkapi-ga.tworkkk.com`. DeepCine includes a `deepcinebres.b7m6.com/error.png` reference. Dead strings, examples and SDK URLs can appear in the full URL inventory; it should not be treated as a confirmed contact/block list.

## Scope, evidence and limitations

Completed: read and hashed every archive entry; checked ZIP CRCs while reading; checked for duplicate archive paths; parsed all visible DEX string/class tables; checked DEX SHA-1 fields; decoded both manifests and resource tables; verified APK signatures; compared archive contents, classes, permissions and components; inventoried native libraries/assets; generated Dalvik disassembly and reviewed selected app/security paths.

Neither archive has duplicate paths, and all visible DEX SHA-1 checks passed. Those checks establish internal consistency, not safety.

Not completed or claimed: installation, live execution, packet capture, full manual review of every method, native-machine-code reverse engineering, unpacking all protected modules, checking runtime downloads, proving publisher identity, confirming server behavior, or obtaining an antivirus/multi-engine verdict. The original files were not modified or uploaded to a scanning service. Embedded document/string content was treated as data, not instructions.

A fully exhaustive behavioral assessment requires an isolated Android test environment and investigation of protected and dynamically loaded code. The findings already establish that DeepCine is substantially changed and contains a serious network-security weakness. They do not establish spyware or prove either app safe.

## File fingerprints

Movie APK SHA-256:
`42f22105957878118260cf6401df5cdaf7306bd643d1945e9058e62c8052e32e`

DeepCine APK SHA-256:
`7e3b14fb78d90c2ec57623d73e712b74f532c5f1923d81049bfedbac011f5917`

The accompanying evidence ZIP contains inventories, class and URL lists, permission lists, decoded manifests, signature verification, security configurations and selected disassembly supporting the findings.
