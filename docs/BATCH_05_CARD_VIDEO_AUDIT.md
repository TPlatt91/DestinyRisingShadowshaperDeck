# Batch 05 — card-detail recording, game metadata, and artwork exports

**Source:** User-uploaded `screen-20261021-171136-1792617031112.mp4` (65.37 s, 2410 × 1080). No external source or reconstruction used. Recorded **21 October 2026**.

## Original on-screen card data

Left diamond is **Power**; right blue circle is **Light cost**. Catalog/detail TSV columns use `light_cost|power`.

| Card | Faction | Rarity | Power | Light cost | Ability shown | Acquisition shown |
|---|---|---|---:|---:|---|---|
| Hexguard | Hive | Legendary | 1 | 1 | When Played: Gains the Inspired effect. | Reach Duelist Level 14 |
| Cursed Thrall | Hive | Legendary | 1 | 1 | When Played: Randomly moves to the opponent's front row, then dies at the beginning of your next turn, dealing 2 damage each to the units on the left and right. | Card Vendor |
| Acolyte | Hive | Rare | 2 | 1 | No special effects. | Card Vendor |
| Thrall | Hive | Rare | 2 | 1 | No special effects. | Card Vendor |
| Legionary | Cabal | Rare | 2 | 1 | Sustained (Column): Gains +1 Power. | Card Vendor |
| War Beast | Cabal | Rare | 2 | 1 | No special effects. | Card Vendor |
| Tan-2 | Humans | Exotic | 2 | 2 | Sustained: Doubles your next Power bonus. | Defeat all duelists in Season of Preparations. |
| Ning Fei | Humans | Exotic | 4 | 2 | When Played: Randomly places a Clone in the opponent's front row. | Reach Duelist Lv.9 |
| Clone | Humans | Mythic | 1 | 1 | Sustained: When Power reaches 0, deals 3 damage to the units on the left and right. | Not independently shown; listed as Ning Fei related card |

**Inspired tooltip:** At the end of your turn, gains 1 Power. **Column tooltip:** Pair units vertically in the same column. Clone's classification remains **Summoned unit** so it is excluded from regular deck construction; do not interpret the Mythic label as making it independently playable.

## GitHub implementation

- All **nine** names were already in the 112-name card catalog. The existing Power, Light cost, rarity and faction values matched the recording. No duplicate card entries were created.
- Updated `cards.tsv` to use the recorded trigger text for **Clone, Ning Fei, Tan-2**.
- Updated `community_cards.tsv` to use the recorded full effect description for **Hexguard and Cursed Thrall**.
- Updated `card_details.tsv`: newly added overlays for six cards, and corrected trigger, effect notes, and acquisition for **Clone, Ning Fei, Tan-2**. The acquisition shown for Tan-2 replaces a different previously stored source; the new value was taken directly from this recording.
- Updated `card_image_inventory.tsv` to **EXTRACTED_AWAITING_UPLOAD** for all nine assets.
- Existing battle engine already has card-specific handlers for Hexguard self-Inspired, Cursed Thrall transfer/delayed blast, Legionary Column +1, Tan-2 one-time Power doubling, Ning Fei's randomized Clone on the opponent's front row, and Clone's on-death adjacent 3-damage effect. The basic Hive/Cabal Rare units require only normal Power and placement rules.
- Authored **six local regression tests** in `ShadowBatch05EffectsTest.kt` for the six scripted mechanics. These tests were **not executed** in Android Studio or GitHub Actions. Existing handlers require device QA and are not claimed to be fully faithful to unobserved edge cases.

## Cropped artwork package — **not pushed** by request

Downloadable `shadow_shaper_batch05_FULL_ART_GIF_MP4.zip` contains:

- Nine `card_art/<slug>.webp` complete static card images, **500 × 754**;
- Three `card_anim_gif/<slug>.gif` animated original effects for **Tan-2, Ning Fei, Clone**, eight frames per second;
- Three `card_anim_mp4/<slug>.mp4` H.264 reference clips, 20 fps;
- `batch05_metadata.tsv`, `README.txt`, `card_verification.jpg`, and `animation_verification.jpg`.

The original source dimensions were **2410 × 1080**. The precise source crop is **x=395..898 and y=170..928**, selected based on the complete border corners. No card edge is cut off, and the neighboring information panel and stray background bars are excluded. Animated crops use the **same exact bounds**. Animation windows (recording time): Tan-2 **31.4–40.3 s**; Ning Fei **44.1–52.8 s**; Clone **54.6–62.2 s**. Sampled GIF frames confirmed distinct frames for each animation.

User upload location for the app: `app/src/main/assets/card_art/`. Copy all nine WebP images; for **Tan-2, Ning Fei, and Clone**, also copy the corresponding GIF images directly into the same `card_art` folder. The app prioritizes local GIF on Android 9+; MP4 exports are reference media, not played by the current Android art loader.

**No GitHub Actions or paid CI were triggered**; neither APK build nor in-device game behavior was verified.
