#!/usr/bin/env python3
import csv
import io
import urllib.request
from collections import defaultdict

BASE = "https://raw.githubusercontent.com/PokeAPI/pokeapi/master/data/v2/csv/"

def fetch_csv(name):
    req = urllib.request.Request(BASE + name, headers={"User-Agent": "POKEDEX-Problem-Forms-Audit/1.0"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return list(csv.DictReader(io.StringIO(r.read().decode("utf-8"))))

pokemon = fetch_csv("pokemon.csv")
forms = fetch_csv("pokemon_forms.csv")
species_by_pokemon = {int(r["id"]): int(r["species_id"]) for r in pokemon}
counts = defaultdict(int)
for row in forms:
    sid = species_by_pokemon.get(int(row["pokemon_id"]))
    if sid:
        counts[sid] += 1

expected = {
    201: 28,   # Unown
    327: 1,    # Spinda
    351: 4,    # Castform
    386: 4,    # Deoxys
    412: 3,    # Burmy
    413: 3,    # Wormadam
    479: 6,    # Rotom
    493: 19,   # Arceus
    585: 4,    # Deerling
    586: 4,    # Sawsbuck
    649: 5,    # Genesect
    666: 20,   # Vivillon
    669: 5,    # Flabebe
    670: 7,    # Floette
    671: 5,    # Florges
    676: 10,   # Furfrou
    710: 4,    # Pumpkaboo
    711: 4,    # Gourgeist
    773: 18,   # Silvally
    774: 14,   # Minior
    845: 3,    # Cramorant
    869: 64,   # Alcremie
    892: 4,    # Urshifu
    898: 3,    # Calyrex
    925: 2,    # Maushold
    931: 4,    # Squawkabilly
    978: 6,    # Tatsugiri
    982: 2,    # Dudunsparce
    1017: 4,   # Ogerpon
    1024: 3,   # Terapagos
}

errors = []
for sid, minimum in expected.items():
    actual = counts[sid]
    if actual < minimum:
        errors.append(f"#{sid:04d}: expected at least {minimum} form rows, found {actual}")

if errors:
    print("PROBLEM_FORMS_AUDIT=FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("PROBLEM_FORMS_AUDIT=OK")
print(f"SPECIES_CHECKED={len(expected)}")
for sid in sorted(expected):
    print(f"#{sid:04d}={counts[sid]}")
