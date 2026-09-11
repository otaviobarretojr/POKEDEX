package com.otaviobarreto.pokedex.data

import android.content.Context

object CompanionPreferences {
    private const val PREFS = "companion_preferences"
    private const val KEY_ACTIVE_GAME = "active_game"
    private const val KEY_ACTIVE_REGION = "active_region"
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
}
