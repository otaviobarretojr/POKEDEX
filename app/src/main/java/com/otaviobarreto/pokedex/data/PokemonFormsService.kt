package com.otaviobarreto.pokedex.data

import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class PokemonFormVariant(
    val name: String,
    val pokemonId: Int?,
    val isDefault: Boolean
)

object PokemonFormsService {
    private val cache = ConcurrentHashMap<Int, List<PokemonFormVariant>>()

    fun cached(id: Int): List<PokemonFormVariant>? = cache[id]

    fun load(id: Int): List<PokemonFormVariant> {
        cache[id]?.let { return it }
        val url = "https://pokeapi.co/api/v2/pokemon-species/$id"
        val json = JSONObject(
            PersistentApiCache.getOrFetch(url) {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 12_000
                connection.readTimeout = 12_000
                connection.requestMethod = "GET"
                connection.connect()
                try {
                    if (connection.responseCode !in 200..299) error("HTTP " + connection.responseCode)
                    connection.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    connection.disconnect()
                }
            }
        )
        val array = json.optJSONArray("varieties") ?: org.json.JSONArray()
        val result = buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val pokemon = item.getJSONObject("pokemon")
                val purl = pokemon.optString("url")
                val pid = purl.trimEnd('/').substringAfterLast('/').toIntOrNull()
                val display = pokemon.optString("name").split('-').joinToString(" ") { part ->
                    part.replaceFirstChar { ch -> ch.uppercase() }
                }
                add(PokemonFormVariant(display, pid, item.optBoolean("is_default")))
            }
        }
        cache[id] = result
        return result
    }
}
