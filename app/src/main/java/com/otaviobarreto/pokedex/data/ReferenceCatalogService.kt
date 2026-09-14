package com.otaviobarreto.pokedex.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class ReferenceEntry(val name: String, val url: String)

data class ReferenceDetail(
    val kind: String,
    val name: String,
    val description: String?,
    val type: String? = null,
    val category: String? = null,
    val power: Int? = null,
    val accuracy: Int? = null,
    val pp: Int? = null,
    val priority: Int? = null,
    val pokemonIds: List<Int> = emptyList(),
    val pokemonNames: List<String> = emptyList(),
    val spriteUrl: String? = null
)

object ReferenceCatalogService {
    private val client = OkHttpClient()
    private val cache = ConcurrentHashMap<String, List<ReferenceEntry>>()
    private val detailCache = ConcurrentHashMap<String, ReferenceDetail>()

    fun cached(kind: String): List<ReferenceEntry>? = cache[kind]

    fun cachedDetail(kind: String, entry: ReferenceEntry, context: GameContext? = null): ReferenceDetail? =
        detailCache[detailKey(kind, entry, context)]

    fun load(kind: String): List<ReferenceEntry> {
        cache[kind]?.let { return it }
        require(kind in setOf("move", "ability", "item"))
        val json = getJson("https://pokeapi.co/api/v2/$kind?limit=2500")
        val array = json.getJSONArray("results")
        val entries = buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(ReferenceEntry(item.getString("name"), item.getString("url")))
            }
        }
        cache[kind] = entries
        return entries
    }

    fun loadDetail(kind: String, entry: ReferenceEntry, context: GameContext? = null): ReferenceDetail {
        val key = detailKey(kind, entry, context)
        detailCache[key]?.let { return it }
        val json = getJson(entry.url)
        val detail = when (kind) {
            "move" -> parseMove(json, context)
            "ability" -> parseAbility(json)
            "item" -> parseItem(json)
            else -> error("Tipo de referência inválido: $kind")
        }
        detailCache[key] = detail
        return detail
    }

    private fun parseMove(json: JSONObject, context: GameContext?): ReferenceDetail {
        val learned = json.optJSONArray("learned_by_pokemon") ?: JSONArray()
        val moveName = pretty(json.getString("name"))
        val ids = mutableListOf<Int>()
        val names = mutableListOf<String>()
        for (i in 0 until learned.length()) {
            val p = learned.getJSONObject(i)
            val id = idFromUrl(p.optString("url")) ?: continue
            val name = pretty(p.optString("name"))
            if(context==null || pokemonLearnsMoveInContext(id, moveName, context)){
                ids += id
                names += name
            }
            if(ids.size>=80) break
        }
        return ReferenceDetail(
            kind = "move",
            name = pretty(json.getString("name")),
            description = localizedEffect(json.optJSONArray("effect_entries")),
            type = contextualMoveType(pretty(json.optJSONObject("type")?.optString("name").orEmpty()), moveName, context).ifBlank { null },
            category = contextualMoveCategory(pretty(json.optJSONObject("damage_class")?.optString("name").orEmpty()), pretty(json.optJSONObject("type")?.optString("name").orEmpty()), context).ifBlank { null },
            power = json.optNullableInt("power"),
            accuracy = json.optNullableInt("accuracy"),
            pp = json.optNullableInt("pp"),
            priority = json.optNullableInt("priority"),
            pokemonIds = ids.take(80),
            pokemonNames = names.take(80)
        )
    }

    private fun pokemonLearnsMoveInContext(id: Int, moveName: String, context: GameContext): Boolean =
        runCatching {
            PokeApiService.loadPokemon(id).moves.any { move ->
                normalize(move.name) == normalize(moveName) &&
                    move.learnDetails.any { context.matchesVersionGroup(it.versionGroup) }
            }
        }.getOrDefault(false)

    private fun contextualMoveCategory(category: String, type: String, context: GameContext?): String {
        if (category.equals("Status", true)) return "Status"
        if (context?.label == "FireRed / LeafGreen") {
            val physical = setOf("Normal","Fighting","Flying","Poison","Ground","Rock","Bug","Ghost","Steel")
            val special = setOf("Fire","Water","Grass","Electric","Psychic","Ice","Dragon","Dark")
            return when (type) {
                in physical -> "Physical"
                in special -> "Special"
                else -> category
            }
        }
        return category
    }

    private fun contextualMoveType(type: String, moveName: String, context: GameContext?): String {
        if (context?.label != "FireRed / LeafGreen") return type
        return when (normalize(moveName)) {
            "charm", "moonlight", "sweetkiss" -> "Normal"
            else -> type
        }
    }

    private fun localizedEffect(array: JSONArray?): String? {
        if (array == null) return null
        val preferred = listOf("pt-BR","pt")
        preferred.forEach { lang ->
            for (i in 0 until array.length()) {
                val e = array.getJSONObject(i)
                if (e.optJSONObject("language")?.optString("name").equals(lang, true)) {
                    return clean(e.optString("short_effect").ifBlank { e.optString("effect") })
                }
            }
        }
        val en = englishEffect(array) ?: return null
        return translateEffect(en)
    }

    private fun translateEffect(value: String): String {
        val exact = mapOf(
            "Has double power if the user has no held item." to "O poder é dobrado se o usuário não estiver segurando nenhum item.",
            "Power is doubled if the target has already received damage this turn." to "O poder é dobrado se o alvo já tiver sofrido dano neste turno.",
            "Inflicts regular damage." to "Causa dano normal."
        )
        exact[value]?.let { return it }
        return value
            .replace("Has a ", "Tem ")
            .replace("% chance to ", "% de chance de ")
            .replace("the target", "o alvo", ignoreCase = true)
            .replace("the user", "o usuário", ignoreCase = true)
            .replace("this turn", "neste turno", ignoreCase = true)
            .replace("damage", "dano", ignoreCase = true)
            .replace("power", "poder", ignoreCase = true)
            .replace("accuracy", "precisão", ignoreCase = true)
            .replace("Raises ", "Aumenta ", ignoreCase = true)
            .replace("Lowers ", "Reduz ", ignoreCase = true)
    }

    private fun detailKey(kind: String, entry: ReferenceEntry, context: GameContext?): String =
        kind + ":" + entry.url + ":" + (context?.label ?: "global")

    private fun normalize(value: String): String = value.lowercase().replace(Regex("[^a-z0-9]+"), "")

    private fun parseAbility(json: JSONObject): ReferenceDetail {
        val pokemon = json.optJSONArray("pokemon") ?: JSONArray()
        val ids = mutableListOf<Int>()
        val names = mutableListOf<String>()
        for (i in 0 until pokemon.length()) {
            val p = pokemon.getJSONObject(i).optJSONObject("pokemon") ?: continue
            idFromUrl(p.optString("url"))?.let { ids += it }
            names += pretty(p.optString("name"))
        }
        return ReferenceDetail(
            kind = "ability",
            name = pretty(json.getString("name")),
            description = englishEffect(json.optJSONArray("effect_entries")),
            pokemonIds = ids.take(80),
            pokemonNames = names.take(80)
        )
    }

    private fun parseItem(json: JSONObject): ReferenceDetail {
        val category = pretty(json.optJSONObject("category")?.optString("name").orEmpty()).ifBlank { null }
        val sprite = json.optJSONObject("sprites")?.optString("default")?.takeIf { it.isNotBlank() && it != "null" }
        return ReferenceDetail(
            kind = "item",
            name = pretty(json.getString("name")),
            description = englishEffect(json.optJSONArray("effect_entries")) ?: englishFlavor(json.optJSONArray("flavor_text_entries")),
            category = category,
            spriteUrl = sprite
        )
    }

    private fun englishEffect(array: JSONArray?): String? {
        if (array == null) return null
        for (i in 0 until array.length()) {
            val e = array.getJSONObject(i)
            if (e.optJSONObject("language")?.optString("name") == "en") {
                return clean(e.optString("short_effect").ifBlank { e.optString("effect") })
            }
        }
        return null
    }

    private fun englishFlavor(array: JSONArray?): String? {
        if (array == null) return null
        for (i in 0 until array.length()) {
            val e = array.getJSONObject(i)
            if (e.optJSONObject("language")?.optString("name") == "en") return clean(e.optString("text"))
        }
        return null
    }

    private fun getJson(url: String): JSONObject {
        val body = PersistentApiCache.getOrFetch(url) {
            val request = Request.Builder().url(url).header("User-Agent", "POKEDEX-Android").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                response.body?.string() ?: error("Resposta vazia")
            }
        }
        return JSONObject(body)
    }

    private fun JSONObject.optNullableInt(key: String): Int? = if (has(key) && !isNull(key)) optInt(key) else null
    private fun idFromUrl(url: String): Int? = url.trimEnd('/').substringAfterLast('/').toIntOrNull()
    private fun clean(value: String): String = value.replace('\n', ' ').replace('\u000c', ' ').replace(Regex("\\s+"), " ").trim()
    private fun pretty(value: String): String = value.split('-', ' ').filter { it.isNotBlank() }.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
