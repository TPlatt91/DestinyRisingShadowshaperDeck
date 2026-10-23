#!/usr/bin/env python3
"""Audit known Shadowshaper effects without GitHub Actions, network or Android.

Outputs a clear inventory, NOT a claim that scripted effects are gameplay-verified.
"""
from pathlib import Path
from collections import Counter
import csv
import re
import sys

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/"app/src/main/assets"
ENGINE=ROOT/"app/src/main/java/com/tplatt/shadowshaper/ShadowEngine.kt"
SCRIPTED=ENGINE.read_text(encoding="utf-8") + "\n" + (ENGINE.parent/"ShadowCardEffects.kt").read_text(encoding="utf-8")
MAIN=ASSETS/"cards.tsv"
TABLES=[MAIN,ASSETS/"community_cards.tsv",ASSETS/"expanded_cards.tsv"]
DETAILS=ASSETS/"card_details.tsv"
BLOCKED_BY_RULES={
    # All Rock token stats have now been extracted from original footage:
    # Xur is SCRIPTED / UNVERIFIED, no longer DATA BLOCKED or incomplete.
    "Tomb Ship":"Random Hive summon pool and weighting not verified",
    "Drop Pod":"Random printed Line summon pool and weighting not verified",
    "Xuan Wei":"Damage and relocation scripted; full-board destination rules need verification",
}
cards={}
errors=[]
for path in TABLES:
    with path.open(newline="",encoding="utf-8") as handle:
        for row in csv.reader(handle,delimiter="|"):
            if not row: continue
            expected=2 if path==MAIN else 6
            if len(row)!=expected:
                errors.append(f"Malformed {path.name}: {row}")
                continue
            name=row[0]
            if name in cards:
                errors.append(f"Duplicate card: {name}");continue
            cards[name]={"name":name,"cost":"" if expected==2 else row[3],
                         "power":"" if expected==2 else row[4],
                         "text":row[-1],"source":path.name,"class":""}
# Match ShadowData.load(): the user's captured card-detail text overrides
# community/wiki text, BEFORE details add the printed stats and categories.
# Without this overlay, the audit classifies some fully printed abilities
# based on obsolete web descriptions rather than the actual in-app effects.
video_overlaid=set()
with (ASSETS/"verified_video_cards.tsv").open(newline="",encoding="utf-8") as handle:
    for line_number,row in enumerate(csv.reader(handle,delimiter="|"),1):
        if not row: continue
        if len(row)!=6:
            errors.append(f"Malformed verified_video_cards.tsv line {line_number}: {row}")
            continue
        name,rarity,faction,cost,power,effect=row
        if name in video_overlaid:
            errors.append(f"Duplicate verified video card: {name}")
        video_overlaid.add(name)
        if name not in cards:
            cards[name]={"name":name,"class":""}
        cards[name].update(cost=cost,power=power,text=effect,
                           source="verified_video_cards.tsv")

with DETAILS.open(newline="",encoding="utf-8") as handle:
    for row in csv.reader(handle,delimiter="|"):
        if not row: continue
        if len(row)!=10 or row[0] not in cards:
            errors.append(f"Malformed or unknown detail: {row}");continue
        c=cards[row[0]];c.update({"cost":row[3],"power":row[4],"class":row[5]})

counts=Counter()
for name,card in cards.items():
    if not str(card["power"]).isdigit() or not str(card["cost"]).isdigit():
        status="DATA BLOCKED"
        note="Cost and/or Power missing or not numeric"
    elif "no special effects" in card["text"].lower():
        status="BASIC"
        note="No-effect card: ordinary placement, cost and Power"
    elif name in BLOCKED_BY_RULES:
        status="INCOMPLETE"
        note=BLOCKED_BY_RULES[name]
    elif f'"{name}"' in SCRIPTED:
        status="SCRIPTED / UNVERIFIED"
        note="Name used by engine; all edge cases and gameplay timing not certified"
    else:
        status="NO HANDLER"
        note="No reference found in engine"
        errors.append("No effect handler for "+name)
    card["status"]=status;card["note"]=note
    counts[status]+=1

print("SHADOWSHAPER EFFECT COVERAGE AUDIT")
print("=================================")
print("Unique records:",len(cards))
print("Verified-video effect overlays:",len(video_overlaid))
for k,v in counts.most_common():print(f"{k}: {v}")
print("IMPORTANT: SCRIPTED / UNVERIFIED is NOT a completed or validated card.")
print()
for c in cards.values():
    if c["status"] in ("DATA BLOCKED","INCOMPLETE","NO HANDLER"):
        print(f"{c['status']}: {c['name']}: {c['note']}")
print()
if errors:
    print("STRUCTURAL ERRORS:")
    for error in errors:print(" - "+error)
    sys.exit(1)
print("PASS: Catalog has non-duplicated entries and every effect name is accounted for.")
if "--csv" in sys.argv:
    output=ROOT/"shadowshaper_effect_coverage.csv"
    with output.open("w",newline="",encoding="utf-8") as handle:
        writer=csv.DictWriter(handle,
            fieldnames=["name","status","note","source","cost","power","text","class"])
        writer.writeheader();writer.writerows(cards.values())
    print("Wrote:",output)
