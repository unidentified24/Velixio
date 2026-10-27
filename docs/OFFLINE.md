# Offline downloads and Android compatibility

## Downloads
Use Download for offline on a movie, Save on an episode card, or Download this episode in the player. Open Downloads from the three-line navigation menu to see progress, cancel a transfer, delete a copy or play completed content. One transfer runs at a time through a foreground service. Android 13+ notification permission is optional; progress is always available on the Downloads page.

Downloads use Media3's downloader/cache model, following the [Android offline media guide](https://developer.android.com/media/media3/exoplayer/downloading-media). HLS/DASH/progressive media use the authenticated playback transport. Adaptive streams may download multiple renditions, so storage needs can exceed one selected quality. Each transfer is capped at 8 GiB and stops if less than 256 MiB remains. Cancellation/failure keeps an entry that can be deleted; interrupted downloads must be restarted from the title. There is no automatic expired-session recovery or DRM offline-license implementation. Separate external caption files are not downloaded.

Completed records play through a read-only cache with no network upstream. A missing cached resource fails rather than falling back to online streaming. Downloads remain inside app storage, are not included in `.zunodata`, and are removed when app data is cleared or the app is uninstalled. Source URLs in the internal index are not exposed in the interface or exports.

Automated checks cover stored bytes, offline cache misses, HLS manifest/segment download and subsequent network-free reads. These are synthetic fixtures, not proof that every configured live stream supports downloads. Verify a real title in airplane mode before claiming live-service offline compatibility.

## Devices and windows
The minimum Android version is Android 7.0/API 24. Browsing grids adapt to available width; large windows use centered content, settings have a bounded reading width, and episode lists use two columns on wider displays. The player remains edge-to-edge with inset-aware controls. Tests cover narrow phone, tablet and wide-window column selection.

Streaming runtime binaries are ARM64 and ARMv7. No x86/x86_64 runtime is bundled. An emulator may play streams if its ARM translation supports these libraries, but MuMuPlayer and LDPlayer have not been tested. A responsive interface is not proof of native streaming compatibility, codec support or 16 KiB page-size compatibility on every Android device.
