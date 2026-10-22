# Video-verified Shadowshaper Duels metadata — Batches 01–08

This register covers card-detail clips sent by the user. The left diamond on the original card is **Power**; the right blue circle is **Light cost**. On-device TSV fields intentionally use `cost|power`.

**Authority and loading order:** `ShadowData.load()` reads `cards.tsv`, `community_cards.tsv`, `expanded_cards.tsv`, then `verified_video_cards.tsv` (overrides name-matching cards), then `card_details.tsv` (overrides faction/rarity/cost/power/classification/trigger/acquisition). Thus the video records supersede older community descriptions. Never interpret the mere presence of a card record as proof its effects have been verified in actual Android gameplay.

Earlier source audits:
- [Batch 01](BATCH_01_ART_AUDIT.md): 8 entries
- [Batch 02](BATCH_02_METADATA_AUDIT.md): 8 entries
- [Batch 03](BATCH_03_METADATA_AUDIT.md): 8 entries, with repeated Batch 01 cards
- [Batch 04](BATCH_04_CARD_VIDEO_AUDIT.md): 3 entries
- [Batch 05](BATCH_05_CARD_VIDEO_AUDIT.md): 9 entries
- [Batches 06–07](BATCH_06_07_VIDEO_IMPLEMENTATION.md): 8 entries each
- Batch 08: 8 entries below

## Batch 08, original video `screen-20261021-171541-1792617289948.mp4`
| Card | Faction | Rarity | Power | Light | Printed effect | Acquisition |
|---|---|---|---:|---:|---|---|
| Shinka Psion | Cabal | Legendary | 3 | 2 | When Played (2-Unit Line): Deals 3 damage to a unit. | Reach Duelist Lv.21 |
| Shinka Legionary | Cabal | Legendary | 4 | 2 | Sustained (Column): Gains +2 Power. | Reach Duelist Lv.26 |
| Shinka Phalanx | Cabal | Legendary | 3 | 2 | Sustained (Column): Units directly in front of or behind take 1 less damage. | Reach Duelist Lv.27 |
| Incendior | Cabal | Legendary | 2 | 2 | When Played: Deals 3 damage to an opposing unit in the front row. | Card Vendor |
| Decurion | Cabal | Legendary | 3 | 2 | Sustained (Column): Grants +2 Power to units directly in front of or behind. | Reach Duelist Lv.28 |
| Psion | Cabal | Rare | 2 | 2 | When Played (Back Row): Deals 2 damage to a unit. | Card Vendor |
| Jolder | Humans | Exotic | 3 | 3 | Sustained: The next instance of damage dealt is doubled. | Shifting Gates Vendor |
| Finnala | Humans | Exotic | 6 | 3 | When Played (Front Row): Grants +8 Power to one of your units and gains -3 Power. | Triumph - 50 Wins Vs. Players |

Batch 08 tooltip glossary is separately preserved in `app/src/main/assets/video_keyword_rules_01_08.tsv`.

### Implementation notes
- **Shinka Psion:** a qualifying 2-unit Line and a chosen target; 3 damage on play.
- **Shinka Legionary:** +2 Power while sharing a qualifying Column with another unit; ceases when Suppressed.
- **Shinka Phalanx:** one less damage to the unit vertically adjacent in its Column.
- **Incendior:** 3 damage to selected enemy Front Row unit when played.
- **Decurion:** +2 Power to the vertically adjacent unit, not to itself.
- **Psion:** requires Back Row placement; deals 2 damage to a selected unit.
- **Jolder:** doubles the next instance of damage dealt. Existing one-use engine support retained.
- **Finnala:** Front Row only; +8 Power to selected allied unit, and `-3 Power` to itself. **The -3 is a Power penalty, not damage**.

Source assets were extracted separately; this commit **does not upload binary artwork files**. The Batch 08 GIF and MP4 packages are already available to the user, including Finnala's corrected animation window. Metadata and rules do not imply a successful device or Gradle test.

**Known gaps across Batches 01–08:** `ShadowEffectCoverage.kt` explicitly tracks unfinished handlers (including Rossi-11 and Mind Disruptor). Do not claim all 8 batches are gameplay-complete unless those are addressed and tested.

## Current source-code audit (21 October 2026)
All **57 distinct video-batch cards** have catalog and detailed stats entries; 60 recorded appearances across eight batches. Every one of their 45 non-basic cards has a source handler reference; Android gameplay correctness is **not** established by that check. See [current coverage](CARD_ENGINE_COVERAGE.md) and `ShadowBatch0108VideoRulesTest.kt`.
