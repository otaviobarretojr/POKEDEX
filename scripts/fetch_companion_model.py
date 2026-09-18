#!/usr/bin/env python3
from __future__ import annotations

import json
import struct
import sys
import urllib.request
from pathlib import Path

SOURCE_URL = (
    "https://raw.githubusercontent.com/AryanShah874/Me/"
    "990a84ca937706dc88df36423bd5d991ca1e5d33/"
    "src/assets/3d/pikachu.glb"
)
EXPECTED_SIZE = 1_876_020
DEST = Path("app/src/main/assets/models/pikachu_companion/Pikachu_final.glb")

def parse_glb(path: Path) -> dict:
    data = path.read_bytes()
    if len(data) < 20 or data[:4] != b"glTF":
        raise RuntimeError("Companion model is not a valid GLB 2.0 file")
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

def main() -> int:
    DEST.parent.mkdir(parents=True, exist_ok=True)
    if not DEST.exists() or DEST.stat().st_size != EXPECTED_SIZE:
        tmp = DEST.with_suffix(".download")
        print(f"Downloading final Pikachu companion model from pinned source...")
        with urllib.request.urlopen(SOURCE_URL, timeout=60) as response:
            tmp.write_bytes(response.read())
        if tmp.stat().st_size != EXPECTED_SIZE:
            actual = tmp.stat().st_size
            tmp.unlink(missing_ok=True)
            raise RuntimeError(f"Unexpected Pikachu GLB size: {actual} != {EXPECTED_SIZE}")
        tmp.replace(DEST)

    gltf = parse_glb(DEST)
    animations = [a.get("name", "") for a in gltf.get("animations", [])]
    required = {"Idle", "Walking", "Dance"}
    missing = required.difference(animations)
    if missing:
        raise RuntimeError(f"Missing required companion animations: {sorted(missing)}")

    meshes = gltf.get("meshes", [])
    skins = gltf.get("skins", [])
    materials = gltf.get("materials", [])
    images = gltf.get("images", [])
    textures = gltf.get("textures", [])
    textured_materials = sum(
        1 for material in materials
        if material.get("pbrMetallicRoughness", {}).get("baseColorTexture")
        or material.get("normalTexture")
        or material.get("emissiveTexture")
    )

    if len(meshes) < 1 or len(skins) < 1:
        raise RuntimeError("Final companion model must contain a skinned mesh/rig")
    if len(animations) < 3:
        raise RuntimeError("Final companion model does not contain enough animation clips")
    if len(images) == 0 and len(textures) == 0 and textured_materials == 0:
        raise RuntimeError("Final companion model has no texture resources; refusing placeholder-quality asset")

    print("COMPANION_MODEL_AUDIT=OK")
    print(f"MODEL_BYTES={DEST.stat().st_size}")
    print(f"MESHES={len(meshes)} SKINS={len(skins)} MATERIALS={len(materials)}")
    print(f"IMAGES={len(images)} TEXTURES={len(textures)} TEXTURED_MATERIALS={textured_materials}")
    print("ANIMATIONS=" + ",".join(animations))
    return 0

if __name__ == "__main__":
    sys.exit(main())
