package com.otaviobarreto.pokedex.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

object RecentActivityStore {
    private const val PREFS = "recent_activity"
    private const val KEY_POKEMON = "recent_pokemon"
    private const val KEY_ROUTE = "last_route"
    private var context: Context? = null
    var recentPokemon by mutableStateOf<List<Int>>(emptyList()); private set
    var lastRoute by mutableStateOf("pokedex"); private set

    fun initialize(context: Context) {
        if (this.context != null) return
        this.context = context.applicationContext
        val p = this.context!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        recentPokemon = decode(p.getString(KEY_POKEMON, "[]") ?: "[]")
        lastRoute = p.getString(KEY_ROUTE, "pokedex") ?: "pokedex"
    }

    fun recordPokemon(id: Int) {
        if (id !in 1..PokeApiService.MAX_NATIONAL_DEX_ID) return
        recentPokemon = (listOf(id) + recentPokemon.filterNot { it == id }).take(20)
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putString(KEY_POKEMON, JSONArray(recentPokemon).toString())?.apply()
    }

    fun recordRoute(route: String) {
        if (route.isBlank() || route == "home") return
        lastRoute = route
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putString(KEY_ROUTE, route)?.apply()
    }

    fun exportSnapshot(): org.json.JSONObject = org.json.JSONObject()
        .put("pokemon", JSONArray(recentPokemon))
        .put("lastRoute", lastRoute)

    fun importSnapshot(snapshot: org.json.JSONObject): Boolean = runCatching {
        recentPokemon = decode(snapshot.optJSONArray("pokemon")?.toString() ?: "[]").take(20)
        lastRoute = snapshot.optString("lastRoute", "pokedex").ifBlank { "pokedex" }
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putString(KEY_POKEMON, JSONArray(recentPokemon).toString())
            ?.putString(KEY_ROUTE, lastRoute)
            ?.apply()
        true
    }.getOrDefault(false)

    private fun decode(raw: String): List<Int> = runCatching {
        val a = JSONArray(raw)
        buildList {
            for (i in 0 until a.length()) {
                a.optInt(i).takeIf { it in 1..PokeApiService.MAX_NATIONAL_DEX_ID }?.let(::add)
            }
        }
    }.getOrDefault(emptyList())
}
