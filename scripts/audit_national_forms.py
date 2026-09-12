#!/usr/bin/env python3
import csv
import io
import urllib.request
from collections import defaultdict

BASE = "https://raw.githubusercontent.com/PokeAPI/pokeapi/master/data/v2/csv/"

def fetch_csv(name):
    req = urllib.request.Request(BASE + name, headers={"User-Agent": "POKEDEX-Forms-Audit/1.0"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return list(csv.DictReader(io.StringIO(r.read().decode("utf-8"))))

species = [r for r in fetch_csv("pokemon_species.csv") if int(r["id"]) <= 1025]
pokemon = fetch_csv("pokemon.csv")
forms = fetch_csv("pokemon_forms.csv")

species_by_pokemon = {int(r["id"]): int(r["species_id"]) for r in pokemon}
forms_by_species = defaultdict(list)
for row in forms:
    pokemon_id = int(row["pokemon_id"])
    species_id = species_by_pokemon.get(pokemon_id)
    if species_id is None or species_id > 1025:
        continue
    forms_by_species[species_id].append(row)

errors = []
if len(species) != 1025:
    errors.append(f"expected 1025 National Dex species, found {len(species)}")

missing = [i for i in range(1, 1026) if not forms_by_species.get(i)]
if missing:
    errors.append("species without form rows: " + ", ".join(map(str, missing[:20])))

without_default = [
    sid for sid in range(1, 1026)
    if forms_by_species.get(sid) and not any(r["is_default"] == "1" for r in forms_by_species[sid])
]
if without_default:
    errors.append("species without a default form: " + ", ".join(map(str, without_default[:20])))

form_rows = sum(len(v) for v in forms_by_species.values())
species_with_multiple = sum(1 for v in forms_by_species.values() if len(v) > 1)

cosmetic_same_pokemon = 0
for rows in forms_by_species.values():
    counts = defaultdict(int)
    for row in rows:
        counts[int(row["pokemon_id"])] += 1
    cosmetic_same_pokemon += sum(max(0, n - 1) for n in counts.values())

if form_rows < 1579:
    errors.append(f"form coverage regressed: expected at least 1579 rows, found {form_rows}")
if species_with_multiple < 249:
    errors.append(f"multi-form species coverage regressed: expected at least 249, found {species_with_multiple}")
if cosmetic_same_pokemon < 228:
    errors.append(f"cosmetic-form coverage regressed: expected at least 228, found {cosmetic_same_pokemon}")

max_sid, max_rows = max(forms_by_species.items(), key=lambda item: len(item[1]))

if errors:
    print("NATIONAL_FORMS_AUDIT=FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("NATIONAL_FORMS_AUDIT=OK")
print("NATIONAL_DEX_SPECIES=1025/1025")
print(f"POKEAPI_FORM_ROWS={form_rows}")
print(f"SPECIES_WITH_MULTIPLE_FORMS={species_with_multiple}")
print(f"COSMETIC_FORMS_SHARING_POKEMON_ID={cosmetic_same_pokemon}")
print(f"MOST_FORMS_SPECIES=#{max_sid:04d} FORMS={len(max_rows)}")
