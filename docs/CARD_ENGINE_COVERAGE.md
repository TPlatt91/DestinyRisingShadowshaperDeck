# Current rule fidelity update — 21 October 2026

**Shared keyword resolution advanced:** Only numbered Line-affix cards count in contiguous Line formations. A plain adjacent Cabal unit now interrupts the Line. Target selection, ongoing 3/4-unit auras, damage bonuses, Drop Pod, and CPU projected formations use the same helper. **Scorched** keeps the inflicting caster as damage source; **Zhiyao** triggers shared Power-bonus resolution, including Tan-2 doubling, with a three-turn expiration. In-app Rules reflect these definitions.

**Local test status:** Updated older Line-related fixtures and added seven keyword/interaction regression cases. They are committed but have not run under Gradle or on Android. These rules remain subject to match-level comparison before declaring 100% parity.

---

# Batches 13 and 14 — gameplay integration status (21 October 2026)

- **20/20 original-video card definitions** committed; unique records for Power, Light, rarity, faction, original effect text and acquisition
- **20/20 corresponding gameplay source references**, including new/adjusted Molkrul, Maru, Shinka Gladiator and Shinka Centurion handling
- **11 local regression cases** in `ShadowBatch1314VideoRulesTest.kt`
- **Paradoxical Neophyte's Shade** remains a non-deckable `Summoned unit` with 1 Power, 6 Light and Inspired Hive scaling
- **No Android build, local unit-test run or actual match verification has occurred**. Do not label the entire batch as runtime-verified.
- **Pending exact original rules:** Line/Column formation qualification is currently based on adjacent matching-faction units, not fully demonstrated from the card-detail recording alone. Instant defeat with an overshield may need a separate interaction check.

Source details: [Batch 13–14 metadata and rules](BATCH_13_14_VIDEO_METADATA.md).

---

# Batch 12 video implementation update

**8/8** cards have unique video-confirmed definitions and detailed metadata: Tenacious Ogre, Tenacious Wizard, Shinka Vigile, Shinka Scorpius, Juggernaut, Pandora-7, Harrow, Helhest. **8/8** have source-code battle handlers, and 10 local JVM regression scenarios have been added.

**Unverified behaviors:** Helhest's precise Power-per-damage conversion is not numerically specified by the video. Juggernaut's Column geometry still needs an original-game behavior check. All Batch 12 regression tests and Android gameplay remain **QA pending** and have not been executed in this environment.

[Batch 12 original video metadata, effects, and rules](BATCH_12_VIDEO_METADATA.md).

---

# Batch 11 implementation status — 21 October 2026

- **8/8 original-video definitions** committed (metadata, Light costs, Power, factions, rarity, acquisitions and printed abilities)
- **8/8 source-code effect handlers** present and checked in repository
- **10 local JVM regression cases** committed as `ShadowBatch11VideoRulesTest.kt`
- **No GitHub Actions credits used**
- **Compilation, JVM tests, and on-device match execution have not been performed** and remain QA pending
- **Tomb Ship**: the two Hive summons now resolve; source video does not reveal the exact random summon pool, so the engine currently uses all eligible verified constructible Hive units
- **Elite Walker**: end-of-turn two-target damage now requires Back Row placement
- **Luksis-3**: counts only `Rock`, not `Rock Pile`, as the card actually says

See [Batch 11 source metadata](BATCH_11_VIDEO_METADATA.md).

---

# Current batch-coverage snapshot — Batches 01–10

**2026-10-21:** All **83 recorded card appearances** (80 unique cards) across Batches 01–10 have card catalog and detailed metadata records. The newest Batches 09–10 include **23/23 video-derived metadata definitions**, source-referenced Kotlin gameplay handlers and 21 authored local JVM test scenarios. Code present does **not** mean Android gameplay has passed QA. Batches 09–10 detail: [video metadata](BATCH_09_10_VIDEO_METADATA.md) and [gameplay implementation](BATCH_09_10_GAMEPLAY_IMPLEMENTATION.md).

The total catalog contains 114 distinct card names; do not claim all cards are complete based solely on these video batches. Original Line affix formations and Drop Pod's exact random summon pool remain test/verification priorities.

---

# Shadowshaper Duels — current video batch metadata and engine audit (21 October 2026)

**This current report supersedes the older 20 October inventory totals beneath it.**

## Batches 01–08: recorded card coverage

| Recorded batch | Card appearances | Catalog and detailed Power/Light metadata | Engine effect reference |
|---|---:|---|---|
| 01 | 8 | 8/8 | All non-basic cards have a source handler |
| 02 | 8 | 8/8 | All non-basic cards have a source handler |
| 03 | 8 | 8/8 | All non-basic cards have a source handler |
| 04 | 3 | 3/3 | All non-basic cards have a source handler |
| 05 | 9 | 9/9 | All non-basic cards have a source handler |
| 06 | 8 | 8/8 | All non-basic cards have a source handler |
| 07 | 8 | 8/8 | All non-basic cards have a source handler |
| 08 | 8 | 8/8 | All non-basic cards have a source handler |

There are **60 video-card appearances covering 57 distinct named cards**, including repeated appearances across Batches 01 and 03. Among these 57, **45 have non-basic card text and all 45 have a literal engine reference**; the remaining 12 are basic cards. This is a source-coverage check and does **not** mean all 45 effects have passed runtime testing or that all edge cases are faithful.

The full app currently has **114 distinct catalog names**. **60** names have extended `card_details.tsv` records (the 57 batch cards plus 3 others), while the remainder have only their base catalog rows. This check is intentionally scoped to video Batches 01–08 and does not imply that the entire 114-card catalog has independently verified original in-game metadata.

Metadata precedence: base catalog files -> `verified_video_cards.tsv` -> `card_details.tsv`. Original Batch 08 video added eight metadata records, and `video_keyword_rules_01_08.tsv` preserves eight glossary terms including Line, Column and Inspired.

### Recent gameplay fixes and outstanding QA

Source handlers added or corrected across the eight batches:
- Rossi-11: targeted Overshield blocks the next damage, Suppression or instant destruction instance, then expires.
- Mind Disruptor: when active, stops Suppression of the immediately left allied unit.
- Resilient Captain: each Fallen damage event can summon a Dreg directly to the right (max 6 per Captain, while space is available).
- Unrelenting Captain: deployed Dregs receive +1 Power, including summoned units.
- Exploder Shank: crosses to an open opposing Front Row slot and detonates at the start of the original owner's next turn; 2 damage to both adjacent units.
- Batch 06–07: corrected Wu Ming identity, Umeko damage, Xur Rock summons, Tenacious Hexguard, Umbliks Prime and Elite Sniper Vandal; source data and Kotlin tests added.
- Batch 08: Shinka Psion, Shinka Legionary, Shinka Phalanx, Incendior, Decurion, Psion, Jolder and Finnala are represented by recorded video metadata; Finnala's printed -3 **Power** is no longer misinterpreted as damage.

**None of the newly authored JVM tests have been executed on this host, and no Android device match has been verified.** The tests run locally at `app:testDebugUnitTest` and do not consume GitHub Actions credits. Existing partial/outstanding cards in the larger 114-card catalog include Tomb Ship and Drop Pod (unverified random summon pools) and Xuan Wei (relocation details to be verified).

The current Line geometry uses a contiguous same-faction approximation; the recorded keyword tooltip explicitly mentions a *Line affix*. This is an outstanding exact-rule fidelity check, **not** something the metadata import can prove. Other edge cases—full destination rows, simultaneous sustained triggers, and shield/ability timing—also remain QA pending.

Artwork packages are distributed separately. Adding their metadata does not automatically import the image or GIF binaries. The video art is not claimed to be installed unless the matching files exist in `app/src/main/assets/card_art`.

### Local verification (without GitHub Actions)

```bat
gradlew.bat :app:testDebugUnitTest
gradlew.bat :app:assembleDebug
```

New regression source: `ShadowBatch0108VideoRulesTest.kt`. Existing per-batch tests: `ShadowBatch04EffectsTest.kt`, `ShadowBatch05EffectsTest.kt`, `ShadowBatch0607EffectsTest.kt`.
---
# Historical 20 October card engine coverage (superseded) — October 20, 2026

This report covers **110 distinct named records** currently included in the app's three card data files. It is **not proof that 110 represents the entire current in-game catalog**, and it does not imply all 110 effects work correctly.

## Current coverage classification

| Classification | Entries | What it means |
|---|---:|---|
| Simple base-Power cards | 14 | Play cost, placement and Power are supported; no printed special ability |
| Has an effect handler or relevant shared rule, pending QA | 91 | Engine contains an ability reference/handler, but accuracy, activation conditions and interactions require gameplay verification |
| Explicitly incomplete / partial | 4 | Script has a known unresolved outcome, token pool, or movement choice |
| Data-blocked | 1 | Lacks verified base Power and/or play cost |
| **Total** | **110** | Distinct card names, not alternate image variants |

These are **inventory classifications**, not verified full implementation counts. The 86 still need effect-by-effect testing. Source-level code checks do not validate Android behavior.

### Data-blocked records

- **Rock:** original summoned token's Power and Light cost remain unverified.


### New card-face verifications and unlocked summons

TodayInDestiny's original card images show **Carrier = 3 Power / 2 Light**, **Guerrilla = 2 Power / 1 Light**, and **Clone = 1 Power / 1 Light**. Clone and Guerrilla remain non-deckable generated units. After populating those verified values:

- **Carrier** now passes deck legality and summons left/right Dregs.
- **Mu Ying** summons a 2-Power Guerrilla on each empty adjacent slot.
- **Denzin** summons a 2-Power Guerrilla on the left when Denzin gains Power.
- **Ning Fei** places a 1-Power Clone in a random free opponent Front Row slot; Clone uses the existing 3-damage adjacent death splash.

Sources: [Carrier card face](https://www.media.todayindestiny.com/images/rising/shadowshaper_duels/card/carrier.webp), [Guerrilla card face](https://www.media.todayindestiny.com/images/rising/shadowshaper_duels/card/guerrilla.webp), [Clone card face](https://www.media.todayindestiny.com/images/rising/shadowshaper_duels/card/clone.webp). These card-specific implementations still need actual device match testing.

### Known incomplete or partial effect resolvers

- **Xur:** Rock placement blocked by missing original Rock card Power.
- **Tomb Ship:** random Hive token selection needs authoritative summon-pool details.
- **Drop Pod:** random Cabal Line summon pool needs authoritative details.
- **Xuan Wei:** damage and relocation are now selectable directly on the target's battlefield, but the original game's precise destination-choice restriction is not yet verified.

### Newly improved effect families

- **One-time amplification:** Tan-2's next Power bonus doubles once without suppressing Tan-2; Jolder's next damage instance doubles once without suppressing Jolder.
- **Fallen generated units:** Unrelenting Captain creates a Dreg to the left when it receives a Power bonus, consistent with the original TodayInDestiny card description.
- **Cross-field delayed effect:** Cursed Thrall transfers to a random open opponent Front Row position, then detonates on its original controller's next turn, damaging adjacent units.
- **Damage enhancement boundaries:** Servitor applies its global damage aura; Juggernaut applies to adjacent Cabal units; Shinka Decurion applies to Cabal units with the qualifying 3-unit faction line.
- **End turn / timed effects:** Elite Walker targets up to two different opposing units; Resilient Grenade Dreg applies two turns of Scorched without adding a third immediate damage hit.
- **Cabal board conditions:** existing line/column checks and AI position evaluation continue to use the shared engine instead of duplicated UI logic.

## Known fidelity risks beyond explicit blockers

- Random-target distribution; exact row/line geometry and whether blank spaces interrupt a Line.
- Interaction between multiple concurrent power auras or Scorched applications.
- Suppression event ordering versus damage and summons, and special abilities on destroyed/removed units.
- Timing of first/second turn draws and summon transfer when destination field is full.
- Original card descriptions may diverge from some community versions. Primary TodayInDestiny names/effect wording take precedence where available; do **not** silently overwrite with inferred variants.

## Audit workflow

Run locally (no GitHub Actions credits):

```bat
python tools\audit_catalog.py
python tools\audit_effect_coverage.py --csv
gradlew.bat :app:testDebugUnitTest
gradlew.bat :app:assembleDebug
```

The coverage CSV can help triage every record. **The 31 authored JUnit tests have not been executed** in this environment. No device build has been verified.

## Source references

- [TodayInDestiny primary card descriptions](https://www.todayindestiny.com/rising/shadowshaper_duels/cards)
- [TodayInDestiny keyword definitions](https://www.todayindestiny.com/rising/shadowshaper_duels/keywords)
- [Community catalog of Human, Hive, Fallen and Cabal cards](https://www.reddit.com/r/ShadowshaperDuels/comments/1nklv84)
