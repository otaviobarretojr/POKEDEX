package com.otaviobarreto.pokedex

import android.app.Application
import android.net.http.HttpResponseCache
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.otaviobarreto.pokedex.data.PersistentApiCache
import com.otaviobarreto.pokedex.data.OfflineGamePackManager
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.otaviobarreto.pokedex.data.PokedexDataStore
import com.otaviobarreto.pokedex.data.CompanionPreferences
import com.otaviobarreto.pokedex.data.RecentActivityStore

class PokedexApplication : Application(), ImageLoaderFactory {
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        PersistentApiCache.initialize(this)
        OfflineGamePackManager.initialize(this)
        CompanionPreferences.initialize(this)
        RecentActivityStore.initialize(this)
        runCatching {
            HttpResponseCache.install(File(cacheDir, "pokeapi-http"), 32L * 1024L * 1024L)
        }
        preloadScope.launch {
            // Cold-start warm-up stays intentionally small. The home Pokédex is
            // the only dataset prepared immediately; game/reference catalogs
            // are loaded cache-first when their screens are opened.
            runCatching { PokedexDataStore.nationalDex() }
            val priorityIds = buildList {
                addAll(RecentActivityStore.recentPokemon.take(10))
                addAll(OfflineGamePackManager.manifestIds(CompanionPreferences.activeGame).take(14))
            }
            runCatching { PokedexDataStore.hydratePinned(priorityIds) }
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.30)
                .strongReferencesEnabled(true)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(File(cacheDir, "pokemon-images"))
                .maxSizeBytes(384L * 1024L * 1024L)
                .build()
        }
        .respectCacheHeaders(false)
        .crossfade(false)
        .build()
}
