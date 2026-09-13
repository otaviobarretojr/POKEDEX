#!/usr/bin/env python3
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
props_path = root / "ci/release.properties"
gradle_path = root / "app/build.gradle.kts"

props = {}
for line in props_path.read_text(encoding="utf-8").splitlines():
    if "=" in line:
        key, value = line.split("=", 1)
        props[key.strip()] = value.strip()

version_code = props["versionCode"]
version_name = props["versionName"]

gradle = gradle_path.read_text(encoding="utf-8")
gradle = re.sub(r'versionCode\s*=\s*\d+', f'versionCode = {version_code}', gradle)
gradle = re.sub(r'versionName\s*=\s*"[^"]+"', f'versionName = "{version_name}"', gradle)
gradle_path.write_text(gradle, encoding="utf-8")

print(f"Applied release version {version_name} ({version_code})")
