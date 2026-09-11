#!/usr/bin/env python3
import csv, json, math, re, statistics, urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

MAX_ID=1025
BASE="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/{id}.png"
OUT=Path("artwork-audit")
IMGDIR=OUT/"images"
CATALOG=Path("app/src/main/java/com/otaviobarreto/pokedex/ui/ArtworkTuningCatalog.kt")
OUT.mkdir(exist_ok=True); IMGDIR.mkdir(exist_ok=True)

def load_tunings():
    text=CATALOG.read_text(encoding="utf-8")
    out={}
    for m in re.finditer(r"(\d+)\s+to\s+ArtworkTuning\(([-0-9.]+)f,\s*([-0-9.]+)f,\s*([-0-9.]+)f\)",text):
        out[int(m.group(1))]=(float(m.group(2)),float(m.group(3)),float(m.group(4)))
    return out

def fetch(i):
    p=IMGDIR/f"{i}.png"
    if p.exists(): return p
    with urllib.request.urlopen(BASE.format(id=i), timeout=30) as r: p.write_bytes(r.read())
    return p

def metrics(i,p,tuning):
    im=Image.open(p).convert("RGBA")
    a=im.getchannel("A"); bbox=a.getbbox()
    if not bbox: return {"id":i,"error":"empty"}
    l,t,r,b=bbox; w,h=im.size; bw,bh=r-l,b-t
    px=a.load(); mass=sx=sy=0.0; opaque=0
    for y in range(t,b):
        for x in range(l,r):
            av=px[x,y]
            if av>8:
                opaque+=1; wt=av/255.0; mass+=wt; sx+=x*wt; sy+=y*wt
    cx=sx/mass if mass else (l+r)/2; cy=sy/mass if mass else (t+b)/2
    pad=max(2,int(max(bw,bh)*0.035))
    cl=max(0,l-pad); ct=max(0,t-pad); cr=min(w-1,r-1+pad); cb=min(h-1,b-1+pad)
    cw=cr-cl+1; ch=cb-ct+1; fit=1.0/max(cw,ch)
    scale,ox,oy=tuning
    crop_cx=cl+cw/2; crop_cy=ct+ch/2
    final_mass_x=(cx-crop_cx)*fit*scale+ox
    final_mass_y=(cy-crop_cy)*fit*scale+oy
    left_edge=(l-crop_cx)*fit*scale+ox; right_edge=(r-crop_cx)*fit*scale+ox
    top_edge=(t-crop_cy)*fit*scale+oy; bottom_edge=(b-crop_cy)*fit*scale+oy
    edge_clearance=min(0.5-abs(left_edge),0.5-abs(right_edge),0.5-abs(top_edge),0.5-abs(bottom_edge))
    density=opaque/max(1,bw*bh)
    return {
      "id":i,"alpha_density":density,"aspect":bw/max(1,bh),
      "final_mass_dx":final_mass_x,"final_mass_dy":final_mass_y,
      "edge_clearance":edge_clearance,"current_scale":scale,
      "current_offset_x":ox,"current_offset_y":oy
    }

tunings=load_tunings()
def audit_one(i):
    try: return metrics(i,fetch(i),tunings.get(i,(1.0,0.0,0.0)))
    except Exception as e: return {"id":i,"error":str(e)}
rows=[]
with ThreadPoolExecutor(max_workers=24) as pool:
    futures={pool.submit(audit_one,i):i for i in range(1,MAX_ID+1)}
    for future in as_completed(futures):
        row=future.result()
        if "error" in row: print("ERR",row["id"],row["error"])
        rows.append(row)
rows.sort(key=lambda r:r["id"])
valid=[r for r in rows if "error" not in r]
med_den=statistics.median(r["alpha_density"] for r in valid)

candidates=[]
for r in valid:
    curated=r["id"] in tunings
    residual=math.hypot(r["final_mass_dx"],r["final_mass_dy"])
    target_scale=max(0.975,min(1.045,math.sqrt(med_den/max(r["alpha_density"],0.05))))
    scale_delta=max(-0.012,min(0.012,target_scale-r["current_scale"]))
    # Protect the safe area if the first pass pushed artwork too close to an edge.
    if r["edge_clearance"] < 0.018:
        scale_delta=min(scale_delta,-0.006)
    dx=max(-0.010,min(0.010,-r["final_mass_dx"]*0.55))
    dy=max(-0.010,min(0.010,-r["final_mass_dy"]*0.55))
    # Second-pass shortlist is intentionally strict. These metrics are aids for
    # human optical review, not instructions to mathematically recenter every PNG.
    needs = residual >= 0.105 or r["edge_clearance"] < -0.020
    if needs:
        ns=max(0.97,min(1.045,r["current_scale"]+scale_delta))
        nox=max(-0.045,min(0.045,r["current_offset_x"]+dx))
        noy=max(-0.045,min(0.045,r["current_offset_y"]+dy))
        candidates.append({
          **r,"curated_before":curated,"residual":residual,
          "new_scale":round(ns,4),"new_offset_x":round(nox,4),"new_offset_y":round(noy,4)
        })

candidates.sort(key=lambda r:(not r["curated_before"],-r["residual"],r["edge_clearance"]))
json.dump(rows,open(OUT/"post_curation_metrics.json","w"),indent=2)
json.dump(candidates,open(OUT/"second_pass_candidates.json","w"),indent=2)
with open(OUT/"second_pass_candidates.csv","w",newline="",encoding="utf-8") as f:
    fields=sorted({k for r in candidates for k in r}); w=csv.DictWriter(f,fieldnames=fields); w.writeheader(); w.writerows(candidates)

# Contact sheets show the actually tuned result using the same crop + scale + offset model.
thumb=180; cols=5; per_page=25; font=ImageFont.load_default()
for page in range((len(candidates)+per_page-1)//per_page):
    subset=candidates[page*per_page:(page+1)*per_page]
    sheet=Image.new("RGB",(cols*thumb,5*(thumb+26)),"white"); d=ImageDraw.Draw(sheet)
    for n,r in enumerate(subset):
        rr=n//cols; cc=n%cols; im=Image.open(IMGDIR/f'{r["id"]}.png').convert("RGBA")
        a=im.getchannel("A"); bb=a.getbbox()
        if bb: im=im.crop(bb)
        im.thumbnail((thumb-24,thumb-24),Image.Resampling.LANCZOS)
        x=cc*thumb+(thumb-im.width)//2; y=rr*(thumb+26)+(thumb-im.height)//2
        sheet.paste(im,(x,y),im)
        d.text((cc*thumb+4,rr*(thumb+26)+thumb+4),f'#{r["id"]} res={r["residual"]:.3f}',fill="black",font=font)
    sheet.save(OUT/f"second_pass_{page+1:02d}.jpg",quality=90)

print("SECOND_PASS_VALID",len(valid))
print("SECOND_PASS_EXISTING",sum(1 for r in candidates if r["curated_before"]))
print("SECOND_PASS_NEW",sum(1 for r in candidates if not r["curated_before"]))
print("SECOND_PASS_TOTAL",len(candidates))
for r in candidates:
    print(f'TUNE {r["id"]} {r["new_scale"]:.4f} {r["new_offset_x"]:.4f} {r["new_offset_y"]:.4f} curated={int(r["curated_before"])} residual={r["residual"]:.4f} edge={r["edge_clearance"]:.4f}')
