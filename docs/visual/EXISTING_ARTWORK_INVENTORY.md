# Existing Artwork Inventory
Audit date: 2026-09-15
Status: PARTIAL - repository code-search index did not expose binary/resource listings reliably, so this document records the verified architectural inventory and the remaining filesystem-level enumeration task.

## Frozen architecture
The stable app already has an offline artwork/content pipeline. Visual work must consume that pipeline and must not create a second downloader, second image cache, or a new bootstrap path.

## Known production-art roles from the current UI architecture
- Pokemon official artwork/sprites: Pokemon grids, collection, details, evolution relationships.
- Game cover/artwork: Journey/Home active-game identity and game selectors.
- Type identity: type-derived colors/badges and Pokemon-detail atmosphere.
- Game identity: game-derived accent colors already represented by PokedexDesignTokens.
- Region/game context: supplied through existing game/region model and current artwork where available.

## Runtime classification rules
EXISTING_PIPELINE: already available through stable offline content/artwork.
REFERENCE_ONLY: visual study under docs/visual/reference-assets.
PRODUCTION_CANDIDATE: reference art intentionally considered for a screen.
PRODUCTION_APPROVED: explicitly reviewed for runtime inclusion.

## Do not do
- Do not point Compose directly at Nintendo reference URLs.
- Do not add reference screenshots to res/drawable simply because they are available.
- Do not change bootstrap/download/SHA/extraction to support the visual redesign.
- Do not duplicate Pokemon artwork in a new visual-only package.
- Do not perform network image loading during normal offline flows.

## Stage-1 consumption contract
Shared premium components receive semantic art/context inputs (Pokemon, game, region, type, cover/artwork handle) and resolve through the existing app services. They do not own downloading.

## Remaining enumeration before Stage 1 closes
A filesystem/resource-tree audit should enumerate exact packaged image files, dimensions and byte sizes where the GitHub connector can expose them. Until then, no assumption about a specific binary asset being locally present is allowed; components must use existing resolvers/models already proven by the app.
