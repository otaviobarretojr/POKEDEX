#!/usr/bin/env python3
from pathlib import Path
import re, urllib.request

root=Path(__file__).resolve().parents[1]
details=(root/"app/src/main/java/com/otaviobarreto/pokedex/data/JourneyObjectiveDetailsCatalog.kt").read_text()
ui=(root/"app/src/main/java/com/otaviobarreto/pokedex/ui/JourneyScreen.kt").read_text()
names=sorted(set(re.findall(r'p\("([^"]+)","',details)))
generic={
 "Elite Four","Geeta","Equipe de Arven","Director Clavell","Cassiopeia","Area Zero",
 "Batalha final","8 Gym Leaders","4 treinadores","Tera Raid 6★",
 "Wo-Chien / Chien-Pao / Ting-Lu / Chi-Yu","Carmine","Encontros de Oni Mountain",
 "Kieran","Treinadores do Terarium","Crispin","Amarys","Lacey","Drayton",
 "Pokémon selvagens / treinadores","Lendários retornantes","Treinadores possuídos","Nemona"
}
aliases=dict((n,int(i)) for n,i in re.findall(r'"([^"]+)" to (\d+)',ui))
errors=[]
checked=0
for name in names:
    if name in generic: continue
    pid=aliases.get(name)
    if pid is None:
        errors.append(f"NO_ID {name}")
        continue
    url=f"https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/{pid}.png"
    try:
        req=urllib.request.Request(url,method="HEAD",headers={"User-Agent":"POKEDEX-journey-audit"})
        with urllib.request.urlopen(req,timeout=12) as r:
            if r.status!=200: errors.append(f"HTTP_{r.status} {name} #{pid}")
            else: checked+=1
    except Exception as e:
        errors.append(f"FETCH {name} #{pid}: {e}")
print(f"JOURNEY_BOSS_POKEMON_CHECKED={checked}")
print(f"JOURNEY_BOSS_POKEMON_ERRORS={len(errors)}")
for e in errors: print(e)
raise SystemExit(1 if errors else 0)
