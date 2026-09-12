package com.otaviobarreto.pokedex

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.otaviobarreto.pokedex.data.AppGameCatalog
import com.otaviobarreto.pokedex.data.AppStatePreferences
import com.otaviobarreto.pokedex.data.CollectionStore
import com.otaviobarreto.pokedex.data.JourneyProgressStore
import com.otaviobarreto.pokedex.data.TeamStore
import com.otaviobarreto.pokedex.data.OfflineGamePackManager
import com.otaviobarreto.pokedex.data.PersistentApiCache
import com.otaviobarreto.pokedex.data.RecentActivityStore
import com.otaviobarreto.pokedex.data.VariantCollectionStore
import com.otaviobarreto.pokedex.audio.HomeAudioManager
import java.io.File

class PokedexApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        PersistentApiCache.initialize(this)
        OfflineGamePackManager.initialize(this)
        AppStatePreferences.initialize(this)
        RecentActivityStore.initialize(this)
        CollectionStore.initialize(this)
        VariantCollectionStore.initialize(this)
        TeamStore.initialize(this)
        JourneyProgressStore.initialize(this)
        HomeAudioManager.initialize(this)

        val legacySource = AppStatePreferences.activeRegionSource
            ?: AppGameCatalog.games
                .firstOrNull { it.label == AppStatePreferences.activeGame }
                ?.regions
                ?.firstOrNull()
                ?.source
        CollectionStore.migrateLegacyCapturedToSource(legacySource)
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
