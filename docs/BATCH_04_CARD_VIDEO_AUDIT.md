# Batch 04 — video card extraction and gameplay integration

**Source:** User-uploaded `screen-20261021-134608-1792604739278.mp4~2.mp4` (13.89 seconds, 2410 × 1080, 21 October 2026). Three card-detail screens appear in the recording: **Servitor**, **Grenade Dreg**, and **Vandal**. The diamond on the left is **Power** and the circle on the right is **Light cost**.

| Name | Faction | Rarity | Power | Light cost | Exact on-screen ability text |
|---|---|---|---:|---:|---|
| Servitor | Fallen | Legendary | 2 | 2 | Sustained: Your units deal 1 additional damage. |
| Grenade Dreg | Fallen | Rare | 1 | 2 | Sustained: Every unit deployed in this row grants this unit +1 Power. |
| Vandal | Fallen | Rare | 2 | 2 | When Played: Deals 2 damage to an opposing unit in the front row. |

**Acquisition sources and card numbers are not shown in this recording**, so the new Servitor detail entry does not invent them.

## Repository changes

- All three cards **already existed** in the 112-card catalog; no duplicates or new catalog names were required.
- Corrected `cards.tsv`: Grenade Dreg was incorrectly labeled **No special effects**. Vandal's existing text was updated to the printed **When Played** trigger. Servitor's `expanded_cards.tsv` entry already matched its on-screen effect.
- Updated `card_details.tsv`: recorded effect conditions for Grenade Dreg and Vandal; added the missing Servitor detail overlay with verified faction, rarity, cost and Power.
- `ShadowCardEffects.kt`: Servitor's +1 damage aura and Vandal's targeted 2 damage already had game-engine handlers. Added Grenade Dreg to known sustained cards and added `fireAllyPlacement` for +1 Power when a unit is deployed in its row.
- `ShadowEngine.kt`: direct plays and token summons both invoke allied placement effects before enemy placement reactions.
- Local regression tests `ShadowBatch04EffectsTest.kt` cover same-row vs other-row deployment, summoned units, Suppression, Servitor's aura, and Vandal's Front Row targeting. These are **authored tests**, not evidence of a successful Android build.
- `card_image_inventory.tsv`: three images are marked **EXTRACTED_AWAITING_UPLOAD**, not bundled.

## Artwork package and correct crop

The user will push artwork when extraction is complete. The downloadable ZIP `shadow_shaper_batch04_full_cards.zip` contains exactly:

- `card_art/servitor.webp`
- `card_art/grenade_dreg.webp`
- `card_art/vandal.webp`
- `batch04_cards_metadata.tsv`, `verification_sheet.jpg`, and `README.txt`

All three are **500 × 754** and were cropped from the *actual* source video at **x=395..900, y=170..930**, visually verifying that **all four full-card border corners and the complete right border are present**. The source video is 2410 × 1080; coordinates are source pixels, not image-preview pixels.

This recording contains one Legendary and two Rare cards, **no animated Exotic or Mythic**. No GIF/MP4 was fabricated. Any effects visible in normal footage are not proof of a dedicated animated card face.

## Gameplay fidelity notes

- Under the literal Grenade Dreg effect wording, **its own deployment counts** as one unit deployed in its row; each later friendly card play or token summon in the same row contributes another +1. This self-count interpretation has not been compared to original gameplay behavior and should be verified in a match.
- The sustained effect ceases to trigger while Grenade Dreg is Suppressed; previously earned bonuses are handled by the existing suppression logic.
- No GitHub Actions or paid CI workflows were triggered. Android compilation and gameplay on device remain unverified.
