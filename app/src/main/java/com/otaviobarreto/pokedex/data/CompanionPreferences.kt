package com.otaviobarreto.pokedex.data

import android.content.Context

object CompanionPreferences {
    private const val PREFS = "companion_preferences"
    private const val KEY_ACTIVE_GAME = "active_game"
    private const val KEY_ACTIVE_REGION = "active_region"
    private const val KEY_GAME_REGION_PREFIX = "game_region_"
    private const val KEY_BOX_PAGE_PREFIX = "box_page_"
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

    private fun key(value: String): String = value.lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
}
