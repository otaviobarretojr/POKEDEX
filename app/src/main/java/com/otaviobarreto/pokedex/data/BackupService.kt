package com.otaviobarreto.pokedex.data

import org.json.JSONArray
import org.json.JSONObject

object BackupService {
    fun exportJson(): String = JSONObject()
        .put("format", "pokedex-companion")
        .put("version", 8)
        .put("collection", CollectionStore.exportSnapshot())
        .put("teams", TeamStore.exportSnapshot())
        .put("activeGame", CompanionPreferences.activeGame)
        .put("preferences", CompanionPreferences.exportSnapshot())
        .put("recent", RecentActivityStore.exportSnapshot())
        .put("journey", JourneyProgressStore.exportSnapshot())
        .toString()

    fun importJson(raw: String): Boolean {
        val parsed = runCatching { JSONObject(raw) }.getOrNull() ?: return false
        if (parsed.optString("format") != "pokedex-companion") return false

        val collection = parsed.optJSONObject("collection") ?: return false
        val teams = parsed.optJSONArray("teams") ?: return false
        val preferences = parsed.optJSONObject("preferences")
        val recent = parsed.optJSONObject("recent")
        val journey = parsed.optJSONObject("journey")

        // Validate the payload shape before mutating any live store.
        if (!collection.has("captured") || !collection.has("boxes")) return false
        if (teams.length() > 100) return false

        val oldCollection = CollectionStore.exportSnapshot()
        val oldTeams = TeamStore.exportSnapshot()
        val oldPreferences = CompanionPreferences.exportSnapshot()
        val oldRecent = RecentActivityStore.exportSnapshot()
        val oldJourney = JourneyProgressStore.exportSnapshot()

        return runCatching {
            check(CollectionStore.importSnapshot(collection))
            check(TeamStore.importSnapshot(teams))

            parsed.optString("activeGame")
                .takeIf { it.isNotBlank() }
                ?.let { CompanionPreferences.activeGame = it }

            preferences?.let(CompanionPreferences::importSnapshot)
            if (recent != null) check(RecentActivityStore.importSnapshot(recent))
            if (journey != null) check(JourneyProgressStore.importSnapshot(journey))
            true
        }.getOrElse {
            // Roll back every store touched by the failed restore.
            CollectionStore.importSnapshot(oldCollection)
            TeamStore.importSnapshot(oldTeams)
            CompanionPreferences.importSnapshot(oldPreferences)
            RecentActivityStore.importSnapshot(oldRecent)
            JourneyProgressStore.importSnapshot(oldJourney)
            false
        }
    }
}
