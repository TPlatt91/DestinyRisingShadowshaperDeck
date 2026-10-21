# Shadow Shaper Duels — original card-face assets

## Current state (Batch 01 committed — 21 October 2026)

This folder now includes **8 complete card-face assets**: 5 static WebP and 3 animated GIF files pushed by the user. Seven of those card names are in the existing 110-name catalog; **Rossi-11** is currently missing from the catalog. See [the Batch 01 audit](../../../../../docs/BATCH_01_ART_AUDIT.md). The baseline source audit covers 110 catalog records:

- **34 cards:** original card images appear in TodayInDestiny's Shadowshaper gallery. They are already loadable by the Android app over HTTPS when a valid image URL is reachable, and are cached by `CardArt.kt`. Most are not bundled yet; the user's Batch 01 recording contributes some of these card images.
- **76 catalog cards:** the reference gallery does not list them. Three of these now have bundled files: the audit status indicates precisely which. Video review priorities and provenance requirements are recorded in [card artwork recovery docs](../../../../docs/CARD_IMAGE_RECOVERY.md).

Use `card_image_inventory.tsv` in the parent `assets` directory for card-to-filename mapping and source status. Five entries are `BUNDLED_LOCAL_WEBP` and two are `BUNDLED_LOCAL_GIF`; Rossi-11 is an eighth asset not yet cataloged. Import accepted full-card faces under their exact lowercase slug name, for example:

- `mishra.webp`
- `hexguard.webp`
- `paradoxical_neophyte_s_shade.webp`

No unrelated Destiny concept artwork or AI-generated substitutions should be placed here and labeled original.

Recovery tools are in `tools/recover_shadowshaper_art.py`. `audit` checks how many binary files are actually present. `gallery` fetches original sources. `video` produces candidate frames **for identification**, and `promote` crops an explicitly confirmed card into its final filename with source attribution. These tools do not run GitHub Actions.
