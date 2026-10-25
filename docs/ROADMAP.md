# Veyra implementation roadmap

## 0 — Baseline (this commit)
- [x] Record APK identities, differences and security concerns.
- [x] Native Android project and Media3 player integration.
- [x] Immutable playback/source contract and preservation tests.
- [x] Local playback-file import with Original/Sub-style source selection.
- [ ] Test playback on an Android device with authorized real content.

## 1 — Same backend
- [ ] Recover and document authenticated catalog/detail/episode/source API contracts.
- [ ] Establish server support for Veyra's new package and signing identity.
- [ ] Map the exact Original/Sub semantics.
- [ ] Implement backend adapter with response fixtures and expiry handling.
- [ ] Verify media format/proxy/DRM compatibility.
- [ ] Verify stream/rendition parity against both original apps.

## 2 — Product
- [ ] Catalog, search, details, seasons/episodes and watch history.
- [ ] Modern full-screen player, explicit quality/audio/subtitle menus and gestures.
- [ ] Persistent resume, next episode, Picture-in-Picture and accessibility.
- [ ] Owner-approved offline/casting functionality.

## 3 — Release
- [ ] Upgrade target SDK/build dependencies and run compatibility tests.
- [ ] Real-device playback and lifecycle testing.
- [ ] Dependency/license inventory, attribution and source-release review.
- [ ] Select an open-source license for owned code.
- [ ] Release signing through CI secrets; no keys in Git.
- [ ] Publish only after an explicit request to change repository visibility.

Do not replicate the old trust-all certificate/hostname checks. No ad SDKs or analytics are needed for the player foundation.

## Owner decisions: credit-free playback, APK-only reconstruction
- Inputs are limited to the two APKs; no backend source will be supplied.
- Remove viewing credits and their acquisition/spending flows from the replacement client.
- Keep legitimate login/session and stream-authorization mechanisms intact.
- Do not inherit reward-ad prerequisites, earned-time exhaustion or episode locks from the old clients.
- Verify server compatibility without spoofed purchases/rewards or invented VIP claims.
- No previews or owner testing until the semi-final checkpoint.

### Semi-final checkpoint
Catalog/search/details/episodes must be connected; Original/Sub mappings verified; real streams play and seek; source changes preserve position; quality/audio/subtitle parity has been checked; zero-credit/expired-earned-time legacy states do not introduce client gates; genuine expired authorization still produces correct reauthentication or URL refresh; no unresolved server credit denial is hidden; install/lifecycle/error tests pass. Only then offer a testing build.
