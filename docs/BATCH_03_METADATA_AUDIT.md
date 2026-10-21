# Batch 03 — in-game card metadata imported

Source: user-provided, full-card-detail screen recording **38646.mp4**, captured at 2410×1080. These are transcriptions of the card text and acquisition labels visible in the recording. The **left diamond is Power**; the **blue circle on the right is Light cost**. Cost/power fields in `card_details.tsv` intentionally use the *opposite* order (`cost|power`) to the visual layout.

| Card | Faction | Rarity | Power | Light cost | Trigger and recorded effect | Acquisition |
| --- | --- | --- | ---: | ---: | --- | --- |
| Rossi-11 | Humans | Exotic | 3 | 1 | When Played: Grants an overshield to one of your units. | Card Vendor |
| Resilient Captain | Fallen | Mythic | 1 | 1 | Sustained: When your Fallen units deal damage, summons a Dreg on the right. (Up to 6 Dregs can be summoned.) | Duel Zhong Wenbin |
| Unrelenting Captain | Fallen | Mythic | 1 | 1 | Sustained: Each Dreg deployed gains 1 additional Power. | Card Vendor |
| Shinka Decurion | Cabal | Mythic | 1 | 1 | Sustained (3-Unit Line): Your Cabal units deal 2 additional damage. | Reach Duelist Lv.22 |
| Mind Disruptor | Fallen | Mythic | 2 | 1 | Sustained: The unit on its left cannot be Suppressed. | Reach Duelist Lv.35 |
| Ayuela | Humans | Legendary | 1 | 1 | Sustained (Back Row): When the opponent places a unit in the front row, deals 2 damage to it for 2 turns. | Duel Ayuela |
| Marauder | Fallen | Legendary | 3 | 1 | No special effects. | Reach Duelist Lv.2 |
| Exploder Shank | Fallen | Legendary | 1 | 1 | When Played: Randomly moves to the opponent's front row, then dies at the beginning of your next turn, dealing 2 damage each to the units on the left and right. | Gauntlet: Blitz |

## Source files and integration

- Corrected existing text in `app/src/main/assets/cards.tsv` for Ayuela, Exploder Shank, Resilient Captain, and Unrelenting Captain. Marauder already matched.
- Confirmed the existing `community_cards.tsv` Shinka Decurion row and standardized its printed effect text.
- Added `app/src/main/assets/verified_video_cards.tsv` containing **Rossi-11** and **Mind Disruptor**, previously missing from all three existing catalogs. `ShadowData.load` now reads this fourth catalog as a six-field `name|rarity|faction|light_cost|power|effect` source, and retains a direct in-game-video attribution instead of mistakenly labeling it a Reddit or TodayInDestiny card.
- `card_details.tsv` now contains a matching overlay for all eight cards: full fields are `name|faction|rarity|light_cost|power|classification|trigger|acquisition|notes|card_number`. The recorded source does **not** establish a classification/card number for the three newly added overlays, so those fields remain blank.
- `card_image_inventory.tsv` now lists **112** unique names. The Rossi-11 animated GIF is already bundled; the Mind Disruptor animated image was extracted in the earlier Batch 03 package but was **not yet uploaded into the GitHub assets folder**.

## Data verification and gameplay fidelity caveats

A source-level audit confirmed all eight names in the catalog, every overlaid stat/trigger/source, all 37 card-details records at exactly 10 columns, and **112 unique names with zero duplicates**. No Android build or GitHub Actions were run.

**Metadata integration is complete, not the same as full gameplay implementation.** In the existing game engine:
- Shinka Decurion's 3-unit-line Cabal damage aura and Ayuela's incoming-front-unit damage trigger already have scripted implementations (still needing gameplay QA).
- Rossi-11's overshield and Mind Disruptor's suppression-prevention effects do not yet have complete dedicated gameplay handlers.
- Resilient Captain, Unrelenting Captain and Exploder Shank previously had different scripted effects; those mechanics must be reconciled with the newly verified card text before they can be called faithful to the original.
- The GIF/MP4 extracts in the earlier Batch 03 ZIP are separate artwork deliverables; this commit concerns **metadata only**, not binary asset imports.

Use the same `verified_video_cards.tsv` source format for additional names found in subsequent batches that are not in the online/community catalogs, and apply later metadata corrections to existing entries rather than creating duplicates.
