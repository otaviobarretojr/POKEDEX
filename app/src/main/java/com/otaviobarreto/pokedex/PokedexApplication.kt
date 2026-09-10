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
import com.otaviobarreto.pokedex.data.AppGameCatalog
import com.otaviobarreto.pokedex.data.GameContext
import com.otaviobarreto.pokedex.data.GameDexService
import com.otaviobarreto.pokedex.data.PokedexDataStore
import com.otaviobarreto.pokedex.data.ReferenceCatalogService

class PokedexApplication : Application(), ImageLoaderFactory {
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        PersistentApiCache.initialize(this)
        OfflineGamePackManager.initialize(this)
        runCatching {
            HttpResponseCache.install(File(cacheDir, "pokeapi-http"), 32L * 1024L * 1024L)
        }
        preloadScope.launch {
            runCatching { PokedexDataStore.nationalDex() }
            AppGameCatalog.games.flatMap { it.regions }.forEach { region ->
                runCatching { GameContext.fromSource(region.source)?.let { GameDexService.loadGameDex(it) } }
            }
            listOf("move", "ability", "item").forEach { kind ->
                runCatching { ReferenceCatalogService.load(kind) }
            }
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.25)
                .strongReferencesEnabled(true)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(File(cacheDir, "pokemon-images"))
                .maxSizeBytes(192L * 1024L * 1024L)
                .build()
        }
        .respectCacheHeaders(false)
        .crossfade(120)
        .build()
}
