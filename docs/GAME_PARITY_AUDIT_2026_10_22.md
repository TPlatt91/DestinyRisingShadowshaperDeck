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

## Helhest damage accounting and summon/placement timing — 22 October 2026

- **Helhest:** Earlier code calculated Power gained by subtracting each victim's `power` *after all reactions* from its Power before the hit. That undercounted damage whenever the target received an on-damage Power bonus and conflated overkill with the visible Power remaining. A new engine helper `resolvedDamage(source, target, amount): Int` reports the post-modifier hit amount when damage is actually inflicted; reduced-to-zero or Overshield-blocked attacks return zero. Helhest now sums these values for the selected enemy and available horizontal neighbors, then applies the same amount as its Power bonus. The one-to-one gain, including overkill, is supported by [player testing](https://www.reddit.com/r/destinyrisingmobile/comments/1od3jej/) but still needs direct comparison with user match footage.
- **Placement phase cleanup:** The normal played-card pipeline now removes zero-Power or self-defeated cards before ally and opposing arrival watchers. The summoned-card pipeline sweeps after allied placement responses before dispatching enemy arrival triggers. Opposing Zhiyao cannot earn a false bonus from a defeated unit.
- **Regression tests:** Six focused tests in `ShadowDamageAccountingAndArrivalTest.kt` cover overkill, Saladin after-damage Power gains, Overshield prevention, Helhest full-hit Power gains, and destroyed summons/played units no longer triggering enemy arrival reactions.
- **Unresolved exactness:** Tomb Ship and Drop Pod random selection pools are still inferred from the constructible catalog because neither available text nor captured card-detail footage enumerates the original game's hidden random choices. The Xuan Wei full-board movement rule also needs original-client evidence. The code was not changed speculatively in these areas.
- **Testing:** The regressions are committed but not run, as the local Android SDK/Gradle workstation is unavailable. Use `run_local_engine_checks.bat` (no GitHub Actions consumption).
