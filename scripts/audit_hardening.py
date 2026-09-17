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
pokedex_ui = read("app/src/main/java/com/otaviobarreto/pokedex/ui/PokedexCatalogScreen.kt")
collection_ui = read("app/src/main/java/com/otaviobarreto/pokedex/ui/CollectionScreen.kt")
journey_hub = read("app/src/main/java/com/otaviobarreto/pokedex/ui/JourneyHubComponents.kt")
form_detail = read("app/src/main/java/com/otaviobarreto/pokedex/ui/PokemonFormDetailScreen.kt")
campaign_guide = read("app/src/main/java/com/otaviobarreto/pokedex/ui/CampaignTeamGuideScreen.kt")
companion_home = read("app/src/main/java/com/otaviobarreto/pokedex/ui/CompanionHomeScreen.kt")
artwork_sync = read("app/src/main/java/com/otaviobarreto/pokedex/data/ArtworkOfflineSync.kt")
bootstrap = read("app/src/main/java/com/otaviobarreto/pokedex/data/ContentBootstrapManager.kt")
boot = read("app/src/main/java/com/otaviobarreto/pokedex/ui/BootExperienceScreen.kt")
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
for route_marker in [
    'const val SEARCH = "search"',
    'const val EVOLUTION_CENTER = "evolutionCenter"',
    'const val GAME_DEX = "gameDex"',
]:
    if route_marker not in routes:
        errors.append(f"stable secondary navigation missing: {route_marker}")
if "rememberSaveable{mutableStateOf(false)}" not in main:
    errors.append("boot completion must survive Activity recreation")
if "if (isFinishing) HomeAudioManager.release()" not in main:
    errors.append("audio must not be torn down during configuration recreation")

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
    ('versionCode = 20000' in gradle and 'versionName = "20.0.0"' in gradle) or
    ('versionCode = 20300' in gradle and 'versionName = "20.3.0"' in gradle) or
    ('versionCode = 20400' in gradle and 'versionName = "20.4.0"' in gradle) or
    ('versionCode = 20500' in gradle and 'versionName = "20.5.0"' in gradle) or
    ('versionCode = 20600' in gradle and 'versionName = "20.6.0"' in gradle) or
    ('versionCode = 20700' in gradle and 'versionName = "20.7.0"' in gradle) or
    ('versionCode = 20701' in gradle and 'versionName = "20.7.1"' in gradle) or
    ('versionCode = 20800' in gradle and 'versionName = "20.8.0"' in gradle) or
    ('versionCode = 20900' in gradle and 'versionName = "20.9.0"' in gradle) or
    ('versionCode = 21000' in gradle and 'versionName = "20.10.0"' in gradle) or
    ('versionCode = 21100' in gradle and 'versionName = "20.11.0"' in gradle) or
    ('versionCode = 21200' in gradle and 'versionName = "20.12.0"' in gradle) or
    ('versionCode = 21201' in gradle and 'versionName = "20.12.1"' in gradle) or
    ('versionCode = 21300' in gradle and 'versionName = "20.13.0"' in gradle)
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

# Artwork offline contract: every core visual surface must resolve artwork
# through PokemonArtwork or the durable offline model, never a raw remote AsyncImage.
remote_async = re.compile(r'AsyncImage\s*\(\s*(?:model\s*=\s*)?["\']https?://', re.S)
for name, source in [
    ("PokedexCatalogScreen.kt", pokedex_ui),
    ("CollectionScreen.kt", collection_ui),
    ("BoxesV2Screen.kt", boxes),
    ("JourneyScreen.kt", journey),
    ("JourneyHubComponents.kt", journey_hub),
    ("PokemonDetailV2Screen.kt", detail),
    ("PokemonFormDetailScreen.kt", form_detail),
    ("CampaignTeamGuideScreen.kt", campaign_guide),
    ("CompanionHomeScreen.kt", companion_home),
]:
    if remote_async.search(source):
        errors.append(f"{name} contains raw remote AsyncImage outside offline artwork resolver")

for marker in [
    "addAll(officialArtworkUrls())",
    "addAll(JourneyTypeIconCatalog.allUrls())",
    "addAll(generalManifestArtworkUrls(context))",
    "VariantCollectionStore.ownedVariants.mapTo(this){it.artworkUrl}",
    "GameCoverCatalog.coversFor(game.label)",
]:
    if marker not in artwork_sync:
        errors.append(f"startup artwork inventory missing: {marker}")

if "ArtworkOfflineSync.sync(context)" not in boot:
    errors.append("boot must visibly audit/sync artwork before releasing Home")
if '"$RAW_ART/shiny/$id.png"' not in artwork_sync:
    errors.append("startup artwork inventory must include official Shiny artwork")
if "installSupplementalArtwork" not in read("app/src/main/java/com/otaviobarreto/pokedex/data/OfflineLibraryManager.kt"):
    errors.append("durable supplemental artwork storage missing")

for marker in [
    "auditCachedLibrary(context,cachedSignature)",
    "OfflineLibraryManager.auditGeneralDetailed(context,generalVersion)",
    "supportedGameKeys",
]:
    if marker not in bootstrap:
        errors.append(f"stable bootstrap guard missing: {marker}")

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
