# Shadowshaper card engine coverage — October 20, 2026

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
- **Xuan Wei:** damage occurs; relocation destination is not yet selectable.

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

The coverage CSV can help triage every record. **The 30 authored JUnit tests have not been executed** in this environment. No device build has been verified.

## Source references

- [TodayInDestiny primary card descriptions](https://www.todayindestiny.com/rising/shadowshaper_duels/cards)
- [TodayInDestiny keyword definitions](https://www.todayindestiny.com/rising/shadowshaper_duels/keywords)
- [Community catalog of Human, Hive, Fallen and Cabal cards](https://www.reddit.com/r/ShadowshaperDuels/comments/1nklv84)
