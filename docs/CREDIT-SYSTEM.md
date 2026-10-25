# Viewing-credit removal: APK-only findings and implementation boundary

## Status

Owner decision: no coins/credits used to watch content in Veyra. No previews until semi-final testing. Only the two APKs are available; backend source/admin access will not be requested.

The current Veyra player has no wallet, credit balance, debit request, reward-ad SDK, viewing-minute countdown or episode-unlock requirement. It opens a resolved source directly. There is therefore no existing Veyra credit subsystem to patch out. The original APKs have not been modified, and end-to-end removal across their servers is not verified.

## Confirmed mechanisms in DeepCine

### Earned viewing time stored locally
`com.mgs.carparking.util.UserUtils` in `classes7.dex` contains:

| Method | Code address | Observed behavior |
|---|---|---|
| `Z()` | `0x34e9c4` (instructions begin `0x34e9d4`) | Reads `KEY_SHORT_FREE_AD_REMAIN_SECONDS` from preferences |
| `L1(long)` | `0x34f890` (instructions begin `0x34f8a0`) | Clamps to zero or greater and writes that preference |
| `a(String)` | `0x34eaf8` | Parses a minute amount, multiplies by 60 and adds it to the stored seconds |
| `c(long)` | `0x34ec2c` | Subtracts consumed seconds and clamps the remainder to zero |
| `b(long)` | `0x34ebdc` | Logs `CONSUME_SKIP_LEGACY`; this particular legacy method does not deduct time |

The active subtraction method is `c(long)`, not every method whose log mentions consumption. These are findings from Dalvik instructions, not just a keyword search.

### Reward-ad and episode gates
`VideoPlayActivity$AdGateScenario` contains explicit user prompts for unlocking videos/episodes, new-user ad unlocks and obtaining more viewing time by watching an ad. `VideoPlayActivity` loads `WxRewardAd` and has an unlock-completion method (`e0`, code around `0x33a244`) that calls `VideoAdLookDao.insertVideoAd(...)` and updates local counters. `ShortDramaAdManager` handles earned-time exhaustion and scheduling an ad for the next episode.

Both Room play history (`unlock_episode`) and the legacy history model (`unlockCurrent`) retain unlock state. Veyra should migrate playback progress independently of these flags if a history importer is later built.

### Coins and membership are not all the same mechanism
Both APKs contain an `ExtensionRecordEntry` field for invited/referral gold coins (`invited_total_gold_coin`, prefixed `netCineVar` in DeepCine). DeepCine also includes VIP/trial modules. A referral-gold field by itself does not prove a per-video coin-debit operation. No specific coin-debit API has yet been established.

### Authentication is separate
DeepCine's `RetrofitClient$1.intercept()` adds a `token` HTTP header using `UserUtils.h0()`. `UserUtils` stores a distinct `NETCINEVAR_KEY_PREF_USER_TOKEN` value. This belongs to the request/session protocol and must not be removed as if it were a viewing coin.

The interceptor also adds app/package/version/device/channel/time metadata and a calculated request value. Reconstructing that normal protocol remains separate work; none of these findings licenses changing a rejected server response into success.

## Recovered API evidence

The DEX annotation parser recovered 48 HTTP method declarations in Movie and 120 in DeepCine. Several methods share routes; these are declaration counts, not unique endpoints. Full method/path mappings are in `reference/api-routes.csv`.

DeepCine routes relevant to further investigation:

| API method | Route | What is actually established |
|---|---|---|
| `getHomeVideoDetailListNew` | POST `/api/vod/info_new` | Video-detail response interface |
| `getLookVideoFree` | POST `/api/user/get_activity` | Returns `VideoFreeEntry`; its visible fields are activity flag and image, not a confirmed credit wallet |
| `netCineFungetActiveVip` | POST `/api/user/active_vip` | VIP activation declaration; not called during this investigation |
| `getVipList` | POST `/api/payment/get_list` | Payment list declaration |
| `getOrderInfo` | POST `/api/order/add` | Order creation declaration; not called |
| `reportStartPlay` | POST `/api/playlet/report_start_play` | Playback reporting declaration; does not prove credit deduction |
| `netCineFungetInitUserDevice` | POST `/api/public/init` | Client initialization declaration |
| `getLoginUserSubmit` | POST `/api/public/login` | Login declaration |

No live API calls, purchases, reward claims, balance changes or account alterations were performed for this trace. These routes are not a tested integration contract yet.

## Removal design

1. Continue using the new player without importing the old commerce, trial, reward-ad, unlock or earned-time components.
2. Do not make a wallet request, deduction, order, ad callback or local unlock flag a prerequisite to resolving or playing an authorized stream.
3. Keep catalog/episode/source selection, Original/Sub semantics, manifests, quality variants, headers and legitimate session authorization intact.
4. Do not implement fake unlimited balances, fake VIP, forged purchases or ad-completion claims. Those retain the old system and can create server-side inconsistencies.
5. If a valid session receives a stream, play it directly without local monetization gates. If the server withholds the stream, surface the real failure and determine whether the normal supported API offers an ungated mode. Do not invent that mode or claim server-side removal from an APK-only change.
6. Exercise both movies and short dramas, including next episode, source switches, resume and long viewing sessions. A zero legacy credit balance or exhausted local earned-time counter must not introduce a Veyra client gate.

## Completion criteria

- No wallet, top-up, earn/spend, insufficient-credit, rewarded unlock or viewing-time exhaustion UI in the final Veyra app.
- No client playback dependency on credit/referral/VIP/reward state.
- No legacy monetization SDKs or local unlock database imported.
- Valid authentication and original stream quality preserved.
- Real backend playback tested for representative titles and both Original/Sub choices.
- Any server-enforced restriction accounted for explicitly; not silently ignored or relabeled as removal.

APK-only limitations remain. The static trace establishes client controls we can omit. It does not establish that the backend is entirely credit-free or that all protected/native code has been decoded.
