# Shadow Shaper Duels — Full interaction audit and remaining validation backlog

**Date:** 2026-10-22. **Branch:** `main`. **Scope:** 121 unique card names in the Android assets, the active Kotlin battle/effect engine, shared helpers, previous source audits, and authored regression-suite structure. **Type:** source inspection, not a passing Android build or original-game parity certification.

[Complete 121-card audit matrix](CARD_INTERACTION_MATRIX_2026_10_22.md) · [Prior gameplay corrections](GAME_PARITY_AUDIT_2026_10_22.md) · [Original Shadowshaper keyword reference](https://www.todayindestiny.com/rising/shadowshaper_duels/keywords)

## 1. What is and is not complete

| Indicator | Confirmed repository inventory | Interpretation |
|---|---:|---|
| Unique named cards | **121** | Does not count art alternatives as new gameplay identities |
| Cards with basic/no-effect wording | **14** | Must still obey play/deck/board rules |
| Cards with non-basic effect wording | **107** | **All 107 names** have literal references in `ShadowEngine.kt` or `ShadowCardEffects.kt` |
| Non-basic effects proven through executed local Gradle tests in this audit | **0 confirmed** | **Not executed**; not equivalent to known broken |
| Video-overlay card effects | **85** | A record in `verified_video_cards.tsv`; additional card-detail notes also reference videos |
| Cards with `card_details.tsv` entries | **119** | Missing: **Gwynn** and **Tenacious Shielded Knight** |
| Missing numeric Power/Light from merged data | **0** | All 121 merged names have numeric values; does not independently authenticate every number |
| Intended non-deckable summoned names | **4** | Clone, Guerrilla, Paradoxical Neophyte's Shade, Rock |
| Priority A / B / C source-interaction checks | **28 / 64 / 29** | An audit-workload ordering, **not** broken/working card counts |
| Authored broad six-round match scenarios | **20** | Mixed/Hive/Fallen/Cabal/Humans × four RNG seeds; have not been run during this audit |

### Source coverage is not rule coverage

The existing `tools/audit_effect_coverage.py` detects **literal card-name references**. This passes even if a card appears only in a "known sustained" list while its exact condition is not activated. Similarly, `ShadowFullMatchSmokeTest.everyConstructibleCardCanBePlacedWithoutUnresolvedEffectBranches` can play a Line-restricted or row-restricted card without triggering its effect; successful placement and `unresolvedEffects == 0` are **not evidence its conditional ability works**. The 20 complete matches probe soft-locks and runtime invariants, not every pairwise interaction.

**Answer to "How many remain?"** There is no defensible count of **confirmed malfunctioning cards** until the local suite and targeted active-state tests run. The concrete structural tasks are **2 detailed metadata records**, **36 names without a current video-overlay row** (some already have original-video notes), plus validating all **107 non-basic abilities** and their high-risk interactions. **No non-basic card lacks a literal source reference.**

## 2. Highest-priority remaining rule questions (P1)

| Open issue / concrete cards | Current source behavior | Why QA still needed | Next validation |
|---|---|---|---|
| **Mishra** | Base text and on-play code grant **+3** to left/right | TodayInDestiny's Mishra entry describes **+2**; this card has no video overlay. Source/game-version conflict, not a proven engine bug | Compare the active in-game card face and a before/after play; update printed text, handler and tests together only if verified |
| **Gwynn / Tenacious Shielded Knight** | Present as valid numeric community records; card handlers exist | Both lack detailed metadata; video overlay rows missing | Acquire/confirm rarity, cost, Power, acquisition, trigger and current exact printed text |
| **Tomb Ship / Drop Pod** | Hive pool includes summon-only Shade; Drop Pod samples literal printed Line cards | Original pool contents, weighting and duplicate allowance unknown | Repeated original-client summons with known board slots; validate distribution and eligible identities |
| **Xuan Wei** | Deals 5 then requests relocation to any free location on the target side; skips if no free slot | Original destination restrictions / ownership / full-board outcome not confirmed | Damage surviving target, lethal target, full 12-slot field, and last-slot cases |
| **Cursed Thrall / Exploder Shank** | Transfer to free opposing Front Row location and later detonate; full row => `TRANSFER BLOCKED`, source stays | The fallback is a safe engineering choice, **not confirmed original-game behavior** | Original client with all opposing Front Row spaces occupied; simultaneous bombs and splash waves |
| **The Disfavored** | Identity-scoped anti-loop guard, once per *causal bonus chain* | Original anti-feedback change reported, but whether limit is per turn or chain is not definitively established | Two Disfavored copies with Inspired, Speaker and externally triggered separate buffs |
| **Scorched: Ayuela / Resilient Grenade Dreg** | Separate 2-damage, two-own-turn effects per watcher; first lethal tick prevents next | Source text supports two-turn damage but does not prove duplicate-stacking or exact tick ordering | Two sources, staggered arrival, Suppression, Overshield, Saladin retaliation, source removed |
| **Elite Sniper Vandal** | Back Row; a finite two-owner-turn response; once per target per action-turn ID | Original duration semantics, multi-source ordering, and whether a denied hit consumes the check need footage | Two snipers + Tan-2 + Saladin + lethal/blocked bonus |
| **Helhest** | Per-hit successful damage adds Power, including overkill; targets include chosen enemy and neighbors | Exact Power conversion and whether blast is simultaneous have not been established | Low-Power targets, Overshields, Saladin recipients, Clone death splash, Jolder |
| **Cabal numbered Line and Column** | Printed Line-affix adjacency; Column requires vertical partner | Some old notes describe same-faction Line while revised code uses printed Line. Formation geometry needs direct-client proof | Line interrupted by non-Line card; 2/3/4/5/6-line cases; suppression and moved/defeated members |
| **Round/draw/board constants** | Six rounds; first/second opening hands 5/4; new-round draw 1; 2×6 spaces | Some timing and board-width comments explicitly remain provisional | Frame/turn-by-turn comparison to original-game match and original replay |
| **AI vs original NPCs** | Legal local CPU heuristic and generated faction decks | Not the original NPC scripts, difficulty tiers or original behavioral choices | Regression smoke + distinguish "legal simulation" from "NPC parity" |

External source note: public TodayInDestiny descriptions can reflect older text (for example, earlier Grenade Dreg/Resilient Captain/Wolf wording); **user-recorded in-game card details take precedence when the versions conflict**. Do not silently overwrite verified video effects from a community page.

## 3. Interactions to validate next (from most likely to reveal breakage)

These are **test plans**, not claims that the cases pass.

| Order | Chains / representative cards | Must assert |
|---|---|---|
| **1** | **Saladin × Tan-2 × Elite Sniper Vandal × Jolder × Servitor** | Damage vs Power-bonus sequence, no bonus on fully prevented hit, separate bonuses, retaliation once per target per turn, dying units removed correctly |
| **2** | **Ayuela × Resilient Grenade Dreg × Saladin × Zhiyao** | Per-source Scorched timers, when first tick fires, lethal first tick cancels later tick, suppression cancellation, original caster attribution |
| **3** | **Zhiyao × Inspired × Tan-2 × Suppression** | Multiple temporary bonuses do not overwrite; each expires on the correct owner turn; no double subtraction or phantom +Power |
| **4** | **The Disfavored × The Speaker × Ogre/Tenacious Ogre × Denzin × Xur** | Finite recursion, correct identity/causal guard, summoned arrivals do not create runaway loops, fresh independent event still works |
| **5** | **Jaren × Ikora × Goliath Tank × Clone × Helhest × Elite Walker** | For sequential effects, remove killed targets before subsequent hits; for area effects, verify simultaneous death-wave behavior, no stale targets or splash doubling |
| **6** | **Resilient Captain × Unrelenting Captain × Elite Sniper Vandal** | Six-summon cap counts deployments even if killed on entry; multiple Captains remain independent; Tan-2 doubles only one actual bonus |
| **7** | **Scorpius × Shinka Scorpius × Pandora-7 × Inspired/Scorched** | End-turn snapshot ordering, Back Row/Line conditions, status expiry before or after Sustained activation, source destruction mid-turn |
| **8** | **Phalanx / Shinka Phalanx × Umeko × Servitor × Jolder × Rossi-11** | Correct outgoing/reduction/shield ordering and one-time consumption; no reflected/phantom damage |
| **9** | **Tomb Ship × Drop Pod × Paradoxical Neophyte × Carrier/Ketch × Xur/Common Cover** | Every summon is legally placed, uses eligible pool, counts placement and applies ally/enemy watchers; no generated card played from hand |
| **10** | **Xuan Wei × Cursed Thrall / Exploder Shank × full-field cases** | No deadlocks, relocation vs new-arrival distinction, deterministic transfer fallback, timed detonation and Clone death splash |
| **11** | **Bazinet × Thinktank × Hyssop × active buffs / Column auras** | Printed "bonus" is tracked separately from injury; damaged yet buffed units remain eligible; Suppression removes active bonuses |
| **12** | **Complete games × 5 AI factions × 4 seeds** | Twelve legal turns / six rounds, no rejected CPU plays or unresolved printed effects, stable board coordinates, no negative Light, correct final score |

The previously authored tests cover **parts** of many of these cases; the missing deliverable is **running them** and then comparing exact outcomes to original-client evidence where provisional.

## 4. Test completeness gaps

1. **Baseline card-by-card activation:** The current generic `everyConstructibleCardCanBePlaced...` test only guarantees a play succeeds. Add **active-trigger fixtures** for conditional When Played, numbered Line, Back Row/Front Row, Column partner, empty vs occupied summon slots, and valid/invalid targets.
2. **Pairwise cross-effect coverage:** There is no exhaustive 107×107 interaction grid. Do not attempt every mathematical pair; first cover all **different mechanic families** in the table above, then add individual card variants where rules differ.
3. **Temporary-status timeline validation:** Test owner-turn countdown versus round boundary, source removal, suppression mid-turn, stacking by multiple copies, and damage prevention. The old scalar Scorched fields are retained as UI-compatible aggregates, but source attribution is null when >1 caster.
4. **Game-client proof:** The current recorded TSV overlays and card-details videos provide card text, not comprehensive resolution traces. Original-client screenshots/recordings are required for timing ambiguities.
5. **Local compilation:** Run `run_local_engine_checks.bat` from the repository's Windows root when the PC is available. It executes `:app:testDebugUnitTest` and `:app:assembleDebug` locally, captures `shadow_engine_test_log.txt`, and uses **zero GitHub Actions credits**. This audit did not run it.

## 5. QA sign-off definition

A card can be marked **fully verified** only after (a) exact current printed effect/source established, (b) correct active-state condition verified, (c) its protected/blocked/suppressed/edge case tested, (d) its interactions with same-event watchers tested, (e) local JVM regression actually **passed**, and (f) a representative original-client trace matches for game-specific timing. Until then: **SOURCE REFERENCED — GAMEPLAY QA PENDING**.

**Summary:** No known missing named handler references. The immediate work is not bulk adding cards; it is resolving **two metadata gaps**, **specific unproven rule behaviors**, and **conditional activation / chain / temporary-status runtime validation** across the 107 non-basic cards. The companion per-card matrix gives a priority for every one of the 121 catalog entries.
