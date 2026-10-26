# Working on Veyra
- Preserve catalog, media servers, stream quality and authentication; remove viewing-credit monetization. Do not silently substitute sample content or another provider.
- Original/Sub are backend source labels until response semantics are verified; do not equate them with subtitle on/off.
- Preserve exact signed URLs, source IDs and required headers. Never log credentials or complete signed URLs.
- Keep the backend adapter separate from playback/UI.
- Do not copy trust-all TLS code, global cleartext settings or unnecessary permissions in service adapters.
- No signing keys, real playback files, private runtime binaries or credentials in Git.
- The repository stays private until the owner explicitly requests publication.
- Report build, contract tests and real-device playback separately. Never claim backend integration from a successful build.
- Use JDK 17 / Gradle 8.9 for the current baseline.
- Do not request backend source, server access, passwords or private keys.
- Veyra must have no coin wallet, paid viewing credits, balance deduction, reward-ad prerequisite, earned viewing-time countdown or credit-based episode lock.
- Preserve authentication/session tokens and signed playback URLs; these are not viewing credits.
- Do not forge rewards, purchases or VIP state to obtain a server authorization. Report any server-enforced incompatibility and keep normal error handling.
- No user-facing preview APKs, screenshots or testing requests until semi-final acceptance criteria are met, unless the owner changes this preference. Internal builds/tests continue.

- Live init/catalog tests now succeeded for both providers; newer short-drama detail calls also succeeded. This is not proof of media playback.
- Keep both provider editions and short-drama IDs distinct. Do not silently hide partial backend failures or merge different content just because numeric IDs match.
- Private assets/native libraries live under ignored private/. Never upload them or real response captures. Each streaming engine is now loaded in its own non-exported service process. This new path and all configured-service playback remain unverified.
- PiP must retain playback across onPause, release onStop, preserve video aspect ratio and disable automatic entry when paused/ended/failed. No overlay permission.
- Local-proxy playback entries are internal-only and must match the active native port; user playback-file imports must reject them.

- Owner now requests completing implementation before further tests. Keep automatic test triggers manual until the final implementation checkpoint. Earlier green results do not cover the new engine/history/settings/caption/UI changes.
