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

## Tomb Ship / Drop Pod / Xuan Wei summons and movement — 22 October 2026

**Verified gameplay constraints from card-detail recordings:**
- **Tomb Ship**: When Played, puts one random Hive unit immediately left and one immediately right, provided those positions are vacant.
- **Drop Pod**: The user's captured card text explicitly says one random Line unit **to the left**, requiring a **2-Unit Line**. Contemporary players have reported the original game deploying only one unit on the left (see [October 3, 2025 discussion](https://www.reddit.com/r/destinyrisingmobile/comments/1nx19ak/)). The repository implements that behavior, not an unsupported two-unit version.
- **Xuan Wei**: Front Row When Played deals 5 damage to a target, then relocates a surviving target to a free space.

**This pass changes:**
1. **Tomb Ship candidate completeness**: The previous `isPlayable`-only pool silently excluded the summon-only **Paradoxical Neophyte's Shade**. Original-game players reported Tomb Ship generating it ([November 7, 2025 deck report](https://www.reddit.com/r/DestinyRising/comments/1oqjbm1/)). The new `ShadowSummonRules.tombShipPool` makes Shade eligible without changing `Card.isPlayable`: it remains **banned from deck construction and direct hand play**. Other Hive candidates must still be constructible with valid Power. These original-client hidden random **weights and the full exact pool remain unconfirmed**.
2. **Drop Pod selection safety**: The shared `ShadowSummonRules.dropPodPool` requires the printed numbered Line affix, validated Power/cost, and excludes the triggering Drop Pod itself. It does not confuse a card that merely **mentions** Line with an actual Line-affix card.
3. **Full-board Xuan Wei**: Damage still resolves, but with no empty target-side destination the movement logs a skipped move and creates no pending relocation deadlock. Lack of space is no longer wrongly counted as an unimplemented card effect. Exact original-client handling of the full-board case remains unproven.
4. **CPU summon placement**: `evaluateCpuPlacement` values both available Tomb Ship neighbors and rewards Drop Pod positions that actually form a two-unit Line **and** have an open left slot. It no longer systematically wastes those summons when a usable position is available.
5. **Local regressions**: `ShadowSummonPoolAndMovementTest.kt` provides **11** tests covering candidate eligibility and Shade deck exclusion, actual random Shade appearances, blocked/edge slots, Drop Pod printed Line filtering and placement, full-board / lethal Xuan Wei targets, movement not counting as new placement, and CPU choices for both summoners.

**Execution:** These changes are committed on `main`. **Kotlin/Gradle and Android runtime tests have not been executed**; `run_local_engine_checks.bat` runs local unit tests and debug compilation without GitHub Actions credits. Do not claim exact hidden summon probabilities without observed original-game data.

## 22 October — match-level verification harness and legal CPU decks

This pass changes runtime code and **adds tests for the actual Android card catalog**:

- **CPU deck legality** (`ShadowData.opponentDeck`): Older generators required 15 distinct card names and returned an empty mixed deck or changed a requested faction even when a legal 15-card single-faction deck could be formed with up to two copies per card. A centralized constructor now tries one copy per eligible name, then fills using second copies while enforcing the game's rarity, copy, deck-size and summon-only restrictions. The Mixed fallback also fills with other constructible cards when its predefined names are missing.
- **CPU deck regressions** (`ShadowCpuDeckIntegrityTest.kt`): Four tests check 8 unique cards producing 15 legal copies, sparse Mixed presets, never using summon-only cards or exceeding Exotic/Mythic limits, and legal fallback when a requested faction truly cannot build a deck.
- **Actual asset-backed match smoke tests** (`ShadowFullMatchSmokeTest.kt`): A local JVM test loads the shipped `cards.tsv`, `community_cards.tsv`, `expanded_cards.tsv`, `verified_video_cards.tsv`, and `card_details.tsv` with the same override precedence as Android. It validates catalog numeric stats, invokes each constructible card in an isolated battle state and checks that no resolver declares `UNIMPLEMENTED`, and plays **20 scripted six-round matches** (five CPU faction choices × four seeds) through real `ShadowEngine` and CPU decision logic. Board ownership, coordinate consistency, nonnegative Light, defeated-card cleanup, pending relocations, final match state, and deck legality are asserted with detailed diagnostic output.

**Critical qualification:** This is *not* a completed pass report. The new tests have been pushed to GitHub but **not run**. A successful invocation of an individual card in a basic state is not proof that its keyword interactions and edge cases are correct; match-level runtime verification and comparison against original Destiny: Rising footage are still required.

Run `run_local_engine_checks.bat` on the user's Gradle/Android SDK machine. It saves `shadow_engine_test_log.txt` and uses **no GitHub Actions minutes**. If tests fail, use the fixture's named card, seed and diagnostic log to correct the actual effect handler before certifying a mechanic.

## CPU target-choice and full-match validation pass

**Changes pushed to `main` on 22 October 2026:**
- `ShadowEngine.chooseAI` scores legal targets according to the intended ability family. Targeted 1–5 damage abilities prefer achievable eliminations and unshielded targets; row/adjacent splash effects value clusters. Wu Ming now favors removing meaningful Power bonuses while avoiding Mind Disruptor-protected or Overshield-blocked attempts. Wizard / Guarding Servitor prioritize allies not already Inspired; Rossi-11 avoids wasting another Overshield on a protected ally and chooses itself if every existing ally is shielded. Gladiator variants prefer allied target clusters so more units receive an outgoing-damage bonus. These are **CPU decision heuristics**, not a claim about the exact proprietary original-game opponent AI.
- Added **nine** new cases in `ShadowCpuTargetEvaluationTest.kt` covering lethal damage selection, protection, row splash targeting, meaningful Suppression, Mind Disruptor immunity, Inspired status, self-Overshield, Gladiator adjacency, and an actual `cpuPlayOne` hit.
- Fixed `ShadowCpuLegalPlacementTest.kt`, where the existing "legal target" scenario incorrectly put Gladiator's target on the opposing board. The test now verifies that an allied unit actually gains `damageBonus`.
- Enhanced the pre-existing `ShadowFullMatchSmokeTest.kt` (all four CPU factions plus mixed across four seeds, **20** match scenarios), adding detection of rejected CPU moves during turns, a final twelve-turn check, no pending relocation, and both final scores matching battlefield totals.
- Updated outdated UI audit labels for Helhest (damage-based bonus after prevention/overkill) and Tomb Ship (summon-only Shade permitted in its pool). Updated an outdated Kotlin migration comment.

**Testing limitation:** The new and strengthened Kotlin tests have been committed and source reviewed, but **not executed** using Gradle, an Android emulator, or a device during this GitHub-only pass. Run `run_local_engine_checks.bat` on the user's PC when available; do not run GitHub Actions or workflows that consume usage credits. Observed failures should be fixed before certifying gameplay parity.

## Full-match event-ordering and durable CPU diagnostics — 22 October 2026

**Implemented and pushed:**
- **Grenade Dreg placement watchers:** `fireAllyPlacement` stops dispatching subsequent arrival reactions if the incoming unit is defeated and removed during an earlier placement bonus (for example, when Unrelenting Captain grants a summoned Dreg +1 Power and two Elite Sniper Vandals counterattack). The behavior now matches the live-unit membership check already used by enemy arrival watchers. Existing live arrivals still trigger one matching bonus for each eligible Grenade Dreg.
- **Two new ordering regressions** in `ShadowSimultaneousEffectsTest.kt`: lethal arrival cancels downstream Grenade Dreg responses, whereas a surviving summoned ally grants +1 to both watching Grenade Dregs.
- **Durable CPU rejection telemetry:** `ShadowEngine.rejectedCpuMoves` records rejected computer plays without relying on the rolling 90-entry event journal. Reset on match start and displayed in `diagnosticReport()`. A rejected play increments this counter at the point the CPU receives the failed result.
- **Twenty full-match test scenarios** now assert `rejectedCpuMoves == 0` after each simulated turn and at final scoring, ensuring errors remain detectable even after older event lines roll off.
- **Three new diagnostic tests** in `ShadowCpuDiagnosticsTest.kt` verify long-log persistence, match reset, and an ordinary legal CPU move.

**Validation status:** Source files and GitHub branch changes are verified. **The JVM/Gradle test cases have not been executed** on the unavailable Android development PC and no GitHub Actions workflows were run. Treat runtime/parity claims as pending until `run_local_engine_checks.bat` successfully completes.

## Trigger lifecycle and simultaneous delayed explosions

**New source corrections:**

- **Scheduled detonation waves:** At the start of the original player's turn, all surviving transferred Cursed Thralls / Exploder Shanks due to explode are gathered and removed from their host battlefield *before* any blast is applied. Each then deals its printed 2 damage to its left/right neighbors. This prevents one scheduled bomb from damaging another already scheduled for the same turn. Bombs due in later rounds stay in play.
- **Resilient Captain watcher lifecycle:** Damage-triggered Dreg summons now require the Captain to still exist in its current battlefield position. Nested summoning and buff retaliation cannot activate a Captain removed earlier in the event.
- **Zhiyao watcher lifecycle:** When its +1 bonus causes an immediate enemy Elite Sniper Vandal retaliation that kills Zhiyao, no temporary bonus bookkeeping is attached to the already-defeated unit.
- **Regression suite:** Five new local tests added in `ShadowDetonationAndTriggerLifecycleTest.kt`, covering simultaneous bomb removal/order, future-round detonation timing, unaffected adjacent units, lethal retaliation deleting a Zhiyao before it can record a timed bonus, and surviving Zhiyao bonuses retaining the normal three-turn duration.
- **Status:** Source changes pushed to `main`; no GitHub Actions, Android emulator or Gradle test task was run during this session. Printed simultaneous detonation timing is handled as one event batch in the simulator; exact original-client animation ordering still needs video comparison.

## Sequential Sustained-hit death resolution / Scorpius patch parity

- **Elite Walker** (`Sustained (Back Row)`): after each of its up-to-two selected damage events, resolve the death wave, including Clone's left/right 3-damage splash, before another selected target is hit. If the first death splash has already removed the second target, do not apply a stale hit; a defeated Walker cannot continue firing.
- **Helhest** (`Sustained (Back Row)`): likewise settle Clone and other fatal reactions between its ordered left/center/right attacks. `resolvedDamage()` counts only events that actually occur, and the Power gain is not applied to a defeated Helhest. This is a conservative sequential-event implementation; exact animation/timing parity still needs client video comparison.
- **Seven local JVM cases** in `ShadowEndTurnChainedDamageTest.kt`: lethal Clone splash before subsequent Helhest/Walker targets, Overshield blocking Helhest's Power gain, suppressed sources, Scorpius's current one-hit text (including Front Row), two Scorpius sources, and Walker's printed Back Row restriction.
- **Scorpius patch discrepancy noted rather than guessed:** the user's recorded card-detail video describes **one random enemy** with no row restriction. September 2025 players reported an older client issue where Scorpius hit **two targets** only from the Back Row ([contemporary player report](https://www.reddit.com/r/DestinyRising/comments/1noflju/)), followed by posts about a Scorpius fix/nerf ([patch reaction](https://www.reddit.com/r/destinyrisingmobile/comments/1nqd1yn/)). We retained the newer **user-recorded printed ability** rather than hardcoding superseded reported behavior.
- **Verification:** source changes and regression source committed; Android/Gradle unit tests **not executed**, and no GitHub Actions workflows triggered.

## Power-bonus feedback chains and hit-by-hit Ogre effects — 22 October 2026

**Gameplay changes committed:**
- **The Disfavored:** A current card text specifies +2 Power for a *different* Hive ally when this Front Row unit receives a buff. Two copies could previously trigger one another recursively until the generic 32-level cutoff, causing artificial massive Power swings and `unresolvedEffects`. `ShadowEngine` now uses a per-causal-chain, unit-identity activation set: each Disfavored may activate once in an originating buff chain, but can activate again when a **separate bonus** starts later in the same turn. We also reject a bonus *before* mutating Power if the general 32-level safety limit is reached. This avoids claiming an unbounded feedback loop is correct behavior.
- **External evidence / scope caution:** [September 18, 2025 player tests](https://www.reddit.com/r/DestinyRising/comments/1nk8bxx/) document the game's Disfavored anti-feedback change and disagree on whether the limitation is per-turn versus per-causal-chain; [October 2025 player reports](https://www.reddit.com/r/DestinyRising/comments/1nyrr05/) describe multiple activations in a turn without infinite looping. Therefore the exact proprietary activation limit is **not confirmed**, and this is an explicit safety/parity approximation, not an assertion of identical hidden behavior.
- **Ogre / Tenacious Ogre / Shrieker:** Each selected damage target now resolves its death/splash wave before the next queued hit. Targets already defeated by Clone splash are skipped; a source that has left the board cannot keep hitting stale snapshot victims. This follows the chained damage ordering already applied to Elite Walker and Helhest.
- **Power-bonus handler correctness:** On-buff handling now checks current battlefield membership and Suppressed status before resolving a source's own buff effect, rather than looping over the entire allied board. Speaker, Denzin, Xur and Guerrilla Captain continue to use the same existing specific card-effect branches.
- **Ten new local regression cases** in `ShadowBuffChainResolutionTest.kt`: mutual Disfavored loop ends, fresh independent bonuses still retrigger it, separate Inspired end-turn buffs form separate chains, self-exclusion and Suppression/row restrictions, two- and four-target Ogre/Clone interactions, cascading Speaker buffs, Denzin summon/Grenade Dreg reaction, and Xur Rock/Zhiyao reaction.

**Verification:** Source commits are pushed to `main`. The Kotlin identity-set generic was separately typechecked with installed `kotlinc`, but the repository's complete Android/Gradle suite and match simulations **have not yet run**; no GitHub Actions usage was consumed. Full original-client Disfavored parity remains subject to footage/test evidence.

## Resilient Captain / Unrelenting Captain / Tan-2 / Jolder chained gameplay — 22 October 2026

**Concrete bugs corrected in `ShadowEngine.kt` and `ShadowCardEffects.kt`:**

1. **Resilient Captain's six-summon limit:** Previously the counter increased only if the summoned Dreg still occupied the destination *after* all arrival reactions. This incorrectly treated a Dreg instantly destroyed by an opposing Elite Sniper Vandal as a nonexistent summon, so further hits could exceed the original printed six-deployment cap. `spawnAt()` now returns whether a legal Dreg actually **entered** the slot, independently of whether it survived; each Captain reserves its next activation **before** recursively dispatching the summoned Dreg's triggers, and rolls the reservation back only on failed placement. The counter remains independent for each Captain. No counter is consumed when damage is entirely prevented by Overshield or when the right slot is occupied.
2. **Unrelenting Captain bonus separation:** Previously two Captains were merged into `buff(newDreg, 2)`. That erroneously caused Tan-2's "**next Power bonus**" effect to double *both* bonuses as one event. Each eligible Captain now grants its own `buff(newDreg, 1)`. Thus one Tan-2 doubles the first +1 into +2 while the next Captain grants its regular +1. If the first bonus activates lethal sniper retaliation, the now-destroyed Dreg cannot receive the second bonus.
3. **Summoned-unit visual revisions:** A newly placed summon increments `placementRevision` and updates the last-placed coordinates and side, allowing existing board revision/placement-cue consumers to observe summoned arrivals instead of seeing only the triggering hand play. Rejected placements do not advance the revision.
4. **Reentrant damage checks:** `fireFallenDamage` now stops processing remaining Captains if an earlier nested summon/retaliation has removed the original Fallen damage source.

**New regression coverage:** `ShadowCaptainAndDoublerChainTest.kt` contains **12** local JVM tests covering separate Unrelenting buffs; Tan-2 doubling exactly one event; lethal sniper response; Resilient Captain's six summon count even when every Dreg is instantly destroyed; Overshield-blocked hits; blocked right slots; multiple Captains; successful/rejected summon placement metadata; Jolder blocked damage and next real damage; two Jolders' separate activations; damage reduction; and the combined Jolder + Captain + Unrelenting + Tan-2 event chain.

**Verification caveat:** Source committed and reviewed on GitHub `main`; the Android/Gradle/JUnit tests and full match simulations were **not executed** in this GitHub-only pass. No GitHub Actions or credit-consuming CI was triggered. Exact hidden original-client ordering for simultaneous Captain triggers remains provisional pending user gameplay footage.

## Saladin damage responses and Elite Sniper Vandal retaliation chains

**Implemented on GitHub main, 22 October 2026:**

- **Multiple Saladins and damage-survival ordering:** `dealDirect` formerly called `buff(target, 3)` separately for every Saladin. Because each buff immediately resolved reactive effects and the death sweep, a damaged Human at temporarily negative Power could be removed after the first +3 and never receive the second Saladin's +3 from the **same hit**. The engine now applies each eligible Saladin's own +3 bonus as a distinct Power acquisition before beginning the corresponding on-buff trigger dispatch. This allows all matching damage reactions to determine whether the Human survives before any sniper/death cleanup.
- **Tan-2 parity preserved:** Every Saladin bonus remains individually applied (rather than one combined +6). Tan-2 doubles only the **first** eligible +3 into +6; a second Saladin's +3 remains a separate event. Other calls to `buff` still resolve their on-buff reactions immediately.
- **Sniper counterattacks:** The `Elite Sniper Vandal` once-per-target-per-turn identity tracking still applies through nested Saladin bonus chains. The new cases assert two snipers each attack once even when their damage causes additional Saladin Power bonuses. Fully shielded or reduced-to-zero damage produces no Saladin reactions, and Suppressed Saladins cannot contribute.
- **Eight regression cases** in `ShadowSaladinRetaliationChainTest.kt` cover two-Saladin rescue from a lethal-looking hit, insufficient single-Saladin rescue, Tan-2's one-bonus consumption, two nested snipers, Suppression of one responder, Overshield and full mitigation, independent later hits, and Saladin's response when it is itself the damaged Human.

**Caveat:** Both printed Saladin (+3 whenever your Human unit takes damage) and Elite Sniper Vandal (one hit per enemy unit per turn) are grounded in the user's original in-game recorded card text, but whether all simultaneously triggered Saladins respond ahead of sniper/defeat resolution needs direct original-client replay verification. This engine uses event-batched Power gains to prevent partial resolution and premature unit removal. No Gradle/JVM tests or Android build were executed during this remote source update and no GitHub Actions were invoked.

## Stacked Scorched effects and independent source damage — 22 October 2026

**Existing engine bug:** Both Ayuela and Resilient Grenade Dreg give a newly arrived opposing unit a two-turn damage effect. The previous model stored only `scorched`, `scorchTurns` and `scorchSource` on the victim, so when two identical watchers reacted to the same arrival, the second watcher **overwrote** the first instead of creating its own damage event. This also lost the original caster's attribution and prevented independent expiry.

**Changes pushed:**
- Introduced `ShadowEngine.ScorchEffect(source, damage, turns)` and a per-unit `scorchEffects` collection. `addScorch()` retains each separate original source and lifetime; `syncScorchView()` continues exposing aggregate `scorched` and `scorchTurns` and the single caster where unambiguous, preserving the legacy UI and preexisting tests.
- Both `fireEnemyPlacement` sources now append separate two-turn 2-damage effects instead of replacing a single pending status.
- Owner end-turn processing applies each hit and settles death effects before attempting the next status hit. A destroyed unit is not damaged again from a second expired/pending source. Each tick keeps the originating card as its damage source (important for Fallen damage reactions and Saladin).
- Suppression removes *all* active Scorched instances; it does **not** confer permanent immunity to being Scorched by a later separate status application. Direct assignments of the previous three status fields are still accepted for compatibility.
- `ShadowStackedScorchTest.kt` adds **seven** JUnit scenarios: two Ayuelas, two Resilient Grenade Dregs, independent source persistence after watcher removal, suppression cleanup, staggered expirations, lethal first tick, and distinct Saladin / Elite Sniper Vandal responses during a stacked tick.

**Original-client caveat:** The user-recorded text confirms each caster deals damage over two turns but does not explicitly reveal exact interactions between duplicate casters; treating them as separate instances is a reasoned implementation based on their independent printed ability activations. Recheck against real-game footage when available. The new Kotlin/JUnit tests have been committed, **not run**, and no GitHub Actions were triggered.

## Full-match unresolved-effect observability and transferred-unit arrivals

**Further source fixes committed on 22 October 2026:**

- **Twenty full-match scenarios now fail on unresolved card branches.** `ShadowFullMatchSmokeTest.fullSixRoundMatchesAgainstEachCpuFactionDoNotSoftLock` checks the cumulative `unresolvedEffects` after each turn and at the match result; a battle can no longer pass merely because it reached round six without deadlocking while skipping a printed-effect implementation.
- **Unresolved reasons survive long matches.** `ShadowEngine.recordUnresolvedEffect` accumulates `unresolvedEffects` while retaining the **first 64** detailed reasons in `unresolvedEffectDetails`, independent of the 90-entry rotating `journal`. `incomplete(card, reason)` and the excessive nested Power-bonus safety guard use the same reporting path. The persistent list clears on a new `start`, and `diagnosticReport` displays the cause list. Added **three** local tests for log rollover, retention limit, and match reset.
- **Transferred-unit placement metadata:** Cursed Thrall and Exploder Shank physically move into the opposing Front Row when played. The transfer now increases `placementRevision` for that new arrival, updates the last-placed opposing position (already present), and performs a sweep after enemy placement watchers. This keeps existing battle UI placement-cue observers from pointing only at the original slot that the card immediately vacated. Added **two** regression tests for both card names and consecutive transfers.

**Validation constraints:** The cumulative unresolved-effect assertion is deliberately strict and may expose older source branches that need implementation. Source changes and test content were inspected after pushing. The suite has **not** been run: neither Gradle/Android tests nor GitHub Actions were executed. A passing six-round match would still establish engine consistency, not proof of perfect parity with the original game's hidden rules.

## Full opposing Front Row transfer fallback — October 22, 2026

- **Cursed Thrall and Exploder Shank:** A full opponent Front Row offers no legal destination for the printed automatic transfer. Previously this ordinary board-space constraint invoked `incomplete`, raising `unresolvedEffects` during otherwise functional six-round matches. The engine now logs `TRANSFER BLOCKED`, keeps the original hand-played unit in its source position, and never overwrites the occupied opponent board. It does not schedule a detonation because no transfer occurred.
- **Regression:** One new parameterized loop test in `ShadowDetonationAndTriggerLifecycleTest.kt` checks both transfer cards against a fully occupied opposing Front Row. It verifies unchanged enemy occupants, no phantom fuse/detonation, placement history, and zero unresolved implementation branches.
- **Related adjustment:** A separate concurrent commit `af7ac781` already fixed the old Tomb Ship CPU test to differentiate the played source from subsequent `lastPlaced` summon metadata; it was preserved unchanged.
- **Original game uncertainty:** The exact fallback in the proprietary client when no destination exists remains unverified, so retaining the played card in its original slot is a documented best-effort policy until original-game footage is available.
- **Validation:** Source and tests were committed for local execution; Gradle, Android builds, and GitHub Actions were not run here.

## CPU scoring prioritizes achievable eliminations over raw target Power

- **Fixed an AI selection defect:** `ShadowEngine.chooseAI` previously scored fixed-damage target options by `unitPower + area + lethal bonus`. This could make the CPU fire a 2-damage Vandal at an 80-Power unit instead of immediately defeating a 2-Power enemy. The high target's 80 raw Power overwhelmed the earlier +18 lethal incentive.
- **New target ranking:** A feasible lethal hit receives a decisive removal bonus; an ordinary nonlethal fixed-damage hit is rewarded for dealing damage and for approaching elimination, rather than for arbitrarily high remaining target Power. Existing area-effect weighting still favors row/neighbor clusters. Random/variable damage cards continue using a modest target-value heuristic until their exact damage is known. Shields still receive their existing anti-waste penalty.
- **Regression cases:** Three new local JVM tests in `ShadowCpuTargetEvaluationTest.kt` verify that a 2-Power enemy wins over an 80-Power enemy for Vandal, that a 3-Power opponent is preferred over a 50-Power opponent when neither can be eliminated by that hit, and that Ikora still favors a populated row over a very high-Power singleton. The class now contains **12** tests.
- **Parity scope:** This is an AI decision-quality correction, not a claim that Destiny: Rising's original opponent has the same scoring formula. As with other local regression tests, no Gradle or GitHub Actions tests were run during this change.

## Regression correction and printed-Power-bonus separation — 22 October 2026

- **Tomb Ship test correction:** `ShadowSummonPoolAndMovementTest.cpuPrefersTombShipPositionWithTwoOpenNeighbors` formerly used the mutable `lastPlacedRow/Column` to find the source card. Since the prior summoning UI update correctly advances that marker on every summoned unit, it actually pointed to the **rightmost summoned Hive card**, not necessarily Tomb Ship. The revised test reads the original `CPU ▶ Tomb Ship` play-log event, verifies the summoned neighbors, and separately checks three placement revisions and the most recent right-hand summon cue. The original gameplay implementation was preserved.
- **Gwynn printed penalty:** The community card text explicitly says Gwynn **loses 2 Power** after its 3-damage opposing splash, not that it deals 2 damage to itself. The previous `dealDirect(actor,actor,2)` incorrectly activated Saladin and allowed Overshield to block the self-penalty. Now Gwynn takes a direct -2 Power penalty, logs it as such, and settles death waves between separate selected splash targets. Added **two** `ShadowSimultaneousEffectsTest` cases for Overshield and Saladin.
- **Bazinet and Thinktank active bonus accounting:** Previous `powerBonus(unit)` used `max(0, currentPower - printedPower)`. This loses actual bonus status when a buffed unit has taken enough damage to fall below printed Power; for example, 10 printed +4 bonus -6 damage leaves 8 Power **and still carries a +4 bonus**. `powerBonus` now returns active stored `unit.bonus` plus any live Column/Line Power aura, independent of previously suffered damage. Suppression still clears stored bonuses and disables the target's ongoing aura. **Five** `ShadowPrintedBonusAccountingTest` regressions cover bonus retention after damage, Bazinet attacking damaged-buffed targets, Thinktank damage based on the highest true Power bonus, Column aura accounting, and Suppression reset. This is specific to the cards' printed "bonus" clauses and does not treat outgoing-damage modifiers as Power bonuses.
- **Testing:** All source and **seven** new regression cases were pushed to `main`; one preexisting test was corrected. Full Kotlin/JUnit/Gradle execution and Android build **not performed**. No GitHub Actions or usage-consuming checks initiated. Original-client comparison is still required for any uncertain timing behavior.

## Printed Line-affix audit and random Line summoning eligibility — 22 October 2026

**Issue fixed:** `ShadowPositionRules.hasLineAffix` previously matched a bare numeric phrase such as `2-Unit Line` anywhere in card effect text. A card merely saying "If an allied 2-Unit Line exists, gain Power" could therefore count as a Line card, extend a formation, and become an eligible random **Drop Pod** summon even without its own printed Line keyword.

**Implementation:** Require an actual printed `When Played (...Unit Line)` or `Sustained (...Unit Line)` ability header, accepting the verified hyphenated (`2-Unit Line`) and unhyphenated (`2 Unit Line`) variants, and combined header conditions such as `Sustained (Back Row, 3-Unit Line)`. Match the card's own printed affix rather than incidental numeric references in its prose. Cache the compiled matcher so repeated CPU placement evaluation does not repeatedly construct regular expressions. This parser remains a simple approximation of printed data, not a full natural-language rules interpreter.

**Catalog audit:** Compared the old and new matchers against the **121-card** combined base/community/expanded/video text records with video overrides. Both select the exact same **11 verified printed Line cards**, and none are removed. It changes future/malformed effect descriptions only. The affected consumers are `ShadowPositionRules.contiguousLineUnits`, conditional Line abilities, `ShadowEngine`'s CPU Line-placement evaluation, and `ShadowSummonRules.dropPodPool`.

**Regression changes:** Extended `ShadowKotlinRulesTest` with negative explanatory-mention cases and a positive combined-header case. Extended `ShadowSummonPoolAndMovementTest` with a fictitious Cabal card whose text mentions a `2-Unit Line` requirement but grants no printed Line affix: this card is excluded from random Drop Pod summons and does not begin or extend a contiguous Line. Added one new dedicated test and further assertions to existing tests.

**Verification:** Code and GitHub content changes are confirmed on `main`; regex matches were cross-checked against the source TSV catalog. No Gradle, Android build, JVM test suite, or GitHub Actions were executed in this pass. The suite still needs local execution before certifying runtime parity.

## Blocked Cursed Thrall / Exploder Shank transfer and metadata-audit alignment

- **Full opposing Front Row:** The engine already logs `TRANSFER BLOCKED` and leaves Cursed Thrall or Exploder Shank in its source slot rather than overwriting a defender or inventing a phantom detonation. Added `blockedTransferKeepsUnitAtOriginalPositionWithoutPhantomDetonation` to `ShadowDetonationAndTriggerLifecycleTest.kt`, covering **both** transfer cards, source ownership/position, occupied opponent slots, no extra placement revision, no scheduled fuse, and no `unresolvedEffects`. The *exact original-client full-row fallback* is still not proven by user footage.
- **Correct static coverage counts:** `tools/audit_effect_coverage.py` now mirrors `ShadowData.load` by reading and applying `verified_video_cards.tsv` after the community catalogs, rather than auditing obsolete effect text for the 85 user-video-overlaid cards. The audit checks duplicate/malformed video records and numeric cost/Power, and reports the overlay total. Xur is no longer incorrectly labeled as incomplete because Rock has known 1 Power / 1 Light data and is summon-only (see `card_details.tsv`).
- This is a **source/metadata audit**, not an assertion that all printed abilities have passed real match parity.
- All changes are pushed to `main`; JVM tests and Android builds **have not been run**, and no GitHub Actions workflows were triggered.
