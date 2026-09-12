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


# v6.0 release guards
offline = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt").read_text(encoding="utf-8")
if "PACK_VERSION = 10" not in offline:
    violations.append("Offline pack version is not v8")

app = (root / "app/src/main/java/com/otaviobarreto/pokedex/PokedexApplication.kt").read_text(encoding="utf-8")
if ".crossfade(false)" not in app or "384L * 1024L * 1024L" not in app:
    violations.append("Image engine v2 tuning missing")

detail = (ui / "PokemonDetailV2Screen.kt").read_text(encoding="utf-8")
if "resolveSaveLocation" not in detail or "saveLocation.saved" not in detail:
    violations.append("Pokemon detail save-location integration missing")

workflow = (root / ".github/workflows/android.yml").read_text(encoding="utf-8")
if "8000" not in workflow or "8.0.0" not in workflow:
    violations.append("CI v8.0.0 version stamping missing")

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
if "missingDetails.take(12)" not in boxes or "missingDetails.drop(12)" not in boxes:
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


# v6.5.1 Journey route visual guards
journey_visual = journey_source
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


# v6.5.3 Smart Journey progress guards
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


# v6.7.1 Journey visual assets guards
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


# v6.10.0 immersive Paldea map guards
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


# v6.11.0 navigation consolidation guards
main_nav=(root/"app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt").read_text(encoding="utf-8")
journey_v611=journey_source
boxes_v611=(ui/"BoxesV2Screen.kt").read_text(encoding="utf-8")
prefs_v611=(root/"app/src/main/java/com/otaviobarreto/pokedex/data/AppStatePreferences.kt").read_text(encoding="utf-8")
main_line=next((line for line in main_nav.splitlines() if line.startswith("private val mainDestinations=")),"")
for forbidden in ('"Pokédex"','"Living Dex"','"Companion"'):
    if forbidden in main_line:
        violations.append(f"Legacy primary tab still present: {forbidden}")
for required in ('MainDestination("home","Jornada"','MainDestination("boxes","Boxes"'):
    if required not in main_line:
        violations.append(f"Primary navigation missing {required}")
for required in ("Boxes do jogo","rememberJourneyCollectionProgress","CollectionStore.contextualCapturedIds","onOpenBoxes"):
    if required not in journey_v611:
        violations.append(f"Journey/Boxes consolidation missing {required}")
for required in ("AppStatePreferences.activeGame","AppStatePreferences.setActiveRegionForGame"):
    if required not in boxes_v611:
        violations.append(f"Boxes context handoff missing {required}")
if "KEY_ACTIVE_REGION" not in prefs_v611:
    violations.append("Persistent active Box region missing")


# v6.12.0 Android back navigation guards
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


# v6.12.0 navigation/state hardening guards
prefs_v612 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/AppStatePreferences.kt").read_text(encoding="utf-8")
for required in ("activeRegionForGame", "setActiveRegionForGame", "boxPage", "setBoxPage", "KEY_BOX_PAGE_PREFIX"):
    if required not in prefs_v612:
        violations.append(f"Persistent navigation context missing {required}")

boxes_v612 = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ("activeRegionForGame(game.label)", "AppStatePreferences.boxPage(regionSource)", "setBoxPage(region.source,current)", "missingDetails.take(12)", "missingDetails.drop(12)"):
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


# v6.13.0 contextual collection + navigation continuity guards
collection_v613 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/CollectionStore.kt").read_text(encoding="utf-8")
for required in ("contextualCapturedIds", "capturedIn", "isCapturedIn", "toggleCapturedIn", "setCapturedIn", "migrateLegacyCapturedToSource", "contextualCaptured"):
    if required not in collection_v613:
        violations.append(f"Contextual collection missing {required}")

boxes_v613 = (ui / "BoxesV2Screen.kt").read_text(encoding="utf-8")
for required in ("CollectionStore.contextualCapturedIds[region.source]", "CollectionStore.isCapturedIn(region.source", "CollectionStore.toggleCapturedIn(region.source"):
    if required not in boxes_v613:
        violations.append(f"Regional Box capture isolation missing {required}")
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


# v6.14.0 data integrity + navigation hardening guards
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


# v6.15.0 current-core hardening guards (Journey + Boxes/offline only)
journey_v615 = journey_source
for required in ("explicitGameSelectionRevision", "LaunchedEffect(explicitGameSelectionRevision)", "if(explicitGameSelectionRevision==0) return@LaunchedEffect"):
    if required not in journey_v615:
        violations.append(f"Journey Android recreation preservation missing {required}")
if "LaunchedEffect(selectedGame){" in journey_v615:
    violations.append("Journey must not reset saved navigation state merely because selectedGame was restored")

offline_v615 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt").read_text(encoding="utf-8")
for required in ("PACK_VERSION = 9", "cachedImages", "expectedImages", "hasOfflineArtwork", 'openSnapshot("pokemon-offline-$id")'):
    if required not in offline_v615:
        violations.append(f"Offline artwork integrity audit missing {required}")

if violations:
    print("Source verification failed:")
    for item in violations:
        print(" -", item)
    sys.exit(1)





# v6.21.0 dead component cleanup guards
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

# v6.19.0 obsolete service cleanup guards
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

# v6.18.0 legacy UI cleanup guards
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
for forbidden_route in ('composable("livingdex")', 'composable("companion")', 'composable("games")', 'composable("pokedex")', 'composable("teams")', 'composable("gameDex?source={source}")', 'composable("regionExplorer?source={source}")'):
    if forbidden_route in main_v618:
        violations.append(f"Obsolete route still present: {forbidden_route}")

# v6.17.0 startup preload + continuous audio guards
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
        "ReferenceCatalogService.load",
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
        "v8.0.0",
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


# v6.21.0 design foundation guards
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
if 'versionName = "8.0.0"' not in local_gradle or "versionCode = 8000" not in local_gradle:
    violations.append("Local build version is not aligned with v8.0.0")

if (root / ".github/workflows/import-home-audio.yml").exists():
    violations.append("Obsolete feature-branch audio import workflow still present")


# v6.22.0 official Journey cover guards
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


# v6.23.0 Journey reference-card visual guards
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


# v6.24.0 Journey full-bleed artwork guards
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


# v6.25.0 user-provided Scarlet/Violet card artwork guards
journey_hub_v625 = (ui / "JourneyHubComponents.kt").read_text(encoding="utf-8")
for required in ("scarlet_user_art_", "Base64.decode", "Arte enviada pelo usuário para Scarlet / Violet"):
    if required not in journey_hub_v625:
        violations.append(f"Scarlet user artwork integration missing {required}")
for part in range(1, 4):
    if not (root / f"app/src/main/assets/journey/scarlet_user_art_{part}.b64").exists():
        violations.append(f"Scarlet user artwork chunk {part} missing")


# v6.26.0 Journey hardening guards
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


# v6.31.0 instant/offline/Scarlet/polish guards
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
    "JourneyReadinessAudit.allAdventureContexts",
    "GameDexService.cached(ctx)",
    "JourneyReadinessAudit.scarletViolet().valid",
    "PokedexDataStore.prefetchFullDetails",
):
    if required not in startup_v631:
        violations.append(f"v6.31 instant preload missing {required}")

offline_v631 = (root / "app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt").read_text(encoding="utf-8")
for required in (
    "PACK_VERSION = 9",
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
