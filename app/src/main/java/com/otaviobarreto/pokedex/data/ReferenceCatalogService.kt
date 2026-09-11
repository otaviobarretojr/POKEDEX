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

    fun cachedDetail(kind: String, entry: ReferenceEntry): ReferenceDetail? =
        detailCache[kind + ":" + entry.url]

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

    fun loadDetail(kind: String, entry: ReferenceEntry): ReferenceDetail {
        val key = "$kind:${entry.url}"
        detailCache[key]?.let { return it }
        val json = getJson(entry.url)
        val detail = when (kind) {
            "move" -> parseMove(json)
            "ability" -> parseAbility(json)
            "item" -> parseItem(json)
            else -> error("Tipo de referência inválido: $kind")
        }
        detailCache[key] = detail
        return detail
    }

    private fun parseMove(json: JSONObject): ReferenceDetail {
        val learned = json.optJSONArray("learned_by_pokemon") ?: JSONArray()
        val ids = mutableListOf<Int>()
        val names = mutableListOf<String>()
        for (i in 0 until learned.length()) {
            val p = learned.getJSONObject(i)
            idFromUrl(p.optString("url"))?.let { ids += it }
            names += pretty(p.optString("name"))
        }
        return ReferenceDetail(
            kind = "move",
            name = pretty(json.getString("name")),
            description = englishEffect(json.optJSONArray("effect_entries")),
            type = pretty(json.optJSONObject("type")?.optString("name").orEmpty()).ifBlank { null },
            category = pretty(json.optJSONObject("damage_class")?.optString("name").orEmpty()).ifBlank { null },
            power = json.optNullableInt("power"),
            accuracy = json.optNullableInt("accuracy"),
            pp = json.optNullableInt("pp"),
            priority = json.optNullableInt("priority"),
            pokemonIds = ids.take(80),
            pokemonNames = names.take(80)
        )
    }

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
