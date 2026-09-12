package com.otaviobarreto.pokedex.data

import org.json.JSONObject

object AppBackupManager {
    private const val SCHEMA_VERSION = 4

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

    /**
     * Restores a backup defensively.
     *
     * A snapshot of the current state is captured first. If any stage of the
     * incoming restore fails, the previous snapshot is applied again so the
     * app is not left with a partially imported collection/journey state.
     */
    fun importJson(raw: String): Boolean {
        val incoming = runCatching { JSONObject(raw) }.getOrNull() ?: return false
        if (!isSupported(incoming)) return false

        val previous = runCatching { JSONObject(exportJson()) }.getOrNull()
        if (applySnapshot(incoming)) return true

        previous?.let { runCatching { applySnapshot(it) } }
        return false
    }

    private fun isSupported(root: JSONObject): Boolean {
        val schema = root.optInt("schemaVersion", 0)
        if (schema !in 1..SCHEMA_VERSION) return false
        if (root.optJSONObject("collection") == null) return false
        if (root.optJSONObject("journey") == null) return false
        if (schema >= 3 && root.has("teams") && root.optJSONArray("teams") == null) return false
        if (schema >= 4 && root.has("variants") && root.optJSONArray("variants") == null) return false
        return true
    }

    private fun applySnapshot(root: JSONObject): Boolean = runCatching {
        val schema = root.optInt("schemaVersion", 0)
        val collection = root.optJSONObject("collection") ?: return@runCatching false
        val journey = root.optJSONObject("journey") ?: return@runCatching false
        val appState = root.optJSONObject("appState") ?: JSONObject()

        if (!CollectionStore.importSnapshot(collection)) return@runCatching false
        if (!JourneyProgressStore.importSnapshot(journey)) return@runCatching false

        AppStatePreferences.importSnapshot(appState)

        if (schema >= 2) {
            val recent = root.optJSONObject("recentActivity")
            if (recent != null && !RecentActivityStore.importSnapshot(recent)) {
                return@runCatching false
            }
        }
        if (schema >= 3) {
            val teams = root.optJSONArray("teams")
            if (teams != null && !TeamStore.importSnapshot(teams)) {
                return@runCatching false
            }
        }
        if (schema >= 4) {
            val variants = root.optJSONArray("variants")
            if (variants != null && !VariantCollectionStore.importSnapshot(variants)) {
                return@runCatching false
            }
        }
        true
    }.getOrDefault(false)
}
