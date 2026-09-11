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
import com.otaviobarreto.pokedex.data.CompanionPreferences
import com.otaviobarreto.pokedex.data.RecentActivityStore
import com.otaviobarreto.pokedex.data.AppSoundManager

class PokedexApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        PersistentApiCache.initialize(this)
        OfflineGamePackManager.initialize(this)
        CompanionPreferences.initialize(this)
        RecentActivityStore.initialize(this)
        AppSoundManager.initialize(this)
        runCatching {
            HttpResponseCache.install(File(cacheDir, "pokeapi-http"), 32L * 1024L * 1024L)
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
