#!/usr/bin/env python3
"""Offline, no-network Shadowshaper card data audit. Uses Python standard library only."""
from pathlib import Path
from collections import Counter
import csv

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/"app"/"src"/"main"/"assets"
SOURCES=[("cards.tsv",2),("community_cards.tsv",6),("expanded_cards.tsv",6)]
records=[]
errors=[]
for filename,width in SOURCES:
    path=ASSETS/filename
    with path.open(encoding="utf-8",newline="") as handle:
        for index,row in enumerate(csv.reader(handle,delimiter="|"),start=1):
            if not row: continue
            if len(row)!=width:
                errors.append(f"{filename}:{index}: expected {width} columns, found {len(row)}")
            else:
                records.append((row[0],filename,row))
detail_names=set()
with (ASSETS/"card_details.tsv").open(encoding="utf-8",newline="") as handle:
    for index,row in enumerate(csv.reader(handle,delimiter="|"),start=1):
        if not row: continue
        if len(row)!=10: errors.append(f"card_details.tsv:{index}: expected 10 columns, found {len(row)}")
        else: detail_names.add(row[0].casefold())
name_counts=Counter(name.casefold() for name,_,_ in records)
dupes=sorted(name for name,count in name_counts.items() if count!=1)
if dupes: errors.append("Duplicate card names: "+", ".join(dupes))
for name in detail_names:
    if name not in name_counts: errors.append("Extra details for unknown card: "+name)
for name,filename,row in records:
    if filename=="cards.tsv" and name.casefold() not in detail_names:
        errors.append(f"Missing metadata for {name}")
    if filename!="cards.tsv":
        if row[3].strip() and row[3]!="Token" and not row[3].isdigit():
            errors.append(f"Invalid cost on {name}: {row[3]}")
        if row[4].strip() and not row[4].isdigit():
            errors.append(f"Invalid Power on {name}: {row[4]}")
art_directory=ASSETS/"card_art"
bundled=sum(any((art_directory/(name.lower().replace(" ","_").replace("-","_")+"."+e)).is_file()
                for e in ("webp","png","jpg","jpeg")) for name,_,_ in records)
source_images=sum(filename=="cards.tsv" for _,filename,_ in records)
print(f"Unique catalog records: {len(name_counts)}")
print(f"Card source images specified: {source_images} (not verified downloaded)")
print(f"Locally bundled artwork entries: {bundled}")
print(f"Card names missing a local image: {len(name_counts)-bundled}")
print(f"Full basic data community rows: {sum(filename!='cards.tsv' for _,filename,_ in records)}")
if errors:
    print("\nERRORS:")
    for msg in errors: print(" - "+msg)
    raise SystemExit(1)
print("DATA AUDIT PASS (not a device build or gameplay correctness test)")
