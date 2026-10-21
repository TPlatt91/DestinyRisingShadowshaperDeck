# Batch 02 — original card video metadata and battle integration

Video source: user recording `38645.mp4`, 2410×1080. The **number in the diamond on the LEFT is Power**, while the **number in the blue circle on the RIGHT is Light cost**. Earlier exported Batch 02 metadata incorrectly reversed those columns for seven of eight cards. All eight **existing** `card_details.tsv` rows were already correct, and the replacement extraction ZIP corrects its own metadata.

| Card | Faction | Rarity | Power | Light cost | Printed ability |
|---|---|---|---:|---:|---|
| Shank | Fallen | Rare | 4 | 2 | No special effects |
| Wolf | Humans | Exotic | 3 | 3 | When Played: If you have more than 5 units on the field, destroys a unit with 5 Power or more |
| Morgran | Humans | Legendary | 6 | 3 | When Played (Back Row): Grants +1 Power to a unit |
| Tracer Shank | Fallen | Legendary | 4 | 3 | When Played (Back Row): Deals 3 damage to an opposing unit |
| Donovan-17 | Humans | Rare | 6 | 3 | No special effects |
| Zhong Wenbin | Humans | Rare | 6 | 4 | When Played (Front Row): Grants +2 Power to the unit on the right |
| Sparrow | Humans | Rare | 7 | 5 | When Played (Front Row): Gains +3 Power |
| Mishra | Humans | Rare | 8 | 6 | When Played: Grants +3 Power to units on the left and right |

## Source and implementation status

- All eight records existed in `cards.tsv` and `card_details.tsv`. Stat and rarity/faction overlays were correct and remain unchanged.
- Effect text in `cards.tsv` now reflects the full user-recorded conditions. **Mishra +3 Power** replaces the incorrect +2 in text and in the battle effect handler.
- Wolf's previous **Power >8** target limit and Front Row restriction were incorrect. The video establishes **more than five allied units on field, including Wolf**, and a target of **5 or more Power**, with no specified row restriction for Wolf.
- `ShadowSpecialCardRules.kt`, `ShadowEngine.kt`, `ShadowCardEffects.kt` now use the six-unit and five-Power thresholds. Wolf now destroys the target via the standard death/sweep pipeline rather than dealing damage that could be modified by damage auras. `ShadowSpecialCardRulesTest.kt` documents threshold boundaries.
- Wolf's target-selection UX remains an approximation when multiple units qualify; the original card text says “a unit,” while the existing simulator restricts selection to opposing units. No gameplay fidelity claim beyond the demonstrated conditions is made.
- Seven Batch 02 still card images have been committed under `app/src/main/assets/card_art`. **Wolf is not yet bundled**; a corrected GIF/MP4 ZIP is provided separately. After the GIF is placed at `app/src/main/assets/card_art/wolf.gif`, the Android loader will display it.
- No Android full build, gameplay interaction test, or GitHub Actions run was executed. Engine code and image/video exports were statically reviewed.

## Wolf animation recut

The old animation included deck/detail transitions. The recut uses **8.2–13.4 seconds** only, showing the gold frame/visual effects inside the card crop without the surrounding details panel or transitions. The crop comes directly from the 2410×1080 recording at `x=399, y=175, width=499, height=753`, scaled to **500×754**; it includes the complete card frame, printed stats and name.

Exports: **animated GIF** (63 frames / 12 fps / ~5.25 seconds) and **H.264 MP4** (125 frames / 24 fps / ~5.21 seconds). Source artwork and effects are not redrawn. Only Wolf is marked animated in Batch 02.

For new clips, identify stable card-only screen intervals before rendering animated versions; never automatically extend a clip across card selection or dismissal UI transitions.
