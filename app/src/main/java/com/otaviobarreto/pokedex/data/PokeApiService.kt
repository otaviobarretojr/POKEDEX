package com.otaviobarreto.pokedex.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object PokeApiService {
    private const val API = "https://pokeapi.co/api/v2"

    data class DexIndexEntry(val id:Int,val name:String,val generation:Int){
        val spriteUrl:String get()="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
    }

    data class MoveLearnDetail(
        val versionGroup:String,
        val method:String,
        val level:Int
    )

    data class RemoteMove(
        val name:String,
        val methods:List<String>,
        val learnDetails:List<MoveLearnDetail> = emptyList()
    )

    data class RemotePokemonDetail(
        val id:Int,
        val name:String,
        val heightDecimeters:Int,
        val weightHectograms:Int,
        val types:List<String>,
        val stats:PokemonStats,
        val abilities:List<String>,
        val moves:List<RemoteMove>
    ){
        val spriteUrl:String get()="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
    }

    data class SpeciesInfo(val captureRate:Int,val baseHappiness:Int,val habitat:String?,val growthRate:String?,val eggGroups:List<String>,val flavorText:String?,val evolutionChainUrl:String?)
    data class EvolutionStage(val pokemonId:Int,val name:String,val requirement:String?)
    data class EncounterDetail(val version:String,val method:String,val minLevel:Int,val maxLevel:Int,val chance:Int,val conditions:List<String>)
    data class EncounterLocation(val location:String,val versions:List<String>,val details:List<EncounterDetail> = emptyList())

    fun loadNationalDex(limit:Int=1025):List<DexIndexEntry>{
        val json=getJson("$API/pokemon?limit=$limit&offset=0")
        val results=json.getJSONArray("results")
        return buildList(results.length()){
            for(i in 0 until results.length()){
                val item=results.getJSONObject(i)
                val id=idFromUrl(item.getString("url"))
                if(id in 1..limit)add(DexIndexEntry(id,item.getString("name").toDisplayName(),generationForNationalDexId(id)))
            }
        }.sortedBy{it.id}
    }

    fun loadPokemonIdsForType(type:String):Set<Int>{
        val json=getJson("$API/type/${type.lowercase()}")
        val array=json.getJSONArray("pokemon")
        return buildSet{
            for(i in 0 until array.length()){
                val id=idFromUrl(array.getJSONObject(i).getJSONObject("pokemon").getString("url"))
                if(id in 1..1025)add(id)
            }
        }
    }

    fun loadPokemon(id:Int):RemotePokemonDetail{
        val json=getJson("$API/pokemon/$id")
        val typesJson=json.getJSONArray("types")
        val types=buildList(typesJson.length()){
            for(i in 0 until typesJson.length())add(typesJson.getJSONObject(i).getJSONObject("type").getString("name").toDisplayName())
        }

        val statMap=mutableMapOf<String,Int>()
        val statsJson=json.getJSONArray("stats")
        for(i in 0 until statsJson.length()){
            val stat=statsJson.getJSONObject(i)
            statMap[stat.getJSONObject("stat").getString("name")]=stat.getInt("base_stat")
        }

        val abilitiesJson=json.getJSONArray("abilities")
        val abilities=buildList(abilitiesJson.length()){
            for(i in 0 until abilitiesJson.length()){
                val item=abilitiesJson.getJSONObject(i)
                val name=item.getJSONObject("ability").getString("name").toDisplayName()
                add(if(item.optBoolean("is_hidden"))"$name (Oculta)" else name)
            }
        }

        val movesJson=json.getJSONArray("moves")
        val moves=buildList(movesJson.length()){
            for(i in 0 until movesJson.length()){
                val move=movesJson.getJSONObject(i)
                val methods=linkedSetOf<String>()
                val learnDetails=mutableListOf<MoveLearnDetail>()
                val details=move.getJSONArray("version_group_details")
                for(j in 0 until details.length()){
                    val detail=details.getJSONObject(j)
                    val method=detail.getJSONObject("move_learn_method").getString("name").toDisplayName()
                    methods+=method
                    learnDetails+=MoveLearnDetail(
                        versionGroup=detail.getJSONObject("version_group").getString("name").toDisplayName(),
                        method=method,
                        level=detail.optInt("level_learned_at",0)
                    )
                }
                add(RemoteMove(move.getJSONObject("move").getString("name").toDisplayName(),methods.toList(),learnDetails.distinct()))
            }
        }.sortedBy{it.name}

        return RemotePokemonDetail(
            id=json.getInt("id"),
            name=json.getString("name").toDisplayName(),
            heightDecimeters=json.getInt("height"),
            weightHectograms=json.getInt("weight"),
            types=types,
            stats=PokemonStats(
                hp=statMap["hp"]?:0,
                attack=statMap["attack"]?:0,
                defense=statMap["defense"]?:0,
                specialAttack=statMap["special-attack"]?:0,
                specialDefense=statMap["special-defense"]?:0,
                speed=statMap["speed"]?:0
            ),
            abilities=abilities,
            moves=moves
        )
    }

    fun loadSpecies(id:Int):SpeciesInfo{
        val json=getJson("$API/pokemon-species/$id")
        val flavorEntries=json.getJSONArray("flavor_text_entries")
        var flavor:String?=null
        for(i in 0 until flavorEntries.length()){
            val entry=flavorEntries.getJSONObject(i)
            if(entry.getJSONObject("language").getString("name")=="en"){
                flavor=entry.getString("flavor_text").replace('\n',' ').replace('\u000c',' ').replace(Regex("\\s+")," ").trim()
                break
            }
        }
        return SpeciesInfo(
            captureRate=json.optInt("capture_rate"),
            baseHappiness=json.optInt("base_happiness"),
            habitat=json.optJSONObject("habitat")?.optString("name")?.toDisplayName(),
            growthRate=json.optJSONObject("growth_rate")?.optString("name")?.toDisplayName(),
            eggGroups=json.getJSONArray("egg_groups").namesFromNamedResources(),
            flavorText=flavor,
            evolutionChainUrl=json.optJSONObject("evolution_chain")?.optString("url")
        )
    }

    fun loadEvolutionChain(url:String):List<EvolutionStage>{
        val root=getJson(url).getJSONObject("chain")
        val result=mutableListOf<EvolutionStage>()
        fun walk(node:JSONObject){
            val species=node.getJSONObject("species")
            val details=node.optJSONArray("evolution_details")
            result+=EvolutionStage(
                pokemonId=idFromUrl(species.getString("url")),
                name=species.getString("name").toDisplayName(),
                requirement=details?.takeIf{it.length()>0}?.getJSONObject(0)?.let(::evolutionRequirement)
            )
            val evolvesTo=node.getJSONArray("evolves_to")
            for(i in 0 until evolvesTo.length())walk(evolvesTo.getJSONObject(i))
        }
        walk(root)
        return result
    }

    fun loadEncounters(id:Int):List<EncounterLocation>{
        val array=getJsonArray("$API/pokemon/$id/encounters")
        return buildList(array.length()){
            for(i in 0 until array.length()){
                val item=array.getJSONObject(i)
                val versionDetails=item.getJSONArray("version_details")
                val versionNames=mutableListOf<String>()
                val encounterDetails=mutableListOf<EncounterDetail>()
                for(j in 0 until versionDetails.length()){
                    val versionDetail=versionDetails.getJSONObject(j)
                    val versionName=versionDetail.getJSONObject("version").getString("name").toDisplayName()
                    versionNames+=versionName
                    val encounters=versionDetail.optJSONArray("encounter_details")?:JSONArray()
                    for(k in 0 until encounters.length()){
                        val encounter=encounters.getJSONObject(k)
                        val conditionsJson=encounter.optJSONArray("condition_values")?:JSONArray()
                        val conditions=buildList(conditionsJson.length()){
                            for(c in 0 until conditionsJson.length())add(conditionsJson.getJSONObject(c).getString("name").toDisplayName())
                        }
                        encounterDetails+=EncounterDetail(
                            version=versionName,
                            method=encounter.optJSONObject("method")?.optString("name")?.toDisplayName().orEmpty().ifBlank{"Encontro"},
                            minLevel=encounter.optInt("min_level",0),
                            maxLevel=encounter.optInt("max_level",0),
                            chance=encounter.optInt("chance",0),
                            conditions=conditions
                        )
                    }
                }
                add(EncounterLocation(item.getJSONObject("location_area").getString("name").toDisplayName(),versionNames.distinct(),encounterDetails.distinct()))
            }
        }.distinctBy{it.location}
    }

    private fun evolutionRequirement(detail:JSONObject):String?{
        val pieces=mutableListOf<String>()
        detail.optInt("min_level").takeIf{it>0}?.let{pieces+="Nível $it"}
        detail.optJSONObject("item")?.optString("name")?.takeIf{it.isNotBlank()}?.let{pieces+=it.toDisplayName()}
        detail.optJSONObject("held_item")?.optString("name")?.takeIf{it.isNotBlank()}?.let{pieces+="Segurando ${it.toDisplayName()}"}
        detail.optInt("min_happiness").takeIf{it>0}?.let{pieces+="Amizade $it+"}
        detail.optString("time_of_day").takeIf{it.isNotBlank()}?.let{pieces+=it.toDisplayName()}
        detail.optJSONObject("trigger")?.optString("name")?.takeIf{it.isNotBlank()&&pieces.isEmpty()}?.let{pieces+=it.toDisplayName()}
        return pieces.takeIf{it.isNotEmpty()}?.joinToString(" • ")
    }

    private fun getJson(url:String):JSONObject=JSONObject(getText(url))
    private fun getJsonArray(url:String):JSONArray=JSONArray(getText(url))
    private fun getText(url:String):String{
        val connection=URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout=12_000;connection.readTimeout=12_000;connection.requestMethod="GET";connection.setRequestProperty("Accept","application/json");connection.connect()
        return try{if(connection.responseCode !in 200..299)error("HTTP ${connection.responseCode} while loading $url");connection.inputStream.bufferedReader().use{it.readText()}}finally{connection.disconnect()}
    }
    private fun idFromUrl(url:String):Int=url.trimEnd('/').substringAfterLast('/').toInt()
}

fun generationForNationalDexId(id:Int):Int=when(id){in 1..151->1;in 152..251->2;in 252..386->3;in 387..493->4;in 494..649->5;in 650..721->6;in 722..809->7;in 810..905->8;in 906..1025->9;else->0}
private fun JSONArray.namesFromNamedResources():List<String> = buildList(length()){for(i in 0 until length())add(getJSONObject(i).getString("name").toDisplayName())}
private fun String.toDisplayName():String=split('-',' ').joinToString(" "){part->part.replaceFirstChar{it.uppercase()}}
