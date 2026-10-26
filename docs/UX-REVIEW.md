# Interface review: 0.6.0

The review used published product descriptions from [Netflix](https://about.netflix.com/en/news/unveiling-our-innovative-new-tv-experience) and [Prime Video](https://www.aboutamazon.com/news/entertainment/prime-video-updated-steaming-experience) as references for clear navigation and artwork-led discovery. These are design references, not direct testing of those apps or a claim of feature parity.

## Problems corrected
- Permanent player header/footer rows reduced the video area. Video now occupies the entire window; app controls overlay it and hide with Media3 controls after three seconds during playback. Android system bars are hidden and remain available by edge swipe.
- Player actions competed with the picture. Source, quality, audio, captions, episode navigation, rotation and reconnect are collected in Playback options. Mini player remains a compact accessible action.
- Movies now open in landscape. Fit preserves the complete image; optional Fill crops the edges without distortion. Letterboxing can remain in Fit when video and screen aspect ratios differ.
- Search previously reused the catalog screen without a clear entry state. It now has a dedicated search entry screen and keeps the search field visible for results.
- Library filters and navigation state could conflict with catalog filters. Library now shows My list and Continue watching; returning from details restores the originating collection position.
- Hero height now adapts to available screen height. Navigation drawing reuses its path object. Existing recycled poster rows and compact 48dp touch targets remain.

## Acceptance still requiring a phone
Confirm the controls disappear while playing and reappear on tap; Android navigation buttons hide; rotation, cutout placement, Fit/Fill and PiP work on the S25 FE. Check readable text at enlarged font sizes, scrolling smoothness and return navigation on actual catalog results. Automated layout/lifecycle checks cannot establish visual acceptance or physical-device performance.

Streaming adapters and private runtime inputs are unchanged. Recommendation ranking and complete content coverage are not established by this interface review.
