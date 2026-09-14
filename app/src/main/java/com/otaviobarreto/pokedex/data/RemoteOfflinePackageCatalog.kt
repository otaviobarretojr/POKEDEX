package com.otaviobarreto.pokedex.data

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object RemoteOfflinePackageCatalog {
    private const val SUPABASE_URL = "https://thhhpzxlletdhhhprawv.supabase.co"
    private const val PUBLISHABLE_KEY = "sb_publishable_kKuaZGT4lyA8kh1Sq2CCrA_VEJAXLKZ"
    private const val ENDPOINT = "$SUPABASE_URL/rest/v1/pokedex_offline_packages" +
        "?select=package_key,display_name,package_type,version,size_bytes,sha256,download_url,status,updated_at" +
        "&order=package_type.asc,display_name.asc"

    data class RemotePackage(
        val packageKey:String,
        val displayName:String,
        val packageType:String,
        val version:Int,
        val sizeBytes:Long?,
        val sha256:String?,
        val downloadUrl:String?,
        val status:String,
        val updatedAt:String?
    ){
        val ready:Boolean
            get() = status=="ready" &&
                !downloadUrl.isNullOrBlank() &&
                (sizeBytes ?: 0L) > 0L &&
                !sha256.isNullOrBlank()
    }

    private val memory=ConcurrentHashMap<String,RemotePackage>()
    @Volatile private var lastLoadedAt=0L
    private const val CACHE_MS=5L*60L*1000L

    fun general():RemotePackage? = memory["general"]

    fun forGameLabel(gameLabel:String):RemotePackage? =
        memory[packageKeyForGame(gameLabel)]

    fun all():List<RemotePackage> = memory.values.sortedBy{it.displayName}

    fun refresh(force:Boolean=false):List<RemotePackage> {
        val now=System.currentTimeMillis()
        if(!force && memory.isNotEmpty() && now-lastLoadedAt<CACHE_MS){
            return all()
        }

        val conn=(URL(ENDPOINT).openConnection() as HttpURLConnection).apply{
            requestMethod="GET"
            connectTimeout=8_000
            readTimeout=12_000
            setRequestProperty("apikey",PUBLISHABLE_KEY)
            setRequestProperty("Accept","application/json")
        }

        return try{
            val code=conn.responseCode
            check(code in 200..299){"Manifest HTTP $code"}
            val body=conn.inputStream.bufferedReader().use{it.readText()}
            val json=JSONArray(body)
            val loaded=buildList{
                for(i in 0 until json.length()){
                    val item=json.getJSONObject(i)
                    add(
                        RemotePackage(
                            packageKey=item.getString("package_key"),
                            displayName=item.getString("display_name"),
                            packageType=item.getString("package_type"),
                            version=item.optInt("version",0),
                            sizeBytes=if(item.isNull("size_bytes")) null else item.optLong("size_bytes"),
                            sha256=item.optString("sha256").takeIf{it.isNotBlank()},
                            downloadUrl=item.optString("download_url").takeIf{it.isNotBlank()},
                            status=item.optString("status","building"),
                            updatedAt=item.optString("updated_at").takeIf{it.isNotBlank()}
                        )
                    )
                }
            }
            memory.clear()
            loaded.forEach{memory[it.packageKey]=it}
            lastLoadedAt=now
            all()
        }finally{
            conn.disconnect()
        }
    }

    fun packageKeyForGame(gameLabel:String):String = when(gameLabel){
        "Pokémon Legends: Z-A" -> "legends-za"
        "Pokémon Scarlet / Violet" -> "scarlet-violet"
        "Pokémon Sword / Shield" -> "sword-shield"
        "Pokémon Let's Go Pikachu / Eevee" -> "lets-go"
        "Pokémon Legends: Arceus" -> "legends-arceus"
        "Pokémon Brilliant Diamond / Shining Pearl" -> "bdsp"
        "Pokémon FireRed / LeafGreen" -> "firered-leafgreen"
        else -> gameLabel.lowercase()
            .replace(Regex("[^a-z0-9]+"),"-")
            .trim('-')
    }
}
