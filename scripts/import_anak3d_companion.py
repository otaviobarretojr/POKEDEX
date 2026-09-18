#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import shutil
import struct
import subprocess
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path

MODEL_UID = "a90e7b35382846af92ddb557b5b6d372"
MODEL_PAGE = (
    "https://sketchfab.com/3d-models/"
    "0025-pikachu-3d-a90e7b35382846af92ddb557b5b6d372"
)
MODEL_AUTHOR = "anak3d (@anak3d1234)"
MODEL_LICENSE = "CC BY"
DEST = Path("app/src/main/assets/models/pikachu_companion/Pikachu_anak3d.glb")
SOURCE_COPY = Path("app/src/main/assets/models/pikachu_companion/Pikachu_anak3d_source.glb")

def parse_glb(path: Path) -> dict:
    data = path.read_bytes()
    if len(data) < 20 or data[:4] != b"glTF":
        raise RuntimeError(f"{path} is not a GLB 2.0 file")
    version, declared_length = struct.unpack_from("<II", data, 4)
    if version != 2 or declared_length != len(data):
        raise RuntimeError(
            f"Invalid GLB header: version={version}, declared={declared_length}, actual={len(data)}"
        )
    json_length, json_type = struct.unpack_from("<II", data, 12)
    if json_type != 0x4E4F534A:
        raise RuntimeError("First GLB chunk is not JSON")
    payload = data[20:20 + json_length].rstrip(b"\x00 \t\r\n")
    return json.loads(payload.decode("utf-8"))

def audit(path: Path, label: str) -> dict:
    gltf = parse_glb(path)
    meshes = gltf.get("meshes", [])
    skins = gltf.get("skins", [])
    materials = gltf.get("materials", [])
    images = gltf.get("images", [])
    textures = gltf.get("textures", [])
    animations = [a.get("name", "") for a in gltf.get("animations", [])]

    textured_materials = sum(
        1
        for material in materials
        if material.get("pbrMetallicRoughness", {}).get("baseColorTexture")
        or material.get("normalTexture")
        or material.get("emissiveTexture")
        or material.get("occlusionTexture")
    )

    if not meshes:
        raise RuntimeError(f"{label}: no meshes found")
    if not skins:
        raise RuntimeError(f"{label}: no skin/rig found")
    if not animations:
        raise RuntimeError(f"{label}: no animation clips found")
    if not images and not textures and not textured_materials:
        raise RuntimeError(f"{label}: no texture resources found")

    print(f"{label}_AUDIT=OK")
    print(f"{label}_BYTES={path.stat().st_size}")
    print(
        f"{label}_MESHES={len(meshes)} SKINS={len(skins)} "
        f"MATERIALS={len(materials)} IMAGES={len(images)} TEXTURES={len(textures)}"
    )
    print(f"{label}_ANIMATIONS=" + ",".join(animations))
    return gltf

def download_from_sketchfab(token: str, destination: Path) -> None:
    api_url = f"https://api.sketchfab.com/v3/models/{MODEL_UID}/download"
    request = urllib.request.Request(
        api_url,
        headers={
            "Authorization": f"Token {token}",
            "User-Agent": "POKEDEX-Companion-Importer/1.0",
        },
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        metadata = json.loads(response.read().decode("utf-8"))

    candidate = metadata.get("glb") or metadata.get("gltf")
    if not candidate or not candidate.get("url"):
        raise RuntimeError("Sketchfab did not return a downloadable GLB/GLTF archive")

    with tempfile.TemporaryDirectory() as td:
        tmp = Path(td)
        payload = tmp / "download"
        with urllib.request.urlopen(candidate["url"], timeout=180) as response:
            payload.write_bytes(response.read())

        if payload.read_bytes()[:4] == b"glTF":
            shutil.copy2(payload, destination)
            return

        if not zipfile.is_zipfile(payload):
            raise RuntimeError("Sketchfab payload is neither GLB nor ZIP")

        with zipfile.ZipFile(payload) as archive:
            archive.extractall(tmp / "archive")
        glbs = sorted((tmp / "archive").rglob("*.glb"))
        if not glbs:
            raise RuntimeError(
                "Sketchfab returned a glTF package without GLB. "
                "Export it to GLB before importing."
            )
        shutil.copy2(glbs[0], destination)

def optimize(source: Path, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as td:
        tmp = Path(td)
        step1 = tmp / "resize.glb"
        step2 = tmp / "dedup.glb"
        step3 = tmp / "prune.glb"

        commands = [
            [
                "npx", "--yes", "@gltf-transform/cli@4.3.0",
                "resize", str(source), str(step1),
                "--width", "1024", "--height", "1024",
            ],
            [
                "npx", "--yes", "@gltf-transform/cli@4.3.0",
                "dedup", str(step1), str(step2),
            ],
            [
                "npx", "--yes", "@gltf-transform/cli@4.3.0",
                "prune", str(step2), str(step3),
            ],
        ]

        for command in commands:
            print("+", " ".join(command))
            subprocess.run(command, check=True)

        shutil.copy2(step3, destination)

def main() -> int:
    parser = argparse.ArgumentParser(
        description="Import and audit the anak3d Pikachu companion model."
    )
    parser.add_argument(
        "--input",
        type=Path,
        help="Downloaded GLB from the official anak3d Sketchfab model.",
    )
    parser.add_argument(
        "--skip-optimize",
        action="store_true",
        help="Copy the source GLB without glTF-Transform optimization.",
    )
    args = parser.parse_args()

    print(f"MODEL_UID={MODEL_UID}")
    print(f"MODEL_PAGE={MODEL_PAGE}")
    print(f"MODEL_AUTHOR={MODEL_AUTHOR}")
    print(f"MODEL_LICENSE={MODEL_LICENSE}")

    SOURCE_COPY.parent.mkdir(parents=True, exist_ok=True)

    if args.input:
        if not args.input.is_file():
            raise RuntimeError(f"Input not found: {args.input}")
        shutil.copy2(args.input, SOURCE_COPY)
    else:
        token = os.environ.get("SKETCHFAB_API_TOKEN", "").strip()
        if not token:
            raise RuntimeError(
                "The anak3d file is download-protected by Sketchfab. "
                "Pass --input <downloaded.glb> or set SKETCHFAB_API_TOKEN."
            )
        download_from_sketchfab(token, SOURCE_COPY)

    audit(SOURCE_COPY, "ANAK3D_SOURCE")

    if args.skip_optimize:
        shutil.copy2(SOURCE_COPY, DEST)
    else:
        optimize(SOURCE_COPY, DEST)

    audit(DEST, "ANAK3D_FINAL")
    print(f"ANAK3D_DEST={DEST}")
    return 0

if __name__ == "__main__":
    sys.exit(main())
