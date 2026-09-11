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


# v6.0 release guards
offline = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt").read_text(encoding="utf-8")
if "PACK_VERSION = 7" not in offline:
    violations.append("Offline pack version is not v7")

app = (root / "app/src/main/java/com/otaviobarreto/pokedex/PokedexApplication.kt").read_text(encoding="utf-8")
if ".crossfade(false)" not in app or "384L * 1024L * 1024L" not in app:
    violations.append("Image engine v2 tuning missing")

detail = (ui / "PokemonDetailV2Screen.kt").read_text(encoding="utf-8")
if "resolveSaveLocation" not in detail or "saveLocation.boxLabel" not in detail:
    violations.append("Pokemon detail save-location integration missing")

workflow = (root / ".github/workflows/android.yml").read_text(encoding="utf-8")
if 'versionName = "6.3.6"' not in workflow or "versionCode = 636" not in workflow:
    violations.append("CI v6.3.6 version stamping missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


companion = (ui / "CompanionHubScreen.kt").read_text(encoding="utf-8")
for required in ("Busca universal", "Guia de captura", "Backup e restauração", "Progresso por jogo"):
    if required not in companion:
        violations.append(f"Companion Hub missing {required}")

boxes = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
if "repeat(5)" not in boxes or "repeat(6)" not in boxes:
    violations.append("Fixed 30-Pokemon Box grid missing")
if "movePokemon(activeBox,target" in boxes or "Mover selecionados" in boxes:
    violations.append("Box movement controls must remain removed")

backup = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/BackupService.kt").read_text(encoding="utf-8")
if "pokedex-companion" not in backup:
    violations.append("Backup format guard missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


planner = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/CapturePlannerService.kt").read_text(encoding="utf-8")
if "obtainableMissing" not in planner or "externalMissing" not in planner:
    violations.append("Capture planner integration missing")

capture = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/CaptureGuideService.kt").read_text(encoding="utf-8")
for required in ("sampleLocations", "methods", "minLevel", "maxLevel"):
    if required not in capture:
        violations.append(f"Capture intelligence missing {required}")

companion = (ui / "CompanionHubScreen.kt").read_text(encoding="utf-8")
for required in ("Planejador de captura", "smartSearchTerm", "Você já tem este Pokémon"):
    if required not in companion:
        violations.append(f"Companion Intelligence missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


prefs = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/CompanionPreferences.kt").read_text(encoding="utf-8")
if "activeGame" not in prefs:
    violations.append("Persistent active game context missing")

collection = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/CollectionStore.kt").read_text(encoding="utf-8")
for required in ("unboxedCapturedIds", "moveMany", "sortBox"):
    if required not in collection:
        violations.append(f"Box 3.0 missing {required}")

living = (ui / "CollectionScreens.kt").read_text(encoding="utf-8")
if "DUPLICATES" not in living:
    violations.append("Living Dex duplicate intelligence missing")

backup = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/BackupService.kt").read_text(encoding="utf-8")
if '.put("version", 6)' not in backup or "activeGame" not in backup:
    violations.append("Backup v5 context missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


home = (ui / "HomeDashboardScreen.kt").read_text(encoding="utf-8")
for required in ("Próximo alvo", "Vistos recentemente", "Continuar de onde parei", "Jogo ativo"):
    if required not in home:
        violations.append(f"Smart Home missing {required}")

recent = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/RecentActivityStore.kt").read_text(encoding="utf-8")
if "recentPokemon" not in recent or "lastRoute" not in recent:
    violations.append("Recent activity persistence missing")

forms = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokemonFormsService.kt").read_text(encoding="utf-8")
if "PokemonFormVariant" not in forms or "varieties" not in forms:
    violations.append("Pokemon Forms service missing")

route = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/CaptureRouteService.kt").read_text(encoding="utf-8")
if "CaptureRouteGroup" not in route:
    violations.append("Capture route planner missing")

companion = (ui / "CompanionHubScreen.kt").read_text(encoding="utf-8")
for required in ("Salvar arquivo", "Abrir arquivo", "Rota por região", "Formas e variantes"):
    if required not in companion:
        violations.append(f"v6 Companion missing {required}")

boxes = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ("Pesquisar Pokémon", "Modifier.weight(1f).fillMaxHeight()", "Deslize para navegar entre as Boxes", "CircularProgressIndicator"):
    if required not in boxes:
        violations.append(f"Compact swipe Box UI missing {required}")

living = (ui / "CollectionScreens.kt").read_text(encoding="utf-8")
if "formas" not in living or "PokemonFormsService.cached" not in living:
    violations.append("Living Dex Forms integration missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


team_catalog = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/TeamCampaignCatalog.kt").read_text(encoding="utf-8")
for required in ("Let's Go Pikachu / Eevee", "Sword / Shield", "Brilliant Diamond / Shining Pearl", "Legends Arceus", "Scarlet / Violet", "Pokémon Legends: Z-A"):
    if required not in team_catalog:
        violations.append(f"Switch campaign guide missing {required}")

team_guide = (ui / "CampaignTeamGuideScreen.kt").read_text(encoding="utf-8")
for required in ("GUIA DE CAMPANHA", "Usar este time", "Build de campanha"):
    if required not in team_guide:
        violations.append(f"Campaign team guide missing {required}")
for required in ("Início", "Mid game", "Late game"):
    if required not in team_catalog:
        violations.append(f"Campaign phase missing {required}")

team_builder = (ui / "TeamBuilderScreen.kt").read_text(encoding="utf-8")
if "CampaignTeamGuideScreen" not in team_builder:
    violations.append("Campaign guide not integrated into My Team")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


catalog = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/AppGameCatalog.kt").read_text(encoding="utf-8")
for required in ("Pokémon Legends: Z-A", "FireRed / LeafGreen", "Pokémon Champions", "Scarlet / Violet", "Sword / Shield", "Let's Go Pikachu / Eevee", "Legends Arceus", "Brilliant Diamond / Shining Pearl"):
    if required not in catalog:
        violations.append(f"Switch catalog missing {required}")
for legacy in ("Black / White", "X / Y", "Omega Ruby / Alpha Sapphire"):
    if legacy in catalog:
        violations.append(f"Legacy non-Switch title still in primary catalog: {legacy}")

contexts = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/GameContext.kt").read_text(encoding="utf-8")
for slug in ("lumiose-city", "hyperspace", "kanto", "champions"):
    if slug not in contexts:
        violations.append(f"Switch GameContext missing {slug}")

games_hub = (ui / "GamesHubScreen.kt").read_text(encoding="utf-8")
if "AppGameCatalog.games.map" not in games_hub:
    violations.append("Games Hub is not derived from central catalog")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


cache = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PersistentApiCache.kt").read_text(encoding="utf-8")
for required in ("pinAll", "unpinAll", "promoteLocal", "pinnedUrls"):
    if required not in cache:
        violations.append(f"Instant cache missing {required}")

offline = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt").read_text(encoding="utf-8")
for required in ("manifest_ids", "resource_urls", "PersistentApiCache.pinAll", "sharedUrls", "sharedIds"):
    if required not in offline:
        violations.append(f"Offline v7 missing {required}")

store = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokedexDataStore.kt").read_text(encoding="utf-8")
if "promoteLocal(PokeApiService.pokemonUrl" not in store or "promoteLocal(PokeApiService.speciesUrl" not in store:
    violations.append("Core detail local-first hydration missing")

games = (ui / "GamesHubScreen.kt").read_text(encoding="utf-8")
if "OfflineGamePackManager.manifestIds" not in games:
    violations.append("Games progress does not use persistent manifest")

boxes = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
if "entries.take(12)" not in boxes or "entries.drop(12)" not in boxes:
    violations.append("Box smart preload priority missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


boxes = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ("rememberSaveable", "gameLabel", "regionSource", "page by rememberSaveable"):
    if required not in boxes:
        violations.append(f"Box return-state persistence missing {required}")
if "loading=true;page=0" in boxes or "loading=true; page=0" in boxes:
    violations.append("Box page reset regression detected in load effect")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


boxes = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
if "migrateCapturedToBox" in boxes or "migrateLegacyGameBox" in boxes:
    violations.append("Box screen must not mutate fixed game ordering")
if "sortBox(" in boxes or "moveMany(" in boxes:
    violations.append("Box screen must stay search/browse focused")
if "padding(horizontal=6.dp)" not in boxes or "height(40.dp)" not in boxes:
    violations.append("Compact Box chrome regression")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


boxes = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ("detectHorizontalDragGestures", "Deslize para navegar entre as Boxes", "CircularProgressIndicator", "ContentScale.Fit"):
    if required not in boxes:
        violations.append(f"Box swipe/header polish missing {required}")
for forbidden in ("ChevronLeft", "ChevronRight", 'Text("BOX"'):
    if forbidden in boxes:
        violations.append(f"Box duplicate/arrow chrome regression: {forbidden}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


boxes = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ("Modifier.size(48.dp)", "Spacer(Modifier.height(1.dp))", "lineHeight=10.sp", "lineHeight=8.sp"):
    if required not in boxes:
        violations.append(f"Box progress ring polish missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


boxes = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ("combinedClickable", "Capturar Pokémon?", "Remover captura?", "Todas as Boxes", "QBAllBoxes", "GridView"):
    if required not in boxes:
        violations.append(f"Box capture/overview missing {required}")

collection = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/CollectionStore.kt").read_text(encoding="utf-8")
for required in ("fun setCaptured", "affected = boxes.filterValues", "toggleCaptured(id: Int) = setCaptured"):
    if required not in collection:
        violations.append(f"Persistent capture state missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


detail = (ui / "PokemonDetailV2Screen.kt").read_text(encoding="utf-8")
for required in ("resolveSaveLocation", 'boxLabel="Box "+(index/30+1)', "saveLocation.boxLabel"):
    if required not in detail:
        violations.append(f"Detail save-location resolver missing {required}")
if 'SectionCard("Coleção"' in detail:
    violations.append("Collection card must stay hidden from Info tab")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)
