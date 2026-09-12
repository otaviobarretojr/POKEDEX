package com.otaviobarreto.pokedex.data

import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

enum class PokemonFormKind {
    DEFAULT, REGIONAL, GENDER, BATTLE, SPECIAL, COSMETIC, OTHER
}

data class PokemonFormVariant(
    val name: String,
    val pokemonId: Int?,
    val isDefault: Boolean,
    val kind: PokemonFormKind = PokemonFormKind.OTHER
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
                val isDefault=item.optBoolean("is_default")
                add(PokemonFormVariant(display, pid, isDefault, classify(display,isDefault)))
            }
        }
        cache[id] = result
        return result
    }

    fun collectible(id:Int):List<PokemonFormVariant> =
        (cached(id) ?: load(id))
            .distinctBy{it.pokemonId}
            .sortedWith(compareByDescending<PokemonFormVariant>{it.isDefault}.thenBy{it.name})

    private fun classify(name:String,isDefault:Boolean):PokemonFormKind{
        if(isDefault) return PokemonFormKind.DEFAULT
        val n=name.lowercase()
        return when{
            listOf("alola","galar","hisui","paldea").any{it in n} -> PokemonFormKind.REGIONAL
            "female" in n || "male" in n -> PokemonFormKind.GENDER
            listOf("mega","gmax","gigantamax","primal").any{it in n} -> PokemonFormKind.BATTLE
            listOf("totem","eternamax").any{it in n} -> PokemonFormKind.BATTLE
            listOf("cap","cosplay","starter","battle bond").any{it in n} -> PokemonFormKind.SPECIAL
            listOf("red striped","blue striped","white striped","dusk","midnight","school","solo","amped","low key","family of","three segment","two segment").any{it in n} -> PokemonFormKind.SPECIAL
            else -> PokemonFormKind.OTHER
        }
    }
}
