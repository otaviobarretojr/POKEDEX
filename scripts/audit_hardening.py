#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []
notes = []

def read(path):
    p = ROOT / path
    if not p.exists():
        errors.append(f"missing required file: {path}")
        return ""
    return p.read_text(encoding="utf-8")

main = read("app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt")
app = read("app/src/main/java/com/otaviobarreto/pokedex/PokedexApplication.kt")
routes = read("app/src/main/java/com/otaviobarreto/pokedex/PokedexRoutes.kt")
boxes = read("app/src/main/java/com/otaviobarreto/pokedex/ui/BoxesV2Screen.kt")
detail = read("app/src/main/java/com/otaviobarreto/pokedex/ui/PokemonDetailV2Screen.kt")
journey = read("app/src/main/java/com/otaviobarreto/pokedex/ui/JourneyScreen.kt")
offline = read("app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt")
cache = read("app/src/main/java/com/otaviobarreto/pokedex/data/PersistentApiCache.kt")
collection = read("app/src/main/java/com/otaviobarreto/pokedex/data/CollectionStore.kt")
variants = read("app/src/main/java/com/otaviobarreto/pokedex/data/VariantCollectionStore.kt")
manifest = read("app/src/main/AndroidManifest.xml")
gradle = read("app/build.gradle.kts")

required_route_markers = [
    'const val HOME = "home"',
    'const val POKEDEX = "pokedex"',
    'const val BOXES = "boxes"',
    'const val CENTRAL = "central"',
]
for marker in required_route_markers:
    if marker not in routes:
        errors.append(f"navigation contract missing: {marker}")

if "PokedexRoutes.isSecondary(currentRoute)" not in main:
    errors.append("MainActivity must use centralized secondary-route detection")

for marker in [
    "PersistentApiCache.initialize(this)",
    "OfflineGamePackManager.initialize(this)",
    "CollectionStore.initialize(this)",
    "VariantCollectionStore.initialize(this)",
    "JourneyProgressStore.initialize(this)",
]:
    if marker not in app:
        errors.append(f"startup initialization missing: {marker}")

if "CollectionIntegrityService.repair()" not in app:
    errors.append("collection integrity repair missing at startup")

if "repeat(5)" not in boxes or "repeat(6)" not in boxes:
    errors.append("Box compact 5x6 layout contract changed")
if "take(30)" not in boxes:
    errors.append("Box page must remain limited to 30 entries")

if "prefetchBoxWindow" not in boxes:
    errors.append("Box prefetch contract missing")
if "PersistentApiCache" not in cache:
    notes.append("PersistentApiCache implementation uses a different class marker")
if "OfflineGamePackManager" not in offline:
    errors.append("offline pack manager declaration missing")

for name, source in [
    ("CollectionStore", collection),
    ("VariantCollectionStore", variants),
]:
    if "SharedPreferences" not in source and "getSharedPreferences" not in source:
        notes.append(f"{name}: persistence backend marker not detected")

if 'android:allowBackup="true"' not in manifest:
    errors.append("Android backup capability unexpectedly disabled")

supported_version = (
    ('versionCode = 19200' in gradle and 'versionName = "19.2.0"' in gradle) or
    ('versionCode = 20000' in gradle and 'versionName = "20.0.0"' in gradle)
)
if not supported_version:
    errors.append("supported version contract changed unexpectedly")

for path, content in [
    ("JourneyScreen.kt", journey),
    ("BoxesV2Screen.kt", boxes),
    ("PokemonDetailV2Screen.kt", detail),
]:
    if "TODO" in content or "FIXME" in content:
        errors.append(f"unfinished marker found in {path}")

# Hardening budget: prevent the largest screens from growing further before extraction.
budgets = {
    "JourneyScreen.kt": (journey, 1250),
    "BoxesV2Screen.kt": (boxes, 820),
    "PokemonDetailV2Screen.kt": (detail, 660),
}
for name, (content, limit) in budgets.items():
    lines = len(content.splitlines())
    if lines > limit:
        errors.append(f"{name} exceeded hardening size budget: {lines}>{limit}")

print("HARDENING_AUDIT_ERRORS=" + str(len(errors)))
for e in errors:
    print("ERROR:", e)
for n in notes:
    print("NOTE:", n)

sys.exit(1 if errors else 0)
