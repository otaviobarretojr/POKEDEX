#!/usr/bin/env python3
import csv, io, json, math, os, statistics, urllib.request
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

MAX_ID=1025
BASE="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/{id}.png"
OUT=Path("artwork-audit")
IMGDIR=OUT/"images"
OUT.mkdir(exist_ok=True); IMGDIR.mkdir(exist_ok=True)

def fetch(i):
    p=IMGDIR/f"{i}.png"
    if p.exists(): return p
    with urllib.request.urlopen(BASE.format(id=i), timeout=20) as r:
        p.write_bytes(r.read())
    return p

def metrics(i,p):
    im=Image.open(p).convert("RGBA")
    a=im.getchannel("A")
    bbox=a.getbbox()
    if not bbox:
        return dict(id=i,error="empty")
    l,t,r,b=bbox
    w,h=im.size
    bw,bh=r-l,b-t
    px=a.load()
    total=mass=sx=sy=0.0
    opaque=0
    for y in range(t,b):
        for x in range(l,r):
            av=px[x,y]
            if av>8:
                opaque+=1
                wt=av/255.0
                mass+=wt; sx+=x*wt; sy+=y*wt
    cx=sx/mass if mass else (l+r)/2
    cy=sy/mass if mass else (t+b)/2
    box_cx=(l+r)/2; box_cy=(t+b)/2
    return {
        "id":i,"canvas_w":w,"canvas_h":h,
        "left":l,"top":t,"right":r,"bottom":b,
        "bbox_w":bw,"bbox_h":bh,
        "bbox_fill":(bw*bh)/(w*h),
        "alpha_density":opaque/max(1,bw*bh),
        "aspect":bw/max(1,bh),
        "bbox_center_dx":(box_cx-w/2)/w,
        "bbox_center_dy":(box_cy-h/2)/h,
        "mass_center_dx":(cx-box_cx)/max(1,bw),
        "mass_center_dy":(cy-box_cy)/max(1,bh),
    }

rows=[]
for i in range(1,MAX_ID+1):
    try:
        rows.append(metrics(i,fetch(i)))
    except Exception as e:
        rows.append({"id":i,"error":str(e)})
        print("ERR",i,e)

valid=[r for r in rows if "error" not in r]
dens=[r["alpha_density"] for r in valid]
fills=[r["bbox_fill"] for r in valid]
med_den=statistics.median(dens); med_fill=statistics.median(fills)

for r in valid:
    # Optical centering suggestions after transparent-bounds crop.
    dx=-r["mass_center_dx"]*0.42
    dy=-r["mass_center_dy"]*0.42
    dx=max(-0.075,min(0.075,dx)); dy=max(-0.075,min(0.075,dy))
    # Thin/airy artworks look smaller even when bbox matches. Mildly compensate.
    density_ratio=med_den/max(r["alpha_density"],0.05)
    scale=math.sqrt(density_ratio)
    scale=max(0.92,min(1.12,scale))
    r["suggested_offset_x"]=round(dx,4)
    r["suggested_offset_y"]=round(dy,4)
    r["suggested_scale"]=round(scale,4)
    score=0.0
    score+=abs(r["mass_center_dx"])*4.0
    score+=abs(r["mass_center_dy"])*4.0
    score+=max(0,0.18-r["alpha_density"])*3.5
    score+=max(0,r["alpha_density"]-0.62)*1.8
    if r["aspect"]>1.9: score+=(r["aspect"]-1.9)*0.4
    if r["aspect"]<0.53: score+=(0.53-r["aspect"])*0.8
    r["outlier_score"]=round(score,4)
    r["needs_review"]=score>=0.22 or abs(dx)>=0.03 or abs(dy)>=0.03 or scale>=1.07 or scale<=0.95

with open(OUT/"artwork_metrics.csv","w",newline="",encoding="utf-8") as f:
    fields=sorted({k for r in rows for k in r.keys()})
    w=csv.DictWriter(f,fieldnames=fields);w.writeheader();w.writerows(rows)
json.dump(rows,open(OUT/"artwork_metrics.json","w"),indent=2)

outliers=sorted([r for r in valid if r["needs_review"]], key=lambda x:x["outlier_score"], reverse=True)
json.dump(outliers,open(OUT/"outliers.json","w"),indent=2)
print("valid",len(valid),"outliers",len(outliers),"median_density",med_den,"median_fill",med_fill)

# Contact sheets for manual visual curation, 30 artworks per page.
thumb=150; cols=6; rows_per=5
font=ImageFont.load_default()
for page in range((len(outliers)+29)//30):
    subset=outliers[page*30:(page+1)*30]
    sheet=Image.new("RGB",(cols*thumb,rows_per*(thumb+24)),"white")
    d=ImageDraw.Draw(sheet)
    for n,r in enumerate(subset):
        rr=n//cols; cc=n%cols
        im=Image.open(IMGDIR/f'{r["id"]}.png').convert("RGBA")
        a=im.getchannel("A"); bbox=a.getbbox()
        if bbox: im=im.crop(bbox)
        im.thumbnail((thumb-16,thumb-16),Image.Resampling.LANCZOS)
        x=cc*thumb+(thumb-im.width)//2
        y=rr*(thumb+24)+(thumb-im.height)//2
        sheet.paste(im,(x,y),im)
        d.text((cc*thumb+4,rr*(thumb+24)+thumb+3),f'#{r["id"]} s={r["outlier_score"]:.2f}',fill="black",font=font)
    sheet.save(OUT/f"outliers_{page+1:02d}.jpg",quality=88)
