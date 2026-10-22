# Shadow Shaper Duels — 13 animated-art repairs

**Status (2026-10-21): Tool ready; the 13 animation assets are NOT yet repaired.**

The existing GitHub connector can verify file paths but this session could not
retrieve the approximately 2–3 MB binary GIF contents or the original uploaded
screen recordings. Accordingly it would be misleading to claim visual inspection,
exact crop rectangles, or replacement animations. **No existing GIF was overwritten.**

## Inventory at audit

| Name | Bundled GIF | Needed |
|---|---:|---|
| Umeko | Yes | Clean video/source + verified crop |
| Gwynn | **No** | Original art/animation and recording |
| Bazinet | Yes | Clean video/source + verified crop |
| Carrier | **No** | Original animation (online static card reference exists) |
| Elite Walker | Yes | Clean video/source + verified crop |
| Luksis-3 | Yes | Clean video/source + verified crop |
| Resilient Grenade Dreg | Yes | Clean video/source + verified crop |
| Resilient Marauder | Yes | Clean video/source + verified crop |
| Tenacious Knight | Yes | Clean video/source + verified crop |
| Tenacious Shielded Knight | **No** | Original art/animation and recording |
| Umbliks Prime | Yes | Clean video/source + verified crop |
| Wu Ming | Yes | Clean video/source + verified crop |
| Xur | Yes | Clean video/source + verified crop |

Any existing GIF is a potential **reference**, not guaranteed recoverable raw
artwork. If its right edge has been cut off, trimming that GIF cannot restore
what is missing. Start from the original screen recording or the original
uncropped animation; do not substitute another card's artwork.

## Source-to-GIF repair

No GitHub Actions or credits are used. Work is local. Requires Python 3,
Pillow (`python -m pip install Pillow`), and FFmpeg + FFprobe on PATH.

1. Place original MP4/MOV/WebM recordings in a local folder in the repo, e.g.
   `artwork_review/gif_repair/sources/umeko.mp4`. You can use different video
   filenames and one recording for multiple cards.
2. Edit `tools/shadowshaper_gif_repair.csv`: for each slug, provide
   `source` (repo-relative path), `start_seconds`, `duration_seconds` and the
   full, clean card-face `x,y,width,height` on the **source recording**.
   Do not crop only the animated illustration if the app expects the full card.
   The crop's ratio must be approximately 500:754.
3. Run `python tools/repair_shadowshaper_gifs.py audit`.
4. Run `python tools/repair_shadowshaper_gifs.py preview` and inspect the
   5-frame contact sheets under `artwork_review/gif_repair/`. Inspect all
   five samples to check that no neighboring screen, unrelated artwork,
   wrong-side clipping, or UI text intrudes.
5. Run `python tools/repair_shadowshaper_gifs.py repair` for staged output.
   The tool rejects missing sources, bad crop geometry, single-frame GIFs,
   and segments without perceptible frame change.
6. After inspecting actual GIF playback, run
   `python tools/repair_shadowshaper_gifs.py repair --apply`.
   Original GIFs are backed up in `artwork_review/gif_repair/backups/`.
   New 500×754 GIFs loop continuously and are automatically read by
   `CardArt.kt`, which already prioritizes local GIFs.
7. Commit only the 13 visually approved replacements to
   `app/src/main/assets/card_art/`. Do not commit raw videos or backups.

One-card example:

```text
python tools/repair_shadowshaper_gifs.py preview --card umeko
python tools/repair_shadowshaper_gifs.py repair --card umeko
python tools/repair_shadowshaper_gifs.py repair --card umeko --apply
```

*The crop numbers are deliberately not guessed.* Bad crop coordinates would
repeat the exact missing-right-edge and stray-screen errors reported by the
user. The original recordings from earlier chat turns are not available in
this workspace; they must be provided in the working tree or attached again
for the actual corrections.

## Validation

The Python tool was exercised locally on synthetic MP4 and GIF inputs.
The resulting 500×754 looping GIF passed frame-count and animation checks.
**This is a test of the repair tool, not confirmation that any of the 13
production GIF files is visually correct.**
