# Shadow Shaper Duels — Destiny-inspired popup visual pass

## Goal

Replace the generic Material dialog appearance shown by the user in two screenshots (Continue Last Match and in-battle Settings), using the two previously approved Destiny-style visual concepts as design direction. This is an original UI implementation, not a representation that Bungie created or endorsed the game and not a copy of official Bungie artwork.

## Reusable visual system

`DuelPopupChrome.kt` now provides the same presentation primitives across screens:

- `DuelPopupDialog`: full-width responsive Dialog (up to 430dp) with a slate/navy black glass gradient, chamfered corners, subtle gold-and-blue border, original concentric orbital glyphs, a hierarchy of large centered titles and restrained secondary text, and thin geometric rule markers.
- `DuelPopupAction`: accessible 43–50dp minimum button targets using a bespoke navy/gold frame and pressed state; styles PRIMARY, SECONDARY, QUIET and DANGER (the latter used for destructive deck actions).
- `DuelPopupMenuRow`: slightly larger menu actions with circular geometric marks, restrained chevrons and descriptive subtitles. No normal Material text-only actions remain in the main Settings panel.
- `DuelPopupSoundSettings`: functional styled Sound Effects switch and SFX Volume slider, wired to the original persistent values, with gold focus accents and no changes to audio playback logic.

All popups are scrollable in portrait so short devices can still access every action, and dismissal permissions mirror the previous UX (Continue Last Match and Opening Duel cannot be dismissed without selecting an action). No network assets, official Bungie trademarks, additional font packages, permissions or build dependencies were added.

## Battle and match popups

`ShadowBattleActivity.kt` replaces the old AlertDialog surfaces for:
1. Continue Last Match — same round/deck details and genuine Continue / Start New actions.
2. Save Recovery error — acknowledges the original recovery handling.
3. Choose Your Deck — legal decks, cover reference, and continue to CPU opponent.
4. Select Opponent — original five options including Mixed, Fallen, Hive, Cabal, Humans.
5. Opening Duel — original starting-order and hand count, unchanged Start Duel callback.
6. Battle Settings — Sound Effects, volume, Test Sound, Resume, New Match, Rules, Home and Close.
7. Leave Skirmish — Stay or Save & Home with original autosave.
8. End This Turn — Keep Playing or End Turn; unspent Light warning remains.
9. Battle Log — complete scrollable match history, Copy Report and Close.

`DuelResultPresentation.kt` now uses the same shared dialog frame for Victory / Defeat / Draw and preserves final Power, unfinished-effect disclosure, View Board, Battle Log, Battle Again, Select Decks and Home.

`DuelCardInspection.kt` retains the original card frame, live statuses, keywords and effects, but uses a matching dark chamfered panel and branded Close control.

## Deck construction / archive popups

`ShadowComposeActivity.kt` replaces its remaining five Material AlertDialogs: deck-update confirmation, cover selector (with thumbnails still visible), create/rename deck, import deck text/file, and delete deck. The deck edit text fields also use gold focus color and white high-contrast text.

The collection's card details and enlarged-artwork windows have matching chamfered borders and Close actions, without modifying the image viewer, collection counters or add/remove-card permissions.

## Behavior and verification

- All user-approved gameplay, deck, sound, save and navigation callbacks are preserved; only popup visuals were replaced.
- `DuelPopupChromeGuardTest.kt` verifies the shared UI, popup presence, saved-match actions, settings persistence, deck manager/import behavior, full final log, card detail and result screen.
- `DuelResumeUiGuardTest.kt` and `DuelAudioIntegrationGuardTest.kt` were adapted from Material-specific source checks to check the new reusable controls.

No GitHub Actions were run. Static Kotlin delimiter/source assertions can detect obvious structure or wiring problems but cannot prove Android compilation or exact on-device rendering. After pulling, run your existing local build and test the dialogs on the phone (especially Settings on short portrait screens).
