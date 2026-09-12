package com.otaviobarreto.pokedex.data

import org.json.JSONObject

object AppBackupManager {
    private const val SCHEMA_VERSION = 2

    fun exportJson(): String = JSONObject()
        .put("schemaVersion", SCHEMA_VERSION)
        .put("createdAt", System.currentTimeMillis())
        .put("collection", CollectionStore.exportSnapshot())
        .put("journey", JourneyProgressStore.exportSnapshot())
        .put("appState", AppStatePreferences.exportSnapshot())
        .put("recentActivity", RecentActivityStore.exportSnapshot())
        .toString()

    fun importJson(raw: String): Boolean = runCatching {
        val root = JSONObject(raw)
        val schema = root.optInt("schemaVersion", 0)
        if (schema !in 1..SCHEMA_VERSION) return@runCatching false

        val collection = root.optJSONObject("collection") ?: return@runCatching false
        val journey = root.optJSONObject("journey") ?: return@runCatching false
        val appState = root.optJSONObject("appState") ?: JSONObject()

        if (!CollectionStore.importSnapshot(collection)) return@runCatching false
        if (!JourneyProgressStore.importSnapshot(journey)) return@runCatching false
        AppStatePreferences.importSnapshot(appState)

        if (schema >= 2) {
            root.optJSONObject("recentActivity")?.let(RecentActivityStore::importSnapshot)
        }
        true
    }.getOrDefault(false)
}
