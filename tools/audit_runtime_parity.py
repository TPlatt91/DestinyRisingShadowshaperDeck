#!/usr/bin/env python3
"""Static catalog/asset/rules coverage audit; NEVER reports gameplay success.

Run from the repository root: python tools/audit_runtime_parity.py
No Gradle, GitHub Actions, network, Android SDK, or third-party packages.
"""
from collections import Counter
from pathlib import Path
import csv
import re

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
JAVA = ROOT / "app/src/main/java/com/tplatt/shadowshaper"
TABLES = ["cards.tsv", "community_cards.tsv", "expanded_cards.tsv"]
VIDEOS = "verified_video_cards.tsv"
DETAILS = "card_details.tsv"

def records(filename):
    with (ASSETS / filename).open(encoding="utf-8", newline="") as f:
        return [row for row in csv.reader(f, delimiter="|") if row]

base = {}
errors = []
for filename in TABLES:
    required = 2 if filename == "cards.tsv" else 6
    for i, row in enumerate(records(filename), 1):
        if len(row) != required:
            errors.append(f"{filename}:{i}: expected {required} fields; got {len(row)}")
            continue
        name = row[0]
        if name in base:
            errors.append(f"Duplicate base name: {name}")
            continue
        base[name] = {
            "effect": row[-1],
            "power": "" if required == 2 else row[4],
            "cost": "" if required == 2 else row[3],
            "rarity": "" if required == 2 else row[1],
            "faction": "" if required == 2 else row[2],
            "classification": "",
            "source": filename,
        }

video_names = set()
for i, row in enumerate(records(VIDEOS), 1):
    if len(row) != 6:
        errors.append(f"{VIDEOS}:{i}: invalid field count {len(row)}")
        continue
    name, rarity, faction, cost, power, effect = row
    if name in video_names:
        errors.append(f"Duplicate video card: {name}")
    video_names.add(name)
    if name not in base:
        base[name] = {"classification": "", "source": VIDEOS}
    base[name].update(
        rarity=rarity, faction=faction, cost=cost, power=power, effect=effect
    )

detail_names = set()
for i, row in enumerate(records(DETAILS), 1):
    if len(row) != 10:
        errors.append(f"{DETAILS}:{i}: invalid field count {len(row)}")
        continue
    name = row[0]
    if name in detail_names:
        errors.append(f"Duplicate detail name: {name}")
    detail_names.add(name)
    if name not in base:
        errors.append(f"Orphan details for {name}")
        continue
    base[name].update(faction=row[1], rarity=row[2], cost=row[3],
                      power=row[4], classification=row[5])

effects_source = (JAVA / "ShadowCardEffects.kt").read_text(encoding="utf-8")
art_files = list((ASSETS / "card_art").iterdir())
art_names = {
    item.stem for item in art_files
    if item.is_file() and item.suffix.lower() in
    {".gif", ".webp", ".png", ".jpg", ".jpeg"}
}

def slug(name):
    return re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")

missing_metadata = sorted(name for name in base if name not in detail_names)
invalid_stats = sorted(
    name for name, d in base.items()
    if not d.get("cost", "").isdigit() or not d.get("power", "").isdigit()
)
missing_art = sorted(name for name in base if slug(name) not in art_names)
orphan_art = sorted(art_names - {slug(n) for n in base})
basic = sorted(name for name, d in base.items()
               if not d["effect"].strip() or "no special effects" in d["effect"].lower())
nonbasic = set(base) - set(basic)
references = {name for name in nonbasic if f'"{name}"' in effects_source}

print("SHADOW SHAPER DUELS — STATIC PARITY AUDIT")
print("========================================")
print("Catalog entries:", len(base))
print("Original video-overlaid entries:", len(video_names))
print("Detailed metadata entries:", len(detail_names))
print("Basic-effect cards:", len(basic))
print("Cards with non-basic printed effects:", len(nonbasic))
print("Non-basic names referenced in handlers:", len(references))
print("Locally bundled art files:", len(art_files) - sum(
    x.name == "README.md" for x in art_files
))
print("Missing detailed metadata:", missing_metadata)
print("Missing local art:", missing_art)
print("Unmatched local art slugs:", orphan_art)
print("Invalid numeric Power/cost:", invalid_stats)
print("Non-basic names without handler references:", sorted(nonbasic - references))
print()
print("IMPORTANT: A referenced handler is NOT proof of correct gameplay.")
print("Runtime test status: UNKNOWN. Match-level Destiny: Rising parity: NOT VERIFIED.")
print("Remaining rule concerns: formation-affix geometry; turn/draw timing;")
print("status/buff stacking; summon selection pools; special full-row behavior;")
print("opponent NPC decks and AI; effect timing; Helhest Power conversion.")
print("Run LOCAL tests separately (does not spend GitHub Actions credits):")
print("  gradlew.bat :app:testDebugUnitTest")
print("  gradlew.bat :app:assembleDebug")
if errors or invalid_stats:
    print("STRUCTURAL ERRORS:", *errors, sep="\n - ")
    raise SystemExit(1)
