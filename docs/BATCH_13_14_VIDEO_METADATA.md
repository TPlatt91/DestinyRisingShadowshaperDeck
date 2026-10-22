# Original card-detail metadata — Batches 13 and 14

Source recordings: `batch 13.mp4` (17 cards) and `batch 14.mp4` (3 cards). The source viewer displays **Power on the left** and **Light cost on the right**. The exact on-card wording and sources are recorded below. Card identity `Thagrius` is preserved as spelled in the existing catalog although the source viewer renders `THAGRIUS` in uppercase.

| Batch | Name | Faction | Rarity | Power | Light | Original-video ability | Acquisition |
|---|---|---|---|---:|---:|---|---|
| 13 | Thinktank | Humans | Mythic | 7 | 5 | When Played: Deals damage to a unit equal to the highest bonus on your field. | Card Vendor |
| 13 | High Servitor | Fallen | Mythic | 7 | 5 | When Played: Deals damage to a unit equal to the number of Fallen units on your field. | Reach Duelist Lv.4 |
| 13 | Shinka Gladiator | Cabal | Mythic | 8 | 5 | When Played (5-Unit Line): A selected unit and the units to its left and right deal 2 additional damage. | Card Vendor |
| 13 | Molkrul | Fallen | Mythic | 8 | 5 | When Played (Back Row): Toss a coin. If the result is heads, instantly defeats an opposing unit with no more than 10 Power. | Reach Duelist Lv.34 |
| 13 | Lady Skorri | Humans | Legendary | 7 | 5 | When Played: If there are units in the opponent's back row, grants +5 Power to the unit on the left. | Duel Skorri |
| 13 | Amu-Atu | Humans | Legendary | 8 | 5 | When Played: Deals damage to an opposing unit equal to the number of units in the opponent's back row. | Reach Duelist Lv.6 |
| 13 | Augmented Shank | Fallen | Legendary | 5 | 5 | When Played (Front Row): Deals 2 damage to a unit and the units on its left and right. | Duel Dobbaq |
| 13 | Shinka Centurion | Cabal | Legendary | 6 | 5 | Sustained (Column): Grants +3 Power to units on the left and right. | Card Vendor |
| 13 | Maru | Humans | Exotic | 13 | 6 | When Played: Grants all units in this row (except for this unit) +2 Power. | Card Vendor |
| 13 | Kabr | Humans | Exotic | 7 | 6 | When Played: Grants all Guerrilla units +3 Power. | Reach Duelist Lv.10 |
| 13 | Ikora | Humans | Exotic | 13 | 6 | When Played: Deals 2 damage to all opposing units in a row. | Triumph reward for collecting 40 cards |
| 13 | Attal | Humans | Exotic | 6 | 6 | When Played: Doubles the Power of a unit. | Card Vendor |
| 13 | Estela | Humans | Exotic | 13 | 6 | When Played: Grants a unit Power equal to 2x the number of units in the opponent's front row. | Card Vendor |
| 13 | Vanok | Fallen | Exotic | 10 | 6 | When Played: Grants +2 Power to all your Fallen on the field. | Card Vendor |
| 13 | Paradoxical Neophyte | Hive | Exotic | 10 | 6 | When Played: Summons 1 Shade on the left. | Reach Duelist Level 20 |
| 13 | Paradoxical Neophyte's Shade | Hive | Exotic | 1 | 6 | Sustained: Gains 4 Power for every Inspired Hive on your field. | Related card — summoned by Paradoxical Neophyte |
| 13 | Goliath Tank | Cabal | Exotic | 10 | 6 | When Played (5-Unit Line): Deals 3 damage to all units in a row. | Reach Duelist Lv.24 |
| 14 | Buhr | Cabal | Exotic | 10 | 6 | When Played (Column): Grants +3 Power to all units in a Column. | Reach Duelist Lv.30 |
| 14 | Thagrius | Cabal | Exotic | 10 | 6 | When Played (6-Unit Line): Deals damage to a unit equal to double the number of Line units. | Reach Duelist Lv.25 |
| 14 | Sabikis | Fallen | Mythic | 8 | 6 | When Played (Back Row): Grants a unit the Power of the unit to its left. | Reach Duelist Level 18 |

Full individual flavor text is also in `card_details.tsv`, under the source notes. `verified_video_cards.tsv` overrides older catalog values; `card_details.tsv` supplies acquisition, trigger, classification and detailed Power/Light fields. These drive deck construction, collection, and battle.

**Shade classification:** `Paradoxical Neophyte's Shade` is explicitly marked `Summoned unit`: has an in-viewer card face with 1 Power and 6 Light, but appears as a related summon rather than confirmed deck-eligible card. It should not be added to ordinary constructed decks.

## Keyword rules (original viewer)

- **When Played:** An effect triggered when the unit is placed on the battlefield.
- **Sustained:** An effect that lasts as long as the unit is on the battlefield. Expires when Suppressed.
- **Back Row:** An effect triggered when the unit is placed in the back field.
- **Front Row:** An effect triggered when the unit is placed in the front row.
- **Line:** Units adjacent to each other in the same horizontal row. Line up a certain number of units with the "Line" affix to trigger certain effects.
- **Column:** Units adjacent to each other in the same vertical column. Place units with the "Column" affix behind or in front of one another to trigger certain effects.
- **Inspired:** At the end of your turn, gains 1 Power.

Rules index: `app/src/main/assets/video_keyword_rules_01_14.tsv`.

**Code-vs-game caution:** Kotlin handler presence is not proof of gameplay correctness; formation semantics are still provisional without a recorded live-play confirmation. Original-image and GIF ZIP exports are separate and were not committed in this source-code metadata push.
