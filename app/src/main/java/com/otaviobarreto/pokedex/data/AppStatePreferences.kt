package com.otaviobarreto.pokedex.data

import android.content.Context
import org.json.JSONObject

object AppStatePreferences {
    // Keep the historical SharedPreferences file name to preserve existing installs.
    private const val PREFS = "companion_preferences"
    private const val KEY_ACTIVE_GAME = "active_game"
    private const val KEY_ACTIVE_REGION = "active_region"
    private const val KEY_GAME_REGION_PREFIX = "game_region_"
    private const val KEY_BOX_PAGE_PREFIX = "box_page_"
    private const val KEY_STARTER_PREFIX = "journey_starter_"
    private var context: Context? = null

    fun initialize(context: Context) { this.context = context.applicationContext }

    var activeGame: String
        get() = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.getString(KEY_ACTIVE_GAME, AppGameCatalog.games.first().label)
            ?: AppGameCatalog.games.first().label
        set(value) {
            if (AppGameCatalog.games.none { it.label == value }) return
            context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
                ?.putString(KEY_ACTIVE_GAME, value)?.apply()
        }

    var activeRegionSource: String?
        get() = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.getString(KEY_ACTIVE_REGION, null)
        set(value) {
            context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
                ?.putString(KEY_ACTIVE_REGION, value)?.apply()
        }

    fun activeRegionForGame(gameLabel: String): String? {
        val game = AppGameCatalog.games.firstOrNull { it.label == gameLabel } ?: return null
        val prefs = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs?.getString(KEY_GAME_REGION_PREFIX + key(gameLabel), null)
        if (stored != null && game.regions.any { it.source == stored }) return stored
        val legacy = activeRegionSource
        return legacy?.takeIf { source -> game.regions.any { it.source == source } }
    }

    fun setActiveRegionForGame(gameLabel: String, source: String?) {
        val game = AppGameCatalog.games.firstOrNull { it.label == gameLabel } ?: return
        if (source != null && game.regions.none { it.source == source }) return
        activeRegionSource = source
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putString(KEY_GAME_REGION_PREFIX + key(gameLabel), source)?.apply()
    }

    fun boxPage(source: String): Int =
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.getInt(KEY_BOX_PAGE_PREFIX + key(source), 0)
            ?.coerceAtLeast(0) ?: 0

    fun setBoxPage(source: String, page: Int) {
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putInt(KEY_BOX_PAGE_PREFIX + key(source), page.coerceAtLeast(0))?.apply()
    }

    fun journeyStarterForGame(gameLabel: String): Int? {
        val valid = TeamCampaignCatalog.starters(gameLabel).map { it.second }.toSet()
        val stored = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.getInt(KEY_STARTER_PREFIX + key(gameLabel), -1) ?: -1
        return stored.takeIf { it in valid }
    }

    fun setJourneyStarterForGame(gameLabel: String, pokemonId: Int) {
        if (TeamCampaignCatalog.starters(gameLabel).none { it.second == pokemonId }) return
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putInt(KEY_STARTER_PREFIX + key(gameLabel), pokemonId)?.apply()
    }

    fun exportSnapshot(): JSONObject {
        val regions = JSONObject()
        val pages = JSONObject()
        val starters = JSONObject()
        AppGameCatalog.games.forEach { game ->
            activeRegionForGame(game.label)?.let { regions.put(game.label, it) }
            game.regions.forEach { region -> pages.put(region.source, boxPage(region.source)) }
            journeyStarterForGame(game.label)?.let { starters.put(game.label, it) }
        }
        return JSONObject()
            .put("activeGame", activeGame)
            .put("activeRegion", activeRegionSource)
            .put("regions", regions)
            .put("boxPages", pages)
            .put("journeyStarters", starters)
    }

    fun importSnapshot(snapshot: JSONObject) {
        snapshot.optString("activeGame").takeIf { it.isNotBlank() }?.let { activeGame = it }
        val regions = snapshot.optJSONObject("regions")
        AppGameCatalog.games.forEach { game ->
            regions?.optString(game.label)?.takeIf { it.isNotBlank() }?.let { setActiveRegionForGame(game.label, it) }
        }
        val pages = snapshot.optJSONObject("boxPages")
        AppGameCatalog.games.flatMap { it.regions }.forEach { region ->
            if (pages?.has(region.source) == true) setBoxPage(region.source, pages.optInt(region.source, 0))
        }
        val starters = snapshot.optJSONObject("journeyStarters")
        AppGameCatalog.games.forEach { game ->
            if (starters?.has(game.label) == true) {
                setJourneyStarterForGame(game.label, starters.optInt(game.label, -1))
            }
        }
        snapshot.optString("activeRegion").takeIf { it.isNotBlank() }?.let { activeRegionSource = it }
    }

    private fun key(value: String): String = value.lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
}
