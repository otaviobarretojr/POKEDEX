from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
main = (root / "app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt").read_text()
routes = (root / "app/src/main/java/com/otaviobarreto/pokedex/PokedexRoutes.kt").read_text()
home = root / "app/src/main/java/com/otaviobarreto/pokedex/ui/PokedexHomeScreen.kt"
collection = root / "app/src/main/java/com/otaviobarreto/pokedex/data/CollectionStore.kt"
backup = root / "app/src/main/java/com/otaviobarreto/pokedex/data/AppBackupManager.kt"
boxes = root / "app/src/main/java/com/otaviobarreto/pokedex/ui/BoxesV2Screen.kt"
detail = root / "app/src/main/java/com/otaviobarreto/pokedex/ui/PokemonDetailV2Screen.kt"

errors = []
required_main = [
    "PokedexHomeScreen", "PokedexCatalogScreen", "CollectionScreen",
    "BoxesV2Screen", "PokemonDetailV2Screen", "GameDexScreen",
    "UniversalSearchScreen", "EvolutionCenterScreen"
]
for token in required_main:
    if token not in main:
        errors.append("missing Pokedex route: " + token)
for path in (home, collection, backup, boxes, detail):
    if not path.exists():
        errors.append("missing core file: " + path.name)
for forbidden in ("PokedexRoutes.GAMES", "PokedexRoutes.JOURNEY_CONTINUE", "PokedexRoutes.CAMPAIGN_GUIDE"):
    if forbidden in main:
        errors.append("retired Journey route still wired: " + forbidden)
if 'const val GAMES = "games"' in routes or 'const val JOURNEY_CONTINUE' in routes or 'const val CAMPAIGN_GUIDE' in routes:
    errors.append("retired Journey route still declared")
if "com.otaviobarreto.pokedex" not in (root / "app/build.gradle.kts").read_text():
    errors.append("application identity guard missing")
if errors:
    print("Pokedex-only verification failed:")
    for item in errors:
        print(" -", item)
    sys.exit(1)
print("POKEDEX_ONLY_ARCHITECTURE=OK")
