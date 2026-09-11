#!/usr/bin/env python3
import re, urllib.request, urllib.error
from pathlib import Path

root=Path(__file__).resolve().parents[1]
paths=[
 root/"app/src/main/java/com/otaviobarreto/pokedex/data/JourneyVisualAssetCatalog.kt",
 root/"app/src/main/java/com/otaviobarreto/pokedex/data/JourneyMapCatalog.kt",
]
urls=[]
for path in paths:
    text=path.read_text(encoding="utf-8")
    urls.extend(re.findall(r'https://[^"\s]+', text))

# keep deterministic order without duplicates
seen=set(); urls=[u for u in urls if not (u in seen or seen.add(u))]
errors=[]
for url in urls:
    try:
        req=urllib.request.Request(url,headers={"User-Agent":"Mozilla/5.0"})
        with urllib.request.urlopen(req,timeout=20) as r:
            status=getattr(r,"status",200)
            ctype=r.headers.get("Content-Type","")
            data=r.read(64)
            ok=status==200 and ("image" in ctype or data.startswith((b"\x89PNG",b"\xff\xd8\xff",b"RIFF")))
            print(f"{status} {ctype} {url}")
            if not ok: errors.append(f"{status} {ctype} {url}")
    except Exception as e:
        print("ERROR",url,e)
        errors.append(f"{url} :: {e}")

print(f"JOURNEY_VISUAL_URLS={len(urls)}")
print(f"JOURNEY_VISUAL_ERRORS={len(errors)}")
if errors:
    print("\n".join(errors))
    raise SystemExit(1)
