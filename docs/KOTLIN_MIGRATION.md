# Kotlin / Jetpack Compose conversion

**Production source migrated: October 20, 2026.**

All Android app production sources are Kotlin. Home, Deck Construction, Card Collection, Rules and the landscape Sim Battle use Jetpack Compose. Match state, AI, card effects, rules, deck storage and image loading are Kotlin modules.

## Kotlin modules

| Feature | Source |
|---|---|
| Home / Collection / Deck / Rules | ShadowComposeActivity.kt |
| Landscape battle UI, hand drag and target selection | ShadowBattleActivity.kt |
| Complete match state, action legality, AI, scoring and turns | ShadowEngine.kt |
| Card-specific effects and triggered ability families | ShadowCardEffects.kt |
| Observable StateFlow match controller | ShadowBattleCoordinator.kt |
| Saved deck CRUD and import/export | ShadowDeckRepository.kt, ShadowDeckViewModel.kt |
| Match rules, deck limits, position geometry and damage math | ShadowRules.kt, ShadowDeckRules.kt, ShadowPositionRules.kt, ShadowDamageRules.kt |
| Card metadata / effects categorization | ShadowData.kt, ShadowEffectRegistry.kt, ShadowEffectCoverage.kt |
| Card-face loading and image caching | CardArt.kt |

**No Java production source files remain.** All earlier Java UI screens and the Java match engine were removed after Kotlin replacements were committed.

## Saved decks

All original decks continue to use the same Android SharedPreferences name, MainActivity, and the same keys:

- deck_names: JSON array of names
- selected_deck: active deck name
- deck_<name>: JSON object of card name to quantity

The new Kotlin deck repository reads these directly; no new account setup or delete/recreate is needed.

## Gameplay status versus migration status

The code conversion does not mean all card rules were verified. The Kotlin effect module retains previous scripted abilities, missing-data warnings, and incomplete effect handling.

- 110 distinct documented card records in current catalog; official full-game total not verified
- Rock token Power/cost still unresolved
- Xur, Tomb Ship, Drop Pod require verification of summon data
- Xuan Wei movement is scripted but precise in-game destination requirements need confirmation
- Exact battlefield slot layout, Line geometry and later-round draw timing remain provisional
- The AI remains single-player only; no multiplayer/PvP

## Verification status

Existing 31 Java regression tests continue to exercise the now-Kotlin engine through Java interoperability. Five Kotlin tests cover deck rules, geometry, damage, StateFlow and ability categorization: **36 authored tests total**.

- Structural Kotlin source checks passed (bracket/literal balancing and engine/effect handler references)
- Android Studio / APK compilation: **not executed**
- On-device battle match and image-cache verification: **not executed**
- No GitHub Actions workflows, Actions tests or credit-consuming CI builds were run

The environment does not contain an Android SDK and Gradle build setup suitable for a real Android compilation. Do not treat source structural checks as a successful APK build.

For eventual *local* validation without GitHub Actions credits:

    gradlew.bat :app:assembleDebug
    gradlew.bat :app:testDebugUnitTest

No user-side action is needed to complete the source conversion itself.
