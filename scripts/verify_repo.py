#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
ui = root / "app/src/main/java/com/otaviobarreto/pokedex/ui"
violations = []

# UI must go through PokedexDataStore instead of bypassing the shared cache layer.
for path in ui.glob("*.kt"):
    text = path.read_text(encoding="utf-8")
    if "PokeApiService.loadPokemon(" in text:
        violations.append(f"{path}: direct loadPokemon call")
    if "PokeApiService.loadSpecies(" in text:
        violations.append(f"{path}: direct loadSpecies call")
    if "PokeApiService.loadEncounters(" in text:
        violations.append(f"{path}: direct loadEncounters call")
    if "PokeApiService.loadEvolutionChain(" in text:
        violations.append(f"{path}: direct loadEvolutionChain call")

store = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokedexDataStore.kt").read_text(encoding="utf-8")
for required in ("prefetchCoreDetails", "prefetchFullDetails", "cachedPokemon", "cachedSpecies"):
    if required not in store:
        violations.append(f"PokedexDataStore missing {required}")

for grid_name in ("PokedexV2Screen.kt", "CollectionScreens.kt"):
    grid_text = (ui / grid_name).read_text(encoding="utf-8")
    if "LaunchedEffect(p.id){PokedexDataStore.prefetchDetails(p.id)}" in grid_text or "LaunchedEffect(p.id) { PokedexDataStore.prefetchDetails(p.id) }" in grid_text:
        violations.append(f"{grid_name}: per-card prefetch regression")

detail = (ui / "PokemonDetailV2Screen.kt").read_text(encoding="utf-8")
if "Render as soon as the two core payloads are ready" not in detail:
    violations.append("progressive detail loading guard missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)

print("Source verification passed.")


# v2.0 release guards
offline = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt").read_text(encoding="utf-8")
if "PACK_VERSION = 3" not in offline:
    violations.append("Offline pack version is not v3")

app = (root / "app/src/main/java/com/otaviobarreto/pokedex/PokedexApplication.kt").read_text(encoding="utf-8")
if ".crossfade(false)" not in app or "256L * 1024L * 1024L" not in app:
    violations.append("Image engine v2 tuning missing")

detail = (ui / "PokemonDetailV2Screen.kt").read_text(encoding="utf-8")
if "CollectionStore.toggleCaptured" not in detail:
    violations.append("Pokemon detail capture integration missing")

workflow = (root / ".github/workflows/android.yml").read_text(encoding="utf-8")
if 'versionName = "2.0.0"' not in workflow or "versionCode = 200" not in workflow:
    violations.append("CI v2.0 version stamping missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)
