# Batch 09–10 gameplay implementation — 21 October 2026

**Source of truth:** `screen-20261021-171710-1792617359884.mp4` (Batch 09) and `batch 10.mp4` (Batch 10), with 23 verified original card faces and printed effect texts. On-device `verified_video_cards.tsv` now overrides community card effects for all 23. The same records feed deck construction, collection metadata and the battle engine.

## Batch 09 (8/8 source handlers)
- **Xuan Wei:** When Played from Front Row, damages the chosen unit for 5 and queues relocation to a different battlefield location if it survives.
- **Jaren:** On Front Row play, distributes six randomly targeted 1-damage events among surviving opposing units.
- **Kevarrh:** Draws two cards.
- **Guarding Servitor:** Inspires a selected allied unit.
- **Blinder Shank:** Suppresses its immediate left/right allied units.
- **Resilient Grenade Dreg:** In Back Row, enemy Back Row deployments inflict 2 damage over 2 turns (Scorched state).
- **Chinnawat:** From Front Row, grants +1 Power to the selected ally and its immediate horizontal neighbors.
- **Common Cover:** From Front Row, summons two verified 1-Power Rock tokens into random free enemy Front Row slots.

## Batch 10 (15/15 source handlers)
- **Guerrilla Captain:** On own Power bonus, damages a random opposing unit for 2.
- **Wretch:** Front Row play damages selected opposing Front Row unit for 1.
- **Inquisitor:** Back Row play damages an opposing unit for 4.
- **Knight:** Gives an Inspired allied Hive unit +3 Power.
- **Wizard:** Inspires an allied Hive unit only; target filtering fixed.
- **Ogre:** On own buff, deals 1 damage to each of two different random opposing units, when two are available.
- **Shrieker:** From Back Row only, on own buff deals 2 damage to a random enemy.
- **Vigile:** On Front Row play, grants +2 Power to a selected unit with an actual printed Line affix (not a unit merely mentioning the word 'Line').
- **Scorpius:** Deals 1 damage to one random enemy at end of owner's turn, in either row.
- **Colossus:** Only vertically adjacent allied Cabal units receive +1 ongoing Power.
- **Phalanx:** A contiguous 3-Cabal-unit Line reduces damage to allied Cabal units by 1.
- **Drop Pod:** With a 2-unit Line, summons a random printed Line-affix card into its left slot. The candidate pool is inferred from the currently verified game catalog; exact game pool remains to be confirmed.
- **Shinka Incendior:** With a 3-unit Line, 3 damage to a selected enemy Back Row unit.
- **Devil Sentry:** Heads coin flip deals 5 damage to selected target; tails does nothing.
- **Gladiator:** From Front Row, selected allied Front Row unit and its immediate left/right units gain +1 *outgoing damage*, not Power.

## Regression coverage and caveats

`app/src/test/java/com/tplatt/shadowshaper/ShadowBatch0910VideoRulesTest.kt` includes **21 focused JVM regression cases**, covering target restrictions, row restrictions, random resolution, triggered auras and tokens. **The tests have been authored and committed but not executed.** No Android APK build or live match has been verified. Run on local computer (no GitHub Actions minutes):

```bat
gradlew.bat :app:testDebugUnitTest
gradlew.bat :app:assembleDebug
```

The keyword text explicitly refers to the **Line affix**. The engine's overall contiguous formation geometry remains an approximation based on faction adjacency, and full-row, summon-pool and simultaneous-trigger edge cases need gameplay confirmation. If the game uses a different Line qualification algorithm, update the shared `ShadowPositionRules` instead of creating per-card exceptions.

Artwork/GIF extraction archives for 09/10 are separate; this batch pushed metadata and gameplay only, not binary artwork files.
