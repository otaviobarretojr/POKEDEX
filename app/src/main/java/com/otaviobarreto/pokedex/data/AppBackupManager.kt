package com.otaviobarreto.pokedex.data

import org.json.JSONArray
import org.json.JSONObject

object AppBackupManager {
    private const val SCHEMA_VERSION = 5

    fun exportJson(): String = JSONObject()
        .put("schemaVersion", SCHEMA_VERSION)
        .put("createdAt", System.currentTimeMillis())
        .put("collection", CollectionStore.exportSnapshot())
        .put("journey", JourneyProgressStore.exportSnapshot())
        .put("appState", AppStatePreferences.exportSnapshot())
        .put("recentActivity", RecentActivityStore.exportSnapshot())
        .put("teams", TeamStore.exportSnapshot())
        .put("variants", VariantCollectionStore.exportSnapshot())
        .toString()

    fun importJson(raw: String): Boolean {
        val parsed = runCatching { JSONObject(raw) }.getOrNull() ?: return false
        val incoming = normalizeForRestore(parsed) ?: return false
        val previous = runCatching { normalizeForRestore(JSONObject(exportJson())) }.getOrNull()

        if (applySnapshot(incoming)) {
            CollectionIntegrityService.repair()
            return true
        }

        previous?.let { snapshot ->
            runCatching {
                applySnapshot(snapshot)
                CollectionIntegrityService.repair()
            }
        }
        return false
    }

    internal fun normalizeForRestore(root: JSONObject): JSONObject? {
        val schema = root.optInt("schemaVersion", 0)
        if (schema !in 1..SCHEMA_VERSION) return null
        val collection = root.optJSONObject("collection") ?: return null
        val journey = root.optJSONObject("journey") ?: return null
        if (schema >= 3 && root.has("teams") && root.optJSONArray("teams") == null) return null
        if (schema >= 4 && root.has("variants") && root.optJSONArray("variants") == null) return null

        val normalized = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("createdAt", root.optLong("createdAt", 0L))
            .put("collection", collection)
            .put("journey", journey)
            .put("appState", root.optJSONObject("appState") ?: JSONObject())
            .put(
                "recentActivity",
                if (schema >= 2) {
                    root.optJSONObject("recentActivity")
                        ?: JSONObject().put("pokemon", JSONArray()).put("lastRoute", "pokedex")
                } else {
                    JSONObject().put("pokemon", JSONArray()).put("lastRoute", "pokedex")
                }
            )
            .put("teams", if (schema >= 3) root.optJSONArray("teams") ?: JSONArray() else JSONArray())
            .put("variants", if (schema >= 4) root.optJSONArray("variants") ?: JSONArray() else JSONArray())

        return normalized
    }

    private fun applySnapshot(root: JSONObject): Boolean = runCatching {
        val collection = root.optJSONObject("collection") ?: return@runCatching false
        val journey = root.optJSONObject("journey") ?: return@runCatching false
        val appState = root.optJSONObject("appState") ?: JSONObject()
        val recent = root.optJSONObject("recentActivity") ?: return@runCatching false
        val teams = root.optJSONArray("teams") ?: return@runCatching false
        val variants = root.optJSONArray("variants") ?: return@runCatching false

        if (!CollectionStore.importSnapshot(collection)) return@runCatching false
        if (!JourneyProgressStore.importSnapshot(journey)) return@runCatching false
        AppStatePreferences.importSnapshot(appState)
        if (!RecentActivityStore.importSnapshot(recent)) return@runCatching false
        if (!TeamStore.importSnapshot(teams)) return@runCatching false
        if (!VariantCollectionStore.importSnapshot(variants)) return@runCatching false
        true
    }.getOrDefault(false)
}
