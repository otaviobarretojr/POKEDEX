package com.otaviobarreto.pokedex.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object GameDexService {
    private const val API = "https://pokeapi.co/api/v2"
    private val cache = ConcurrentHashMap<String, List<GameDexEntry>>()

    data class GameDexEntry(
        val nationalId: Int,
        val gameNumber: Int,
        val name: String
    ) {
        val spriteUrl: String
            get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$nationalId.png"
    }

    fun loadGameDex(context: GameContext): List<GameDexEntry> {
        cache[context.pokedexSlug]?.let { return it }
        val json = getJson("$API/pokedex/${context.pokedexSlug}")
        val entries = json.getJSONArray("pokemon_entries")
        val result = buildList(entries.length()) {
            for (i in 0 until entries.length()) {
                val item = entries.getJSONObject(i)
                val species = item.getJSONObject("pokemon_species")
                val nationalId = idFromUrl(species.getString("url"))
                if (nationalId in 1..PokeApiService.MAX_NATIONAL_DEX_ID) {
                    add(
                        GameDexEntry(
                            nationalId = nationalId,
                            gameNumber = item.getInt("entry_number"),
                            name = species.getString("name").toDisplayName()
                        )
                    )
                }
            }
        }.sortedBy { it.gameNumber }
        cache[context.pokedexSlug] = result
        return result
    }

    fun cached(context: GameContext): List<GameDexEntry>? = cache[context.pokedexSlug]

    fun clearCache() = cache.clear()

    private fun getJson(url: String): JSONObject = JSONObject(getText(url))

    private fun getText(url: String): String = PersistentApiCache.getOrFetch(url) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 12_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.connect()
        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode} while loading $url")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun idFromUrl(url: String): Int = url.trimEnd('/').substringAfterLast('/').toInt()

    private fun String.toDisplayName(): String =
        split('-', ' ').joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
}
