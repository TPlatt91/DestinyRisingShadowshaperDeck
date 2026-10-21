# Batch 06–07 original card-detail video implementation

Source recordings:
- Batch 06: `screen-20261021-171326-1792617115655.mp4`
- Batch 07: `screen-20261021-171431-1792617226257.mp4`

The two recordings are authoritative for card names, factions, rarity, base Power, Light cost, printed effects, keyword requirements and acquisition details. `verified_video_cards.tsv` overlays earlier entries, and `card_details.tsv` supplies the collection/deck metadata.

Batch 06 (8): Umeko, Xur, **Wu Ming** (not "Yu Ming"), Ketch, Tracker, Tenacious Hexguard, The Disfavored, Centurion.

Batch 07 (8): Umbliks Prime, Mu Ying, The Speaker, Denzin, Hyssop, Elite Vandal, Elite Sniper Vandal, Tenacious Thrall.

Related token shown in Xur's card-detail panel: **Rock has 1 Power / 1 Light**. Rock remains a **Summoned unit**, unavailable in deck construction.

Changes:
- Missing card entries and handlers added for Umbliks Prime and Tenacious Hexguard. Other 14 cards retain saved-deck identities.
- Wu Ming's original uploaded `yu_ming.gif` is moved to `wu_ming.gif` without changing the image data.
- Rock now spawns into a random available opposing front-row slot when Xur is buffed.
- Umbliks Prime flips a coin, giving +6 Power on heads to the selected allied target or itself when no prior ally can be selected.
- Tenacious Hexguard grants Inspired to directly adjacent allied Hive units.
- Umeko's damage aura also applies to itself; Elite Sniper Vandal can retaliate against either row, once per opposing unit per turn, over two owner turns.
- Existing movement, summoning, buff targeting, line formation, and basic stats preserved for the other cards.
- Batch 06 GIF files are already in GitHub. Batch 07 extracted artwork is still awaiting upload, per the inventory. Image extraction packages are separate from these gameplay commits.

Validation: `ShadowBatch0607EffectsTest.kt` contains focused JVM regression tests for these 16 cards and Rock. Tests are **authored but not executed** in this environment. Neither Android device play nor an APK build was verified. Run locally, without GitHub Actions credits:

```bat
gradlew.bat :app:testDebugUnitTest
gradlew.bat :app:assembleDebug
```
