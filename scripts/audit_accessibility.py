#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors=[]

def read(path):
    p=ROOT/path
    if not p.exists():
        errors.append(f"missing {path}")
        return ""
    return p.read_text(encoding="utf-8")

chrome=read("app/src/main/java/com/otaviobarreto/pokedex/ui/PokedexChrome.kt")
boxes=read("app/src/main/java/com/otaviobarreto/pokedex/ui/BoxesV2Screen.kt")
manifest=read("app/src/main/AndroidManifest.xml")

checks=[
    ("bottom navigation labels", 'contentDescription=item.label' in chrome or 'Icon(item.icon,item.label' in chrome),
    ("Box slot TalkBack semantics", '.semantics {' in boxes and 'contentDescription = buildString' in boxes),
    ("RTL support", 'android:supportsRtl="true"' in manifest),
    ("system navigation inset", 'navigationBarsPadding()' in chrome),
]
for label, ok in checks:
    if not ok:
        errors.append(label)

print("ACCESSIBILITY_AUDIT_ERRORS="+str(len(errors)))
for e in errors:
    print("ERROR:",e)
sys.exit(1 if errors else 0)
