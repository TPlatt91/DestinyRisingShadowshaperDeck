# Batch 12 — video-verified metadata and rules

Original recording: `batch 12.mp4`. **Power** is printed on the left and **Light cost** on the right. Source is the full card-detail viewer, not wiki estimates.

| Name | Faction | Rarity | Power | Light | Card ability | Acquisition |
|---|---|---|---:|---:|---|---|
| Tenacious Ogre | Hive | Mythic | 5 | 4 | Sustained: When acquiring a buff, deals 1 damage to 4 random opposing units. | Reach Duelist Level 13 |
| Tenacious Wizard | Hive | Mythic | 5 | 4 | When Played (Back Row): Grants Inspired to 2 random Hive units. | Reach Duelist Level 17 |
| Shinka Vigile | Cabal | Mythic | 8 | 4 | When Played (3-Unit Line): Deals 4 damage to a unit in the back row. | Duel Azizah |
| Shinka Scorpius | Cabal | Mythic | 7 | 4 | Sustained (3-Unit Line): At the end of your turn, deals 2 damage to a random opposing unit. | Card Vendor |
| Juggernaut | Cabal | Mythic | 8 | 4 | Sustained (Column): Cabal units to the left and right deal 1 additional damage. | Challenge Saladin |
| Pandora-7 | Humans | Legendary | 5 | 4 | Sustained: When your turn ends, grants the unit on the right +2 Power. | Duel Chinnawat |
| Harrow | Humans | Legendary | 7 | 4 | When Played: Toss a coin. If the result is heads, this unit gains +5 Power. | Reach Duelist Lv.31 |
| Helhest | Humans | Exotic | 6 | 5 | Sustained (Back Row): When your turn ends, deal 1 damage to 1 random opposing unit and the units to its left and right, while also gaining Power based on the damage dealt. | Card Vendor |

## Exact printed keyword definitions from the video

- **When Played:** An effect triggered when the unit is placed on the battlefield.
- **Sustained:** An effect that lasts as long as the unit is on the battlefield. Expires when Suppressed.
- **Back Row:** An effect triggered when the unit is placed in the back field.
- **Inspired:** At the end of your turn, gains 1 Power.
- **Line:** Units adjacent to each other in the same horizontal row. Line up a certain number of units with the "Line" affix to trigger certain effects.
- **Column:** Units adjacent to each other in the same vertical column. Place units with the "Column" affix behind or in front of one another to trigger certain effects.

## Important mechanics and exact-wording caveats

**Juggernaut** has printed *Sustained (Column)*, but says that *Cabal units to its left and right* deal 1 additional damage; this is preserved without editing it to say “above and below.” **Helhest** states it gains Power “based on the damage dealt,” without a numerical formula; its exact gain rate is not established by this card-detail clip alone. Tests should distinguish verified printed wording from inferred engine behavior.

Cards load into deck building, Card Collection, and battle from the same `verified_video_cards.tsv` and `card_details.tsv` metadata. The separately delivered ZIP includes artwork and animations; those binaries are not uploaded in these changes.
