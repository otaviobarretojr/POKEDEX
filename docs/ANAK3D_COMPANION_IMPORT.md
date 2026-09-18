# anak3d Pikachu Companion Import

Production target for the next Companion iteration:

- **Model:** 0025 Pikachu 3D
- **Author:** anak3d (@anak3d1234)
- **Sketchfab UID:** `a90e7b35382846af92ddb557b5b6d372`
- **Model page:** https://sketchfab.com/3d-models/0025-pikachu-3d-a90e7b35382846af92ddb557b5b6d372
- **License shown by Sketchfab:** CC Attribution
- **Published by the author as:** modeled, textured and animated in Blender
- **Approximate source complexity shown by Sketchfab:** 26.7k triangles / 13.4k vertices

This is intentionally not wired into the Android runtime until the original downloadable asset has been imported and audited successfully.

## Import paths

### Existing downloaded GLB

```bash
python3 scripts/import_anak3d_companion.py --input /path/to/downloaded/pikachu.glb
```

### Sketchfab API token

```bash
SKETCHFAB_API_TOKEN=... python3 scripts/import_anak3d_companion.py
```

The importer:

1. validates GLB 2.0 structure;
2. requires at least one mesh, skin/rig, animation clip and texture resource;
3. keeps an untouched local source copy as `Pikachu_anak3d_source.glb`;
4. resizes textures to 1024px;
5. deduplicates and prunes the GLB without Draco/Meshopt compression;
6. audits the final `Pikachu_anak3d.glb` again.

Compression extensions are intentionally avoided for the first Android/Filament validation.

The existing Companion renderer stays unchanged until this file passes the import audit and a physical-device static render test.

Pokémon/Pikachu character IP rights are separate from the model author's CC attribution license.
