# POKEDEX - Nintendo Companion Visual Roadmap

Status: ACTIVE
Created: 2026-09-15
Baseline: v20.12.1 Trainer Companion Polish

## Goal
Evolve the existing POKEDEX into a premium, context-first Pokemon companion inspired by Nintendo UI/UX principles, without rebuilding the project or destabilizing its offline architecture.

## Architecture freeze
Preserve:
- bootstrap, initial download, SHA/integrity, extraction and offline library
- offline artwork pipeline
- CollectionStore as the single collection/capture source
- existing Journey engines and progress stores
- existing navigation architecture

Do not restore manual per-game downloads or a second installation system.
Paused: Teams/My Team and Shiny Hunter 2.0.

## Nintendo Companion Design Language
1. World before interface: artwork, game, region and Pokemon identity create atmosphere; chrome stays restrained.
2. One screen, one primary question.
3. Progressive disclosure: contextual actions only where useful.
4. Active game gives personality: Paldea, Hisui, Galar etc. may influence artwork, accent, subtle surfaces and headers without separate architectures.
5. Motion supports feedback, not decoration.
6. Premium means hierarchy, memory, context, whitespace and meaningful imagery rather than excessive cards, gradients and shadows.

Screen responsibilities:
- Home/Companion: Where am I and where do I go now?
- Journey: What do I do now?
- Pokedex: What exists and what is missing?
- Game Dex: What exists/is missing in this game/region?
- Planner: How do I obtain what is missing?
- Collection: What do I own?
- Box: How did I organize my Pokemon?
- Evolution: What/how can I evolve?
- Search: Where can I find this information?
- Settings: How is the app configured?

## Current baseline to preserve
Home/Companion already has active-game context, next objective, unified progress, Journey progress, real Game Dex progress, regional progress where applicable, one dominant Continue Journey action, and no redundant shortcut grid.

# Execution stages

## Stage 0 - Baseline lock and visual inventory
- Confirm HEAD/version/green CI.
- Inventory main screens/shared components.
- Record tokens: spacing, radius, typography, motion, game/type colors.
- Find duplicated cards, headers, search and filter implementations.
- Define visual-regression checklist for phone widths and light/dark mode.
Exit: no functional change; baseline green; every screen mapped to its responsibility.

## Stage 1 - Nintendo Companion Design System
Create/evolve reusable primitives:
- GameContextHeader
- PokemonHeroHeader
- CompanionSectionHeader
- PokemonGridCard
- ProgressRing
- EvolutionPathCard
- PremiumSearchBar
- PremiumEmptyState
- ContextChip/filter controls
- consistent loading/skeleton treatment
- shared motion specifications
Rules: extend current Material 3/tokens, no parallel theme, performant in Lazy lists, accessible touch targets.
Exit: primitives compile; existing screens remain functional; no offline/data changes.

## Stage 2 - Five primary tabs
### 2A Home/Companion
Preserve current hierarchy; refine spacing/artwork/progress; subtle game/region atmosphere; one dominant action; never restore shortcut grid.
### 2B Pokedex
Editorial contextual header; expressive Pokemon cards; elegant registered/missing state; stronger type badges/artwork hierarchy; compact filters.
### 2C Collection
Personal ownership feel; captured Pokemon as protagonists; subtle shiny treatment; no duplicate Home statistics; strong empty/completion states.
### 2D Box
Preserve 6x5/30-slot model; improve Box selector, selection feedback and storage atmosphere; keep dense organization readable.
### 2E Settings
Calmer than Pokemon screens. Group Experience, Offline Content, Data/Storage and About. Reduce heavy card stacking and re-audit scrolling.
Exit: five tabs share one language; navigation stays five tabs; no responsibility duplication.

## Stage 3 - Pokemon experience surfaces
### 3A Pokemon Detail
Pokemon-first hero, type-derived atmosphere, content as chapters of one illustrated profile.
### 3B Game Dex
Region-aware compact hero; registered/total/progress; multi-dex games adapt correctly; practical filters.
### 3C Capture Planner / Best Route
Visual route/timeline. Priority: capture by location -> evolve -> transfer -> trade/special -> unconfirmed. Never invent locations.
### 3D Evolution Center
Pokemon -> condition -> Pokemon visual paths; multi-stage chains; active-game context; preserve resolution engines.
### 3E Universal Search
Premium integrated search; group Pokemon/moves/abilities/items; preserve debounce/performance.
Exit: every tool visually answers its primary question; no parallel stores; game-aware context preserved.

## Stage 4 - Contextual worlds
Define restrained context profiles for supported games/regions. Use existing approved/offline artwork only. Context may affect accent, hero artwork, subtle surface/background treatment and transitions. Layout behavior stays consistent.
Exit: game switching changes atmosphere, not architecture; artwork pipeline unchanged.

## Stage 5 - Motion, touch and delight
Standardize screen transitions, selection feedback, animated progress, context transitions and press states. Respect reduced motion. No continuous decorative animation.
Exit: motion never blocks navigation and causes no meaningful scroll regression.

## Stage 6 - Responsive, dark mode and accessibility
Audit small/tall/large phones, system navigation insets, light/dark mode, text scaling, contrast, touch targets, descriptions, long Portuguese labels/localization.
Exit: no clipping/overlap; accessibility guard green.

## Stage 7 - Performance polish
Audit recompositions, Lazy keys/contentType, artwork loading, main-thread IO, repeated CollectionStore/GameDex/CapturePlanner work, derived state/remember, Settings scroll, Search filtering and large lists.
Exit: reject visual features that create meaningful scroll/input regression.

## Stage 8 - Final product audit
Visual audit -> navigation audit -> duplication/responsibility audit -> contextual game-data audit -> performance -> accessibility -> architecture/hardening/artwork/Journey/National Dex/Forms guards -> unit tests -> Android Lint -> build -> full CI 100% green -> newly validated update-safe APK only.

## Quality gate per stage
IMPLEMENT -> AUDIT -> CORRECT -> AUDIT AGAIN -> TEST -> RELEVANT GUARDS/LINT -> BUILD/CI.
Do not accumulate multiple unvalidated architecture changes before checking compilation.

## Anti-patterns
Avoid dashboard duplication, Home shortcut grids, card-inside-card everywhere, excessive glassmorphism, strong gradients everywhere, constant glow/particles, decorative motion, giant headers, repeated game names and unsupported game data presented as fact.

## Nintendo research principles - 2026
Official Nintendo materials emphasize comfortable player experience, repeated-use usability, experience/use-scenario design, game imagery as memory cues, contextual/theme discovery, franchise personalization, game-specific companion experiences, and incremental app refinement.

Official references:
- Nintendo mobile apps page
- Nintendo Switch App page
- Nintendo UI/UX design recruitment article
- Nintendo Music design article about designing around memories
- Nintendo Switch App update history
- Nintendo Music 2026 update article

## Resume protocol
For a new chat:
1. Open this roadmap first.
2. Read latest commits and CI runs.
3. Identify the last completed stage and its green pipeline.
4. Continue from the first incomplete stage.
5. Never recreate the project or restart completed stages.

## Companion Home architecture - 2026-09-18
- HOME is a fixed companion surface; SceneView/Filament must not live inside Journey LazyColumn content.
- Games owns Journey discovery and continuation.
- The active game card shows the next objective and continues directly to the Journey route.
- Companion world context follows active game + active region (Lumiose/Hyperspace, Paldea/Kitakami/Blueberry, Galar/Isle of Armor/Crown Tundra, Hisui, Kanto, Sinnoh).
- Keep Pikachu as the persistent protagonist; change the world behind it rather than duplicating companion systems.
