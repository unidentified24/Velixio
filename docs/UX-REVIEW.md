# Interface behavior

## Discovery and library
Home combines rotating featured picks, a Next pick action, and distinct discovery rows using available ratings and release years. Additional catalog pages load in the background. My list and Continue watching appear when they contain entries. Poster save controls, empty-state guidance and viewing-history progress support the library flow.

## Player
Video fills the available window. Playback controls and app overlays hide together during playback and return on tap. Movies open in landscape. Fit preserves the full picture; Fill crops without stretching. Source, quality, audio, captions and episode controls are available from playback options. Picture-in-picture uses Android's native support.

## Settings
Settings is a dedicated page with Appearance, Playback and Your data sections. Palette and density choices apply inline. Backup review and history-clearing confirmation use dedicated pages. Progress and results appear in a status banner. Back follows the page hierarchy, and changes are reflected when returning to browsing.

## Acceptance criteria
Check readable contrast, comfortable touch targets, enlarged fonts, small and tablet widths, TalkBack labels, scrolling, file-picker return paths and the complete browse/save/watch/restore flow on devices. Automated layout and behavior checks do not replace visual acceptance or performance measurements.

## 0.9.0 responsiveness
Poster bitmaps share a bounded 16 MiB memory cache across screen recreation. Rebinding an already displayed poster keeps its drawable, and pending artwork work is capped at 96 jobs with stale queued work discarded first. Home shelves reuse their adapters and retain horizontal scroll positions. Recently visited catalog pages use a bounded two-minute in-memory cache, including expanded Home results and partial-result notices.

These changes remove redundant work; actual frame timing and perceived smoothness still require device measurement.


## 0.10.0 redesign
The brand uses a film-frame/play mark with rounded lowercase lettering. The navigation header uses a three-line icon; Continue and My list are separate destinations. Episode cards expose Watch and Save actions, and synopsis expansion stays inside its About card. Initial loading has visible placeholders while the catalog responds; this does not eliminate server latency.

Player controls include a visible version selector, rotation and picture-in-picture icons. Styled bottom panels replace platform list dialogs. Fit, Fill/crop and Stretch are explicit choices. The redundant audio-language menu is removed; service-defined Original/Sub/Dub labels remain intact. Available alternate catalog connections can be selected from the player when a matching edition identity is known. Season changes discard stale connection identities. A buffering spinner appears only after 800 ms, avoiding flashes during short seeks.

Home labels ratings as catalog-supplied, omits the specifically disliked Baddies franchise where other recommendations exist, and supports long-press hide preferences. Metadata quality and subjective recommendation quality still require user review; no external verified rating source is claimed.
