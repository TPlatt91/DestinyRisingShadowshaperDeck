#!/usr/bin/env python3
"""Inventory, fetch original full cards, and stage/curate YouTube frames.

A gallery link or a staged video frame is NOT a verified imported card image.
Promote only a visibly identified and manually cropped original game card face.
Run without any GitHub Actions or account credits.
"""
import argparse
import csv
from pathlib import Path
import shutil
import subprocess
import sys
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
INVENTORY = ASSETS / "card_image_inventory.tsv"
ART = ASSETS / "card_art"
REVIEW = ROOT / "artwork_review"
PROVENANCE = ROOT / "docs/card_image_provenance.tsv"
VIDEOS = {
    "fallen_showcase": "https://www.youtube.com/watch?v=WbaNIhYOB1E",
    "hive_showcase": "https://www.youtube.com/watch?v=esgR1GzPYdY",
    "cabal_gameplay": "https://www.youtube.com/watch?v=6azKczzbE9w",
    "mixed_gameplay": "https://www.youtube.com/watch?v=jylPLoyW86o",
}


def records():
    with INVENTORY.open(encoding="utf-8", newline="") as f:
        rows = list(csv.DictReader(f, delimiter="\t"))
    if len(rows) != 110 or len(set(r["slug"] for r in rows)) != 110:
        raise SystemExit("Catalog must have 110 unique slugs; regenerate inventory.")
    return rows


def is_webp(path):
    try:
        with path.open("rb") as f:
            signature = f.read(16)
        return path.stat().st_size > 1024 and signature[:4] == b"RIFF" and signature[8:12] == b"WEBP"
    except OSError:
        return False


def art_path(row):
    return ART / (row["slug"] + ".webp")


def audit(args):
    rows = records()
    bundled = [r for r in rows if is_webp(art_path(r))]
    referenced = [r for r in rows if r["reference_url"] and r not in bundled]
    missing = [r for r in rows if not r["reference_url"] and r not in bundled]
    print(f"Inventory: {len(rows)} | bundled: {len(bundled)} | original online source only: "
          f"{len(referenced)} | needs further research/video: {len(missing)}")
    for r in missing:
        print("VIDEO REVIEW:", r["card_name"], "=>", r["video_source_id"])


def gallery(args):
    ART.mkdir(parents=True, exist_ok=True)
    failures, imported = [], 0
    for r in records():
        if not r["reference_url"] or is_webp(art_path(r)):
            continue
        if args.limit and imported >= args.limit:
            break
        path = art_path(r)
        try:
            request = urllib.request.Request(
                r["reference_url"], headers={"User-Agent": "Mozilla/5.0"}
            )
            with urllib.request.urlopen(request, timeout=15) as response:
                data = response.read(5_000_000)
            if len(data) <= 1024 or data[:4] != b"RIFF" or data[8:12] != b"WEBP":
                raise ValueError("Invalid source image (not a WebP card)")
            tmp = path.with_suffix(".part")
            tmp.write_bytes(data)
            tmp.replace(path)
            imported += 1
            print("ORIGINAL:", r["card_name"], len(data), "bytes")
        except (OSError, ValueError) as exc:
            failures.append((r["card_name"], str(exc)))
            print("FAILED:", r["card_name"], exc, file=sys.stderr)
    REVIEW.mkdir(exist_ok=True)
    (REVIEW / "gallery_errors.tsv").write_text(
        "\n".join("\t".join(x) for x in failures), encoding="utf-8"
    )
    print("Newly imported:", imported, "| failures:", len(failures))
    if failures:
        raise SystemExit(2)


def need(name):
    if shutil.which(name) is None:
        raise SystemExit(f"Missing {name}; extraction did not run.")


def video(args):
    need("ffmpeg")
    if args.start < 0 or args.end <= args.start or args.interval <= 0:
        raise SystemExit("Invalid interval or timestamp range")
    if bool(args.input) == bool(args.url):
        raise SystemExit("Supply exactly one of --input / --url.")
    folder = REVIEW / args.source
    folder.mkdir(parents=True, exist_ok=True)
    if args.url:
        need("yt-dlp")
        subprocess.run([
            "yt-dlp", "-f", "bv*[height<=1080]+ba/b[height<=1080]/b",
            "--merge-output-format", "mp4", "-o", str(folder / "source.%(ext)s"),
            args.url
        ], check=True)
        options = [p for p in folder.glob("source.*")
                   if p.suffix in (".mp4", ".mkv", ".webm")]
        if not options:
            raise SystemExit("No downloaded video file.")
        source = options[0]
    else:
        source = Path(args.input).resolve()
        if not source.is_file():
            raise SystemExit("Video file does not exist.")
    frames = folder / "frames"
    frames.mkdir(exist_ok=True)
    subprocess.run([
        "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
        "-ss", str(args.start), "-t", str(args.end - args.start),
        "-i", str(source), "-vf", "fps=1/" + str(args.interval),
        "-q:v", "2", str(frames / "frame_%05d.jpg")
    ], check=True)
    found = sorted(frames.glob("frame_*.jpg"))
    with (folder / "frame_review.tsv").open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f, delimiter="\t")
        writer.writerow(["frame", "approx_timestamp_seconds", "source_url",
                         "card_name_confirmed"])
        for index, frame in enumerate(found):
            writer.writerow([str(frame), args.start + index * args.interval,
                             args.url or VIDEOS[args.source], "NO"])
    print("Staged unidentified video frames:", len(found), "in", frames)
    print("IMPORTANT: Frames are not card artwork until inspected and cropped.")


def promote(args):
    match = [r for r in records() if r["card_name"].casefold() == args.card.casefold()]
    if len(match) != 1:
        raise SystemExit("Card name not uniquely present in catalog.")
    frame = Path(args.frame).resolve()
    if not frame.is_file():
        raise SystemExit("Source frame does not exist.")
    try:
        x, y, width, height = [int(v) for v in args.crop.split(",")]
    except ValueError:
        raise SystemExit("Crop must be x,y,width,height.")
    if min(x, y) < 0 or min(width, height) < 150:
        raise SystemExit("Crop too small or negative.")
    need("ffmpeg")
    ART.mkdir(exist_ok=True)
    dest = art_path(match[0])
    if dest.exists() and not args.replace:
        raise SystemExit("A card image already exists; use --replace to overwrite.")
    temp = dest.with_suffix(".tmp.webp")
    subprocess.run([
        "ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(frame),
        "-vf", f"crop={width}:{height}:{x}:{y}", "-frames:v", "1",
        "-c:v", "libwebp", "-quality", "92", str(temp)
    ], check=True)
    if not is_webp(temp):
        temp.unlink(missing_ok=True)
        raise SystemExit("Cropped image is invalid.")
    temp.replace(dest)
    PROVENANCE.parent.mkdir(parents=True, exist_ok=True)
    first = not PROVENANCE.exists()
    with PROVENANCE.open("a", newline="", encoding="utf-8") as f:
        writer = csv.writer(f, delimiter="\t")
        if first:
            writer.writerow(["card", "path", "type", "video_url",
                             "timestamp_seconds", "frame", "crop"])
        writer.writerow([match[0]["card_name"], str(dest.relative_to(ROOT)),
                         "video_frame", VIDEOS[args.source], args.timestamp,
                         str(frame), args.crop])
    print("Staged source-attributed full-card crop:", dest)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    commands = p.add_subparsers(dest="command", required=True)
    commands.add_parser("audit").set_defaults(run=audit)
    g = commands.add_parser("gallery")
    g.add_argument("--limit", type=int, default=0)
    g.set_defaults(run=gallery)
    v = commands.add_parser("video")
    v.add_argument("--input")
    v.add_argument("--url")
    v.add_argument("--source", required=True, choices=VIDEOS)
    v.add_argument("--start", type=float, default=0)
    v.add_argument("--end", type=float, required=True)
    v.add_argument("--interval", type=float, default=2)
    v.set_defaults(run=video)
    c = commands.add_parser("promote")
    c.add_argument("--card", required=True)
    c.add_argument("--frame", required=True)
    c.add_argument("--crop", required=True)
    c.add_argument("--source", required=True, choices=VIDEOS)
    c.add_argument("--timestamp", type=float, required=True)
    c.add_argument("--replace", action="store_true")
    c.set_defaults(run=promote)
    args = p.parse_args()
    args.run(args)


if __name__ == "__main__":
    main()
