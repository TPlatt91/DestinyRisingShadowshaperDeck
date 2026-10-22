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

1. **Original Line and Column geometry:** The engine uses a contiguous same-faction rule (`ShadowPositionRules.contiguousFactionLine`). The original tooltip describes the special *Line affix*, and the exact formation rule, gap behavior, and mixed-faction interaction need live-match evidence. Board width (six columns) is explicitly labeled provisional in `ShadowRules.kt`.
2. **Turn sequencing and draws:** The current 5/4 opening hands and one subsequent draw per round are based on reported behavior, not a fully proven timing reconstruction. Confirm ending statuses, card draws, and effects for both Offensive and Defensive sides.
3. **Card-effect interaction accuracy:** Effects are hand-coded cases with shared state. Suppression, Inspired, delayed damage, Overshield, stacking/replacements, simultaneous destruction, random-choice semantics, and order of triggers require deterministic multi-card regression tests and game-footage comparison.
4. **Provisional or unknown rules:** Helhest's Power-gain rate is inferred as 1 Power per actual damage; the footage is not explicit. Tomb Ship's random Hive pool and Drop Pod's random Line pool are approximations. Xuan Wei's full-board movement behavior is incomplete.
5. **Opponent behavior:** `ShadowData.opponentDeck` creates constructed mixed/faction decks, while `ShadowEngine.cpuPlayOne` uses a heuristic. This does not reproduce Destiny: Rising's NPC beginner/veteran/expert decks, scripted choices, or online players.
6. **Playable Android QA:** Tests exist but no confirmed Gradle run or on-device match evidence is present. UI flow, ability targeting and move choices, asset animation, and CPU end-turn behavior must be evaluated in actual play before certifying a card.

## Recommended acceptance standard

A card is **verified** only if its metadata and printed effect match the original, its gameplay handler passes automated tests for activation and non-activation, its interactions with relevant statuses/auras/adjacency/empty/full rows pass, its CPU target decisions are valid, and its effects are compared against an actual original-client battle reference where possible. Then exercise at least one complete six-round match with varied faction combinations and reproduce it through a fixed random seed.

Use `python tools/audit_runtime_parity.py` for repeatable **static** counts; run `gradlew.bat :app:testDebugUnitTest` and `gradlew.bat :app:assembleDebug` locally without consuming GitHub Actions minutes.

**Do not call any card or the overall game 'fully functioning' solely from code-name coverage.**
