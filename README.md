# Destiny Rising — Shadowshaper Duels Deck Creator

Native Android Studio (Java) deck creator. Current catalog: **110 distinct names** from TodayInDestiny and a detailed community list; this is **not guaranteed to be the full current game roster**.

## Card collection and details

- Original card-face artwork displayed for 34 TodayInDestiny catalog entries when internet access is available; successful images are cached.
- The other 76 names have detailed community-sourced effects, cost, power, faction, and rarity, but **no verified official card-face URLs yet**. The app shows honest missing-artwork placeholders, not synthetic artwork.
- Large scrollable card viewer with name, rarity, faction, Power (red circle value), play cost (blue diamond value), effect, activation/position conditions when documented, acquisition method where known, card class, and linked source reference.
- Printed card number is only displayed if verified: **none of the sources used currently establishes collectible card numbers**. Do not create sequential IDs and pass them off as printed card numbers.
- Search by card name, faction, rarity, or effect; add/remove copies in locally saved named decks; import and copy/share text deck lists. A battle engine is not part of this app.

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

Open the repository root folder in Android Studio, using JDK 17 and the Gradle 8.11.1 distribution compatible with Android Gradle Plugin 8.9.2. The project does not yet contain a Gradle wrapper JAR. If Android Studio requires a Gradle wrapper, run `gradle wrapper --gradle-version 8.11.1` with a local Gradle installation and JDK 17, then sync and build the `app` module.

**Status:** Android project and viewer source committed, but full Gradle compilation and on-device verification have not yet been completed.

## References

- https://www.todayindestiny.com/rising/shadowshaper_duels/cards
- https://www.reddit.com/r/ShadowshaperDuels/comments/1nklv84/shadowshaper_card_list/
- https://game8.co/games/Destiny-Rising/archives/546603

Use original assets responsibly and check content-owner permissions before redistributing artwork.
