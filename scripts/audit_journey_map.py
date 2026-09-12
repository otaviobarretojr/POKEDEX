#!/usr/bin/env python3
from pathlib import Path
import base64, re
from PIL import Image, ImageDraw, ImageFont

root=Path(__file__).resolve().parents[1]
parts=[]
for i in range(17):
    p=root/f"app/src/main/assets/maps/paldea_journey_map_{i:02d}.b64"
    parts.append(p.read_text(encoding="utf-8").strip())
raw=base64.b64decode("".join(parts), validate=True)
out=root/"journey-map-audit"
out.mkdir(exist_ok=True)
webp=out/"paldea-map.webp"
webp.write_bytes(raw)
img=Image.open(webp).convert("RGBA")
w,h=img.size
src=(root/"app/src/main/java/com/otaviobarreto/pokedex/data/JourneyMapCatalog.kt").read_text(encoding="utf-8")
pat=re.compile(r'JourneyMapPoint\(\s*"(sv-\d+)"\s*,\s*([0-9.]+)f\s*,\s*([0-9.]+)f\s*\)')
points=[(m.group(1),float(m.group(2)),float(m.group(3))) for m in pat.finditer(src)]
if len(points)!=18:
    raise SystemExit(f"expected 18 Paldea map points, found {len(points)}")
draw=ImageDraw.Draw(img)
radius=max(18,int(min(w,h)*0.032))
for step,x,y in points:
    px=int(round(x*w)); py=int(round(y*h))
    draw.ellipse((px-radius,py-radius,px+radius,py+radius),outline=(255,0,0,255),width=max(3,radius//7))
    draw.line((px-radius,py,px+radius,py),fill=(255,255,255,255),width=2)
    draw.line((px,py-radius,px,py+radius),fill=(255,255,255,255),width=2)
    label=step.replace("sv-","")
    box=(px+radius+3,py-radius)
    draw.rectangle((box[0]-2,box[1]-2,box[0]+30,box[1]+16),fill=(0,0,0,190))
    draw.text(box,label,fill=(255,255,255,255))
img.save(out/"paldea-hitbox-audit.png")
print(f"Paldea hitbox audit generated: {w}x{h}, {len(points)} points")
