# Kotlin + Jetpack Compose migration

**Status:** Stage 1 implemented on October 20, 2026. The installed launcher and all companion screens are Kotlin/Compose. The original Java landscape battle renderer and individual complex card effect resolvers are deliberately retained during the migration.

## Kotlin toolchain

- Android Gradle Plugin: **8.9.2** (unchanged)
- Gradle Wrapper: **8.11.1** (unchanged)
- JDK target: **17** (unchanged)
- Kotlin Android and Compose Compiler Gradle plugins: **2.1.20**
- Compose BOM: **2025.04.01**
- Activity Compose: **1.10.1**; Lifecycle ViewModel Compose/ktx: **2.8.7**
- `compileSdk=35` / `targetSdk=35`; these are retained to avoid requiring an AGP 9 migration.

## Completed now

| System | Kotlin class / implementation | Migration status |
|---|---|---|
| Home screen | `ShadowComposeActivity.kt` | Kotlin/Compose launcher |
| Deck Construction | `DeckScreen` in `ShadowComposeActivity.kt` | Kotlin/Compose with legality, selected decks, deck curve and export/import |
| Card Collection | `CollectionScreen` in `ShadowComposeActivity.kt` | Kotlin/Compose with rarity/faction/search filters and original card-image integration |
| Card viewer | `CardDetailsDialog` in `ShadowComposeActivity.kt` | Kotlin/Compose, effect text/statistics/provenance/coverage and add/remove copy |
| Rules | `RulesScreen` in `ShadowComposeActivity.kt` | Kotlin/Compose |
| Deck storage | `ShadowDeckRepository.kt` | Kotlin, reuses current SharedPreferences format |
| Deck state | `ShadowDeckViewModel.kt` | Kotlin, StateFlow, single saved-deck source of truth |
| Deck legality | `ShadowDeckRules.kt` | Kotlin; used by both old Java and Compose UI |
| Position geometry / faction lines | `ShadowPositionRules.kt` | Kotlin; Java engine delegates to these rules |
| Damage arithmetic | `ShadowDamageRules.kt` | Kotlin; Java engine uses it |
| Effect classification | `ShadowEffectRegistry.kt` | Kotlin registry; engine uses for unresolved effect reporting |
| Battle controller | `ShadowBattleCoordinator.kt` | Kotlin StateFlow boundary; **Java BattleActivity routes all game mutations through it** |
| Landscape battlefield | `BattleActivity.java` | Retained unchanged visually, game mutations switched to Kotlin coordinator |
| Complex card effect resolution | `ShadowEngine.java` | Preserved while porting families one at a time with regression tests |

## Save format compatibility (critical)

The original `MainActivity.java` stored data in activity-private `getPreferences(0)`, equivalent to SharedPreferences named **MainActivity**, using:

- `deck_names`: JSON array of deck names
- `selected_deck`: current deck name
- `deck_<name>`: JSON object mapping card names to integer quantities

`ShadowDeckRepository.kt` reads and writes the **same filename and keys**. It never clears a save during initialization, and the old Java screens remain available for rollback/debugging.

Imports validate the entire list before replacing the stored deck. Invalid names, duplicate counts, rarity-limit or 15-card-limit violations cause an error without overwriting a saved deck. Incomplete legal-by-card decks can be saved; only fully legal 15-card decks can launch Sim Battle.

## Verification

Local Kotlin unit tests (`ShadowKotlinRulesTest.kt`) cover canonical deck legality, position geometry, damage, StateFlow updates, and effect-family classification. Existing Java engine tests remain intact: **36 test methods authored in total**.

On the user's PC, after pulling:

```bat
gradlew.bat :app:assembleDebug
gradlew.bat :app:testDebugUnitTest
```

These are local commands; they do **not** start GitHub Actions or consume GitHub Actions minutes.

**NOT VERIFIED YET:** this environment did not compile an Android APK, run the local unit suite, exercise external document pickers, or confirm on-device touch interactions. Report actual Gradle or Android runtime logs if a problem appears; do not mistake static source checks for a successful build.

## Next Kotlin migration phase

1. Validate the Compose screens and saved-deck compatibility on an Android device.
2. Port the battle rendering and touch/targeting system to Kotlin, retaining the existing position/hand behavior until parity is demonstrated.
3. Migrate `ShadowEngine.java` effect families into Kotlin resolvers incrementally (rather than translating 1,000 lines at once and risking silent card-rule regressions).
4. Continue verifying the 110 recorded card entries individually. The previous engine audit did **not** establish full gameplay fidelity.
5. Once Kotlin battle and resolvers pass local tests, remove the unused Java UI fallback files in a separate commit.

The game remains **single-player against AI**. No PvP, paid services, GitHub workflows or Actions CI were added.
