# Shadow Shaper Duels — Card inspector restoration and background music

## Restored Skirmish card viewer

The in-battle card inspection overlay was broken after the shared Destiny-inspired popup visual pass: the new decorative **CLOSE** control used `heightIn(min = 50.dp)`, while its internal Canvas used `fillMaxSize()`. In an unconstrained row, that made the header consume almost the entire available dialog height and pushed the full card face and printed effects outside the visible viewport (see the user's Vandal screenshot).

Fixes:
- Shared `DuelPopupAction` now has an **exact height** of 50dp (43dp for QUIET), so no popup button can unexpectedly expand a column.
- `DuelCardInspection` uses a bounded **61dp header** for name, rarity, faction and Close; the card artwork and the live Power/Light, statuses, effect text, card ID and keyword definitions occupy a separate `weight(1f).verticalScroll(...)` viewport.
- The dialog uses a proportional 88%-screen height and 94%-screen width rather than an unconstrained header swallowing 94% of the device. Artwork remains upright at the original 0.72 aspect ratio; board rules and unit stats are unchanged.

## Bundled Destiny soundtrack

**Source of truth:** the user uploaded 13 original `.opus` assets to `app/src/main/assets/background_music`; the app uses the exact shipped filenames and displays properly formatted series/expansion names. The two base releases are labelled **Base Game** rather than incorrectly called DLC.

| # | Label inside app | Original asset |
|---|---|---|
| 001 | Destiny — Base Game (2014) | 001. Destiny 1 - Vanilla (2014).opus |
| 002 | The Taken King (2015) | 002. Destiny 1 - The Taken King (2015).opus |
| 003 | Rise of Iron (2016) | 003. Destiny 1 - Rise of Iron (2016).opus |
| 004 | Destiny 2 — Base Game (2017) | 004. Destiny 2 - Vanilla (2017).opus |
| 005 | Curse of Osiris (2017) | 005. Destiny 2 - Curse of Osiris (2017).opus |
| 006 | Warmind (2018) | 006. Destiny 2 - Warmind (2018).opus |
| 007 | Forsaken (2018) | 007. Destiny 2 - Forsaken (2018).opus |
| 008 | Shadowkeep (2019) | 008. Destiny 2 - Shadowkeep (2019).opus |
| 009 | Beyond Light (2020) | 009. Destiny 2 - Beyond Light (2020).opus |
| 010 | The Witch Queen (2022) | 010. Destiny 2 - The Witch Queen (2022).opus |
| 011 | Lightfall (2023) | 011. Destiny 2 - Lightfall (2023).opus |
| 012 | The Final Shape (2024) | 012. Destiny 2 - The Final Shape (2024).opus |
| 013 | The Edge of Fate (2025) | 013. Destiny 2 - The Edge of Fate (2025).opus |

### Playback and control behavior

- The music plays **during Skirmish**, including the in-battle menus and match results. It loops the chosen track indefinitely at a default 27% volume, balanced below game effects.
- Battle Settings now includes **Background Music** on/off and **Music Volume** slider, distinct from **Sound Effects / SFX Volume**. **Select Music Track** opens a scrollable list of all thirteen songs, grouped by Destiny / Destiny 2 labels. Choosing a different song switches immediately, without touching the SFX bank or combat logic.
- Selected track, enabled state and music volume live in app-private SharedPreferences. The selection persists across matches/app restarts.
- `DuelBackgroundMusic.kt` uses MediaPlayer with asynchronous `prepareAsync`, an Ogg/Opus `AssetFileDescriptor`, a looping track, AudioFocusRequest handling/ducking and explicit pause on Activity onPause/onBackground. Music is released on Activity destruction. Calls fail gracefully when the device cannot decode the asset and status is visible in Settings; no network requests or external file permissions.
- `app/build.gradle.kts` includes `androidResources { noCompress += "opus" }`, so the APK stores the long-form music uncompressed for direct streaming without duplicating the approximately 50MB soundtrack into a local cache.
- Audio playback stops when leaving Skirmish; the launcher, Deck Construction and Card Collection remain silent. System media volume still affects the final loudness.

### Regression and verification

`DuelMusicAndInspectorGuardTest.kt` checks the 13 catalog/asset pairs, correct base-game naming, music settings and activity lifecycle, persisted controls and viewport constraints. Existing popup/audio regression tests remain in place.

No GitHub Actions were run. Compile and install locally, then:
1. Open a Vandal or another card in Skirmish. Verify full artwork is visible, and scroll to read its complete printed effect and keywords. Close must not expand the header.
2. Open Menu > Sound Settings. Turn music off/on, choose a different expansion, adjust its volume, and test SFX separately.
3. Exit and relaunch Skirmish. Confirm last music preference persists and playback pauses when app is minimized or another app takes audio focus.
4. If an Android device does not support these `.opus` tracks, Settings will show a playback error; local conversion to compatible AAC or Ogg Vorbis would be the appropriate follow-up (not implemented in this change).

The code has not been Android-compiled or on-device audio-tested in this environment. Asset filenames have been verified against the actual GitHub directory, not inferred from a generic DLC list.
