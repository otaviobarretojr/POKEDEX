#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import re
import shutil
import zipfile
from pathlib import Path
from typing import Any

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

API = "https://pokeapi.co/api/v2"
PACKAGE_VERSION = 20
ROOT = Path(__file__).resolve().parents[1]
DIST = ROOT / "dist" / "game-packages"
BUILD_ROOT = ROOT / "build" / "offline-game-packages"

GAMES = {
    "legends-za": {
        "label": "Pokémon Legends: Z-A",
        "regions": ["lumiose-city", "hyperspace"],
        "hero_ids": [448],
    },
    "scarlet-violet": {
        "label": "Scarlet / Violet",
        "regions": ["paldea", "kitakami", "blueberry"],
        "hero_ids": [1007, 1008],
    },
    "sword-shield": {
        "label": "Sword / Shield",
        "regions": ["galar", "isle-of-armor", "crown-tundra"],
        "hero_ids": [888, 889],
    },
    "lets-go": {
        "label": "Let's Go Pikachu / Eevee",
        "regions": ["letsgo-kanto"],
        "hero_ids": [25, 133],
    },
    "legends-arceus": {
        "label": "Legends Arceus",
        "regions": ["hisui"],
        "hero_ids": [493],
    },
    "bdsp": {
        "label": "Brilliant Diamond / Shining Pearl",
        "regions": ["original-sinnoh"],
        "hero_ids": [483, 484],
    },
    "firered-leafgreen": {
        "label": "FireRed / LeafGreen",
        "regions": ["kanto"],
        "hero_ids": [6, 3],
    },
}

COVERS = {
    "legends-za": [
        "https://legends.pokemon.com/images/box-art/poke-legends-box-art-NS-UKV-2x.png",
    ],
    "sword-shield": [
        "https://swordshield.pokemon.com/assets/img/common/packshot/en-gb/packshot_sword.png",
        "https://swordshield.pokemon.com/assets/img/common/packshot/en-gb/packshot_shield.png",
    ],
    "lets-go": [
        "https://pokemonletsgo.pokemon.com/assets/img/en-us/packshot-pikachu.png",
        "https://pokemonletsgo.pokemon.com/assets/img/en-us/packshot-eevee.png",
    ],
    "legends-arceus": [
        "https://www.nintendo.com/ph/switch/aw7k/img/hero_sp.jpg",
    ],
    "bdsp": [
        "https://diamondpearl.pokemon.com/en-us/assets/boxart_bd.png",
        "https://diamondpearl.pokemon.com/en-us/assets/boxart_sp.png",
    ],
    "firered-leafgreen": [
        "https://assets.nintendo.com/image/upload/ar_16%3A9%2Cb_auto%3Aborder%2Cc_lpad/b_white/f_auto/q_auto/dpr_1.5/store/software/switch/70010000118613/86f3c6d4e129d185cbbc441169856733dea45ff231446a4edf5fec4624041849",
        "https://assets.nintendo.com/image/upload/ar_16%3A9%2Cb_auto%3Aborder%2Cc_lpad/b_white/f_auto/q_auto/dpr_1.5/store/software/switch/70010000118618/af2c94d85c4b3bc671d0a88d3c0ea4b1eb96a8b5d45c758ceea10e29a0ffda44",
    ],
}

ART = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"
DLC_OFFICIAL = "https://www.pokemon.co.jp/ex/sv_dlc/assets/img/character/"
TYPE_BASE = "https://pokesprite.tootaio.com/sprites/types/generation-ix/scarlet-violet/"

session = requests.Session()
retry = Retry(
    total=4,
    connect=4,
    read=4,
    status=4,
    backoff_factor=0.6,
    status_forcelist=(429, 500, 502, 503, 504),
    allowed_methods=frozenset({"GET"}),
)
adapter = HTTPAdapter(max_retries=retry, pool_connections=20, pool_maxsize=20)
session.mount("https://", adapter)
session.headers.update({"User-Agent": "POKEDEX-game-package-builder/20"})


def get(url: str, timeout=(15, 60)) -> requests.Response:
    r = session.get(url, timeout=timeout)
    r.raise_for_status()
    return r


def safe_name(url: str, suffix: str = "") -> str:
    return hashlib.sha256(url.encode("utf-8")).hexdigest() + suffix


def ext_from_response(url: str, response: requests.Response) -> str:
    clean = url.lower().split("?")[0]
    for ext in (".png", ".jpg", ".jpeg", ".webp"):
        if clean.endswith(ext):
            return ext
    ctype = response.headers.get("Content-Type", "")
    if "webp" in ctype:
        return ".webp"
    if "jpeg" in ctype:
        return ".jpg"
    return ".png"


def parse_visual_catalog() -> dict[str, list[str]]:
    path = ROOT / "app/src/main/java/com/otaviobarreto/pokedex/data/JourneyVisualAssetCatalog.kt"
    text = path.read_text(encoding="utf-8")
    result = {k: [] for k in GAMES}

    prefix_map = {
        "za-": "legends-za",
        "sv-": "scarlet-violet",
        "swsh-": "sword-shield",
        "lgpe-": "lets-go",
        "la-": "legends-arceus",
        "bdsp-": "bdsp",
        "frlg-": "firered-leafgreen",
    }

    for line in text.splitlines():
        m = re.search(r'"([^"]+)"\s+to\s+JourneyVisualAsset\(', line)
        if not m:
            continue
        step_id = m.group(1)
        key = next((game for prefix, game in prefix_map.items() if step_id.startswith(prefix)), None)
        if not key:
            continue

        literal_urls = re.findall(r'"(https://[^"]+)"', line)
        if literal_urls:
            result[key].append(literal_urls[0])
            continue

        art = re.search(r'ART\+"([^"]+)"', line)
        if art:
            result[key].append(ART + art.group(1))
            continue

        dlc = re.search(r'DLC_OFFICIAL\+"([^"]+)"', line)
        if dlc:
            result[key].append(DLC_OFFICIAL + dlc.group(1))
            continue

    return result


def build_game(package_key: str, cfg: dict[str, Any], catalog_visuals: dict[str, list[str]]) -> dict[str, Any]:
    root = BUILD_ROOT / package_key
    if root.exists():
        shutil.rmtree(root)
    (root / "regions").mkdir(parents=True, exist_ok=True)
    (root / "visuals").mkdir(parents=True, exist_ok=True)

    region_entries = []
    pokemon_ids: set[int] = set()

    for slug in cfg["regions"]:
        url = f"{API}/pokedex/{slug}"
        r = get(url, timeout=(15, 45))
        data = r.json()
        rel = f"regions/{slug}.json"
        (root / rel).write_bytes(r.content)
        region_entries.append({"slug": slug, "url": url, "path": rel})
        for entry in data.get("pokemon_entries", []):
            species_url = ((entry.get("pokemon_species") or {}).get("url") or "").rstrip("/")
            try:
                national_id = int(species_url.split("/")[-1])
            except (TypeError, ValueError):
                continue
            if 1 <= national_id <= 1025:
                pokemon_ids.add(national_id)

    visual_urls = []
    visual_urls.extend(COVERS.get(package_key, []))
    visual_urls.extend(ART + f"{pid}.png" for pid in cfg.get("hero_ids", []))
    visual_urls.extend(catalog_visuals.get(package_key, []))
    if package_key == "scarlet-violet":
        visual_urls.extend(TYPE_BASE + f"{i}.png" for i in range(1, 19))

    dedup = []
    seen = set()
    for url in visual_urls:
        if url not in seen:
            seen.add(url)
            dedup.append(url)

    visuals = []
    skipped = []
    for index, url in enumerate(dedup, start=1):
        try:
            r = get(url)
        except requests.RequestException as exc:
            skipped.append({"url": url, "error": str(exc)})
            continue
        ext = ext_from_response(url, r)
        rel = f"visuals/{safe_name(url)}{ext}"
        (root / rel).write_bytes(r.content)
        visuals.append({"url": url, "path": rel})

    manifest = {
        "schema": 1,
        "package_key": package_key,
        "game_label": cfg["label"],
        "package_version": PACKAGE_VERSION,
        "pokemon_ids": sorted(pokemon_ids),
        "regions": region_entries,
        "visuals": visuals,
        "skipped_optional_visuals": skipped,
    }
    (root / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )

    DIST.mkdir(parents=True, exist_ok=True)
    zip_path = DIST / f"pokedex-{package_key}-v{PACKAGE_VERSION}.zip"
    if zip_path.exists():
        zip_path.unlink()
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
        for file in sorted(root.rglob("*")):
            if file.is_file():
                zf.write(file, file.relative_to(root).as_posix())

    sha256 = hashlib.sha256(zip_path.read_bytes()).hexdigest()
    return {
        "package_key": package_key,
        "display_name": cfg["label"],
        "version": PACKAGE_VERSION,
        "size_bytes": zip_path.stat().st_size,
        "sha256": sha256,
        "file_name": zip_path.name,
        "pokemon_count": len(pokemon_ids),
        "region_count": len(region_entries),
        "visual_count": len(visuals),
        "skipped_visual_count": len(skipped),
    }


def main() -> None:
    BUILD_ROOT.mkdir(parents=True, exist_ok=True)
    DIST.mkdir(parents=True, exist_ok=True)

    catalog_visuals = parse_visual_catalog()
    metadata = []
    failures = []

    for package_key, cfg in GAMES.items():
        print(f"Construindo {package_key}...", flush=True)
        try:
            meta = build_game(package_key, cfg, catalog_visuals)
            metadata.append(meta)
            print(json.dumps(meta, ensure_ascii=False), flush=True)
        except Exception as exc:
            failures.append({"package_key": package_key, "error": str(exc)})
            print(f"ERRO {package_key}: {exc}", flush=True)

    (DIST / "game-packages-v20.meta.json").write_text(
        json.dumps({"packages": metadata, "failures": failures}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    if failures:
        raise RuntimeError("Falharam pacotes: " + ", ".join(f["package_key"] for f in failures))


if __name__ == "__main__":
    main()
