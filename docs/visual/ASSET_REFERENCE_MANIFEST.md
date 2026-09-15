# Asset Reference Manifest
Research snapshot: 2026-09-15. Status: ACTIVE / Stage 0.

## Layers
Reference material stays under docs/visual/reference-assets. Android runtime resources remain separate. This prevents study material from bloating the APK or changing the frozen artwork pipeline.

## Canonical sources
NIN-UX-01 | Nintendo UI/UX | https://www.nintendo.co.jp/jobs/introduction/design/work03.html | global hierarchy, graphical communication
NIN-MUS-01 | Nintendo Music design | https://www.nintendo.co.jp/jobs/keyword/166.html | memory cues, contextual imagery, discovery
NIN-TOD-01 | Nintendo Today | https://www.nintendo.com/pt-br/mobile-apps/nintendo-today/ | Home, themes, personalization
NIN-SWA-01 | Nintendo Switch App | https://www.nintendo.com/pt-br/mobile-apps/nintendo-switch-app/ | shell plus game-specific companion modules
NIN-ZEL-01 | ZELDA NOTES | https://www.nintendo.com/au/apps/zelda-notes/ | Journey, progress, navigation
NIN-MUS-02 | Nintendo Music | https://www.nintendo.com/pt-br/online/nintendo-switch-online/nintendo-music/ | Search, discovery, restrained controls
NIN-STR-01 | Nintendo Store | https://www.nintendo.com/pt-br/mobile-apps/nintendo-store/ | categories, search, utility lists
PKM-HOM-01 | Pokemon HOME | https://home.pokemon.com/en-gb/features/ | Dex, Collection, Box, Detail
PKM-HOM-02 | Pokemon HOME BR | https://www.pokemon.com/br/videogames-pokemon/pokemon-home | storage/collection framing
PKM-SLP-01 | Pokemon Sleep | https://press.pokemon.com/pt-BR/Pokemon-Sleep/Focus/Pokemon-Sleep-Screenshots | friendly character-led utility
PKM-CAF-01 | Pokemon Cafe ReMix | https://www.pokemon.com/br/videogames-pokemon/pokemon-cafe-remix | selective playful presentation

## Capture set
Nintendo Today: home/calendar hierarchy, theme variants, personalization, content cards, navigation.
Nintendo Switch App: shell/navigation, game-service entry, media browsing.
ZELDA NOTES: landing, progress/navigation, game imagery with utility, adventure-support hierarchy.
Nintendo Music: home/discovery, search, library/list, settings.
Nintendo Store: categories/search, history/list, product browsing.
Pokemon HOME: mobile home/Your Room, Pokemon list, Box, National Dex, game Dex, detail, completion states.

## Normalization
WebP preferred for screenshots; PNG for transparency/lossless needs. Preserve readable UI and navigation context. Portrait max long edge about 1600px; landscape/tablet about 1920px; WebP quality 82-88. Filename: source-id_surface_variant_nn.webp.

## Folder map
docs/visual/reference-assets/nintendo-today/
docs/visual/reference-assets/nintendo-switch-app/
docs/visual/reference-assets/zelda-notes/
docs/visual/reference-assets/nintendo-music/
docs/visual/reference-assets/nintendo-store/
docs/visual/reference-assets/pokemon-home/
docs/visual/reference-assets/pokemon-sleep/
docs/visual/reference-assets/pokemon-cafe/

## Classification
REFERENCE_ONLY: study.
PRODUCTION_CANDIDATE: technically suitable, not wired.
PRODUCTION_APPROVED: intentionally selected for runtime.
EXISTING_PIPELINE: already supplied by stable offline artwork architecture.

## Screen map
Home: NIN-TOD-01 + NIN-ZEL-01
Journey: NIN-ZEL-01 + NIN-UX-01
Pokedex: PKM-HOM-01 + NIN-UX-01
Game Dex: PKM-HOM-01 + NIN-ZEL-01
Collection/Box: PKM-HOM-01/02
Detail: PKM-HOM-01 + internal contextual game art
Evolution: NIN-UX-01 + internal Pokemon art/data
Best Route: NIN-ZEL-01
Search: NIN-MUS-01/02 + NIN-STR-01
Settings: NIN-MUS-02 + NIN-STR-01

## Stage 0 gate
Current source inventory; reference structure; in-repo runtime-art inventory; screen reference map; visual audit checklist; no runtime pipeline changes; explicit review before any reference asset is promoted into runtime.
