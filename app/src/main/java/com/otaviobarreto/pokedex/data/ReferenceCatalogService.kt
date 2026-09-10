package com.otaviobarreto.pokedex.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class ReferenceEntry(val name: String, val url: String)

object ReferenceCatalogService {
    private val client = OkHttpClient()
    private val cache = ConcurrentHashMap<String, List<ReferenceEntry>>()

    fun load(kind: String): List<ReferenceEntry> {
        cache[kind]?.let { return it }
        require(kind in setOf("move", "ability", "item"))
        val request = Request.Builder()
            .url("https://pokeapi.co/api/v2/$kind?limit=2500")
            .header("User-Agent", "POKEDEX-Android")
            .build()
        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            response.body?.string() ?: error("Resposta vazia")
        }
        val array = JSONObject(body).getJSONArray("results")
        val entries = buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(ReferenceEntry(item.getString("name"), item.getString("url")))
            }
        }
        cache[kind] = entries
        return entries
    }
}
