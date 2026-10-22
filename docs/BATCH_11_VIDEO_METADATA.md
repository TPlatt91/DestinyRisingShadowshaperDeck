# Batch 11: video-confirmed card definitions and rules

Source of truth: user-supplied **`batch 11.mp4`** (2410 × 1080). Card viewer shows **Power** at upper left and **Light cost** upper right.

| Card | Faction | Rarity | Power | Light | Printed effect | Acquisition |
|---|---|---|---:|---:|---|---|
| Tomb Ship | Hive | Exotic | 6 | 4 | When Played: Summons 2 random Hive units to its left and right. | Challenge Abaca |
| Saladin | Humans | Exotic | 6 | 4 | Sustained: Whenever one of your Human units takes damage, they gain +3 Power. | Challenge Saladin |
| Radegast | Humans | Mythic | 7 | 4 | When Played: If there are Fallen units on the opponent's field, grants all Guerrillas in this row +4 Power. | Duel The Speaker |
| Bazinet | Humans | Mythic | 2 | 4 | When Played (Back Row): Deals 3 damage to all opposing units with a bonus. | Duel Ata |
| Elite Walker | Fallen | Mythic | 2 | 4 | Sustained (Back Row): When your turn ends, deals 2 damage to 2 random opposing units. | Card Vendor |
| Resilient Marauder | Fallen | Mythic | 6 | 4 | When Played (Front Row): Deals 5 damage to an opposing unit. | Duel Dabbaq |
| Luksis-3 | Fallen | Mythic | 5 | 4 | When Played: Grants Power equal to 2x the number of Rocks on your opponent's field. | Card Vendor |
| Tenacious Knight | Hive | Mythic | 6 | 4 | When Played (Front Row): Randomly grants +3 Power to 2 of your Inspired Hive units. | Challenge Saladin |

The eight definitions override older community descriptions in `verified_video_cards.tsv` and `card_details.tsv`. The base community/expanded catalogs are also synchronized for fallback. The same catalog is used for card collection, deck construction and battle.

## Keyword rules visible in the recording

- **When Played:** Triggers when the unit enters the battlefield.
- **Sustained:** Operates while the unit is present; expires when Suppressed.
- **Back Row / Front Row:** An effect with this restriction requires deployment to that row.
- **Inspired:** Gains 1 Power at the end of its owner's turn.

Keyword definitions are recorded in `app/src/main/assets/video_keyword_rules_01_11.tsv`.

## Implementation QA

Battle handlers are located in `ShadowCardEffects.kt` and `ShadowEngine.kt`. Tomb Ship's random Hive pool is necessarily based on available catalog cards; the recording does **not** show the complete in-game random pool. For all eight cards, full match-level testing and compilation remain required before calling gameplay verified.

The separate Batch 11 ZIP contains artwork, GIFs and MP4s; **this source commit does not upload those binary files**.
