package com.otaviobarreto.pokedex.data

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

object AppBackupManager {
    private const val SCHEMA_VERSION = 6

    fun exportJson(): String {
        val payload = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("createdAt", System.currentTimeMillis())
            .put("collection", CollectionStore.exportSnapshot())
            .put("journey", JourneyProgressStore.exportSnapshot())
            .put("appState", AppStatePreferences.exportSnapshot())
            .put("recentActivity", RecentActivityStore.exportSnapshot())
            .put("teams", TeamStore.exportSnapshot())
            .put("variants", VariantCollectionStore.exportSnapshot())
            .put(
                "offlinePacks",
                JSONArray(
                    AppGameCatalog.adventureGames
                        .filter { OfflineGamePackManager.status(it.label).downloaded }
                        .map { it.label }
                )
            )
        return JSONObject(payload.toString())
            .put("integritySha256", sha256(payload.toString()))
            .toString()
    }

    fun importJson(raw: String): Boolean {
        val parsed = runCatching { JSONObject(raw) }.getOrNull() ?: return false
        if (!integrityValid(parsed)) return false
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
            .put("offlinePacks", if (schema >= 6) root.optJSONArray("offlinePacks") ?: JSONArray() else JSONArray())

        return normalized
    }

    internal fun integrityValid(root: JSONObject): Boolean {
        val schema = root.optInt("schemaVersion", 0)
        if (schema < 6) return true
        val expected = root.optString("integritySha256", "")
        if (expected.isBlank()) return false
        val payload = JSONObject(root.toString()).apply { remove("integritySha256") }
        return sha256(payload.toString()) == expected
    }

    internal fun downloadedPackLabels(root: JSONObject): List<String> {
        val array = root.optJSONArray("offlinePacks") ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    fun downloadedPackLabels(raw: String): List<String> =
        runCatching { downloadedPackLabels(JSONObject(raw)) }.getOrDefault(emptyList())

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

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
