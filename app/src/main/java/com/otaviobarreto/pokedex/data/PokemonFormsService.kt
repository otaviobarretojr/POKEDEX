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
) {
    val countsForLivingDex:Boolean
        get() = kind != PokemonFormKind.BATTLE
}

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

    fun livingDexForms(id:Int):List<PokemonFormVariant> =
        collectible(id).filter{it.countsForLivingDex}

    private fun classify(name:String,isDefault:Boolean):PokemonFormKind{
        if(isDefault) return PokemonFormKind.DEFAULT
        val n=name.lowercase()
        return when{
            listOf("alola","galar","hisui","paldea").any{it in n} -> PokemonFormKind.REGIONAL
            "female" in n || "male" in n -> PokemonFormKind.GENDER

            // Temporary transformations: visible in details, but not required for Living Dex completion.
            listOf(
                "mega","gmax","gigantamax","primal","eternamax",
                "busted","school","zen","blade","shield","complete",
                "crowned","gulping","gorging","hero","hangry"
            ).any{it in n} -> PokemonFormKind.BATTLE

            // Persistent/collectible identity variants.
            listOf(
                "red striped","blue striped","white striped",
                "midday","midnight","dusk",
                "amped","low key",
                "family of three","family of four",
                "three segment","two segment",
                "curly","droopy","stretchy",
                "chest","roaming",
                "artisan","counterfeit","antique","phony",
                "teal mask","wellspring","hearthflame","cornerstone",
                "terastal","stellar",
                "male","female"
            ).any{it in n} -> PokemonFormKind.SPECIAL

            // Cosmetic collections that users may want to track independently.
            listOf(
                "cap","cosplay","partner","original","hoenn","sinnoh","unova","kalos","alola",
                "world","fancy","pokeball","poke ball","garden","meadow","marine","archipelago",
                "high plains","sandstorm","river","monsoon","savanna","sun","ocean","jungle",
                "elegant","modern","polar","tundra","continental","icy snow",
                "debutante","diamond","heart","kabuki","la reine","matron","dandy","star",
                "lemon","matcha","mint","ruby","salted","ruby swirl","caramel swirl","rainbow swirl"
            ).any{it in n} -> PokemonFormKind.COSMETIC

            listOf("starter","battle bond","ash").any{it in n} -> PokemonFormKind.SPECIAL
            else -> PokemonFormKind.OTHER
        }
    }
}
