# Shadow Shaper Duels — Resume, rematch, and final battlefield

## User-visible workflow

**Close or leave an unfinished match:** Every published battle action creates an atomic checkpoint in app-private storage. This includes a newly started match, legal player card placements, CPU plays, end turns, and resolved unit relocations. Android killing/closing the activity leaves the last committed checkpoint available. This does not require Internet access or GitHub Actions.

**Open the app again:** The home screen shows **CONTINUE BATTLE** only when a recoverable unfinished save exists, alongside **NEW BATTLE**. Continue launches the existing match directly. Entering Battle via the normal New Battle button when a save exists opens **CONTINUE LAST MATCH?** with two explicit choices: **CONTINUE** (original state) or **START NEW** (discard prior checkpoint and choose decks).

**Saved state includes:** complete human/CPU hidden deck order; both hands; four physical card rows (seven columns each); live card Power and all timed Power bonuses, Inspired/Suppressed/Scorched/Overshield flags; multiple independently sourced Scorch effects; sniper per-target hit history; Cursed Thrall's fuse origin; pending movement and caster; Light, round, turn, scoring and outcome flags; audio draw counters; last placement identity; unresolved-effect diagnostics; battle journal; Java Random's *actual state*, not merely the original seed. Card references are matched to current catalog by exact card name. If a saved card no longer exists or the serialized version is unsupported, loading fails safely rather than trying to play a wrong match.

**Replays are genuinely new:** Victory/defeat now offers **BATTLE AGAIN**, creating a completely new `ShadowBattleCoordinator` with a fresh `System.nanoTime()` seed. Both decks are rebuilt from the same chosen deck and faction, shuffled separately, and fresh opening hands/round one are dealt. **SELECT DECKS** returns to deck and opponent selection rather than reusing exhausted cards. The old game's journal and effects do not carry over.

**After the game:** Result screen has **VIEW BOARD**, **BATTLE LOG**, **BATTLE AGAIN**, **SELECT DECKS**, **HOME**. View Board dismisses the result *temporarily*, leaving the original final 7-column stone arena visible so the player can tap any occupied position to inspect its card, statuses, and final Power. The upper-left **‹ RESULTS** button returns to victory/defeat. Back also returns. Board inspection is strictly read-only (no legal placements, transmat/landing replays or End Turn). The battle LOG button remains available from the board, and the separate result-level Battle Log button opens the same journal; closing either returns to the corresponding board or result context. Copy Report is preserved.

## Technical implementation

- `ShadowMatchState.kt`: pure Kotlin/Java-serializable, versioned DTO and two-pass identity-preserving codec. Cards stored by catalog names. Reference targets stored by stable snapshot integers. RNG is serialized as the standard `java.util.Random`, so future draws and random ability outcomes resume exactly. DTOs include stable serialVersionUIDs.
- `ShadowMatchRepository.kt`: app-private `android.util.AtomicFile` with fsync/atomic finish; no public storage or unnecessary permissions. A finished match automatically clears the unfinished-game slot. Corrupt/obsolete checkpoints fail closed.
- `ShadowBattleCoordinator.kt`: immediate checkpoint callback after each `publish()`. Restoration reconstructs the engine once and **never calls start()**; this prevents accidental reshuffle.
- `ShadowBattleActivity.kt`: explicit continue/new choice, saved deck/faction/cover metadata, recovery, new shuffled match, temporary post-game board inspection, log and return to results. Recovered matches do not replay opening shuffle/deal audio or the original entrance animation.
- `ShadowComposeActivity.kt`: dynamic launcher entry for Continue Battle, updated when the launcher activity resumes.
- `DuelResultPresentation.kt`: distinct post-game actions for inspecting, reviewing, rematching and deck reselection.

## Local verification (no GitHub Actions)

Run the repository's existing `run_local_engine_checks.bat` on your Windows computer and test these cases with a debug APK:

1. Begin a match; play a card; send the app away or force-stop it. Reopen, select Continue and verify cards, Light, turn, deck/hand counts, journal, and Power remain identical.
2. Finish the same match on a resumed game; confirm status effects and random outcomes continue without being reset.
3. From finished victory/defeat, tap View Board, inspect CPU and player cards, open/close the battle log, return to Results, and repeat.
4. Tap Battle Again twice and compare opening hand/deck/first-turn states. Each is freshly shuffled and scores/boards reset.
5. From the regular launcher, tap New Battle when a save exists; confirm it presents Continue or Start New, not silent save destruction.

Regression coverage in `ShadowMatchPersistenceTest.kt` (pure JVM snapshot serialization, ordered decks, status/identity parity, RNG parity and shuffled rematches) and `DuelResumeUiGuardTest.kt` (launcher, board/log controls, read-only mode and save wiring).

These source changes have not been Android-compiled or phone-tested in this environment; code inspections alone cannot certify all UI behavior.
