> **CURRENT STATUS (21 October 2026):** This document is a historical extraction audit. See [the current Batch 01–08 audit](CARD_ENGINE_COVERAGE.md) and [video metadata index](VIDEO_CARD_METADATA_BATCHES_01_08.md). Some missing-card/ability claims below have been superseded by subsequent source commits. Device gameplay tests are still pending.

# Batch 01 — card art / animation integration

Recorded after the user's Batch 01 GitHub push, 21 October 2026.

## Files present in Android assets

| Card | Bundled asset | Catalog match | Android playback |
|---|---|---|---|
| Dreg | `card_art/dreg.webp` | yes | full-face static |
| Novice Marauder | `card_art/novice_marauder.webp` | yes | full-face static |
| Resilient Dreg | `card_art/resilient_dreg.webp` | yes | full-face static |
| Rock Pile | `card_art/rock_pile.webp` | yes | full-face static |
| Zhiyao | `card_art/zhiyao.webp` | yes | full-face static |
| Rossi-11 | `card_art/rossi_11.gif` | **NO** | GIF loader supports it, but this card does not yet exist in any of the three on-device card catalogs |
| Resilient Captain | `card_art/resilient_captain.gif` | yes | animated on Android 9+ |
| Unrelenting Captain | `card_art/unrelenting_captain.gif` | yes | animated on Android 9+ |

The three GIFs are user-supplied, complete game-card recordings. The Android loader now searches **`card_art/<slug>.gif`** before WebP, PNG or JPEG and uses `ImageDecoder.decodeDrawable` on API 28+; animated drawables are started after assigning them to the view. On API 26–27, a non-animated GIF first-frame fallback is attempted with `BitmapFactory`. When replacing a card, any previous animation stops.

A GIF's extension is not sufficient to guarantee it actually contains motion; frame counts and Android playback remain to be tested on device. No GitHub Actions or device builds were run.

### Outstanding source/game-data integration

- **Rossi-11** was present in the Batch 01 recording as a Humans / Exotic card with cost **3**, Power **1**, and `When Played: Grants an overshield to one of your units.`
- Rossi-11 is not currently defined in `cards.tsv`, `community_cards.tsv`, `expanded_cards.tsv`, or `card_details.tsv`, so it cannot appear in the playable deck collection merely by importing `rossi_11.gif`.
- The present engine does not implement a verified Rossi-11 overshield mechanic. **Do not declare it fully playable or treat its effect as implemented** until gameplay code and targeting/overshield tests are added.
- Animation fidelity and device-performance checks remain outstanding, particularly when multiple GIF cards are visible at once.

### Next batches

Keep a single original full-face card asset per card under `app/src/main/assets/card_art`, named from the actual card title. Use a GIF for animated Exotic/Mythic versions, WebP for ordinary static cards. If a GIF and WebP exist with the same slug, GIF takes priority on Android 9+. Store any provenance (recording filename, timestamp, card stats/effects) alongside the per-batch review, not as false claims about online artwork availability.
