#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT=Path(__file__).resolve().parents[1]
errors=[]

def read(path):
    p=ROOT/path
    if not p.exists():
        errors.append(f"missing {path}")
        return ""
    return p.read_text(encoding="utf-8")

gradle=read("app/build.gradle.kts")
manifest=read("app/src/main/AndroidManifest.xml")
backup_rules=read("app/src/main/res/xml/backup_rules.xml")
data_rules=read("app/src/main/res/xml/data_extraction_rules.xml")
main=read("app/src/main/java/com/otaviobarreto/pokedex/MainActivity.kt")
chrome=read("app/src/main/java/com/otaviobarreto/pokedex/ui/PokedexChrome.kt")
boot=read("app/src/main/java/com/otaviobarreto/pokedex/ui/BootExperienceScreen.kt")
preloader=read("app/src/main/java/com/otaviobarreto/pokedex/data/StartupPreloader.kt")
backup=read("app/src/main/java/com/otaviobarreto/pokedex/data/AppBackupManager.kt")
offline=read("app/src/main/java/com/otaviobarreto/pokedex/data/OfflineGamePackManager.kt")
workflow=read(".github/workflows/android.yml")

checks={
    "stable package id": 'applicationId = "com.otaviobarreto.pokedex"' in gradle,
    "update-safe version code": ('versionCode = 19200' in gradle) or ('versionCode = 20000' in gradle) or ('versionCode = 20300' in gradle) or ('versionCode = 20400' in gradle) or ('versionCode = 20500' in gradle) or ('versionCode = 20600' in gradle) or ('versionCode = 20700' in gradle) or ('versionCode = 20701' in gradle) or ('versionCode = 20800' in gradle) or ('versionCode = 20900' in gradle) or ('versionCode = 21000' in gradle) or ('versionCode = 21100' in gradle) or ('versionCode = 21200' in gradle) or ('versionCode = 21201' in gradle) or ('versionCode = 21300' in gradle),
    "minimum supported Android": 'minSdk = 26' in gradle,
    "current target SDK": 'targetSdk = 35' in gradle,
    "stable signing config": 'stableDebug' in gradle and 'pokedex-release.jks' in gradle,
    "CI restores stable signing key": 'Restore stable signing key' in workflow,
    "CI uses current Node 24 actions": all(marker in workflow for marker in (
        "actions/checkout@v7",
        "actions/setup-java@v6",
        "gradle/actions/setup-gradle@v6",
        "actions/upload-artifact@v7",
    )),
    "CI uses read-only repository permission": "contents: read" in workflow,
    "instrumentation tests compile in CI": 'assembleDebugAndroidTest' in workflow,
    "Android backup enabled": 'android:allowBackup="true"' in manifest,
    "Android backup rules wired": 'android:fullBackupContent="@xml/backup_rules"' in manifest and 'android:dataExtractionRules="@xml/data_extraction_rules"' in manifest,
    "offline cache excluded from backup": 'offline_game_packs_v2.xml' in backup_rules and 'offline_game_packs_v2.xml' in data_rules,
    "cleartext blocked": 'android:usesCleartextTraffic="false"' in manifest,
    "RTL supported": 'android:supportsRtl="true"' in manifest,
    "bottom navigation respects system inset": 'navigationBarsPadding()' in chrome,
    "secondary screens use Android back stack": 'navController.popBackStack()' in main,
    "app foreground lifecycle present": 'onAppForegrounded()' in main,
    "app background lifecycle present": 'onAppBackgrounded()' in main,
    "essential bootstrap gates first entry": 'ContentBootstrapManager.ensureReady(context)' in boot,
    "optional startup warmup is non-blocking": 'StartupPreloader.launchWarmInBackground(context)' in boot,
    "blocking cache warmup absent from boot": 'StartupPreloader.warm(context)' not in boot,
    "background warmup owns extended phase": 'launchExtendedWarm(appContext)' in preloader,
    "background warmup isolated from UI scope": 'SupervisorJob() + Dispatchers.IO' in preloader,
    "backup integrity checksum": 'integritySha256' in backup and 'MessageDigest.getInstance("SHA-256")' in backup,
    "offline resume manifest": 'completed_ids' in offline and 'pendingIds' in offline,
    "offline concurrency bounded": 'DOWNLOAD_CONCURRENCY = 10' in offline and 'FORM_DOWNLOAD_CONCURRENCY = 4' in offline,
}
for label,ok in checks.items():
    if not ok:
        errors.append(label)

print("DEVICE_READINESS_ERRORS="+str(len(errors)))
for e in errors:
    print("ERROR:",e)
sys.exit(1 if errors else 0)
