#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import os
import re
import shutil
import threading
import time
import zipfile
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Any

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

API = "https://pokeapi.co/api/v2"
RAW_ART = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork"
MAX_ID = 1025
PACKAGE_VERSION = 20
WORKERS = int(os.environ.get("POKEDEX_PACKAGE_WORKERS", "12"))

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "build" / "offline-package-general"
API_DIR = BUILD / "api"
IMG_DIR = BUILD / "images"
DIST = ROOT / "dist"
ZIP_PATH = DIST / f"pokedex-general-v{PACKAGE_VERSION}.zip"
META_PATH = DIST / f"pokedex-general-v{PACKAGE_VERSION}.meta.json"

REFERENCE_URLS = [
    f"{API}/move?limit=2500",
    f"{API}/ability?limit=2500",
    f"{API}/item?limit=2500",
]

thread_local = threading.local()
cache_lock = threading.Lock()
written_urls: dict[str, str] = {}
written_images: dict[str, str] = {}


def session() -> requests.Session:
    if not hasattr(thread_local, "session"):
        s = requests.Session()
        retry = Retry(
            total=5,
            connect=5,
            read=5,
            status=5,
            backoff_factor=0.5,
            status_forcelist=(429, 500, 502, 503, 504),
            allowed_methods=frozenset({"GET"}),
        )
        adapter = HTTPAdapter(max_retries=retry, pool_connections=32, pool_maxsize=32)
        s.mount("https://", adapter)
        s.headers.update({"User-Agent": "POKEDEX-offline-package-builder/20"})
        thread_local.session = s
    return thread_local.session


def sha(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def get_bytes(url: str) -> bytes:
    r = session().get(url, timeout=(15, 45))
    r.raise_for_status()
    return r.content


def get_json_bytes(url: str) -> tuple[dict[str, Any] | list[Any], bytes]:
    raw = get_bytes(url)
    return json.loads(raw), raw


def write_api(url: str, raw: bytes) -> str:
    with cache_lock:
        existing = written_urls.get(url)
        if existing:
            return existing
        rel = f"api/{sha(url)}.json"
        target = BUILD / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        tmp = target.with_suffix(".tmp")
        tmp.write_bytes(raw)
        tmp.replace(target)
        written_urls[url] = rel
        return rel


def extension_for(url: str, content_type: str | None = None) -> str:
    lower = url.lower().split("?")[0]
    for ext in (".png", ".webp", ".jpg", ".jpeg"):
        if lower.endswith(ext):
            return ext
    if content_type:
        if "webp" in content_type:
            return ".webp"
        if "jpeg" in content_type:
            return ".jpg"
    return ".png"


def write_image(url: str) -> str:
    with cache_lock:
        existing = written_images.get(url)
        if existing:
            return existing

    r = session().get(url, timeout=(15, 60))
    r.raise_for_status()
    ext = extension_for(url, r.headers.get("Content-Type"))
    rel = f"images/{sha(url)}{ext}"
    target = BUILD / rel
    target.parent.mkdir(parents=True, exist_ok=True)

    with cache_lock:
        existing = written_images.get(url)
        if existing:
            return existing
        tmp = target.with_suffix(target.suffix + ".tmp")
        tmp.write_bytes(r.content)
        tmp.replace(target)
        written_images[url] = rel
    return rel


def safe_form_key(raw: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", raw.lower()).strip("-")


def resource_entry(url: str, raw: bytes) -> dict[str, str]:
    return {"url": url, "path": write_api(url, raw)}


def fetch_resource(url: str) -> tuple[dict[str, Any] | list[Any], dict[str, str]]:
    data, raw = get_json_bytes(url)
    return data, resource_entry(url, raw)


def image_entry(cache_key: str, url: str) -> dict[str, str]:
    return {"cache_key": cache_key, "path": write_image(url), "source_url": url}


def optional_image_entry(cache_key: str, url: str) -> dict[str, str] | None:
    try:
        return image_entry(cache_key, url)
    except requests.RequestException:
        return None


def build_pokemon(species_id: int) -> dict[str, Any]:
    pokemon_url = f"{API}/pokemon/{species_id}"
    species_url = f"{API}/pokemon-species/{species_id}"
    encounters_url = f"{API}/pokemon/{species_id}/encounters"

    pokemon, pokemon_res = fetch_resource(pokemon_url)
    species, species_res = fetch_resource(species_url)
    _, encounters_res = fetch_resource(encounters_url)

    resources = [pokemon_res, species_res, encounters_res]
    resource_urls = {r["url"] for r in resources}

    evo_url = ((species.get("evolution_chain") or {}).get("url") or "").strip()
    if evo_url and evo_url not in resource_urls:
        _, evo_res = fetch_resource(evo_url)
        resources.append(evo_res)
        resource_urls.add(evo_url)

    default_art_url = f"{RAW_ART}/{species_id}.png"
    images = [image_entry(f"pokemon-offline-{species_id}", default_art_url)]

    varieties = species.get("varieties") or []
    for variety in varieties:
        p = variety.get("pokemon") or {}
        purl = (p.get("url") or "").strip()
        if not purl:
            continue
        try:
            pid = int(purl.rstrip("/").split("/")[-1])
        except ValueError:
            continue

        variety_json, variety_res = fetch_resource(purl)
        if purl not in resource_urls:
            resources.append(variety_res)
            resource_urls.add(purl)

        forms = variety_json.get("forms") or []
        official = (((variety_json.get("sprites") or {}).get("other") or {}).get("official-artwork") or {})

        if not forms:
            raw_name = (p.get("name") or str(pid)).strip()
            normal_url = official.get("front_default") or f"{RAW_ART}/{pid}.png"
            shiny_url = official.get("front_shiny") or f"{RAW_ART}/shiny/{pid}.png"
            key = safe_form_key(raw_name) or str(pid)
            normal_entry = optional_image_entry(
                f"pokemon-form-offline-{species_id}-{pid}-{key}-normal",
                normal_url,
            )
            shiny_entry = optional_image_entry(
                f"pokemon-form-offline-{species_id}-{pid}-{key}-shiny",
                shiny_url,
            )
            if normal_entry:
                images.append(normal_entry)
            if shiny_entry:
                images.append(shiny_entry)
            continue

        for form_ref in forms:
            raw_name = (form_ref.get("name") or p.get("name") or str(pid)).strip()
            form_url = (form_ref.get("url") or "").strip()
            form_json = None
            if form_url:
                try:
                    form_json, form_res = fetch_resource(form_url)
                    if form_url not in resource_urls:
                        resources.append(form_res)
                        resource_urls.add(form_url)
                except requests.RequestException:
                    form_json = None

            form_sprites = (form_json or {}).get("sprites") or {}
            normal_url = form_sprites.get("front_default") or official.get("front_default") or f"{RAW_ART}/{pid}.png"
            shiny_url = form_sprites.get("front_shiny") or official.get("front_shiny") or f"{RAW_ART}/shiny/{pid}.png"
            key = safe_form_key(raw_name) or str(pid)

            if normal_url:
                normal_entry = optional_image_entry(
                    f"pokemon-form-offline-{species_id}-{pid}-{key}-normal",
                    normal_url,
                )
                if normal_entry:
                    images.append(normal_entry)
            if shiny_url:
                shiny_entry = optional_image_entry(
                    f"pokemon-form-offline-{species_id}-{pid}-{key}-shiny",
                    shiny_url,
                )
                if shiny_entry:
                    images.append(shiny_entry)

    dedup_images = []
    seen_keys = set()
    for item in images:
        if item["cache_key"] not in seen_keys:
            seen_keys.add(item["cache_key"])
            dedup_images.append(item)

    return {
        "id": species_id,
        "resources": resources,
        "images": dedup_images,
    }


def main() -> None:
    if BUILD.exists():
        shutil.rmtree(BUILD)
    API_DIR.mkdir(parents=True, exist_ok=True)
    IMG_DIR.mkdir(parents=True, exist_ok=True)
    DIST.mkdir(parents=True, exist_ok=True)

    reference_resources = []
    for url in REFERENCE_URLS:
        _, entry = fetch_resource(url)
        reference_resources.append(entry)

    pokemon: list[dict[str, Any]] = []
    failures: list[tuple[int, str]] = []

    started = time.time()
    with ThreadPoolExecutor(max_workers=WORKERS) as pool:
        futures = {pool.submit(build_pokemon, i): i for i in range(1, MAX_ID + 1)}
        for index, future in enumerate(as_completed(futures), start=1):
            species_id = futures[future]
            try:
                pokemon.append(future.result())
            except Exception as exc:
                failures.append((species_id, str(exc)))
            if index % 25 == 0 or index == MAX_ID:
                print(f"[{index}/{MAX_ID}] concluídos; falhas={len(failures)}", flush=True)

    if failures:
        preview = "\n".join(f"#{pid}: {msg}" for pid, msg in failures[:30])
        raise RuntimeError(f"Falharam {len(failures)} Pokémon:\n{preview}")

    pokemon.sort(key=lambda x: x["id"])
    manifest = {
        "schema": 1,
        "package_key": "general",
        "package_version": PACKAGE_VERSION,
        "pokemon_count": len(pokemon),
        "reference_resources": reference_resources,
        "pokemon": pokemon,
    }
    (BUILD / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )

    if ZIP_PATH.exists():
        ZIP_PATH.unlink()
    with zipfile.ZipFile(ZIP_PATH, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
        for file in sorted(BUILD.rglob("*")):
            if file.is_file():
                zf.write(file, file.relative_to(BUILD).as_posix())

    digest = hashlib.sha256(ZIP_PATH.read_bytes()).hexdigest()
    size = ZIP_PATH.stat().st_size
    meta = {
        "package_key": "general",
        "version": PACKAGE_VERSION,
        "size_bytes": size,
        "sha256": digest,
        "file_name": ZIP_PATH.name,
        "elapsed_seconds": round(time.time() - started, 1),
    }
    META_PATH.write_text(json.dumps(meta, indent=2), encoding="utf-8")
    print(json.dumps(meta, indent=2))


if __name__ == "__main__":
    main()
