# Destiny Rising — Shadowshaper Deck Companion

Native Android Studio (Java) single-player Shadowshaper Duels companion.

## Main menu (four sections)

1. **Sim Battle** — six-round playable practice match against CPU. Choose a legal saved deck, draw a starting hand, place cards in front/back-row positions, spend 1–6 round play points, inspect cards, end turns, and see total Power scores and final victory/draw/defeat. Includes original card art when a matching online or bundled image exists. Currently an **early gameplay prototype**; some targeted, summon, ongoing and faction-line effects remain **explicitly marked PENDING** in the match log and do not silently pretend to resolve. The six-position row representation and one-card-per-round draw timing need in-game validation.
2. **Deck Construction** — local multiple saved decks, rename/delete, copy/share/import deck text, enforce the maximum 2 copies per name, 3 Exotic, 5 Mythic and 15-card total. Incomplete decks can be saved but **cannot enter Sim Battle**. Tokens and entries without verified Power or cost cannot be added. Launch a valid deck straight into battle.
3. **Card Collection** — browse the 110 known card records, original card-face images where available, stats, rarity, faction, effects, and source details; search by name/faction/rarity/effect.
4. **Rules** — in-app Shadowshaper deck limits, six-round scoring, positioning, Inspired/Sustained/Suppressed definitions, and gameplay source link.

The app opens to the new four-section **HomeActivity**. BattleActivity is single-player against a legal CPU practice deck; it is not a reimplementation of Destiny Rising's online PvP client. No background multiplayer or game-account access.


Native Android Studio (Java) deck creator. Current catalog: **110 distinct names** from TodayInDestiny and a detailed community list; this is **not guaranteed to be the full current game roster**.

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

**Status:** Android project and viewer source committed, but full Gradle compilation and on-device verification have not yet been completed.

## References

- https://www.todayindestiny.com/rising/shadowshaper_duels/cards
- https://www.reddit.com/r/ShadowshaperDuels/comments/1nklv84/shadowshaper_card_list/
- https://game8.co/games/Destiny-Rising/archives/546603

Use original assets responsibly and check content-owner permissions before redistributing artwork.
