package com.otaviobarreto.pokedex.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

enum class PokemonFormKind {
    DEFAULT, REGIONAL, GENDER, BATTLE, SPECIAL, COSMETIC, OTHER
}

data class PokemonFormVariant(
    val name: String,
    val pokemonId: Int?,
    val isDefault: Boolean,
    val kind: PokemonFormKind = PokemonFormKind.OTHER,
    val formKey: String = (pokemonId?.toString() ?: name),
    val spriteUrl: String? = null,
    val shinySpriteUrl: String? = null
) {
    val countsForLivingDex:Boolean
        get() = kind != PokemonFormKind.BATTLE
}

object PokemonFormsService {
    private val cache = ConcurrentHashMap<Int, List<PokemonFormVariant>>()

    fun cached(id: Int): List<PokemonFormVariant>? = cache[id]

    fun load(id: Int): List<PokemonFormVariant> {
        cache[id]?.let { return it }
        val speciesUrl = "https://pokeapi.co/api/v2/pokemon-species/$id"
        val species = JSONObject(fetch(speciesUrl))
        val varieties = species.optJSONArray("varieties") ?: JSONArray()

        val result = buildList {
            for (i in 0 until varieties.length()) {
                val item = varieties.getJSONObject(i)
                val pokemon = item.getJSONObject("pokemon")
                val purl = pokemon.optString("url")
                val pid = purl.trimEnd('/').substringAfterLast('/').toIntOrNull() ?: continue
                val varietyName = pokemon.optString("name")
                val varietyDefault = item.optBoolean("is_default")

                val pokemonJson = runCatching {
                    JSONObject(fetch("https://pokeapi.co/api/v2/pokemon/$pid"))
                }.getOrNull()
                val forms = pokemonJson?.optJSONArray("forms")

                if (forms == null || forms.length() == 0) {
                    val display = pretty(varietyName)
                    add(
                        PokemonFormVariant(
                            name = display,
                            pokemonId = pid,
                            isDefault = varietyDefault,
                            kind = classify(display, varietyDefault),
                            formKey = varietyName.ifBlank { pid.toString() },
                            spriteUrl = pokemonJson?.optJSONObject("sprites")
                                ?.optJSONObject("other")
                                ?.optJSONObject("official-artwork")
                                ?.optString("front_default")
                                ?.takeIf { it.isNotBlank() },
                            shinySpriteUrl = pokemonJson?.optJSONObject("sprites")
                                ?.optJSONObject("other")
                                ?.optJSONObject("official-artwork")
                                ?.optString("front_shiny")
                                ?.takeIf { it.isNotBlank() }
                        )
                    )
                    continue
                }

                for (j in 0 until forms.length()) {
                    val form = forms.getJSONObject(j)
                    val rawName = form.optString("name").ifBlank { varietyName }
                    val display = pretty(rawName)
                    val defaultForm = varietyDefault && j == 0
                    val needsExactFormSprite = forms.length() > 1 || rawName != varietyName
                    val formJson = if(needsExactFormSprite) {
                        runCatching {
                            JSONObject(fetch(form.optString("url")))
                        }.getOrNull()
                    } else null
                    val formSprites = formJson?.optJSONObject("sprites")
                    val normalSprite = formSprites?.optString("front_default")?.takeIf { it.isNotBlank() }
                    val shinySprite = formSprites?.optString("front_shiny")?.takeIf { it.isNotBlank() }
                    val official = pokemonJson?.optJSONObject("sprites")
                        ?.optJSONObject("other")
                        ?.optJSONObject("official-artwork")
                    add(
                        PokemonFormVariant(
                            name = display,
                            pokemonId = pid,
                            isDefault = defaultForm,
                            kind = classify(display, defaultForm),
                            formKey = rawName,
                            spriteUrl = normalSprite ?: official?.optString("front_default")?.takeIf { it.isNotBlank() },
                            shinySpriteUrl = shinySprite ?: official?.optString("front_shiny")?.takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }

        cache[id] = result
        return result
    }

    fun collectible(id:Int):List<PokemonFormVariant> =
        (cached(id) ?: load(id))
            .distinctBy{it.formKey}
            .sortedWith(
                compareByDescending<PokemonFormVariant>{it.isDefault}
                    .thenBy{kindOrder(it.kind)}
                    .thenBy{it.name}
            )

    fun livingDexForms(id:Int):List<PokemonFormVariant> =
        collectible(id).filter{it.countsForLivingDex}

    internal fun classify(name:String,isDefault:Boolean):PokemonFormKind{
        if(isDefault) return PokemonFormKind.DEFAULT
        val n=name.lowercase()
        return when{
            listOf("alola","galar","hisui","paldea").any{it in n} -> PokemonFormKind.REGIONAL
            "female" in n || "male" in n -> PokemonFormKind.GENDER
            listOf(
                "mega","gmax","gigantamax","primal","eternamax",
                "busted","school","zen","blade","shield","complete",
                "crowned","gulping","gorging","hero","hangry",
                "sunny","rainy","snowy","attack","defense","speed"
            ).any{it in n} -> PokemonFormKind.BATTLE
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
            listOf(
                "cap","cosplay","partner","original","hoenn","sinnoh","unova","kalos","alola",
                "world","fancy","pokeball","poke ball","garden","meadow","marine","archipelago",
                "high plains","sandstorm","river","monsoon","savanna","sun","ocean","jungle",
                "elegant","modern","polar","tundra","continental","icy snow",
                "debutante","diamond","heart","kabuki","la reine","matron","dandy","star",
                "lemon","matcha","mint","ruby","salted","ruby swirl","caramel swirl","rainbow swirl",
                "unown","spinda"
            ).any{it in n} -> PokemonFormKind.COSMETIC
            listOf("starter","battle bond","ash").any{it in n} -> PokemonFormKind.SPECIAL
            else -> PokemonFormKind.OTHER
        }
    }

    private fun kindOrder(kind:PokemonFormKind):Int = when(kind){
        PokemonFormKind.DEFAULT -> 0
        PokemonFormKind.REGIONAL -> 1
        PokemonFormKind.SPECIAL -> 2
        PokemonFormKind.GENDER -> 3
        PokemonFormKind.COSMETIC -> 4
        PokemonFormKind.BATTLE -> 5
        PokemonFormKind.OTHER -> 6
    }

    private fun pretty(raw:String):String =
        raw.split('-').joinToString(" "){part->
            part.replaceFirstChar{ch->ch.uppercase()}
        }

    private fun fetch(url:String):String =
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
}
