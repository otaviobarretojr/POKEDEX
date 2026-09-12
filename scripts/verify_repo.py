#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
ui = root / "app/src/main/java/com/otaviobarreto/pokedex/ui"
violations = []

journey_screen_path = ui / "JourneyScreen.kt"
journey_hub_path = ui / "JourneyHubComponents.kt"
journey_source = (
    journey_screen_path.read_text(encoding="utf-8")
    + "\n"
    + (journey_hub_path.read_text(encoding="utf-8") if journey_hub_path.exists() else "")
)
application_source = (root / "app/src/main/java/com/otaviobarreto/pokedex/PokedexApplication.kt").read_text(encoding="utf-8")

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

detail = (ui / "PokemonDetailV2Screen.kt").read_text(encoding="utf-8")
if "Render as soon as the two core payloads are ready" not in detail:
    violations.append("progressive detail loading guard missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)

print("Source verification passed.")


# Current stable release guards
offline = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt").read_text(encoding="utf-8")
if "PACK_VERSION = 17" not in offline:
    violations.append("Offline pack version is not v16")

app = (root / "app/src/main/java/com/otaviobarreto/pokedex/PokedexApplication.kt").read_text(encoding="utf-8")
if ".crossfade(false)" not in app or "384L * 1024L * 1024L" not in app:
    violations.append("Image engine v2 tuning missing")

detail = (ui / "PokemonDetailV2Screen.kt").read_text(encoding="utf-8")
if "resolveSaveLocation" not in detail or "saveLocation.saved" not in detail:
    violations.append("Pokemon detail save-location integration missing")

workflow = (root / ".github/workflows/android.yml").read_text(encoding="utf-8")
if "18200" not in workflow or "18.2.0" not in workflow:
    violations.append("CI v18.2.0 version validation missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


boxes = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
if "repeat(5)" not in boxes or "repeat(6)" not in boxes:
    violations.append("Fixed 30-Pokemon Box grid missing")
if "movePokemon(activeBox,target" in boxes or "Mover selecionados" in boxes:
    violations.append("Box movement controls must remain removed")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


prefs = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/AppStatePreferences.kt").read_text(encoding="utf-8")
if "activeGame" not in prefs:
    violations.append("Persistent active game context missing")

collection = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/CollectionStore.kt").read_text(encoding="utf-8")
for required in ("unboxedCapturedIds", "moveMany", "sortBox"):
    if required not in collection:
        violations.append(f"Box 3.0 missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


recent = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/RecentActivityStore.kt").read_text(encoding="utf-8")
if "recentPokemon" not in recent or "lastRoute" not in recent:
    violations.append("Recent activity persistence missing")

forms = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokemonFormsService.kt").read_text(encoding="utf-8")
if "PokemonFormVariant" not in forms or "varieties" not in forms:
    violations.append("Pokemon Forms service missing")

boxes = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ("Pesquisar Pokémon", "Modifier.weight(1f).fillMaxHeight()", "Deslize para navegar entre as Boxes", "CircularProgressIndicator"):
    if required not in boxes:
        violations.append(f"Compact swipe Box UI missing {required}")

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

boxes = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
if "prefetchBoxWindow(dex,current)" not in boxes or "prefetchBoxWindow" not in store:
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
for required in ("combinedClickable", "Todas as Boxes", "QBAllBoxes", "GridView"):
    if required not in boxes:
        violations.append(f"Box capture/overview missing {required}")
if "Capturar Pokémon?" not in boxes and "QBVariantManager" not in boxes:
    violations.append("Box capture/variant manager missing")
if "Remover captura?" not in boxes and "★ Shiny" not in boxes:
    violations.append("Box removal/variant controls missing")

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


# Compatibility guard — Journey
journey = journey_source
journey_catalog = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyCatalog.kt").read_text(encoding="utf-8")
journey_progress = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyProgressStore.kt").read_text(encoding="utf-8")
main = (root / "app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt").read_text(encoding="utf-8")
for required in ("Jornada", "Melhor rota", "Time ideal", "Boxes do jogo"):
    if required not in journey:
        violations.append(f"Journey hub missing {required}")
for required in ("Katy", "Klawf", "Giacomo", "Eri", "sv-18"):
    if required not in journey_catalog:
        violations.append(f"Scarlet/Violet Journey route missing {required}")
for required in ("completed", "toggle", "clear"):
    if required not in journey_progress:
        violations.append(f"Journey progress persistence missing {required}")
if 'MainDestination("home","Jornada"' not in main or "JourneyProgressStore.initialize" not in application_source:
    violations.append("Journey is not wired as the primary tab")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — Journey route visual
journey_visual = journey_source
for required in ("Progresso da campanha", "PRÓXIMO PASSO INTELIGENTE", "JourneyStepCard", "JourneyCountPill", "JourneyInfoChip", "background(", "Próximo recomendado"):
    if required not in journey_visual:
        violations.append(f"Journey route visual missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — Journey objective detail
objective_catalog = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyObjectiveDetailsCatalog.kt").read_text(encoding="utf-8")
journey_detail = journey_source
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


# Compatibility guard — Smart Journey progress
smart = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneySmartProgress.kt").read_text(encoding="utf-8")
for required in ("JourneySmartContext", "CampaignPhase.EARLY", "CampaignPhase.MID", "CampaignPhase.LATE", "Próximo alvo"):
    if required not in smart:
        violations.append(f"Smart Journey progress missing {required}")
journey_progress = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyProgressStore.kt").read_text(encoding="utf-8")
for required in ("setCompleted", "completeThrough"):
    if required not in journey_progress:
        violations.append(f"Smart Journey progress operation missing {required}")
journey_ui = journey_source
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
journey = journey_source
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


# Compatibility guard — Journey visual assets
visual_catalog = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyVisualAssetCatalog.kt").read_text(encoding="utf-8")
journey_ui = journey_source
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


# Compatibility guard — complete Scarlet/Violet post-game
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


# Compatibility guard — Hidden Treasure + Mochi Mayhem
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


# Compatibility guard — Journey visual audit
journey_visual_catalog=(root/"app/src/main/java/com/otaviobarreto/pokedex/data/JourneyVisualAssetCatalog.kt").read_text(encoding="utf-8")
journey_map=(root/"app/src/main/java/com/otaviobarreto/pokedex/data/JourneyMapCatalog.kt").read_text(encoding="utf-8")
journey_ui=journey_source
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


# Compatibility guard — immersive Paldea map
journey_ui_v610=journey_source
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


# Compatibility guard — navigation consolidation
main_nav=(root/"app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt").read_text(encoding="utf-8")
journey_v611=journey_source
boxes_v611=(ui/"BoxesV2Screen.kt").read_text(encoding="utf-8")
prefs_v611=(root/"app/src/main/java/com/otaviobarreto/pokedex/data/AppStatePreferences.kt").read_text(encoding="utf-8")
for forbidden in ('"Living Dex"','"Companion"'):
    if forbidden in main_nav:
        violations.append(f"Legacy primary tab still present: {forbidden}")
for required in (
    'MainDestination("home","Jornada"',
    'MainDestination("pokedex","Pokédex"',
    'MainDestination("central","Config."',
    'MainDestination("boxes","Boxes"'
):
    if required not in main_nav:
        violations.append(f"Primary navigation missing {required}")
for required in ("Boxes do jogo","rememberJourneyCollectionProgress","CollectionStore.contextualCapturedIds","onOpenBoxes"):
    if required not in journey_v611:
        violations.append(f"Journey/Boxes consolidation missing {required}")
for required in ("AppStatePreferences.activeGame","AppStatePreferences.setActiveRegionForGame"):
    if required not in boxes_v611:
        violations.append(f"Boxes context handoff missing {required}")
if "KEY_ACTIVE_REGION" not in prefs_v611:
    violations.append("Persistent active Box region missing")


# Compatibility guard — Android back navigation
journey_back=journey_source
for required in ("BackHandler(enabled=view!=JourneyView.GAMES)","detailReturnView","JourneyView.MAP","JourneyView.ROUTE","selectedGame=null"):
    if required not in journey_back:
        violations.append(f"Android back hierarchy missing {required}")
if "onBack={selectedStepId=null;view=detailReturnView}" not in journey_back:
    violations.append("Journey toolbar back does not match Android back origin")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — navigation/state hardening
prefs_v612 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/AppStatePreferences.kt").read_text(encoding="utf-8")
for required in ("activeRegionForGame", "setActiveRegionForGame", "boxPage", "setBoxPage", "KEY_BOX_PAGE_PREFIX"):
    if required not in prefs_v612:
        violations.append(f"Persistent navigation context missing {required}")

boxes_v612 = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ("activeRegionForGame(game.label)", "AppStatePreferences.boxPage(regionSource)", "setBoxPage(region.source,current)", "prefetchBoxWindow(dex,current)"):
    if required not in boxes_v612:
        violations.append(f"Box state/performance hardening missing {required}")

journey_v612 = journey_source
for required in ("rememberSaveable", "journeySourceForStep", "onPokemonClick:(Int,String?)->Unit", "activeRegionForGame(game.label)"):
    if required not in journey_v612:
        violations.append(f"Journey state/context hardening missing {required}")

main_v612 = (root / "app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt").read_text(encoding="utf-8")
for required in ("resolvedSource=source ?: AppStatePreferences.activeRegionForGame(resolvedGame)", "onPokemonClick={id,source->openPokemon(id,source)}"):
    if required not in main_v612:
        violations.append(f"Cross-route context preservation missing {required}")

detail_v612 = (ui / "PokemonDetailV2Screen.kt").read_text(encoding="utf-8")
for required in ("resolveSaveLocation", "CollectionStore.contextualCapturedIds"):
    if required not in detail_v612:
        violations.append(f"Detail contextual capture resolver missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — contextual collection + navigation continuity
collection_v613 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/CollectionStore.kt").read_text(encoding="utf-8")
for required in ("contextualCapturedIds", "capturedIn", "isCapturedIn", "toggleCapturedIn", "setCapturedIn", "migrateLegacyCapturedToSource", "contextualCaptured"):
    if required not in collection_v613:
        violations.append(f"Contextual collection missing {required}")

boxes_v613 = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ("CollectionStore.contextualCapturedIds[region.source]", "region.source", "QBVariantManager"):
    if required not in boxes_v613:
        violations.append(f"Regional Box capture isolation missing {required}")
if "CollectionStore.isCapturedIn(region.source" not in boxes_v613 and "VariantCollectionStore" not in boxes_v613:
    violations.append("Regional Box capture lookup missing")
if "CollectionStore.toggleCapturedIn(region.source" not in boxes_v613 and "VariantCollectionStore.toggle" not in boxes_v613:
    violations.append("Regional Box capture mutation missing")
if "val capturedIds=CollectionStore.capturedIds" in boxes_v613:
    violations.append("Boxes must not use global capturedIds as regional progress")

journey_v613 = journey_source
for required in ("CollectionStore.contextualCapturedIds", "LazyListState", "state=listState", "mapZoom", "mapPanX", "onSelectedStepChange"):
    if required not in journey_v613:
        violations.append(f"Journey continuity/context missing {required}")

main_v613 = (root / "app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt").read_text(encoding="utf-8") + "\n" + application_source
for required in ("migrateLegacyCapturedToSource", "source={source}", "openReference(kind,name,source)", "onPokemonClick={id,source->openPokemon(id,source)}"):
    if required not in main_v613:
        violations.append(f"Cross-screen source propagation missing {required}")

campaign_v613 = (ui / "CampaignTeamGuideScreen.kt").read_text(encoding="utf-8")
if "onPokemonClick:(Int,String?)->Unit" not in campaign_v613 or "onPokemonClick(slot.pokemonId,source)" not in campaign_v613:
    violations.append("Campaign guide loses game source when opening Pokémon")

reference_v613 = (ui / "ReferenceHubScreen.kt").read_text(encoding="utf-8")
if "source:String?=null" not in reference_v613 or "onPokemonClick(id,source)" not in reference_v613:
    violations.append("Reference hub loses game source when opening Pokémon")

detail_v613 = (ui / "PokemonDetailV2Screen.kt").read_text(encoding="utf-8")
for required in ("rememberSaveable(id)", "CollectionStore.isCapturedIn(source,pokemonId)"):
    if required not in detail_v613:
        violations.append(f"Detail contextual state missing {required}")
if "GameDexService.loadGameDex(context)" in detail_v613:
    violations.append("Pokémon detail must not force-load GameDex only for capture indicator")

prefs_v613 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/AppStatePreferences.kt").read_text(encoding="utf-8")
for required in ("exportSnapshot", "importSnapshot", "boxPages", "regions"):
    if required not in prefs_v613:
        violations.append(f"Navigation backup missing {required}")

cache_v613 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PersistentApiCache.kt").read_text(encoding="utf-8")
promote_body = cache_v613.split("fun promoteLocal",1)[1].split("fun clear",1)[0] if "fun promoteLocal" in cache_v613 else ""
if "setLastModified" in promote_body:
    violations.append("promoteLocal must not refresh TTL timestamps")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — data integrity + navigation hardening
integrity_rules = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/DataIntegrityRules.kt").read_text(encoding="utf-8")
for required in ("capturedForScope", "shouldEnsureOwned", "completedCount"):
    if required not in integrity_rules:
        violations.append(f"Data integrity rules missing {required}")

collection_v614 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/CollectionStore.kt").read_text(encoding="utf-8")
if "DataIntegrityRules.shouldEnsureOwned" not in collection_v614:
    violations.append("Collection ownership/Box integrity rule missing")

journey_progress_v614 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyProgressStore.kt").read_text(encoding="utf-8")
for required in ("exportSnapshot", "importSnapshot", "JourneyCatalog.steps"):
    if required not in journey_progress_v614:
        violations.append(f"Journey backup support missing {required}")

journey_v614 = journey_source
for required in ("LaunchedEffect(explicitGameSelectionRevision)", "routeListState.scrollToItem(0)", "rememberUpdatedState(zoom)", "rememberUpdatedState(pan)", "DataIntegrityRules.completedCount"):
    if required not in journey_v614:
        violations.append(f"Journey state isolation/gesture hardening missing {required}")

test_v614 = root / "app/src/test/java/com/otaviobarreto/pokedex/data/DataIntegrityRulesTest.kt"
if not test_v614.exists():
    violations.append("Behavioral data integrity regression tests missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility milestone — current-core hardening guards (Journey + Boxes/offline only)
journey_v615 = journey_source
for required in ("explicitGameSelectionRevision", "LaunchedEffect(explicitGameSelectionRevision)", "if(explicitGameSelectionRevision==0) return@LaunchedEffect"):
    if required not in journey_v615:
        violations.append(f"Journey Android recreation preservation missing {required}")
if "LaunchedEffect(selectedGame){" in journey_v615:
    violations.append("Journey must not reset saved navigation state merely because selectedGame was restored")

offline_v615 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt").read_text(encoding="utf-8")
for required in ("PACK_VERSION = 17", "cachedImages", "expectedImages", "hasOfflineArtwork", 'openSnapshot("pokemon-offline-$id")'):
    if required not in offline_v615:
        violations.append(f"Offline artwork integrity audit missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)





# Compatibility guard — dead component cleanup
dead_ui = (
    "UnifiedRegionExplorerScreen.kt",
    "RegionExplorerV2Screen.kt",
    "RegionExplorerV3Screen.kt",
    "RegionExplorerV4Screen.kt",
    "PokemonCollectionActions.kt",
)
for dead_file in dead_ui:
    if (ui / dead_file).exists():
        violations.append(f"Dead UI still compiled: {dead_file}")

dead_data = (
    "OfflineGameDownloadService.kt",
    "CommunityEncounterIndex.kt",
    "RegionMapVisualCatalog.kt",
)
data_dir = root / "app/src/main/java/com/otaviobarreto/pokedex/data"
for dead_file in dead_data:
    if (data_dir / dead_file).exists():
        violations.append(f"Dead data component still compiled: {dead_file}")

manifest_v620 = (root / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
for forbidden in (
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
    "OfflineGameDownloadService",
):
    if forbidden in manifest_v620:
        violations.append(f"Obsolete manifest entry still present: {forbidden}")

# Compatibility guard — obsolete service cleanup
obsolete_services = (
    "BackupService.kt",
    "CaptureGuideService.kt",
    "CapturePlannerService.kt",
    "CaptureRouteService.kt",
)
data_dir = root / "app/src/main/java/com/otaviobarreto/pokedex/data"
for obsolete_service in obsolete_services:
    if (data_dir / obsolete_service).exists():
        violations.append(f"Obsolete service still compiled: {obsolete_service}")

# Compatibility guard — legacy UI cleanup
legacy_ui_files = (
    "CompanionHubScreen.kt",
    "CollectionScreens.kt",
    "GamesHubScreen.kt",
    "PokedexV2Screen.kt",
    "PokedexScreens.kt",
    "TeamBuilderScreen.kt",
    "GameDexScreen.kt",
    "RegionExplorerScreen.kt",
    "HomeDashboardScreen.kt",
)
for legacy_file in legacy_ui_files:
    if (ui / legacy_file).exists():
        violations.append(f"Obsolete legacy UI still compiled: {legacy_file}")

main_v618 = (root / "app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt").read_text(encoding="utf-8")
for forbidden_route in ('composable("livingdex")', 'composable("companion")', 'composable("games")', 'composable("teams")', 'composable("gameDex?source={source}")', 'composable("regionExplorer?source={source}")'):
    if forbidden_route in main_v618:
        violations.append(f"Obsolete route still present: {forbidden_route}")

# Compatibility guard — startup preload + continuous audio
audio_manager_path = root / "app/src/main/java/com/otaviobarreto/pokedex/audio/HomeAudioManager.kt"
startup_preloader_path = root / "app/src/main/java/com/otaviobarreto/pokedex/data/StartupPreloader.kt"
boot_screen_path = root / "app/src/main/java/com/otaviobarreto/pokedex/ui/BootExperienceScreen.kt"

if not audio_manager_path.exists():
    violations.append("HOME audio manager missing")
else:
    audio_manager = audio_manager_path.read_text(encoding="utf-8")
    for required in (
        "R.raw.pokehome_st_sys01",
        "R.raw.pokehome_st_sys02",
        "AudioAttributes.USAGE_GAME",
        "playMainTrack",
        "onAppBackgrounded",
        "onAppForegrounded",
    ):
        if required not in audio_manager:
            violations.append(f"Continuous HOME audio missing {required}")
    for forbidden in (
        "playForRoute",
        "HomeAudioScene.JOURNEY",
        "HomeAudioScene.DETAIL",
    ):
        if forbidden in audio_manager:
            violations.append(f"Route-based audio switching still present: {forbidden}")

if not startup_preloader_path.exists():
    violations.append("Startup preload coordinator missing")
else:
    startup_preloader = startup_preloader_path.read_text(encoding="utf-8")
    for required in (
        "PokedexDataStore.nationalDex()",
        "GameDexService.loadGameDex",
        "PokedexDataStore.prefetchCoreDetails",
        "context.imageLoader.execute",
    ):
        if required not in startup_preloader:
            violations.append(f"Startup preload missing {required}")

if not boot_screen_path.exists():
    violations.append("Boot loading screen missing")
else:
    boot_screen = boot_screen_path.read_text(encoding="utf-8")
    for required in (
        "StartupPreloader.warm",
        "progress.fraction",
        "progress.label",
        "BuildConfig.VERSION_NAME",
    ):
        if required not in boot_screen:
            violations.append(f"Real loading UI missing {required}")

main_activity_v617 = (root / "app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt").read_text(encoding="utf-8")
for required in (
    "HomeAudioManager.playBoot()",
    "HomeAudioManager.playMainTrack()",
    "HomeAudioManager.onAppBackgrounded()",
    "HomeAudioManager.onAppForegrounded()",
):
    if required not in main_activity_v617:
        violations.append(f"Audio lifecycle wiring missing {required}")
if "HomeAudioManager.playForRoute(currentRoute)" in main_activity_v617:
    violations.append("Navigation must not switch background music")

app_v617 = (root / "app/src/main/java/com/otaviobarreto/pokedex/PokedexApplication.kt").read_text(encoding="utf-8")
if "HomeAudioManager.initialize(this)" not in app_v617:
    violations.append("Audio manager must initialize from Application")
if "preloadScope.launch" in app_v617:
    violations.append("Legacy parallel preload still runs outside loading screen")

for raw_name in (
    "pokehome_st_sys01.ogg",
    "pokehome_st_sys02.ogg",
):
    raw_path = root / "app/src/main/res/raw" / raw_name
    if not raw_path.exists() or raw_path.stat().st_size == 0:
        violations.append(f"Required audio asset missing {raw_name}")


# Compatibility guard — design foundation
design_tokens = ui / "PokedexDesignTokens.kt"
if not design_tokens.exists():
    violations.append("Central design tokens missing")
else:
    design_source = design_tokens.read_text(encoding="utf-8")
    for required in ("object Colors", "object Spacing", "object Radius", "object Elevation", "val AppTypography", "val Shapes"):
        if required not in design_source:
            violations.append(f"Design foundation missing {required}")

theme_source = (ui / "PokedexTheme.kt").read_text(encoding="utf-8")
for required in ("PokedexDesignTokens.Colors", "PokedexDesignTokens.AppTypography", "PokedexDesignTokens.Shapes"):
    if required not in theme_source:
        violations.append(f"Theme not routed through design system: {required}")

app_state = root / "app/src/main/java/com/otaviobarreto/pokedex/data/AppStatePreferences.kt"
if not app_state.exists():
    violations.append("AppStatePreferences missing")
if (root / "app/src/main/java/com/otaviobarreto/pokedex/data/CompanionPreferences.kt").exists():
    violations.append("Obsolete CompanionPreferences file returned")
if (root / "app/src/test/java/com/otaviobarreto/pokedex/data/CompanionRegressionTest.kt").exists():
    violations.append("Obsolete Companion regression test returned")
if not (root / "app/src/test/java/com/otaviobarreto/pokedex/data/GameContextRegressionTest.kt").exists():
    violations.append("GameContext regression test missing")

if not journey_hub_path.exists():
    violations.append("Journey hub extraction missing")
else:
    hub_source = journey_hub_path.read_text(encoding="utf-8")
    for required in ("JourneyGamePicker", "JourneyGameMenu", "rememberJourneyCollectionProgress"):
        if required not in hub_source:
            violations.append(f"Journey hub component missing {required}")

for required in (
    "CollectionStore.initialize(this)",
    "TeamStore.initialize(this)",
    "JourneyProgressStore.initialize(this)",
    "AppStatePreferences.initialize(this)",
    "HomeAudioManager.initialize(this)",
):
    if required not in application_source:
        violations.append(f"Application initialization missing {required}")

main_foundation = (root / "app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt").read_text(encoding="utf-8")
for forbidden in ("CollectionStore.initialize(this)", "TeamStore.initialize(this)", "JourneyProgressStore.initialize(this)", "AppStatePreferences.initialize(this)"):
    if forbidden in main_foundation:
        violations.append(f"Duplicate Activity initialization remains: {forbidden}")

local_gradle = (root / "app/build.gradle.kts").read_text(encoding="utf-8")
local_v1610 = 'versionName = "16.1.0"' in local_gradle and "versionCode = 16100" in local_gradle
local_v1611 = 'versionName = "16.1.1"' in local_gradle and "versionCode = 16110" in local_gradle
local_v1612 = 'versionName = "16.1.2"' in local_gradle and "versionCode = 16120" in local_gradle
local_v1613 = 'versionName = "16.1.3"' in local_gradle and "versionCode = 16130" in local_gradle
local_v1614 = 'versionName = "16.1.4"' in local_gradle and "versionCode = 16140" in local_gradle
local_v1615 = 'versionName = "16.1.5"' in local_gradle and "versionCode = 16150" in local_gradle
local_v1620 = 'versionName = "16.2.0"' in local_gradle and "versionCode = 16200" in local_gradle
local_v1700 = 'versionName = "17.0.0"' in local_gradle and "versionCode = 17000" in local_gradle
local_v1800 = 'versionName = "18.0.0"' in local_gradle and "versionCode = 18000" in local_gradle
local_v1810 = 'versionName = "18.1.0"' in local_gradle and "versionCode = 18100" in local_gradle
local_v1820 = 'versionName = "18.2.0"' in local_gradle and "versionCode = 18200" in local_gradle
if not (local_v1610 or local_v1611 or local_v1612 or local_v1613 or local_v1614 or local_v1615 or local_v1620 or local_v1700 or local_v1800 or local_v1810 or local_v1820):
    violations.append("Local build version is not aligned with supported v16/v17/v18 releases")

if (root / ".github/workflows/import-home-audio.yml").exists():
    violations.append("Obsolete feature-branch audio import workflow still present")


# Compatibility guard — official Journey cover
game_cover_catalog = root / "app/src/main/java/com/otaviobarreto/pokedex/data/GameCoverCatalog.kt"
if not game_cover_catalog.exists():
    violations.append("Official game cover catalog missing")
else:
    cover_source = game_cover_catalog.read_text(encoding="utf-8")
    for required in (
        "Pokémon Legends: Z-A",
        "Scarlet / Violet",
        "Sword / Shield",
        "Let's Go Pikachu / Eevee",
        "Legends Arceus",
        "Brilliant Diamond / Shining Pearl",
        "FireRed / LeafGreen",
    ):
        if required not in cover_source:
            violations.append(f"Journey cover missing game {required}")

journey_hub_v622 = (ui / "JourneyHubComponents.kt").read_text(encoding="utf-8")
for required in ("JourneyGameCover", "GameCoverCatalog.coversFor", "AsyncImage", "ContentScale.Fit"):
    if required not in journey_hub_v622:
        violations.append(f"Journey official cover rendering missing {required}")

startup_v622 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/StartupPreloader.kt").read_text(encoding="utf-8")
for required in ("GameCoverCatalog.coversFor", "startup-journey-art", "Preparando arte da Jornada"):
    if required not in startup_v622:
        violations.append(f"Journey cover preload missing {required}")


# Compatibility guard — Journey reference-card visual
journey_visual_catalog = root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyGameVisualCatalog.kt"
if not journey_visual_catalog.exists():
    violations.append("Journey hero artwork catalog missing")
else:
    visual_source = journey_visual_catalog.read_text(encoding="utf-8")
    for required in ("448", "1007", "1008", "888", "889", "25", "133", "493", "483", "484", "6", "3"):
        if required not in visual_source:
            violations.append(f"Journey hero artwork mapping missing {required}")

journey_hub_v623 = (ui / "JourneyHubComponents.kt").read_text(encoding="utf-8")
for required in (
    "JourneyGameReferenceCard",
    "JourneyHeroArtwork",
    "Brush.horizontalGradient",
    "JourneyGameCover",
    "compactJourneyRegionLabel",
    "RoundedCornerShape(PokedexDesignTokens.Journey.CardRadius)",
):
    if required not in journey_hub_v623:
        violations.append(f"Journey reference-card visual missing {required}")

startup_v623 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/StartupPreloader.kt").read_text(encoding="utf-8")
for required in ("JourneyGameVisualCatalog.forGame", "startup-journey-art", "Preparando arte da Jornada"):
    if required not in startup_v623:
        violations.append(f"Journey reference art preload missing {required}")


# Compatibility guard — Journey full-bleed artwork
journey_hub_v624 = (ui / "JourneyHubComponents.kt").read_text(encoding="utf-8")
for required in (
    "ContentScale.Crop",
    ".width(coverWidth)",
    "Brush.verticalGradient",
    "Arte oficial de $gameLabel",
):
    if required not in journey_hub_v624:
        violations.append(f"Journey full-bleed artwork missing {required}")

if 'color = Color(0xFF142548)' in journey_hub_v624 and 'contentDescription = "Abrir " + game.label' in journey_hub_v624:
    violations.append("Obsolete black Journey arrow returned")


# Compatibility guard — user-provided Scarlet/Violet card artwork
journey_hub_v625 = (ui / "JourneyHubComponents.kt").read_text(encoding="utf-8")
for required in ("scarlet_user_art_", "Base64.decode", "Arte enviada pelo usuário para Scarlet / Violet"):
    if required not in journey_hub_v625:
        violations.append(f"Scarlet user artwork integration missing {required}")
for part in range(1, 4):
    if not (root / f"app/src/main/assets/journey/scarlet_user_art_{part}.b64").exists():
        violations.append(f"Scarlet user artwork chunk {part} missing")


# Compatibility guard — Journey hardening
journey_hub_v626 = (ui / "JourneyHubComponents.kt").read_text(encoding="utf-8")
for required in (
    "BoxWithConstraints",
    "maxWidth < 360.dp",
    "PokedexDesignTokens.Journey.CardHeightCompact",
    "PokedexDesignTokens.Journey.CoverWidthCompact",
    "JourneyLocalArtworkCache",
    "rememberJourneyDexIdsByGame",
):
    if required not in journey_hub_v626:
        violations.append(f"Journey hardening missing {required}")

design_v626 = (ui / "PokedexDesignTokens.kt").read_text(encoding="utf-8")
for required in (
    "object Journey",
    "CardSurface",
    "ArtworkBackdrop",
    "CardHeightCompact",
    "CoverWidthCompact",
    "HeroWidthCompact",
    "FadeWidthCompact",
):
    if required not in design_v626:
        violations.append(f"Journey design token missing {required}")

startup_v626 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/StartupPreloader.kt").read_text(encoding="utf-8")
for required in (
    'filterNot { it.label == "Scarlet / Violet" }',
    "journeyArtworkUrls",
):
    if required not in startup_v626:
        violations.append(f"Journey preload hardening missing {required}")


# Compatibility guard — instant/offline/Scarlet/polish
readiness = root / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyReadinessAudit.kt"
if not readiness.exists():
    violations.append("Journey readiness audit missing")
else:
    readiness_source = readiness.read_text(encoding="utf-8")
    for required in (
        '"Paldea" to "paldea"',
        '"Kitakami" to "kitakami"',
        '"Blueberry" to "blueberry"',
        "allAdventureContexts",
        "journeyVisualUrls",
        "referenceCatalogUrls",
        "JourneyVisualAssetCatalog.allUrls",
        "JourneyTypeIconCatalog.allUrls",
        "JourneyMapCatalog.backgroundUrl",
    ):
        if required not in readiness_source:
            violations.append(f"v6.31 Journey readiness missing {required}")

startup_v631 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/StartupPreloader.kt").read_text(encoding="utf-8")
for required in (
    "activeContexts",
    "GameDexService.cached(ctx)",
    "activeRegionSource",
    "PokedexDataStore.prefetchFullDetails",
):
    if required not in startup_v631:
        violations.append(f"v6.31 instant preload missing {required}")

offline_v631 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt").read_text(encoding="utf-8")
for required in (
    "PACK_VERSION = 17",
    "cachedJourneyVisuals",
    "expectedJourneyVisuals",
    "JourneyReadinessAudit.referenceCatalogUrls",
    "JourneyReadinessAudit.journeyVisualUrls",
    "visual_urls",
    "journeyVisualKey",
):
    if required not in offline_v631:
        violations.append(f"v6.31 offline pack missing {required}")

if not (root / "app/src/test/java/com/otaviobarreto/pokedex/data/JourneyReadinessAuditTest.kt").exists():
    violations.append("v6.31 Journey readiness regression tests missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# v8.1-v8.9 consolidated feature guards
central = (ui / "CompanionCenterScreen.kt")
backup = root / "app/src/main/java/com/otaviobarreto/pokedex/data/AppBackupManager.kt"
insights = root / "app/src/main/java/com/otaviobarreto/pokedex/data/CollectionInsightsService.kt"
if not central.exists(): violations.append("v8.6-v8.8 Central screen missing")
else:
    central_source = central.read_text(encoding="utf-8")
    for required in ("CompanionCenterScreen", "AppBackupManager.exportJson", "AppBackupManager.importJson", "CollectionInsightsService.current", "Pokémon, número, tipo, jogo ou Box"):
        if required not in central_source and "Configurações" not in central_source: violations.append(f"Central feature missing {required}")
if not backup.exists(): violations.append("Backup manager missing")
if not insights.exists(): violations.append("Collection insights missing")
main_v89 = (root / "app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt").read_text(encoding="utf-8")
if ('MainDestination("central","Central"' not in main_v89 and 'MainDestination("central","Config."' not in main_v89) or 'composable("central")' not in main_v89:
    violations.append("Central navigation missing")
if violations:
    print("Source verification failed:")
    for item in violations: print(" -", item)
    sys.exit(1)


# Compatibility guard — stable product
backup_v9 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/AppBackupManager.kt").read_text(encoding="utf-8")
if '"recentActivity"' not in backup_v9:
    violations.append('v9 backup missing "recentActivity"')
if "RecentActivityStore::importSnapshot" not in backup_v9 and "RecentActivityStore.importSnapshot" not in backup_v9:
    violations.append("v9 backup recent-activity restore missing")
if all(marker not in backup_v9 for marker in ("SCHEMA_VERSION = 2","SCHEMA_VERSION = 3","SCHEMA_VERSION = 4")):
    violations.append("v9 backup schema compatibility missing")

insights_v9 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/CollectionInsightsService.kt").read_text(encoding="utf-8")
for required in ("GameCollectionProgress", "byGame", "regionsWithProgress"):
    if required not in insights_v9:
        violations.append(f"v9 collection insights missing {required}")

central_v9 = (ui / "CompanionCenterScreen.kt").read_text(encoding="utf-8")
for required in (
    "ActivityResultContracts.CreateDocument",
    "ActivityResultContracts.OpenDocument",
    "Pokémon, número, tipo, jogo ou Box",
    "Progresso por jogo",
    "Vistos recentemente",
    "AppBackupManager.exportJson",
    "AppBackupManager.importJson",
):
    if required not in central_v9 and "Configurações" not in central_v9:
        violations.append(f"v9 Central missing {required}")

main_v9 = (root / "app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt").read_text(encoding="utf-8")
if "onOpenBoxes=::openBoxes" not in main_v9:
    violations.append("v9 Central navigation wiring missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — Living Dex / performance / intelligence
store_v10 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokedexDataStore.kt").read_text(encoding="utf-8")
for required in ("Semaphore(permits = 6)", "data class CacheStats", "fun cacheStats()"):
    if required not in store_v10:
        violations.append(f"v10 performance store missing {required}")

startup_v10 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/StartupPreloader.kt").read_text(encoding="utf-8")
for required in (".distinct().take(48)", "priorityIds.take(8)", "chunked(6)", "priorityIds.take(18)"):
    if required not in startup_v10:
        violations.append(f"v10 startup preload missing {required}")

boxes_v10 = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ('"Capturados"', '"Faltando"', '"Regional"', '"Nacional"', '"Nome"'):
    if required not in boxes_v10:
        violations.append(f"v10 Box filter/sort missing {required}")

journey_v10 = (ui / "JourneyHubComponents.kt").read_text(encoding="utf-8")
for required in ("nextMissing", "Próximo alvo"):
    if required not in journey_v10:
        violations.append(f"v10 intelligent Journey missing {required}")

backup_v10 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/AppBackupManager.kt").read_text(encoding="utf-8")
if '"teams"' not in backup_v10:
    violations.append('v10 backup missing "teams"')
if "TeamStore::importSnapshot" not in backup_v10 and "TeamStore.importSnapshot" not in backup_v10:
    violations.append("v10 backup team restore missing")
if "SCHEMA_VERSION = 3" not in backup_v10 and "SCHEMA_VERSION = 4" not in backup_v10:
    violations.append("v10 backup schema compatibility missing")

central_v10 = (ui / "CompanionCenterScreen.kt").read_text(encoding="utf-8")
for required in ("Living Dex", "Offline e desempenho", "PokedexDataStore.cacheStats", "OfflineGamePackManager.status"):
    if required not in central_v10 and "Configurações" not in central_v10:
        violations.append(f"v10 Central missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# v10.1-v10.4 Forms & Shiny collection guards
variant_store = root / "app/src/main/java/com/otaviobarreto/pokedex/data/VariantCollectionStore.kt"
if not variant_store.exists():
    violations.append("Variant collection store missing")
else:
    variant_source = variant_store.read_text(encoding="utf-8")
    for required in ("OwnedPokemonVariant", "shiny:Boolean", "artworkUrl", "exportSnapshot", "importSnapshot"):
        if required not in variant_source:
            violations.append(f"Variant collection missing {required}")

application_v104 = (root / "app/src/main/java/com/otaviobarreto/pokedex/PokedexApplication.kt").read_text(encoding="utf-8")
if "VariantCollectionStore.initialize(this)" not in application_v104:
    violations.append("Variant collection initialization missing")

boxes_v104 = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ("QBVariantManager", "Formas e Shiny", "★ Shiny", "PokemonFormsService", "VariantCollectionStore.preferred"):
    if required not in boxes_v104:
        violations.append(f"Forms/Shiny Box UI missing {required}")

backup_v104 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/AppBackupManager.kt").read_text(encoding="utf-8")
for required in ("SCHEMA_VERSION = 4", '"variants"'):
    if required not in backup_v104:
        violations.append(f"Forms/Shiny backup missing {required}")
if "VariantCollectionStore::importSnapshot" not in backup_v104 and "VariantCollectionStore.importSnapshot" not in backup_v104:
    violations.append("Forms/Shiny backup variant restore missing")

central_v104 = (ui / "CompanionCenterScreen.kt").read_text(encoding="utf-8")
for required in ('"Formas"', '"Shiny"', "insights.ownedForms", "insights.shinyVariants"):
    if required not in central_v104 and "Configurações" not in central_v104:
        violations.append(f"Forms/Shiny insights missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — Evolution methods audit
pokeapi_v105 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokeApiService.kt").read_text(encoding="utf-8")
for required in (
    "known_move",
    "known_move_type",
    "location",
    "min_affection",
    "min_beauty",
    "min_happiness",
    "needs_overworld_rain",
    "party_species",
    "party_type",
    "relative_physical_stats",
    "time_of_day",
    "trade_species",
    "turn_upside_down",
    "gender",
    "held_item",
    "mergeEvolutionRequirements",
    "specialEvolutionRequirements",
    'joinToString("  OU  ")',
):
    if required not in pokeapi_v105:
        violations.append(f"Evolution audit missing {required}")

for special_id in (
    "266 to", "268 to", "292 to", "687 to", "745 to", "849 to",
    "865 to", "867 to", "869 to", "892 to", "899 to", "901 to",
    "902 to", "904 to", "923 to", "947 to", "954 to", "964 to",
    "979 to", "983 to", "1000 to"
):
    if special_id not in pokeapi_v105:
        violations.append(f"Special evolution rule missing {special_id}")

if "getJSONObject(0)?.let(::evolutionRequirement)" in pokeapi_v105:
    violations.append("Evolution parser must not keep only the first evolution_details entry")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — definitive evolution/forms/detail
forms_v11 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokemonFormsService.kt").read_text(encoding="utf-8")
for required in ("PokemonFormKind", "REGIONAL", "GENDER", "BATTLE", "SPECIAL", "fun collectible", "classify("):
    if required not in forms_v11:
        violations.append(f"v11 forms catalog missing {required}")

detail_v11 = (ui / "PokemonDetailV2Screen.kt").read_text(encoding="utf-8")
for required in ("PokemonFormsSummaryCard", "Formas e Shiny", "VariantCollectionStore.preferred", "preferredVariant?.artworkUrl"):
    if required not in detail_v11:
        violations.append(f"v11 detail forms integration missing {required}")

boxes_v11 = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
if "PokemonFormsService.collectible" not in boxes_v11:
    violations.append("v11 Box does not use normalized collectible forms")

pokeapi_v11 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokeApiService.kt").read_text(encoding="utf-8")
for required in ('925 to', '982 to', '1019 to', "mergeEvolutionRequirements", "specialEvolutionRequirements"):
    if required not in pokeapi_v11:
        violations.append(f"v11 evolution curation missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — definitive Living Dex / Forms / Shiny / Offline
planner_v12 = root / "app/src/main/java/com/otaviobarreto/pokedex/data/LivingDexPlanner.kt"
if not planner_v12.exists():
    violations.append("v12 LivingDexPlanner missing")
else:
    planner_source = planner_v12.read_text(encoding="utf-8")
    for required in ("LivingDexPlan", "missingSpecies", "shinySpecies", "nextMissing"):
        if required not in planner_source:
            violations.append(f"v12 Living Dex planner missing {required}")

forms_v12 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokemonFormsService.kt").read_text(encoding="utf-8")
for required in ("countsForLivingDex", "livingDexForms", "PokemonFormKind.BATTLE"):
    if required not in forms_v12:
        violations.append(f"v12 form target policy missing {required}")

boxes_v12 = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ('"Shiny"', '"Formas"', "VariantCollectionStore.ownedVariants", "temporária"):
    if required not in boxes_v12:
        violations.append(f"v12 Box forms/shiny filter missing {required}")

central_v12 = (ui / "CompanionCenterScreen.kt").read_text(encoding="utf-8")
for required in ("LivingDexPlanner.current", "O que falta", "Próximas espécies ausentes", "Shiny espécies"):
    if required not in central_v12 and "Configurações" not in central_v12:
        violations.append(f"v12 Living Dex dashboard missing {required}")

startup_v12 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/StartupPreloader.kt").read_text(encoding="utf-8")
if "PokemonFormsService.collectible" not in startup_v12:
    violations.append("v12 startup form preload missing")

offline_v12 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt").read_text(encoding="utf-8")
for required in ("PACK_VERSION = 17", "pokemon-form-offline-", "pokemon-form-shiny-offline-", "countsForLivingDex"):
    if required not in offline_v12:
        violations.append(f"v12 offline forms/shiny cache missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — Companion / acquisition / Shiny Dex
advisor_v13 = root / "app/src/main/java/com/otaviobarreto/pokedex/data/CollectionAdvisor.kt"
if not advisor_v13.exists():
    violations.append("v13 CollectionAdvisor missing")
else:
    advisor_source = advisor_v13.read_text(encoding="utf-8")
    for required in ("AcquisitionOption", "warmAllGames", "recommendation", "cachedOptions"):
        if required not in advisor_source:
            violations.append(f"v13 CollectionAdvisor missing {required}")

planner_v13 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/LivingDexPlanner.kt").read_text(encoding="utf-8")
for required in ("GenerationDexProgress", "byGeneration", "shinySpecies"):
    if required not in planner_v13:
        violations.append(f"v13 generation/shiny progress missing {required}")

central_v13 = (ui / "CompanionCenterScreen.kt").read_text(encoding="utf-8")
for required in ("CollectionAdvisor.warmAllGames", "Progresso por geração", "CollectionAdvisor.recommendation"):
    if required not in central_v13 and "Configurações" not in central_v13:
        violations.append(f"v13 Central advisor missing {required}")

boxes_v13 = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ('"Normal"', '"Shiny"', '"Formas"'):
    if required not in boxes_v13:
        violations.append(f"v13/v18 Box variant filter missing {required}")

forms_v13 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokemonFormsService.kt").read_text(encoding="utf-8")
for required in ("COSMETIC", "countsForLivingDex", "family of three", "three segment", "teal mask"):
    if required not in forms_v13:
        violations.append(f"v13 forms curation missing {required}")

offline_v13 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt").read_text(encoding="utf-8")
if "PACK_VERSION = 17" not in offline_v13:
    violations.append("v13 offline pack version missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — Living Dex companion / capture planner
advisor_v14 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/CollectionAdvisor.kt").read_text(encoding="utf-8")
for required in ("CaptureTarget", "capturePlan", "preferredOption", "Melhor opção na sua base"):
    if required not in advisor_v14:
        violations.append(f"v14 capture planner missing {required}")

central_v14 = (ui / "CompanionCenterScreen.kt").read_text(encoding="utf-8")
for required in ("Plano de captura", "CollectionAdvisor.capturePlan", 'Text("Abrir "+option.region)', "Progresso por geração"):
    if required not in central_v14 and "Configurações" not in central_v14:
        violations.append(f"v14 Central capture planner missing {required}")

detail_v14 = (ui / "PokemonDetailV2Screen.kt").read_text(encoding="utf-8")
for required in ('"Onde conseguir"', "CollectionAdvisor.recommendation", "CollectionAdvisor.cachedOptions"):
    if required not in detail_v14:
        violations.append(f"v14 Pokémon detail advisor missing {required}")

offline_v14 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt").read_text(encoding="utf-8")
if "PACK_VERSION = 17" not in offline_v14:
    violations.append("v14 offline pack version missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — smart collection / route optimizer
advisor_v15 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/CollectionAdvisor.kt").read_text(encoding="utf-8")
for required in (
    "AcquisitionKind",
    "GameRouteStep",
    "NextCollectionAction",
    "gameRoutePlan",
    "nextAction",
    "acquisitionLabel",
):
    if required not in advisor_v15:
        violations.append(f"v15 smart advisor missing {required}")

central_v15 = (ui / "CompanionCenterScreen.kt").read_text(encoding="utf-8")
for required in (
    "O que faço agora?",
    "Rota recomendada",
    "CollectionAdvisor.gameRoutePlan",
    "CollectionAdvisor.nextAction",
    "Abrir melhor jogo agora",
):
    if required not in central_v15 and "Configurações" not in central_v15:
        violations.append(f"v15 Central route UI missing {required}")

detail_v15 = (ui / "PokemonDetailV2Screen.kt").read_text(encoding="utf-8")
for required in ("CollectionAdvisor.acquisitionLabel", "Onde conseguir"):
    if required not in detail_v15:
        violations.append(f"v15 acquisition classification missing {required}")

offline_v15 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt").read_text(encoding="utf-8")
if "PACK_VERSION = 17" not in offline_v15:
    violations.append("v15 offline pack version missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — acquisition intelligence / evolution-first
resolver_v16 = root / "app/src/main/java/com/otaviobarreto/pokedex/data/AcquisitionMethodResolver.kt"
if not resolver_v16.exists():
    violations.append("v16 AcquisitionMethodResolver missing")
else:
    resolver_source = resolver_v16.read_text(encoding="utf-8")
    for required in (
        "DetailedAcquisitionMethod",
        "AcquisitionAdvice",
        "sourceOwned",
        "resolveBatch",
        "PokedexDataStore.evolutions",
        "CollectionAdvisor.cachedOptions",
    ):
        if required not in resolver_source:
            violations.append(f"v16 acquisition resolver missing {required}")

central_v16 = (ui / "CompanionCenterScreen.kt").read_text(encoding="utf-8")
for required in (
    "Métodos de obtenção",
    "Evoluir primeiro",
    "AcquisitionMethodResolver.resolveBatch",
    "DetailedAcquisitionMethod.EVOLUTION",
    "DetailedAcquisitionMethod.TRADE",
):
    if required not in central_v16 and "Configurações" not in central_v16:
        violations.append(f"v16 Central acquisition UI missing {required}")

offline_v16 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt").read_text(encoding="utf-8")
if "PACK_VERSION = 17" not in offline_v16:
    violations.append("v16 offline pack version missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — settings + form-aware Pokedex
main_v161 = (root / "app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt").read_text(encoding="utf-8")
for required in ('MainDestination("pokedex","Pokédex"', 'MainDestination("central","Config."', "PokemonFormDetailScreen", "PokedexCatalogScreen"):
    if required not in main_v161:
        violations.append(f"v16.1 navigation missing {required}")

settings_v161 = (ui / "CompanionCenterScreen.kt").read_text(encoding="utf-8")
for required in ("Configurações", "Downloads dos jogos", "Backup e restauração", "OfflineGamePackManager.download"):
    if required not in settings_v161:
        violations.append(f"v16.1 settings center missing {required}")
for forbidden in ("O que faço agora?", "Plano de captura", "Rota recomendada", "Evoluir primeiro"):
    if forbidden in settings_v161:
        violations.append(f"v16.1 settings center still contains advice: {forbidden}")

pokedex_v161 = (ui / "PokedexCatalogScreen.kt").read_text(encoding="utf-8")
for required in ("Formas e variantes", "Shiny", "Ver ficha completa", "PokemonFormKind.BATTLE"):
    if required not in pokedex_v161:
        violations.append(f"v16.1 Pokedex forms missing {required}")

form_detail_v161 = (ui / "PokemonFormDetailScreen.kt").read_text(encoding="utf-8")
for required in ("Status base", "Habilidades", "PokedexDataStore.pokemon"):
    if required not in form_detail_v161:
        violations.append(f"v16.1 form detail missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — navigation + Box removal
main_v1612 = (root / "app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt").read_text(encoding="utf-8")
expected_order = [
    'MainDestination("home","Jornada"',
    'MainDestination("pokedex","Pokédex"',
    'MainDestination("boxes","Boxes"',
    'MainDestination("central","Config."',
]
positions = [main_v1612.find(marker) for marker in expected_order]
if any(pos < 0 for pos in positions) or positions != sorted(positions):
    violations.append("v16.1.2 bottom navigation order must be Jornada, Pokédex, Boxes, Config.")

variant_v1612 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/VariantCollectionStore.kt").read_text(encoding="utf-8")
for required in ("fun removeAll(", "CollectionStore.setCapturedIn(source,speciesId,false)"):
    if required not in variant_v1612:
        violations.append(f"v16.1.2 variant removal missing {required}")

boxes_v1612 = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ("Remover Pokémon da Box", "VariantCollectionStore.removeAll(source,pk.nationalId)", "DeleteOutline"):
    if required not in boxes_v1612:
        violations.append(f"v16.1.2 Box removal UI missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — complete National Forms
forms_v1613 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokemonFormsService.kt").read_text(encoding="utf-8")
for required in (
    'https://pokeapi.co/api/v2/pokemon/$pid',
    'formKey',
    '.distinctBy{it.formKey}',
    'pokemonJson?.optJSONArray("forms")',
):
    if required not in forms_v1613:
        violations.append(f"v16.1.3 complete forms discovery missing {required}")

variant_v1613 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/VariantCollectionStore.kt").read_text(encoding="utf-8")
for required in (
    'formName.lowercase()',
    'formName:String?=null',
    'it.formName.equals(formName,true)',
):
    if required not in variant_v1613:
        violations.append(f"v16.1.3 exact form identity missing {required}")

workflow_v1613 = (root / ".github/workflows/android.yml").read_text(encoding="utf-8")
for required in ("National Forms audit", "audit_national_forms.py"):
    if required not in workflow_v1613:
        violations.append(f"v16.1.3 forms audit workflow missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — exact visual-form artwork
forms_v1614 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokemonFormsService.kt").read_text(encoding="utf-8")
for required in (
    'val spriteUrl: String? = null',
    'val shinySpriteUrl: String? = null',
    'optJSONObject("sprites")',
    'optString("front_default")',
    'optString("front_shiny")',
):
    if required not in forms_v1614:
        violations.append(f"v16.1.4 form artwork support missing {required}")

variant_v1614 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/VariantCollectionStore.kt").read_text(encoding="utf-8")
for required in (
    'normalArtworkUrl:String?=null',
    'shinyArtworkUrl:String?=null',
    '.put("normalArtworkUrl",v.normalArtworkUrl)',
    '.put("shinyArtworkUrl",v.shinyArtworkUrl)',
):
    if required not in variant_v1614:
        violations.append(f"v16.1.4 persisted artwork support missing {required}")

boxes_v1614 = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ("normalArtworkUrl=form.spriteUrl", "shinyArtworkUrl=form.shinySpriteUrl"):
    if required not in boxes_v1614:
        violations.append(f"v16.1.4 Box form artwork wiring missing {required}")

pokedex_v1614 = (ui / "PokedexCatalogScreen.kt").read_text(encoding="utf-8")
for required in ("form.spriteUrl", "form.shinySpriteUrl"):
    if required not in pokedex_v1614:
        violations.append(f"v16.1.4 Pokédex form artwork wiring missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — Box quick-action + visual framing
boxes_v1615 = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in (
    "VariantCollectionStore.toggle(",
    "dismiss()",
    "Modifier.size(64.dp)",
    "Modifier.fillMaxSize().padding(4.dp)",
):
    if required not in boxes_v1615:
        violations.append(f"v16.1.5 Box quick UX missing {required}")

pokedex_v1615 = (ui / "PokedexCatalogScreen.kt").read_text(encoding="utf-8")
for required in (
    "surfaceContainerLow",
    "RoundedCornerShape(16.dp)",
    "Modifier.fillMaxSize().padding(6.dp)",
):
    if required not in pokedex_v1615:
        violations.append(f"v16.1.5 artwork framing missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — integrated collection + performance milestone
detail_v1620 = (ui / "PokemonDetailV2Screen.kt").read_text(encoding="utf-8")
for required in (
    "collectionSource",
    "Coleção · Formas e Shiny",
    "VariantCollectionStore.toggle(",
    "VariantCollectionStore.removeAll(",
    "prefetchDetailWindow(id,radius=2)",
):
    if required not in detail_v1620:
        violations.append(f"v16.2.0 detail/collection integration missing {required}")

pokedex_v1620 = (ui / "PokedexCatalogScreen.kt").read_text(encoding="utf-8")
for required in (
    "selectedKind",
    "PokemonFormKind.COSMETIC",
    "Regionais",
    "Cosméticas",
):
    if required not in pokedex_v1620:
        violations.append(f"v16.2.0 form grouping missing {required}")

boxes_v1620 = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
if "items(available,key={it.formKey})" not in boxes_v1620:
    violations.append("v16.2.0 Box exact-form list key missing")

forms_v1620 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokemonFormsService.kt").read_text(encoding="utf-8")
for required in (
    "needsExactFormSprite",
    "forms.length() > 1 || rawName != varietyName",
):
    if required not in forms_v1620:
        violations.append(f"v16.2.0 forms performance guard missing {required}")

workflow_v1620 = (root / ".github/workflows/android.yml").read_text(encoding="utf-8")
for required in ("Problem Forms audit", "audit_problem_forms.py"):
    if required not in workflow_v1620:
        violations.append(f"v16.2.0 targeted visual audit missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# Compatibility guard — stable milestone
detail_v1700 = (ui / "PokemonDetailV2Screen.kt").read_text(encoding="utf-8")
for required in (
    "DetailDexNavigator",
    "Coleção · Formas e Shiny",
    "prefetchDetailWindow(id,radius=2)",
):
    if required not in detail_v1700:
        violations.append(f"v17.0.0 detail navigation/integration missing {required}")

forms_v1700 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokemonFormsService.kt").read_text(encoding="utf-8")
for required in (
    "resourceUrlsFor",
    "resourceCache",
    "needsExactFormSprite",
):
    if required not in forms_v1700:
        violations.append(f"v17.0.0 form/offline resource tracking missing {required}")

offline_v1700 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt").read_text(encoding="utf-8")
for required in (
    "PACK_VERSION = 17",
    "PokemonFormsService.resourceUrlsFor(id)",
    "form.spriteUrl",
    "form.shinySpriteUrl",
):
    if required not in offline_v1700:
        violations.append(f"v17.0.0 offline forms support missing {required}")

startup_v1700 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/StartupPreloader.kt").read_text(encoding="utf-8")
for required in (
    "activePageIds",
    "ownedVariantIds",
    "take(64)",
):
    if required not in startup_v1700:
        violations.append(f"v17.0.0 startup performance support missing {required}")

collection_v1700 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/CollectionStore.kt").read_text(encoding="utf-8")
if "stillReferenced" not in collection_v1700:
    violations.append("v17.0.0 collection consistency cleanup missing")

presentation_v1700 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokemonFormPresentation.kt").read_text(encoding="utf-8")
for required in ("categoryLabel", "behaviorLabel", "Forma de Alola", "Gigantamax"):
    if required not in presentation_v1700:
        violations.append(f"v17.0.0 Portuguese form presentation missing {required}")

# Compatibility guard — definitive forms/collection/performance/settings
forms_v1800 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokemonFormsService.kt").read_text(encoding="utf-8")
for required in ("hasBattleDataChanges", "battleSignature", "defaultBattleSignature"):
    if required not in forms_v1800:
        violations.append(f"v18 form battle-data detection missing {required}")

variant_v1800 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/VariantCollectionStore.kt").read_text(encoding="utf-8")
for required in ("formKey", 'put("formKey"', "ownedVariants.none { it.source==source && it.speciesId==speciesId }"):
    if required not in variant_v1800:
        violations.append(f"v18 canonical collection identity/sync missing {required}")

data_v1800 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/PokedexDataStore.kt").read_text(encoding="utf-8")
for required in ("prefetchDetailWindow", "prefetchBoxWindow", "page-1,page,page+1"):
    if required not in data_v1800:
        violations.append(f"v18 bounded preload missing {required}")

boxes_v1800 = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ('"Faltantes"', '"Normal"', '"Shiny"', '"Formas"', "formKey=form.formKey", "prefetchBoxWindow"):
    if required not in boxes_v1800:
        violations.append(f"v18 Box definitive behavior missing {required}")

settings_v1800 = (ui / "CompanionCenterScreen.kt").read_text(encoding="utf-8")
for required in ('SettingsSectionTitle("Áudio")', "HomeAudioManager.setEnabled", 'SettingsSectionTitle("Informações da versão")'):
    if required not in settings_v1800:
        violations.append(f"v18 settings consolidation missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# v18.1.0 audit-hardening guards
backup_v1810 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/AppBackupManager.kt").read_text(encoding="utf-8")
for required in ("previous", "applySnapshot(incoming)", "applySnapshot(it)", "isSupported"):
    if required not in backup_v1810:
        violations.append(f"v18.1 backup rollback guard missing {required}")

audio_v1810 = (root / "app/src/main/java/com/otaviobarreto/pokedex/audio/HomeAudioManager.kt").read_text(encoding="utf-8")
if "prepareAsync()" not in audio_v1810:
    violations.append("v18.1 asynchronous audio preparation missing")
if "\n                prepare()\n" in audio_v1810:
    violations.append("v18.1 synchronous audio preparation regression")

variant_v1810 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/VariantCollectionStore.kt").read_text(encoding="utf-8")
if "sameOwnedVariantIdentity" not in variant_v1810:
    violations.append("v18.1 canonical variant regression helper missing")

readme_v1810 = (root / "README.md").read_text(encoding="utf-8")
if "Estado atual — v18.2.0" not in readme_v1810:
    violations.append("README current version is not v18.2.0")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)


# v18.2.0 offline/startup hardening guards
offline_v1820 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt").read_text(encoding="utf-8")
for required in (
    "form_artwork_keys",
    "formArtworkKey(",
    "sharedVisualUrls",
    "sharedArtworkKeys",
    "cachedFormArtworks",
):
    if required not in offline_v1820:
        violations.append(f"v18.2 offline hardening missing {required}")

startup_v1820 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/StartupPreloader.kt").read_text(encoding="utf-8")
if "JourneyReadinessAudit.allAdventureContexts()" in startup_v1820:
    violations.append("v18.2 startup must not block on all adventure contexts")
if 'check(JourneyReadinessAudit.scarletViolet().valid)' in startup_v1820:
    violations.append("v18.2 startup must not contain fatal journey audit")
if 'listOf("move", "ability", "item")' in startup_v1820:
    violations.append("v18.2 startup must not block on reference catalogs")

boot_v1820 = (ui / "BootExperienceScreen.kt").read_text(encoding="utf-8")
if "BuildConfig.VERSION_NAME" not in boot_v1820:
    violations.append("v18.2 splash version must be dynamic")

application_v1820 = (root / "app/src/main/java/com/otaviobarreto/pokedex/PokedexApplication.kt").read_text(encoding="utf-8")
if "HttpResponseCache" in application_v1820:
    violations.append("v18.2 redundant HttpResponseCache must stay removed")

settings_v1820 = (ui / "CompanionCenterScreen.kt").read_text(encoding="utf-8")
if "withContext(Dispatchers.IO)" not in settings_v1820 or "AppBackupManager.importJson" not in settings_v1820:
    violations.append("v18.2 backup restore IO guard missing")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)
