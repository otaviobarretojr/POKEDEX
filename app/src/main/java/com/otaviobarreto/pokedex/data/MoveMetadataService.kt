package com.otaviobarreto.pokedex.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

data class MoveMetadata(
    val type:String,
    val category:String,
    val power:Int?,
    val accuracy:Int?,
    val pp:Int?,
    val priority:Int,
    val machineLabel:String?,
    val effect:String?
)

object MoveMetadataService {
    private val memory=ConcurrentHashMap<String,MoveMetadata>()
    private val gen3PhysicalTypes=setOf("Normal","Fighting","Flying","Poison","Ground","Rock","Bug","Ghost","Steel")
    private val gen3SpecialTypes=setOf("Fire","Water","Grass","Electric","Psychic","Ice","Dragon","Dark")

    fun cached(url:String,context:GameContext?):MoveMetadata? =
        memory[cacheKey(url,context)]

    fun effectiveCategory(metadata:MoveMetadata,context:GameContext?):String {
        if(metadata.category.equals("Status",true)) return "Status"
        if(context?.label=="FireRed / LeafGreen"){
            return when(metadata.type){
                in gen3PhysicalTypes -> "Physical"
                in gen3SpecialTypes -> "Special"
                else -> metadata.category
            }
        }
        return metadata.category
    }

    fun load(url:String,context:GameContext?):MoveMetadata {
        val key=cacheKey(url,context)
        memory[key]?.let{return it}
        val json=getJson(url)
        val type=json.optJSONObject("type")?.optString("name").orEmpty().toDisplayName()
        val category=json.optJSONObject("damage_class")?.optString("name").orEmpty().toDisplayName()
        val power=json.optInt("power").takeIf{json.has("power") && !json.isNull("power")}
        val accuracy=json.optInt("accuracy").takeIf{json.has("accuracy") && !json.isNull("accuracy")}
        val pp=json.optInt("pp").takeIf{json.has("pp") && !json.isNull("pp")}
        val priority=json.optInt("priority",0)
        var effect:String?=null
        val effects=json.optJSONArray("effect_entries")
        if(effects!=null){
            for(i in 0 until effects.length()){
                val entry=effects.optJSONObject(i)?:continue
                if(entry.optJSONObject("language")?.optString("name")=="en"){
                    effect=entry.optString("short_effect").replace("\$effect_chance","chance").takeIf{it.isNotBlank()}
                    break
                }
            }
        }

        var machineLabel:String?=null
        val machines=json.optJSONArray("machines")
        if(machines!=null){
            for(i in 0 until machines.length()){
                val entry=machines.optJSONObject(i)?:continue
                val group=entry.optJSONObject("version_group")?.optString("name").orEmpty().toDisplayName()
                if(context!=null && !context.matchesVersionGroup(group)) continue
                val machineUrl=entry.optJSONObject("machine")?.optString("url").orEmpty()
                if(machineUrl.isBlank()) continue
                val machine=runCatching{getJson(machineUrl)}.getOrNull()?:continue
                val item=machine.optJSONObject("item")?.optString("name").orEmpty()
                machineLabel=item.uppercase().replace("-"," ").takeIf{it.isNotBlank()}
                if(machineLabel!=null) break
            }
        }

        return MoveMetadata(type,category,power,accuracy,pp,priority,machineLabel,effect).also{memory[key]=it}
    }

    private fun cacheKey(url:String,context:GameContext?)=url+"|"+(context?.label.orEmpty())

    private fun getJson(url:String)=JSONObject(
        PersistentApiCache.getOrFetch(url){
            val connection=URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout=12_000
            connection.readTimeout=12_000
            connection.requestMethod="GET"
            connection.setRequestProperty("Accept","application/json")
            connection.connect()
            try{
                if(connection.responseCode !in 200..299) error("HTTP ${connection.responseCode} while loading $url")
                connection.inputStream.bufferedReader().use{it.readText()}
            }finally{
                connection.disconnect()
            }
        }
    )

    private fun String.toDisplayName():String =
        split('-',' ').joinToString(" "){part->part.replaceFirstChar{it.uppercase()}}
}
