# Shadow Shaper Duels — Battle sound design

## Scope and fidelity

The Android app now includes original, procedurally synthesized, low-latency sound effects inspired by the sci-fi presentation of Destiny: Rising and its Shadowshaper minigame. **These are not extracted proprietary Destiny sounds and are not yet verified to be exact to the game.** The user-provided 38669.mp4 clip contains an AAC track but its signal is effectively silent (~−91 dB), so it cannot supply isolated SFX. Public Shadowshaper gameplay videos exist, but a clean, rights-cleared source library was not available for this implementation.

There is no network streaming or live synthesis on the animation thread. At start the app generates the WAV files once on a background dispatcher, caches them privately, and loads them into Android `SoundPool`. All audio effects are optional: loading errors do not prevent a battle action, and sound obeys the device media volume.

## Actual effects

| Sound file stem | Cue |
|---|---|
| ui_tap | Button and menu confirmation |
| card_select | Card selected from the hand |
| transmat_default | Blue-white Guardian-style effect: Humans, Exos, Awoken and unknown |
| transmat_hive | Dark ritual/green harmonic arrival |
| transmat_cabal | Heavy orange-red militaristic arrival |
| transmat_vex | Precise amber portal/gate synth, prepared for future Vex cards |
| transmat_fallen | Cyan electrical and Ether-like arrival |
| card_land | Final physical card drop/impact |
| damage | Successful damage to a unit |
| destroy | Destroyed unit / detonation |
| buff | Power gain / Inspired |
| shield | Overshield activation or damage prevention |
| status | Suppression, scorch or relocation |
| turn | Turn or round passes |
| win | Match win |
| lose | Match loss or draw (neutral fallback for ties) |

Events are mapped from `ShadowEngine.journal`, using an overlap comparison to suppress replays when the same Compose state is redrawn. The normal placement/summon journal line **does not** play a second arrival sound. Transmat audio comes from the actual `unit.card.faction` passed into `DuelAnimatedUnit`; the physical landing sound occurs at approximately **170 ms + 0.72 × 760 ms = 717 ms** after entrance, aligned with the animation's slam moment. CPU visual pacing is unchanged by this audio pass.

## Settings

Battle Menu → **SOUND EFFECTS** toggles SFX. The **SFX VOLUME** slider scales the sound volume; **TEST SOUND** previews the current effect. Enabled/volume persist in app-private SharedPreferences. A disabled sound engine continues to generate no audible effect, but the duel game rules and visuals continue unchanged.

## Optional clean, original in-game recordings

The implementation intentionally supports **optional user-provided replacement recordings** for closer original-game fidelity. For each sound listed above, an OGG or WAV file under:

`app/src/main/assets/shadow_sfx/<stem>.ogg` (or `<stem>.wav`)

overrides that one generated cue. Examples: `transmat_hive.ogg`, `card_land.wav`, `turn.ogg`. A 44.1 kHz or 48 kHz OGG at sensible normalized level works well. Keep the clip clean, without music, voices or neighboring actions, preferably under ~1 second for responsive mobile gameplay. Audio files should be incorporated only when the user has the appropriate rights to redistribute them in an APK.

Generated fallback clips are cached in the application's private `shadow_sound_v1` directory; a supplied override is recopied there when audio is prepared. Gameplay code and card effects need no modification when one sound is replaced. Do not add dozens of MP3 streams that run during every Compose recomposition.

## Local verification

1. Pull repository changes; run `run_local_engine_checks.bat` locally, **not GitHub Actions**.
2. In a phone duel, test Human/Hive/Cabal/Fallen placements, then a placement slam, damage, shield and end of turn. Vex is currently unsupported by the card catalog but has a ready renderer and audio cue.
3. Confirm no audio repeats after scrolling the hand or opening/closing the battle log, and that the volume switch survives restarting the app.
4. If a compiler error is encountered, push the resulting local log for correction. Static source validation alone does not establish successful APK compilation.

Core sources: `DuelSoundDesign.kt`, `DuelSoundSynthesis.kt`, `DuelSoundPlayer.kt`; integration: `ShadowBattleActivity.kt` and `DuelBoardAnimations.kt`. JUnit coverage: `DuelSoundDesignTest.kt`, `DuelAudioIntegrationGuardTest.kt`.


## Second audio pass: drawing, riffling and card-hand interaction (23 October 2026)

**Reference:** Real Destiny: Rising / Shadowshaper gameplay footage and Sound Lab's Destiny: Rising case study describe a layered, bespoke sci-fi sound aesthetic specifically tuned for mobile phone speakers: https://www.soundlab.la/game-audio-case-studies/destiny-rising. The added effects are ORIGINAL deterministic synthesized layered paper-like noise, delicate card-edge scrapes, short interface harmonics and subtle mechanical textures; they are NOT isolated original-game recordings. If clean permitted clips become available, override by the corresponding `assets/shadow_sfx/<stem>.ogg` filename.

**Eleven new stems / 27 total sounds:**
- `draw_single`: quick card-edge slide with soft futuristic swish — active for real one-card draws
- `draw_multi`: several interleaved card swipes — active for real multi-card draws
- `shuffle`: layered card riffle with granular pulses — active on genuine match-start deck shuffle
- `opening_deal`: multiple clipped swipes mixed with subtle crystalline harmonics — active at opening-hand dealing
- `mulligan`: subtle returning/reshuffling cue — **prepared only**, because the current ShadowEngine has NO mulligan action
- `discard`: single sharp card flick — **prepared only**, because the current ShadowEngine has NO normal discard action/pile
- `card_lift`: short slide-lift cue — active on a legal drag start
- `target_lock`: quick holographic confirmation — active when a targeted play prompts the user
- `ability_trigger`: electronic activation chime — active when a targeted ability successfully resolves
- `card_break`: brittle destruction accent layered with the existing `destroy` sample — active only for actual destruction journal entries
- `deck_empty`: quiet double-warning tone — active when the engine attempts to draw cards from an exhausted deck

**Sound synchronization:** The original engine now exposes four observation-only counters: `humanDrawTotal`, `cpuDrawTotal`, `humanEmptyDrawTotal`, `cpuEmptyDrawTotal`. The existing `draw(side, count)` increments these ONLY when it really removes a card from a draw pile (or actually fails due to an empty pile). The counters reset on match start and do **not** affect rules, decks, RNG, AI, effects, hand sizes or placement. The Compose battle audio listener reads counter deltas when the existing coordinator publishes a new snapshot, including hand-count-only changes. Multi-card effects such as Kevarrh's draw 2 and simultaneous start-of-round player/CPU draws trigger `draw_multi` followed by up to four short staggered card slides. The opening hand is special: the engine shuffles both decks when `start()` runs and deals 4 or 5 cards based on turn order; one match-only coroutine waits for the `shuffle` sample to decode before playing the riffle, `opening_deal`, and a small staggered hand draw sequence. Ordinary Compose redraws must NOT replay the opening cue or previous journal entries. A card drawn and played in the same tick still generates a draw cue.

**Limitations:** The eleven new cues are short synthesized WAVs. None are the exact Destiny: Rising SFX. There is no implemented mulligan/discard UI or rule, so those two sounds are intentionally unused until real corresponding mechanics exist. There's no fake card shuffle on every turn: the actual deck shuffles once at match creation. CPU/heroes' play rules remain unchanged; GitHub Actions remain disabled and do not need to be run.

**Tests:** `DuelDrawAudioTest.kt` covers single, double, two-sided round draw, empty deck, no changes, reset monotonicity and real engine counter placement. `DuelAudioIntegrationGuardTest.kt` checks that startup audio, drag lift, target lock, ability resolution, destruction and snapshot observation are wired while absent mechanics remain silent. Existing `DuelSoundDesignTest.kt` now checks all **27** generated PCM WAV files. These are source/pure-JVM tests, not proof of device playback. Pull and run `run_local_engine_checks.bat` locally; inspect the new APK on a phone. No GitHub Actions required.
