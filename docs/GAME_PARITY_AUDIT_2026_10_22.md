# Shadow Shaper Duels — Gameplay Engine Parity Audit (2026-10-22)

## Scope

No new cards or assets were added in this pass. All changes target the existing battle engine's **match determinism, temporary Power bonus timing, simultaneous Sustained damage auras, and chained retaliation**.

The [original game guide](https://game8.co/games/Destiny-Rising/archives/546603) describes six rounds, 1–6 Light per round, ending after each player acts six times, and a final surviving-Power score. The [original keyword definitions](https://www.todayindestiny.com/rising/shadowshaper_duels/keywords) define Inspired as gaining 1 Power at your turn end and Suppressed as removing existing Power bonuses and Inspired/Scorched. Original in-game footage is still needed for unresolved card-specific edge cases.

## Source changes

1. **Seed-stable restart:** `ShadowEngine.start()` now explicitly resets the match's RNG to its constructor seed before the coin toss and both deck shuffles. Restarting the *same* engine/seed after earlier random effects produces the same randomized start again.
2. **Chained Power-bonus retaliation:** Each Elite Sniper Vandal bonus-triggered damage event now fully resolves deaths before another sniper reacts. Removed snipers and targets are checked for current battlefield membership. A target already destroyed by the first retaliation cannot be hit again by the second.
3. **Overlapping Sustained effects:** Source behavior is now covered by local tests: two Servitor outgoing damage bonuses stack, are disabled by Suppression and disappear when the source leaves play; two separate Phalanx Line formations stack their Cabal damage reduction and stop reducing after their respective effects expire.
4. **Independent temporary bonuses:** Regression scenarios cover multiple separate Zhiyao +1 Power effects gained on different turns, two simultaneously active Zhiyaos, and Suppression clearing multiple timed modifiers without causing their expired values to be subtracted a second time.
5. **Full-round stability:** Tests cover both first-player assignments across five deterministic seeds, every Light reset, completing after exactly twelve alternating player turns, no further actions after completion, and repeatable opening hands/draw piles following a seeded restart.
6. **Sniper timing:** The once-per-target-per-turn constraint is tested across consecutive rounds.

## Local regression coverage

- `ShadowTurnAndTimedBonusRegressionTest.kt`: **6** additional local JVM regression tests.
- `ShadowSustainedStackingTest.kt`: **3** additional local JVM regression tests.
- Total in this pass: **9** new tests committed.

### Execute tests locally (no GitHub Actions)

Run the root `run_local_engine_checks.bat` on an Android SDK / Gradle configured Windows machine. It runs `:app:testDebugUnitTest` and `:app:assembleDebug`, writes `shadow_engine_test_log.txt`, and pauses instead of auto-closing.

## Remaining uncertainty

- No Gradle, emulator, or physical Android tests were executed during this GitHub source pass.
- Exact Destiny: Rising character/NPC AI, random summon pools, Helhest's damage-to-Power conversion formula, unusual relocation/full-board behavior, and some simultaneous-effect ordering still need original-game match evidence.
- Passing static code checks does **not** certify all cards or the engine as fully equivalent to the original Destiny: Rising client.
