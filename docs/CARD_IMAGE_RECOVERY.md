# Shadow Shaper Duels — card artwork recovery

Audit date: **21 October 2026**. This is an audit of the project's **110 known card records**, not proof of the full game-wide card count.

## Verified repository inventory

- **110** distinct catalog names: 34 in `cards.tsv`, 46 in `community_cards.tsv`, 30 in `expanded_cards.tsv`.
- **0 bundled card-face image binaries** in `app/src/main/assets/card_art`. The folder currently contains only a README.
- **34 names appear with individual image links** in the [TodayInDestiny Shadowshaper card gallery](https://www.todayindestiny.com/rising/shadowshaper_duels/cards). The `reference_url` values in `card_image_inventory.tsv` follow those published slugs; a URL is **not the same as a successfully retrieved binary**.
- **76** catalog names absent from that gallery. All 76 require other original-image sources or carefully selected card frames.
- Inventory: [`app/src/main/assets/card_image_inventory.tsv`](../app/src/main/assets/card_image_inventory.tsv). Status is intentionally **published-source versus video-review-required**, not “image downloaded.”

### Video reference queue

| Source ID | Coverage | Video | Relevant chapters / first review location |
|---|---|---|---|
| `fallen_showcase` | 14 Fallen records without images | [Fallen Cards — DanS118](https://www.youtube.com/watch?v=WbaNIhYOB1E) | All-card presentation starts **01:39**; match footage **27:12**, **32:15**, **36:24**. |
| `hive_showcase` | 18 Hive records without images | [Hive Cards — DanS118](https://www.youtube.com/watch?v=esgR1GzPYdY) | All-card presentation starts **00:18**; bot games **25:03**, then **27:50**, **32:22**, **37:19**, **41:04**. |
| `cabal_gameplay` | 25 Cabal records without images | [Cabal Cards Gameplay & Commentary — DanS118](https://www.youtube.com/watch?v=6azKczzbE9w) | Gameplay starts **00:56**, then **05:12**, **08:52**, **10:32**, **14:08**, **17:17**, **20:58**, **25:15**; this is gameplay, **not** a guaranteed full collection showcase. |
| `mixed_gameplay` | 19 Human/mixed records without images | [How to Get the OTK Triumph — DanS118](https://www.youtube.com/watch?v=jylPLoyW86o) | Deck discussion **01:15**, gameplay examples **13:28** and **15:06**; not guaranteed to contain all 19 cards. |

The categorization assigns a **review priority** only. There is no claim that each assigned video actually shows every matching card. Specific cards need full-frame confirmation before use.

## Image acceptance / naming

1. Prefer the clean source gallery's full printed card image. Place downloaded full original card faces at `app/src/main/assets/card_art/<slug>.webp` where slug is lowercase, non-alphanumerics collapsed to underscores. This matches `CardArt.kt`.
2. For a video-only card, pause on the largest **fully visible, readable card** in the collection/card-inspection screen. Avoid battlefield thumbnails, animation effects, captions, hand overlap, streamer logos, or cropped edges.
3. Extract a **timestamped source frame** using FFmpeg. Stage it separately and manually confirm the actual card name. Do not associate a frame with a card solely because they are in the same faction showcase.
4. Crop exactly to the printed card boundary. Avoid altering the original card's typography, illustration, borders or numeric Power/Light values. Use perspective correction only when necessary, not generative repainting.
5. Mark images **`video_frame`** and retain the source video URL, timestamp, crop region, and width/height in the provenance ledger. The images will typically be lower resolution than original WebP cards.
6. Never mark a card **complete** until its actual local image binary is committed or a direct image has been verified available at runtime. A source URL or video candidate is not sufficient.

## Downloader and video extractor

The repository's `tools/recover_shadowshaper_art.py` provides offline audit, network gallery downloads and source-frame staging. It requires no GitHub Actions:

```text
python tools/recover_shadowshaper_art.py audit
python tools/recover_shadowshaper_art.py gallery
python tools/recover_shadowshaper_art.py video --input shadowshaper_video.mp4 --source fallen_showcase --start 99 --end 600 --interval 3
python tools/recover_shadowshaper_art.py promote --card "Unrelenting Captain" --frame staged/frame_000123.png --crop 50,100,440,650 --source fallen_showcase --timestamp 180
```

For direct video URLs, `video --url https://www.youtube.com/watch?v=...` additionally requires `yt-dlp` and `ffmpeg`. A video download is not bypassed or simulated when those dependencies / network access are unavailable.

## Blocker currently observed

This environment cannot resolve/reach the external media or YouTube hosts for direct binary downloads. The clean source links were inspected via web tools, but the **original WebP file transfers failed**; YouTube video bytes are likewise unavailable here. Consequently **no card images have been claimed to be extracted or committed**. The 110-card inventory and repeatable recovery scripts are committed so work can resume as soon video bytes or a working download route is available.

**Copyright note:** Original Destiny: Rising artwork belongs to its respective rightsholders. Keep original-source attribution; do not misrepresent edits or extracted stills as newly licensed assets.
