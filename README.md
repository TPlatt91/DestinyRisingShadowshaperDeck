# Destiny Rising — Shadowshaper Deck Creator

Android Studio native Java project. The first milestone indexes **34 named cards** displayed in TodayInDestiny's Shadowshaper Duels catalog. It is not yet a full inventory of every card in the minigame.

## Build

Open this repository's root folder in Android Studio, use JDK 17 and allow Gradle sync. Run the `app` configuration on Android 8.0+.

Features: card search; original card face image streaming and disk cache; tap for full artwork and catalog effect; local multiple-deck editing; copy/paste and share text deck lists. Card artwork is loaded over HTTPS, with a placeholder if offline. Card effects are catalog descriptions, *not* a battle simulation. Full card image downloads are not included in the repository.

## Sources and data quality

Card names and shortened effect descriptions are transcribed from https://www.todayindestiny.com/rising/shadowshaper_duels/cards . Images follow the observed website image pattern at `https://www.media.todayindestiny.com/images/rising/shadowshaper_duels/card/<slug>.webp`. URL construction is a *candidate* until individually fetched successfully. The app caches successful downloads; failures show a placeholder rather than a fake image. Do not assume this is a complete catalog.

Other documented cards missing from this catalog include High Servitor, Amu-Atu, Kabr, Knight, Sabikis and Paradoxical Neophyte: https://game8.co/games/Destiny-Rising/archives/546603 . These are not added with invented properties.

No GitHub Actions/paid CI required. Please respect the sources' asset usage and copyright requirements before redistribution.
