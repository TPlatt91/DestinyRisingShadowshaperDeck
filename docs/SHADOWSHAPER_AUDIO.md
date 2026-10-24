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
