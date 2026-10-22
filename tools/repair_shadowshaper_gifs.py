#!/usr/bin/env python3
"""Review/rebuild Shadowshaper animated card images from original screen recordings.

NO guessing: a damaged crop cannot be 'uncropped' from a damaged GIF. Supply
original video for that slug and an inspected, full-card crop box before repair.
Never generates fake animation or substitutes another card's art.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "app/src/main/assets/card_art"
REVIEW = ROOT / "artwork_review/gif_repair"
MANIFEST = ROOT / "tools/shadowshaper_gif_repair.csv"
WIDTH, HEIGHT = 500, 754
NAMES = {
    "umeko": "Umeko",
    "gwynn": "Gwynn",
    "bazinet": "Bazinet",
    "carrier": "Carrier",
    "elite_walker": "Elite Walker",
    "luksis_3": "Luksis-3",
    "resilient_grenade_dreg": "Resilient Grenade Dreg",
    "resilient_marauder": "Resilient Marauder",
    "tenacious_knight": "Tenacious Knight",
    "tenacious_shielded_knight": "Tenacious Shielded Knight",
    "umbliks_prime": "Umbliks Prime",
    "wu_ming": "Wu Ming",
    "xur": "Xur",
}


def rows():
    with MANIFEST.open(newline="", encoding="utf-8") as f:
        entries = list(csv.DictReader(f))
    if {row["slug"] for row in entries} != set(NAMES):
        raise ValueError("Manifest does not match the 13 target cards")
    return entries


def source_path(row):
    source = row["source"].strip()
    return (ROOT / source).resolve() if source else None


def crop(row):
    fields = ("x", "y", "width", "height")
    if any(not row[c].strip() for c in fields):
        return None
    values = [int(row[c]) for c in fields]
    x, y, w, h = values
    if x < 0 or y < 0 or min(w, h) <= 0:
        raise ValueError(f"{row['slug']}: invalid crop bounds")
    if abs(w / h - WIDTH / HEIGHT) > 0.012:
        raise ValueError(f"{row['slug']}: crop aspect ratio {w/h:.4f} is not 500:754; "
                         "do not stretch or chop off the right card edge")
    return x, y, w, h


def probe_size(source):
    p = subprocess.run(
        ["ffprobe", "-v", "error", "-select_streams", "v:0",
         "-show_entries", "stream=width,height", "-of", "json", str(source)],
        capture_output=True, text=True, check=True)
    stream = json.loads(p.stdout)["streams"][0]
    return int(stream["width"]), int(stream["height"])


def check_crop(source, rect, slug):
    sw, sh = probe_size(source)
    x, y, w, h = rect
    if x + w > sw or y + h > sh:
        raise ValueError(f"{slug}: crop {rect} extends beyond source {sw}x{sh}")


def checked_source(row):
    path = source_path(row)
    if path is None or not path.is_file():
        return None
    if path.suffix.lower() not in (".mp4", ".mkv", ".mov", ".webm", ".gif"):
        raise ValueError(f"{row['slug']}: unsupported recording type: {path}")
    return path


def draw_previews(slug, source, rect, start, duration):
    """Contact sheet shows start, quarter, midpoint, 3/4, and near-end frames."""
    from PIL import Image, ImageDraw
    REVIEW.mkdir(parents=True, exist_ok=True)
    previews = []
    for fraction in (0.0, 0.25, 0.5, 0.75, 0.95):
        timestamp = start + duration * fraction
        command = ["ffmpeg", "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
                   "-ss", f"{timestamp:.3f}", "-i", str(source), "-frames:v", "1"]
        if rect:
            x, y, w, h = rect
            command += ["-vf", f"crop={w}:{h}:{x}:{y}"]
        command += [str(REVIEW / f".{slug}.{int(fraction*100)}.png")]
        result = subprocess.run(command, capture_output=True, text=True)
        if result.returncode:
            raise RuntimeError(f"Cannot extract frame at {timestamp}s: {result.stderr.strip()}")
        frame = REVIEW / f".{slug}.{int(fraction*100)}.png"
        with Image.open(frame) as im:
            preview = im.convert("RGB")
            preview.thumbnail((250, 377), Image.Resampling.LANCZOS)
            previews.append((int(fraction * 100), preview.copy()))
        frame.unlink(missing_ok=True)
    sheet = Image.new("RGB", (5 * 270, 425), "#20242c")
    painter = ImageDraw.Draw(sheet)
    for i, (percent, preview) in enumerate(previews):
        sheet.paste(preview, (i * 270 + (270-preview.width)//2, 12))
        painter.text((i * 270 + 8, 395), f"{NAMES[slug]} — {percent}%", fill="white")
    path = REVIEW / f"{slug}_contact_sheet.jpg"
    sheet.save(path, quality=92)
    return path


def duration_for(row):
    start = float(row["start_seconds"] or 0)
    duration = float(row["duration_seconds"] or 0)
    if start < 0 or duration <= 0 or duration > 30:
        raise ValueError(f"{row['slug']}: set a valid 0–30 second animation clip")
    return start, duration


def audit():
    print(f"{'CARD':30} {'GIF IN APP':12} {'ORIGINAL SOURCE':22} {'CROP':10}")
    for row in rows():
        slug = row["slug"]
        art = ART / f"{slug}.gif"
        gif = "present" if art.exists() else "MISSING"
        path = source_path(row)
        source = "ready" if path and path.is_file() else "NEEDS SOURCE"
        rect = "ready" if crop(row) else "NEEDS CROP"
        print(f"{NAMES[slug]:30} {gif:12} {source:22} {rect:10}")
    print("Note: a present GIF is not evidence its visible content is correctly cropped.")


def prepare(row, mode, apply=False):
    slug = row["slug"]
    source = checked_source(row)
    if source is None:
        raise ValueError(f"{slug}: original recording missing; do not create fake artwork")
    rect = crop(row)
    if rect is None:
        raise ValueError(f"{slug}: inspected full-card crop is missing in manifest")
    check_crop(source, rect, slug)
    start, duration = duration_for(row)
    if mode == "preview":
        return draw_previews(slug, source, rect, start, duration)
    REVIEW.mkdir(parents=True, exist_ok=True)
    out = REVIEW / f"{slug}_repaired.gif"
    x, y, w, h = rect
    filtergraph = (f"fps=12,crop={w}:{h}:{x}:{y},"
                   f"scale={WIDTH}:{HEIGHT}:flags=lanczos,split[a][b];"
                   f"[a]palettegen=stats_mode=full:reserve_transparent=0[p];"
                   f"[b][p]paletteuse=dither=bayer:bayer_scale=3")
    args = ["ffmpeg", "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
            "-ss", str(start), "-t", str(duration), "-i", str(source),
            "-filter_complex", filtergraph, "-gifflags", "0", "-loop", "0", str(out)]
    proc = subprocess.run(args, capture_output=True, text=True)
    if proc.returncode != 0:
        out.unlink(missing_ok=True)
        raise RuntimeError(f"{slug}: FFmpeg conversion failed: {proc.stderr.strip()}")
    # Reject one-frame placeholders and malformed output rather than pretending it animates.
    from PIL import Image, ImageSequence
    with Image.open(out) as im:
        if im.size != (WIDTH, HEIGHT) or getattr(im, "n_frames", 1) < 2:
            raise ValueError(f"{slug}: output is not a correctly sized animated GIF")
        sample_hashes = {hashlib.md5(f.convert("RGB").tobytes()[::127]).hexdigest()
                         for i, f in enumerate(ImageSequence.Iterator(im)) if i % 3 == 0}
        if len(sample_hashes) < 2:
            raise ValueError(f"{slug}: no visible animation detected — source segment is static")
    if apply:
        target = ART / f"{slug}.gif"
        backups = REVIEW / "backups"
        backups.mkdir(parents=True, exist_ok=True)
        if target.exists():
            save = backups / f"{slug}_{hashlib.sha256(target.read_bytes()).hexdigest()[:12]}.gif"
            if not save.exists():
                shutil.copy2(target, save)
        shutil.copy2(out, target)
        print(f"REPLACED {target.relative_to(ROOT)} (original backed up)")
    return out


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("audit", "preview", "repair"))
    parser.add_argument("--card", help="Slug; omit to process all 13")
    parser.add_argument("--apply", action="store_true", help="Replace live GIF files (creates backups)")
    args = parser.parse_args()
    if args.mode == "audit":
        audit()
        return
    if shutil.which("ffmpeg") is None or shutil.which("ffprobe") is None:
        parser.error("FFmpeg and FFprobe must be installed and on PATH")
    if args.apply and args.mode != "repair":
        parser.error("--apply only works with repair")
    selected = [r for r in rows() if not args.card or r["slug"] == args.card]
    if not selected:
        parser.error("Unknown card slug")
    failures = 0
    for row in selected:
        try:
            result = prepare(row, args.mode, args.apply)
            print(f"{row['slug']}: {result}")
        except Exception as exc:
            failures += 1
            print(f"NOT REPAIRED {row['slug']}: {exc}")
    if failures:
        raise SystemExit(f"{failures} card(s) still need valid original footage/crop or review")


if __name__ == "__main__":
    main()
