# Destiny Rising — Shadowshaper Deck Companion

> **Kotlin/Compose source conversion completed (October 20, 2026):** The home menu, battle screen, deck builder, collection, and rules screens use Jetpack Compose. Match rules, card effects, AI, saved decks, and artwork loading are Kotlin. All obsolete Java production files were removed. This is **not** an APK build confirmation or a guarantee that every card effect is accurate. [Conversion details](docs/KOTLIN_MIGRATION.md).


Native Android Studio (Kotlin + Jetpack Compose) single-player Shadowshaper Duels companion.

### Current battle UI follow-up

The simulator now has a larger in-match card inspector showing readable effects, current Power and status flags. Battle turns and triggered events have brief on-screen announcements; pending target/relocation choices remain highlighted. The bottom hand overlaps like a compact fan, and the card-drag preview now compensates for window insets so it stays under the finger. `DuelHitTestingTest.kt` adds five local regression checks for player-only drop zones and drag coordinates.

**52 local tests are authored** (previous 41 plus five battlefield scaling and two card-back catalog checks), but the test suite has **not been run**, and APK/device behavior remains unverified. The visuals are a closer recreation rather than a confirmed pixel-perfect copy.

### Responsive field and original covers

The game now fits portrait card art into the actual slot dimensions on short landscape screens, with narrower HUD panels and an adaptive hand dock. Deck Construction also lets you choose from the nine original Shadowshaper card-back designs listed in TodayInDestiny; each choice is saved alongside its deck and displayed over that deck's draw pile in battle. The opponent hand uses the default card cover. Original cover images are loaded from bundled assets when available or downloaded and cached over HTTPS; offline-first installs still show a matching placeholder until an asset is available.

These changes **do not alter gameplay rules or deck card counts**. Android compilation, unit test execution, and exact pixel-level comparison against the original game's battle interface remain pending.

### Placement and match transitions

The Kotlin tabletop now shows brief one-time card arrival animations, subtle under-slot copper etching, smoothly changing displayed Power totals and an animated Victory/Defeat/Draw panel. Actual scoring and effect resolution are **unchanged**. `DuelPlacementCueTest.kt` includes four authored local tests for animation eligibility; a separate lightweight Kotlin/JVM check of the same pure predicate passed five assertions. No Android build, device test or GitHub Actions was run.

## Updated Shadowshaper battle presentation

The active Kotlin/Compose simulator now has a dark bronze tabletop with gold dividers, portrait card faces, rarity-colored frames, Power and Light in their respective upper corners, status emblems, six-round milestones, blue Light crystals, a two-sided Power display, and concealed CPU hand backs. The bottom hand uses a slight fan, selection lift, drag preview and on-field legal-placement/target highlights. Game rules remain in `ShadowEngine.kt` and `ShadowCardEffects.kt`, not in the UI.

These changes approximate the game's published visual language. They are **not** verified pixel-for-pixel against a complete in-game battle screenshot or tested on Android hardware yet. See [UI Fidelity Audit](docs/UI_FIDELITY_AUDIT.md).

## Main menu (four sections)

1. **Sim Battle** — six-round playable practice match against CPU. Choose a legal saved deck, draw a starting hand, place cards in front/back-row positions, spend 1–6 round play points, inspect cards, end turns, and see total Power scores and final victory/draw/defeat. Includes original card art when a matching online or bundled image exists. Now uses a dedicated `ShadowEngine` with shared targeting, damage, buffs, turn resolution, line/column synergies and AI opponents. Incomplete effects are explicitly logged rather than treated as resolved. See `docs/GAMEPLAY_IMPLEMENTATION.md` and [`docs/RULES_AUDIT.md`](docs/RULES_AUDIT.md) for rules, verified assumptions, and unimplemented interactions.
2. **Deck Construction** — local multiple saved decks, rename/delete, copy/share/import deck text, enforce the maximum 2 copies per name, 3 Exotic, 5 Mythic and 15-card total. Incomplete decks can be saved but **cannot enter Sim Battle**. Tokens and entries without verified Power or cost cannot be added. Launch a valid deck straight into battle.
3. **Card Collection** — browse the 110 known card records, original card-face images where available, stats, rarity, faction, effects, and source details; search by name/faction/rarity/effect.
4. **Rules** — in-app Shadowshaper deck limits, six-round scoring, positioning, Inspired/Sustained/Suppressed definitions, and gameplay source link.

The app opens to the four-section **ShadowComposeActivity** (Kotlin/Compose). The simulator launches the Kotlin **ShadowBattleActivity** in landscape. BattleActivity is single-player against a legal CPU practice deck; it is not a reimplementation of Destiny Rising's online PvP client. No background multiplayer or game-account access.


Native Android Studio (Java) deck creator. Current catalog: **110 distinct names** from TodayInDestiny and a detailed community list; this is **not guaranteed to be the full current game roster**.


### Current visual and gameplay progress (October 2026)

- The Android battle screen uses a bronze-and-gold Shadowshaper-inspired landscape interface: six round markers, opponent hand counter, visible Power/Light badges, a Power scoreboard and an overlapping hand.
- Hand controls now support deliberate tap, press-and-hold inspection, and native press-drag placement (a previous touch-dispatch bug was fixed).
- Abilities with legal choices use **in-board highlighted targets** with a Cancel Target action instead of a text-list dialog.
- The CPU places cards sequentially, with UI updates between each play. Dialogs pause AI progression; a placement animates once, and each new round highlights once.
- Deck construction shows base deck Power, copies and rarity counts, and a six-cost distribution, reflecting the real game's deck summary layout.
- **Scorpius** has a community-documented corrected Back Row, two-target end-turn ability; two regression tests protect the correction.
- The project has **36 locally authored JUnit tests** (31 existing Java regression tests + 5 Kotlin tests) across two test files. These are committed but **not executed**; no GitHub Actions workflows were invoked.

For reference sources, what is implemented, and remaining exact-match limitations, read [UI fidelity audit](docs/UI_FIDELITY_AUDIT.md) and [rules audit](docs/RULES_AUDIT.md). Do not treat source-level structural checks as an Android build or visual test.

## Card collection and details

- Original card-face artwork displayed for 34 TodayInDestiny catalog entries when internet access is available; successful images are cached.
- The other 76 names have detailed community-sourced effects, cost, power, faction, and rarity, but **no verified official card-face URLs yet**. The app shows honest missing-artwork placeholders, not synthetic artwork.
- Large scrollable card viewer with name, rarity, faction, Power (red circle value), play cost (blue diamond value), effect, activation/position conditions when documented, acquisition method where known, card class, and linked source reference.
- Printed card number is only displayed if verified: **none of the sources used currently establishes collectible card numbers**. Do not create sequential IDs and pass them off as printed card numbers.
- Search by card name, faction, rarity, or effect; add/remove copies in locally saved named decks; import and copy/share text deck lists. A first-pass practice battle simulator is included. Full card ability coverage is still in progress.

## Data files

- `app/src/main/assets/cards.tsv` — 34 TodayInDestiny names and effect summaries.
- `app/src/main/assets/community_cards.tsv` — 46 other community catalog entries with name, rarity, faction, cost, power, ability text.
- `app/src/main/assets/expanded_cards.tsv` — 30 additional community entries in the same schema.
- `app/src/main/assets/card_details.tsv` — overlays structured rarity, faction, cost, power, class, trigger, acquisition, notes, and printed number on the original 34 records. TSV fields: `name|faction|rarity|cost|power|class|trigger|acquisition|notes|number`. Empty values mean **not documented**, not 0.
- `app/src/main/assets/card_art/` — drop in original card art named by lower-case underscore slug such as `hexguard.webp`, `tenacious_wizard.png` or `shinka_vigile.jpg`. Bundled artwork overrides network images; supports WebP, PNG, JPG and JPEG. No extra code changes needed for each new image.

The original 34 image paths follow TodayInDestiny's card-face URL convention: `https://www.media.todayindestiny.com/images/rising/shadowshaper_duels/card/<slug>.webp`. URL accessibility has not been validated on Android devices for all 34 cards.

Known naming and effect discrepancies between primary and community listings need review (e.g., Carrier/Ketch, Morgran/Morgan, Ning Fei/Ning Fang and certain conflicting effect texts). The first 34 retain their primary-source descriptions until independently verified. Source links in the viewer make follow-up checks possible.

### Audit the catalog offline

`python tools/audit_catalog.py`

This local metadata audit does not use GitHub Actions or GitHub usage credits.

## Opening in Android Studio

Open the repository root folder in Android Studio, using JDK 17 and the Gradle 8.11.1 distribution compatible with Android Gradle Plugin 8.9.2. The project includes the Gradle wrapper JAR and launcher scripts. Open the repository root in Android Studio and sync; the wrapper will download Gradle 8.11.1 on first run. On Windows, run `gradlew.bat :app:assembleDebug` from the project root if you want a local command-line build. This needs internet for the first Gradle download but does not use GitHub Actions or GitHub usage credits.

**Status:** The four-section app and landscape simulator are committed. Local Gradle compilation and on-device verification have not yet been completed.

## References

- https://www.todayindestiny.com/rising/shadowshaper_duels/cards
- https://www.reddit.com/r/ShadowshaperDuels/comments/1nklv84/shadowshaper_card_list/
- https://game8.co/games/Destiny-Rising/archives/546603

Use original assets responsibly and check content-owner permissions before redistributing artwork.

## Local rule verification

The shared `ShadowRules.java` constants and `ShadowEngine.validatePlay` enforce match legality before any Light or card movement. The CPU uses projected same-faction lines, column partners, and row effects to choose positions. The battle event viewer can copy/share a reproducible report containing the match seed, board and event history.

For local testing without GitHub Actions credits, run `gradlew.bat :app:testDebugUnitTest` and `gradlew.bat :app:assembleDebug` on your computer. These commands have not been executed here.
