# Shadow Shaper Duels — original card-face assets

## Current state (21 October 2026)

There are **no card-face WebP image binaries checked into this folder**. It contains only this README. The full audit covers 110 catalog records:

- **34 cards:** original card images appear in TodayInDestiny's Shadowshaper gallery. They are already loadable by the Android app over HTTPS when a valid image URL is reachable, and are cached by `CardArt.kt`. These are **not bundled** until their actual bytes are downloaded and committed.
- **76 cards:** the reference gallery does not list them. Video review priorities and provenance requirements are recorded in [card artwork recovery docs](../../../../docs/CARD_IMAGE_RECOVERY.md).

Use `card_image_inventory.tsv` in the parent `assets` directory for the authoritative card-to-filename mapping and source status. Import accepted full-card faces under their exact lowercase slug name, for example:

- `mishra.webp`
- `hexguard.webp`
- `paradoxical_neophyte_s_shade.webp`

No unrelated Destiny concept artwork or AI-generated substitutions should be placed here and labeled original.

Recovery tools are in `tools/recover_shadowshaper_art.py`. `audit` checks how many binary files are actually present. `gallery` fetches original sources. `video` produces candidate frames **for identification**, and `promote` crops an explicitly confirmed card into its final filename with source attribution. These tools do not run GitHub Actions.
