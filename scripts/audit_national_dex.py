#!/usr/bin/env python3
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
import re
import time
import urllib.request
import urllib.error

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "app/src/main/java/com/otaviobarreto/pokedex/data/NationalDexCatalog.kt"
text = CATALOG.read_text(encoding="utf-8")
rows = [
    (int(i), slug, int(gen))
    for i, slug, gen in re.findall(r'NationalDexSpecies\((\d+), "([^"]+)", (\d+)\)', text)
]

errors = []
if len(rows) != 1025:
    errors.append(f"expected 1025 species, found {len(rows)}")
ids = [row[0] for row in rows]
if ids != list(range(1, 1026)):
    errors.append("National Dex IDs are not strictly sequential from #0001 to #1025")
if len({row[1] for row in rows}) != 1025:
    errors.append("duplicate National Dex identifiers found")

def check_artwork(pokemon_id: int):
    url = (
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/"
        f"sprites/pokemon/other/official-artwork/{pokemon_id}.png"
    )
    last_error = None
    for attempt in range(3):
        try:
            req = urllib.request.Request(
                url,
                method="HEAD",
                headers={"User-Agent": "POKEDEX-NationalDex-Audit/1.0"},
            )
            with urllib.request.urlopen(req, timeout=12) as response:
                status = getattr(response, "status", 200)
                content_type = response.headers.get("Content-Type", "")
                if status == 200 and "image/" in content_type:
                    return pokemon_id, None
                last_error = f"HTTP {status} {content_type}"
        except Exception as exc:
            last_error = str(exc)
        time.sleep(0.25 * (attempt + 1))
    return pokemon_id, last_error

artwork_errors = []
if not errors:
    with ThreadPoolExecutor(max_workers=24) as pool:
        futures = [pool.submit(check_artwork, pokemon_id) for pokemon_id in ids]
        for future in as_completed(futures):
            pokemon_id, err = future.result()
            if err:
                artwork_errors.append((pokemon_id, err))

if errors or artwork_errors:
    print("NATIONAL_DEX_AUDIT=FAILED")
    for err in errors:
        print(" -", err)
    for pokemon_id, err in sorted(artwork_errors):
        print(f" - artwork #{pokemon_id:04d}: {err}")
    raise SystemExit(1)

print("NATIONAL_DEX_AUDIT=OK")
print("NATIONAL_DEX_SPECIES=1025")
print("NATIONAL_DEX_FIRST=#0001 Bulbasaur")
print("NATIONAL_DEX_LAST=#1025 Pecharunt")
print("NATIONAL_DEX_ARTWORK=1025/1025")
