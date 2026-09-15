# Stage 1 - Nintendo Companion Design System
Status: READY FOR IMPLEMENTATION
Research baseline: 2026-09-15

## Objective
Extend the existing Material 3 + PokedexDesignTokens system into a shared Nintendo Companion visual layer. Do not create a parallel theme and do not change data/offline architecture.

## Existing foundation verified
PokedexDesignTokens already provides:
- semantic light/dark colors
- Pokemon type colors
- game accent mapping
- spacing scale
- radius scale
- elevation scale
- motion durations
- Journey artwork dimensions
- shared Shapes and Typography

Journey/Home already demonstrates the desired direction with game-context gradient, large contextual artwork, progress visualization and one dominant Continue Journey action.

Box already demonstrates dense-content constraints: 30-slot mental model, game/region context, circular Dex progress and selection interaction.

## First-party Nintendo rules translated into code
1. Less chrome: primitives must not force Card containers. Prefer optional surface/background.
2. At-a-glance state: headers expose one primary context and at most one compact progress state.
3. Contextual help: explanatory copy is optional and appears only when state requires it.
4. Artwork as memory cue: hero primitives accept semantic artwork slots/handles, never raw remote URLs.
5. One-action clarity: primary action component has one visually dominant CTA.
6. Motion is state feedback: use existing Fast/Standard/Emphasis durations; no infinite decorative animation.
7. Graphical communication: progress, evolution and route relationships get visual primitives before extra prose.

## Components

### CompanionContextHeader
Purpose: Home/Journey/Game Dex contextual header.
Inputs: eyebrow, title, optional subtitle, accent, optional artwork slot, optional compact progress.
Behavior: collapsible/compact-friendly; no giant mandatory height.
Use: Home, Journey, Game Dex.
Do not use: Settings.

### PokemonHeroHeader
Purpose: Pokemon Detail.
Inputs: Pokemon identity, type accent(s), artwork slot, number/name/types, optional status.
Rule: Pokemon is protagonist; content remains readable without artwork.

### CompanionSectionHeader
Purpose: consistent section hierarchy without creating a Card.
Inputs: title, optional supporting label/action.
Use globally.

### PokemonGridCard
Purpose: Pokedex/Collection shared species presentation.
Inputs: artwork slot, id/name/types, owned/missing state, optional shiny/form state.
Modes: discovery, owned, compact.
Performance: stable layout; no IO or data lookup inside component.

### CompanionProgress
Modes: ring, linear, compact text.
Inputs: current,total,accent,label.
Rule: calculation supplied by caller; component only renders.

### EvolutionPathCard
Purpose: Pokemon -> condition -> Pokemon.
Supports multi-stage composition without fetching evolution data itself.

### PremiumSearchBar
Purpose: Universal Search.
Inputs: query/onQueryChange, state, optional category context.
Preserve caller debounce; component never launches search.

### CompanionEmptyState
Purpose: empty/loading-recovery states with optional contextual artwork/icon.
Rule: concise explanation + at most one recovery action.

### ContextFilter
Purpose: compact filters/tabs/chips.
Rule: filters must change the current question; no redundant filter collections.

### PrimaryCompanionAction
Purpose: one dominant next action.
Use sparingly; screen should rarely contain two competing primary actions.

## Surface intensity
High: Home, Pokemon Detail.
Medium: Journey, Pokedex, Game Dex, Evolution.
Low: Collection, Box.
Minimal: Settings/maintenance.

## Token extensions planned
Do not hardcode per-screen values when semantic tokens suffice.
Add semantic alpha tokens for:
- contextualBackdrop
- subtleAccentSurface
- artworkFade
- disabled/missingArtwork
Add semantic component dimensions only after two or more screens need the same value.
Do not add arbitrary game colors if Colors.game already resolves the context.

## Performance contract
- shared components are stateless where practical
- callers precompute counts/sets
- Lazy items use stable keys
- no repository/service calls from visual primitives
- artwork slots use existing PokemonArtwork/Journey artwork pipeline
- no new AsyncImage remote reference path
- animations target primitive state only
- avoid nested animateContentSize in large grids

## Accessibility contract
- 48dp interactive targets unless compact non-touch visual
- meaningful content descriptions for actionable imagery
- text contrast independent of artwork
- missing/owned state not communicated by color alone
- reduced-motion compatible behavior

## Implementation order
1. Add semantic visual tokens without changing existing values.
2. Implement CompanionSectionHeader + CompanionProgress.
3. Implement ContextFilter + PrimaryCompanionAction.
4. Implement PokemonGridCard.
5. Implement CompanionContextHeader.
6. Implement PokemonHeroHeader.
7. Implement EvolutionPathCard.
8. Implement PremiumSearchBar + CompanionEmptyState.
9. Compile and run unit/architecture guards.
10. Only then begin Stage 2 screen migration.

## Stage 1 exit gate
- no functional navigation/data changes
- no offline/bootstrap/artwork-pipeline changes
- components compile
- accessibility semantics preserved
- no duplicate visual data source
- existing Home/Box remain functional
- unit tests + lint/guards green
