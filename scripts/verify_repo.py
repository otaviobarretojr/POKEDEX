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
if "resolveSaveLocation" not in detail or "saveLocation.saved" not in detail:
    violations.append("Pokemon detail save-location integration missing")

workflow = (root / ".github/workflows/android.yml").read_text(encoding="utf-8")
if 'versionName = "6.11.0"' not in workflow or "versionCode = 6110" not in workflow:
    violations.append("CI v6.11.0 version stamping missing")

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
for required in ("resolveSaveLocation", 'boxLabel="Box "+(index/30+1)', "saveLocation.saved"):
    if required not in detail:
        violations.append(f"Detail save-location resolver missing {required}")
if 'SectionCard("Coleção"' in detail:
    violations.append("Collection card must stay hidden from Info tab")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


artwork = (ui / "PokemonArtwork.kt").read_text(encoding="utf-8")
for required in ("TransparentBoundsCropTransformation", "Color.alpha", "paddingRatio", "ContentScale.Fit"):
    if required not in artwork:
        violations.append(f"Artwork alignment missing {required}")

detail = (ui / "PokemonDetailV2Screen.kt").read_text(encoding="utf-8")
if detail.count("PokemonArtwork(") < 3:
    violations.append("Pokemon detail artwork normalization not applied consistently")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


tuning = (ui / "ArtworkTuningCatalog.kt").read_text(encoding="utf-8")
if tuning.count("to ArtworkTuning(") != 109:
    violations.append("Artwork curation override count must stay at 109 reviewed artworks")
artwork = (ui / "PokemonArtwork.kt").read_text(encoding="utf-8")
for required in ("ArtworkTuningCatalog.forPokemon", "graphicsLayer", "offset(x = dx, y = dy)", "pokemonId: Int? = null"):
    if required not in artwork:
        violations.append(f"Curated artwork renderer missing {required}")
detail = (ui / "PokemonDetailV2Screen.kt").read_text(encoding="utf-8")
if detail.count("pokemonId=") < 3:
    violations.append("Detail artwork calls are not wired to per-Pokemon curation")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# v6.5 Journey guards
journey = (ui / "JourneyScreen.kt").read_text(encoding="utf-8")
journey_catalog = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyCatalog.kt").read_text(encoding="utf-8")
journey_progress = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyProgressStore.kt").read_text(encoding="utf-8")
main = (root / "app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt").read_text(encoding="utf-8")
for required in ("JORNADA", "Melhor rota", "Time ideal", "Boxes do jogo"):
    if required not in journey:
        violations.append(f"Journey hub missing {required}")
for required in ("Katy", "Klawf", "Giacomo", "Eri", "sv-18"):
    if required not in journey_catalog:
        violations.append(f"Scarlet/Violet Journey route missing {required}")
for required in ("completed", "toggle", "clear"):
    if required not in journey_progress:
        violations.append(f"Journey progress persistence missing {required}")
if 'MainDestination("home","Jornada"' not in main or "JourneyProgressStore.initialize" not in main:
    violations.append("Journey is not wired as the primary tab")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# v6.5.1 Journey route visual guards
journey_visual = (ui / "JourneyScreen.kt").read_text(encoding="utf-8")
for required in ("Progresso da campanha", "PRÓXIMO PASSO INTELIGENTE", "JourneyStepCard", "JourneyCountPill", "JourneyInfoChip", "background(", "Próximo recomendado"):
    if required not in journey_visual:
        violations.append(f"Journey route visual missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# v6.5.2 Journey objective detail guards
objective_catalog = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyObjectiveDetailsCatalog.kt").read_text(encoding="utf-8")
journey_detail = (ui / "JourneyScreen.kt").read_text(encoding="utf-8")
for required in ("JourneyObjectiveDetail", "JourneyBossMember", '"sv-01"', '"sv-18"', "Mismagius", "Caph Starmobile"):
    if required not in objective_catalog:
        violations.append(f"Journey objective detail catalog missing {required}")
for required in ("JourneyObjectiveDetailScreen", "Equipe / adversários", "Fraquezas e resposta", "O que você ganha", "Ver time ideal para esta fase"):
    if required not in journey_detail:
        violations.append(f"Journey objective detail UI missing {required}")
if "onOpen:()->Unit" not in journey_detail or "IconButton(onClick=onToggle" not in journey_detail:
    violations.append("Journey card open/complete interaction split missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# v6.5.3 Smart Journey progress guards
smart = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneySmartProgress.kt").read_text(encoding="utf-8")
for required in ("JourneySmartContext", "CampaignPhase.EARLY", "CampaignPhase.MID", "CampaignPhase.LATE", "Próximo alvo"):
    if required not in smart:
        violations.append(f"Smart Journey progress missing {required}")
journey_progress = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyProgressStore.kt").read_text(encoding="utf-8")
for required in ("setCompleted", "completeThrough"):
    if required not in journey_progress:
        violations.append(f"Smart Journey progress operation missing {required}")
journey_ui = (ui / "JourneyScreen.kt").read_text(encoding="utf-8")
for required in ("FASE AUTOMÁTICA", "Concluir progresso até aqui", "JourneySmartProgress.context"):
    if required not in journey_ui:
        violations.append(f"Smart Journey UI missing {required}")
team_guide = (ui / "CampaignTeamGuideScreen.kt").read_text(encoding="utf-8")
if "initialPhase" not in team_guide or '"Automático"' not in team_guide:
    violations.append("Team guide does not accept automatic Journey phase")
main = (root / "app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt").read_text(encoding="utf-8")
if "phase={phase}" not in main:
    violations.append("Smart Journey phase navigation missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# v6.5.4-v6.7.0 Complete Journey guards
prep = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyPreparationCatalog.kt").read_text(encoding="utf-8")
map_catalog = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyMapCatalog.kt").read_text(encoding="utf-8")
dynamic_team = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyDynamicTeamCatalog.kt").read_text(encoding="utf-8")
journey = (ui / "JourneyScreen.kt").read_text(encoding="utf-8")
team_guide = (ui / "CampaignTeamGuideScreen.kt").read_text(encoding="utf-8")
journey_catalog = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyCatalog.kt").read_text(encoding="utf-8")

for required in ("PRÓXIMO PASSO INTELIGENTE", "Preparação recomendada", "Pokémon úteis agora", "Mapa da Jornada", "JourneyMapScreen"):
    if required not in journey:
        violations.append(f"Complete Journey UI missing {required}")
for required in ("sv-01", "sv-18", "recommendedLevel", "pokemonIds", "items"):
    if required not in prep:
        violations.append(f"Journey preparation catalog missing {required}")
for required in ("JourneyMapPoint", "sv-01", "sv-18"):
    if required not in map_catalog:
        violations.append(f"Journey map catalog missing {required}")
for required in ("adjustedSlots", "CollectionStore.capturedIds", "adjustSlots"):
    if required not in dynamic_team:
        violations.append(f"Dynamic Journey team missing {required}")
for required in ("TIME DINÂMICO DA JORNADA", "displaySlots", "JourneyDynamicTeamCatalog.suggestion"):
    if required not in team_guide:
        violations.append(f"Dynamic team guide UI missing {required}")
if "Campanha + pós-jogo + DLC · rota completa" not in journey_catalog:
    violations.append("Scarlet/Violet curated route annotation missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# v6.7.1 Journey visual assets guards
visual_catalog = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyVisualAssetCatalog.kt").read_text(encoding="utf-8")
journey_ui = (ui / "JourneyScreen.kt").read_text(encoding="utf-8")
for required in ("Katy","Brassius","Iono","Kofu","Larry","Ryme","Tulip","Grusha","Giacomo","Mela","Atticus","Ortega","Eri","Klawf","Bombirdier","Orthworm","Great Tusk / Iron Treads","Dondozo & Tatsugiri"):
    if required not in visual_catalog:
        violations.append(f"Journey visual asset missing {required}")
for required in ("JourneyVisualThumb","JourneyVisualHero","JourneyVisualAssetCatalog.forStep"):
    if required not in journey_ui:
        violations.append(f"Journey visual rendering missing {required}")
if visual_catalog.count("JourneyVisualAsset(") < 19:
    violations.append("Journey visual catalog does not cover all 18 Scarlet/Violet objectives")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# v6.8.0 complete Scarlet/Violet post-game guards
for required in ("sv-pg-01","sv-pg-02","sv-pg-03","sv-pg-04","sv-pg-05","sv-pg-06","sv-pg-07","sv-pg-08","POSTGAME"):
    if required not in journey_catalog:
        violations.append(f"Post-game Journey catalog missing {required}")
for required in ("Geeta","Arven","Penny","Koraidon / Miraidon","Academy Ace Tournament","Black Crystal Tera Raid","Treasures of Ruin"):
    if required not in visual_catalog:
        violations.append(f"Post-game visual coverage missing {required}")
for required in ("sv-pg-01","sv-pg-04","sv-pg-07","sv-pg-08"):
    if required not in objective_catalog:
        violations.append(f"Post-game objective detail missing {required}")
    if required not in prep:
        violations.append(f"Post-game preparation missing {required}")
if visual_catalog.count("JourneyVisualAsset(") < 27:
    violations.append("Journey visual catalog does not cover campaign + 8 post-game objectives")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# v6.9.0 Hidden Treasure + Mochi Mayhem guards
for required in ("sv-dlc-01","sv-dlc-07","sv-dlc-08","sv-dlc-15","sv-dlc-16","sv-epi-01","sv-epi-04","DLC","EPILOGUE"):
    if required not in journey_catalog:
        violations.append(f"DLC Journey catalog missing {required}")
for required in ("Carmine","Kieran","Crispin","Amarys","Lacey","Drayton","Terapagos","Pecharunt","230228_02.png","230622_02.png","230808_04.png"):
    if required not in visual_catalog:
        violations.append(f"DLC visual coverage missing {required}")
for required in ("sv-dlc-05","sv-dlc-06","sv-dlc-13","sv-dlc-15","sv-epi-04"):
    if required not in objective_catalog:
        violations.append(f"DLC objective detail missing {required}")
    if required not in prep:
        violations.append(f"DLC preparation missing {required}")
for required in ("THE TEAL MASK","THE INDIGO DISK","MOCHI MAYHEM"):
    if required not in journey:
        violations.append(f"Journey section header missing {required}")
if "The Teal Mask" not in smart or "The Indigo Disk" not in smart or "Mochi Mayhem" not in smart:
    violations.append("Smart progress is not DLC-aware")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# v6.9.1 Journey visual audit guards
journey_visual_catalog=(root/"app/src/main/java/com/otaviobarreto/pokedex/data/JourneyVisualAssetCatalog.kt").read_text(encoding="utf-8")
journey_map=(root/"app/src/main/java/com/otaviobarreto/pokedex/data/JourneyMapCatalog.kt").read_text(encoding="utf-8")
journey_ui=(ui/"JourneyScreen.kt").read_text(encoding="utf-8")
for required in ("230112_01/img_01.jpg","230112_06/img_01.jpg","220907_03/ja/img_01.jpg","230112_07/img_01.jpg"):
    if required not in journey_visual_catalog:
        violations.append(f"Validated official Journey artwork missing {required}")
if "story_img_01.jpg" not in journey_map or "backgroundUrl" not in journey_map:
    violations.append("Official Paldea map background missing")
if "contentDescription=\"Mapa de Paldea\"" not in journey_ui:
    violations.append("Official Paldea map is not rendered in Journey")
type_icons=(root/"app/src/main/java/com/otaviobarreto/pokedex/data/JourneyTypeIconCatalog.kt").read_text(encoding="utf-8")
if "generation-ix/scarlet-violet" not in type_icons or "JourneyTypeIconCatalog" not in journey_ui:
    violations.append("Scarlet/Violet type icons are not wired into Journey")
if not (root/"scripts/audit_journey_visuals.py").exists():
    violations.append("Journey visual URL audit script missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# v6.10.0 immersive Paldea map guards
journey_ui_v610=(ui/"JourneyScreen.kt").read_text(encoding="utf-8")
for required in ("detectTransformGestures","ContentScale.FillBounds","JourneyMapControl","PRÓXIMO OBJETIVO","Ver objetivo","Icons.Default.MyLocation"):
    if required not in journey_ui_v610:
        violations.append(f"Immersive Journey map missing {required}")
for obsolete in ("● Concluído   ● Próximo   ○ Pendente","Mapa oficial de Paldea com os 18 objetivos principais posicionados por região. Toque em um ponto para abrir o objetivo."):
    if obsolete in journey_ui_v610:
        violations.append(f"Legacy map UI still present: {obsolete}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# v6.11.0 navigation consolidation guards
main_nav=(root/"app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt").read_text(encoding="utf-8")
journey_v611=(ui/"JourneyScreen.kt").read_text(encoding="utf-8")
boxes_v611=(ui/"BoxesV2Screen.kt").read_text(encoding="utf-8")
prefs_v611=(root/"app/src/main/java/com/otaviobarreto/pokedex/data/CompanionPreferences.kt").read_text(encoding="utf-8")
main_line=next((line for line in main_nav.splitlines() if line.startswith("private val mainDestinations=")),"")
for forbidden in ('"Pokédex"','"Living Dex"','"Companion"'):
    if forbidden in main_line:
        violations.append(f"Legacy primary tab still present: {forbidden}")
for required in ('MainDestination("home","Jornada"','MainDestination("boxes","Boxes"'):
    if required not in main_line:
        violations.append(f"Primary navigation missing {required}")
for required in ("Boxes do jogo","rememberJourneyCollectionProgress","CollectionStore.capturedIds","onOpenBoxes"):
    if required not in journey_v611:
        violations.append(f"Journey/Boxes consolidation missing {required}")
for required in ("CompanionPreferences.activeGame","CompanionPreferences.activeRegionSource"):
    if required not in boxes_v611:
        violations.append(f"Boxes context handoff missing {required}")
if "KEY_ACTIVE_REGION" not in prefs_v611:
    violations.append("Persistent active Box region missing")
# Legacy screens intentionally remain compiled as internal compatibility routes during migration.
