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
    "Xur":"Rock token Power unknown",
    "Tomb Ship":"Random Hive summon pool not verified",
    "Drop Pod":"Random Cabal Line summon pool not verified",
    "Xuan Wei":"Damage and relocation scripted; exact destination rules need verification",
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
with DETAILS.open(newline="",encoding="utf-8") as handle:
    for row in csv.reader(handle,delimiter="|"):
        if not row: continue
        if len(row)!=10 or row[0] not in cards:
            errors.append(f"Malformed or unknown detail: {row}");continue
        c=cards[row[0]];c.update({"cost":row[3],"power":row[4],"class":row[5]})

counts=Counter()
for name,card in cards.items():
    if not card["power"] or not card["cost"]:
        status="DATA BLOCKED"
        note="Cost and/or Power still unknown"
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
