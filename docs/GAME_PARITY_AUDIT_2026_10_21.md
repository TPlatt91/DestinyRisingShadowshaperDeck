# Destiny: Rising Shadowshaper Duels — exact-gameplay parity audit

Audit date: **21 October 2026**, repository `main` source. This is an evidence-based assessment of the **single-player Android simulator**, not a claim that it reproduces the original online duel client.

## Current catalog and coverage

- **121 distinct card names** are present across the three base catalogs and video overrides.
- **85 video-verified entries** override older community effect wording.
- **119 of 121 cards** have the detailed metadata overlay. **Tenacious Shielded Knight** and **Gwynn** lack individual detailed rows; their basic catalog records still load.
- **107 cards have non-basic printed effects**, and all 107 names occur in `ShadowCardEffects.kt`. This is **name-reference coverage only**, not verified behavior or exact resolution.
- **14** have basic/no-special-effect text.
- The original card-art loader normalizes names with `lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')`. One existing Shade animation had a mismatched apostrophe-based slug; this patch renames it to the loader's expected `paradoxical_neophyte_s_shade.gif`.
- **Four locally missing images** remain after that rename: Carrier, Guerrilla, Tenacious Shielded Knight, Gwynn. Some source-gallery assets may load over HTTPS; local offline coverage is incomplete.
- Compilation, full local unit-test suite, Android installation, and true six-round match QA are **not verified**.

## Supported framework

A 15-card constructed deck, two copies of each card at most, Exotic and Mythic limits, six rounds, alternating Offensive/Defensive turns, Light costs per round, vacant-slot placement, score as remaining Power, destruction at zero, and basic Inspired/Suppressed processing are implemented in Kotlin. Published gameplay basics are described at [Game8](https://game8.co/games/Destiny-Rising/archives/546603); keyword definitions are at [TodayInDestiny](https://www.todayindestiny.com/rising/shadowshaper_duels/keywords).

## Gaps before claiming exact Destiny: Rising parity

1. **Line and Column formations:** Updated the engine to require adjacent units with a printed numbered **Line affix**, not merely the same faction. Ordinary neighboring Cabal units break the Line; Column continues to use a direct vertical partner. Original player reports support the affix rule, but unusual mixed-faction Line cases and board width (six columns) still need original-client evidence.
2. **Turn sequencing and draws:** The current 5/4 opening hands and one subsequent draw per round are based on reported behavior, not a fully proven timing reconstruction. Confirm ending statuses, card draws, and effects for both Offensive and Defensive sides.
3. **Card-effect interaction accuracy:** Effects are hand-coded cases with shared state. Suppression, Inspired, delayed damage, Overshield, stacking/replacements, simultaneous destruction, random-choice semantics, and order of triggers require deterministic multi-card regression tests and game-footage comparison.
4. **Provisional or unknown rules:** Helhest's Power-gain rate is inferred as 1 Power per actual damage; the footage is not explicit. Tomb Ship's random Hive pool and Drop Pod's random Line pool are approximations. Xuan Wei's full-board movement behavior is incomplete.
5. **Opponent behavior:** `ShadowData.opponentDeck` creates constructed mixed/faction decks, while `ShadowEngine.cpuPlayOne` uses a heuristic. This does not reproduce Destiny: Rising's NPC beginner/veteran/expert decks, scripted choices, or online players.
6. **Playable Android QA:** Tests exist but no confirmed Gradle run or on-device match evidence is present. UI flow, ability targeting and move choices, asset animation, and CPU end-turn behavior must be evaluated in actual play before certifying a card.

## Recommended acceptance standard

A card is **verified** only if its metadata and printed effect match the original, its gameplay handler passes automated tests for activation and non-activation, its interactions with relevant statuses/auras/adjacency/empty/full rows pass, its CPU target decisions are valid, and its effects are compared against an actual original-client battle reference where possible. Then exercise at least one complete six-round match with varied faction combinations and reproduce it through a fixed random seed.

Use `python tools/audit_runtime_parity.py` for repeatable **static** counts; run `gradlew.bat :app:testDebugUnitTest` and `gradlew.bat :app:assembleDebug` locally without consuming GitHub Actions minutes.

**Do not call any card or the overall game 'fully functioning' solely from code-name coverage.**

## Shared-engine repairs applied 21 October 2026

Source commits:
- `3d17fa30a863d0d95c7082cadcbb90a1dcdfb3f0`: Line-affix-only formation logic, projected Line for CPU/players, corrected Drop Pod blocked-left-slot behavior, and updated affected batch test fixtures.
- `8036d66162acdcf0b06a5474f5d7fef9854f36f4`: Scorched origin attribution and end-of-status cleanup; expanded in-app keyword glossary.
- `9000a5597e29767bb25f5ac46936620bd5fb4393`: Zhiyao's temporary Power gain uses shared buff/trigger logic and records the actual granted value for three-turn expiration.
- `ea6edb9f99252d998a417899acd01b0191901c8e`: regressions for Inspired triggering Xur's summons and Suppressed blocking Zhiyao's Sustained reaction.

Keyword mechanics now have separate centralized checks for **When Played, Front/Back Row, Sustained, Suppressed, Inspired, Scorched, Line, and Column**. These repairs aim to align with observed rules, but a source-code change is **not** an Android gameplay test. There is still no verified successful local Gradle build in this session; GitHub Actions were not invoked.

## Next shared-engine safety pass — 21 October 2026

- **Stale unit references:** `ShadowEngine.isPresent` prevents old snapshot objects from receiving buffs, damage, direct damage, Inspired, or Suppressed after leaving the board. Clone death splash still permits the dead card as the *damage source*, while requiring a valid on-board target.
- **End-turn status timing:** After Inspired activates, remaining status processing checks that its target is still present; Scorched damage now resolves deaths before processing additional effects. Removed units cannot activate later Sustained effects.
- **Placement watchers:** Only watchers still present on the board can react to an enemy deployment.
- **Focused local tests:** Added regressions for stale removed cards and lethal Scorched resolution in `ShadowKeywordEngineMechanicsTest.kt`.
- **Verification workflow:** The root `run_local_engine_checks.bat` runs the local JVM regression suite and assembles the debug APK, displaying results and writing `shadow_engine_test_log.txt`. It **does not** run GitHub Actions or spend workflow credits.

**QA status remains unverified until this script completes on a configured Android/Gradle workstation.**

## Next engine pass — targeting, AI placement and relocation

The following source changes were made on `main`:

- **Relocation deadlock prevention:** If the target of a pending relocation has left the battlefield, or if the destination side no longer has any free positions, the pending choice is cleared. The match may advance instead of remaining stuck on an impossible selection. An attempted *occupied* destination is still rejected without losing the existing valid choice.
- **CPU relocation:** Computer-controlled Xuan Wei relocations check that the target and potential destination still exist before attempting to resolve.
- **AI targeted placement:** A position where a targeted When Played ability can actually select a legal unit now scores higher than a position where the same effect cannot activate. Target-free plays are still legal for their printed Power.
- **Local regression suite:** Added six scenarios in `ShadowRelocationAndAiTest.kt` for full-board relocation cancellation, destroyed movement targets, invalid destinations, automatic CPU movement, AI Back Row targeting and target-free CPU deployment.

These are **source-code changes only**, not an asserted match to the original Destiny: Rising NPC AI. Exact NPC behavior, movement constraints on a filled battlefield and battle runtime remain pending original-game verification. No GitHub Actions or workflow credits were used. Run `run_local_engine_checks.bat` from the repository root to compile and run the local Android/JVM verification suite.
