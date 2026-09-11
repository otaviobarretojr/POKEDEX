package com.otaviobarreto.pokedex.data

import org.json.JSONObject

object BackupService {
    fun exportJson(): String = JSONObject()
        .put("format", "pokedex-companion")
        .put("version", 3)
        .put("collection", CollectionStore.exportSnapshot())
        .put("teams", TeamStore.exportSnapshot())
        .toString()

    fun importJson(raw: String): Boolean = runCatching {
        val root = JSONObject(raw)
        require(root.optString("format") == "pokedex-companion")
        val collectionOk = root.optJSONObject("collection")?.let(CollectionStore::importSnapshot) ?: false
        val teamsOk = root.optJSONArray("teams")?.let(TeamStore::importSnapshot) ?: false
        collectionOk && teamsOk
    }.getOrDefault(false)
}
