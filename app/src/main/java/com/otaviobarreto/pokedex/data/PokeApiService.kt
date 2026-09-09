package com.otaviobarreto.pokedex.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Remote source used while the full offline database is being assembled.
 * UI code never talks to the network directly; this class can later be
 * replaced by a Room/asset-backed implementation without changing screens.
 */
object PokeApiService {
    private const val API = "https://pokeapi.co/api/v2"

    data class DexIndexEntry(
        val id: Int,
        val name: String,
        val generation: Int
    ) {
        val spriteUrl: String
            get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
    }

    data class RemoteMove(
        val name: String,
        val methods: List<String>
    )

    data class RemotePokemonDetail(
        val id: Int,
        val name: String,
        val heightDecimeters: Int,
        val weightHectograms: Int,
        val types: List<String>,
        val stats: PokemonStats,
        val abilities: List<String>,
        val moves: List<RemoteMove>
    ) {
        val spriteUrl: String
            get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
    }

    fun loadNationalDex(limit: Int = 1025): List<DexIndexEntry> {
        val json = getJson("$API/pokemon?limit=$limit&offset=0")
        val results = json.getJSONArray("results")
        return buildList(results.length()) {
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val id = item.getString("url").trimEnd('/').substringAfterLast('/').toInt()
                add(
                    DexIndexEntry(
                        id = id,
                        name = item.getString("name").toDisplayName(),
                        generation = generationForNationalDexId(id)
                    )
                )
            }
        }.sortedBy { it.id }
    }

    fun loadPokemon(id: Int): RemotePokemonDetail {
        val json = getJson("$API/pokemon/$id")

        val typesJson = json.getJSONArray("types")
        val types = buildList(typesJson.length()) {
            for (i in 0 until typesJson.length()) {
                add(typesJson.getJSONObject(i).getJSONObject("type").getString("name").toDisplayName())
            }
        }

        val statMap = mutableMapOf<String, Int>()
        val statsJson = json.getJSONArray("stats")
        for (i in 0 until statsJson.length()) {
            val stat = statsJson.getJSONObject(i)
            statMap[stat.getJSONObject("stat").getString("name")] = stat.getInt("base_stat")
        }

        val abilitiesJson = json.getJSONArray("abilities")
        val abilities = buildList(abilitiesJson.length()) {
            for (i in 0 until abilitiesJson.length()) {
                val item = abilitiesJson.getJSONObject(i)
                val abilityName = item.getJSONObject("ability").getString("name").toDisplayName()
                add(if (item.optBoolean("is_hidden")) "$abilityName (Oculta)" else abilityName)
            }
        }

        val movesJson = json.getJSONArray("moves")
        val moves = buildList(movesJson.length()) {
            for (i in 0 until movesJson.length()) {
                val move = movesJson.getJSONObject(i)
                val methods = linkedSetOf<String>()
                val details = move.getJSONArray("version_group_details")
                for (j in 0 until details.length()) {
                    val method = details.getJSONObject(j)
                        .getJSONObject("move_learn_method")
                        .getString("name")
                        .toDisplayName()
                    methods += method
                }
                add(RemoteMove(move.getJSONObject("move").getString("name").toDisplayName(), methods.toList()))
            }
        }.sortedBy { it.name }

        return RemotePokemonDetail(
            id = json.getInt("id"),
            name = json.getString("name").toDisplayName(),
            heightDecimeters = json.getInt("height"),
            weightHectograms = json.getInt("weight"),
            types = types,
            stats = PokemonStats(
                hp = statMap["hp"] ?: 0,
                attack = statMap["attack"] ?: 0,
                defense = statMap["defense"] ?: 0,
                specialAttack = statMap["special-attack"] ?: 0,
                specialDefense = statMap["special-defense"] ?: 0,
                speed = statMap["speed"] ?: 0
            ),
            abilities = abilities,
            moves = moves
        )
    }

    private fun getJson(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 12_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.connect()
        return try {
            if (connection.responseCode !in 200..299) {
                error("HTTP ${connection.responseCode} while loading $url")
            }
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }
}

fun generationForNationalDexId(id: Int): Int = when (id) {
    in 1..151 -> 1
    in 152..251 -> 2
    in 252..386 -> 3
    in 387..493 -> 4
    in 494..649 -> 5
    in 650..721 -> 6
    in 722..809 -> 7
    in 810..905 -> 8
    in 906..1025 -> 9
    else -> 0
}

private fun String.toDisplayName(): String =
    split('-', ' ').joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
