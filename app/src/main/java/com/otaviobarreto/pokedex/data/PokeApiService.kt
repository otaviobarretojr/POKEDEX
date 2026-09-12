package com.otaviobarreto.pokedex.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object PokeApiService {
    const val MAX_NATIONAL_DEX_ID = 1025
    private const val API = "https://pokeapi.co/api/v2"
    fun pokemonUrl(id:Int) = "$API/pokemon/$id"
    fun speciesUrl(id:Int) = "$API/pokemon-species/$id"
    fun encountersUrl(id:Int) = "$API/pokemon/$id/encounters"
    data class DexIndexEntry(val id:Int,val name:String,val generation:Int){val spriteUrl:String get()="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"}
    data class MoveLearnDetail(val versionGroup:String,val method:String,val level:Int)
    data class RemoteMove(val name:String,val methods:List<String>,val learnDetails:List<MoveLearnDetail> = emptyList())
    data class RemotePokemonDetail(val id:Int,val name:String,val heightDecimeters:Int,val weightHectograms:Int,val types:List<String>,val stats:PokemonStats,val abilities:List<String>,val moves:List<RemoteMove>){val spriteUrl:String get()="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"}
    data class SpeciesInfo(val captureRate:Int,val baseHappiness:Int,val habitat:String?,val growthRate:String?,val eggGroups:List<String>,val flavorText:String?,val evolutionChainUrl:String?,val genus:String?=null)
    data class EvolutionStage(val pokemonId:Int,val name:String,val requirement:String?)
    data class EncounterDetail(val version:String,val method:String,val minLevel:Int,val maxLevel:Int,val chance:Int,val conditions:List<String>)
    data class EncounterLocation(val location:String,val versions:List<String>,val details:List<EncounterDetail> = emptyList())

    fun loadNationalDex(limit:Int=MAX_NATIONAL_DEX_ID):List<DexIndexEntry>{val json=getJson("$API/pokemon?limit=$limit&offset=0");val results=json.getJSONArray("results");return buildList(results.length()){for(i in 0 until results.length()){val item=results.getJSONObject(i);val id=idFromUrl(item.getString("url"));if(id in 1..limit)add(DexIndexEntry(id,item.getString("name").toDisplayName(),generationForNationalDexId(id)))}}.sortedBy{it.id}}
    fun loadPokemonIdsForType(type:String):Set<Int>{val json=getJson("$API/type/${type.lowercase()}");val array=json.getJSONArray("pokemon");return buildSet{for(i in 0 until array.length()){val id=idFromUrl(array.getJSONObject(i).getJSONObject("pokemon").getString("url"));if(id in 1..MAX_NATIONAL_DEX_ID)add(id)}}}

    fun loadPokemon(id:Int):RemotePokemonDetail{
        val json=getJson(pokemonUrl(id));val typesJson=json.getJSONArray("types");val types=buildList(typesJson.length()){for(i in 0 until typesJson.length())add(typesJson.getJSONObject(i).getJSONObject("type").getString("name").toDisplayName())}
        val statMap=mutableMapOf<String,Int>();val statsJson=json.getJSONArray("stats");for(i in 0 until statsJson.length()){val stat=statsJson.getJSONObject(i);statMap[stat.getJSONObject("stat").getString("name")]=stat.getInt("base_stat")}
        val abilitiesJson=json.getJSONArray("abilities");val abilities=buildList(abilitiesJson.length()){for(i in 0 until abilitiesJson.length()){val item=abilitiesJson.getJSONObject(i);val name=item.getJSONObject("ability").getString("name").toDisplayName();add(if(item.optBoolean("is_hidden"))"$name (Oculta)" else name)}}
        val movesJson=json.getJSONArray("moves");val moves=buildList(movesJson.length()){for(i in 0 until movesJson.length()){val move=movesJson.getJSONObject(i);val methods=linkedSetOf<String>();val learnDetails=mutableListOf<MoveLearnDetail>();val details=move.getJSONArray("version_group_details");for(j in 0 until details.length()){val detail=details.getJSONObject(j);val method=detail.getJSONObject("move_learn_method").getString("name").toDisplayName();methods+=method;learnDetails+=MoveLearnDetail(detail.getJSONObject("version_group").getString("name").toDisplayName(),method,detail.optInt("level_learned_at",0))};add(RemoteMove(move.getJSONObject("move").getString("name").toDisplayName(),methods.toList(),learnDetails.distinct()))}}.sortedBy{it.name}
        return RemotePokemonDetail(json.getInt("id"),json.getString("name").toDisplayName(),json.getInt("height"),json.getInt("weight"),types,PokemonStats(statMap["hp"]?:0,statMap["attack"]?:0,statMap["defense"]?:0,statMap["special-attack"]?:0,statMap["special-defense"]?:0,statMap["speed"]?:0),abilities,moves)
    }

    fun loadSpecies(id:Int):SpeciesInfo{
        val json=getJson(speciesUrl(id));val flavorEntries=json.getJSONArray("flavor_text_entries");var flavor:String?=null
        for(i in 0 until flavorEntries.length()){val entry=flavorEntries.getJSONObject(i);if(entry.getJSONObject("language").getString("name")=="en"){flavor=entry.getString("flavor_text").replace('\n',' ').replace('\u000c',' ').replace(Regex("\\s+")," ").trim();break}}
        var genus:String?=null;val genera=json.optJSONArray("genera")?:JSONArray();for(i in 0 until genera.length()){val g=genera.getJSONObject(i);if(g.optJSONObject("language")?.optString("name")=="en"){genus=g.optString("genus").takeIf{it.isNotBlank()};break}}
        return SpeciesInfo(json.optInt("capture_rate"),json.optInt("base_happiness"),json.optJSONObject("habitat")?.optString("name")?.toDisplayName(),json.optJSONObject("growth_rate")?.optString("name")?.toDisplayName(),json.getJSONArray("egg_groups").namesFromNamedResources(),flavor,json.optJSONObject("evolution_chain")?.optString("url"),genus)
    }

    fun loadEvolutionChain(url:String):List<EvolutionStage>{
        val root=getJson(url).getJSONObject("chain")
        val result=mutableListOf<EvolutionStage>()
        fun walk(node:JSONObject){
            val species=node.getJSONObject("species")
            val pokemonId=idFromUrl(species.getString("url"))
            val details=node.optJSONArray("evolution_details")
            val apiRequirements=buildList{
                if(details!=null){
                    for(i in 0 until details.length()){
                        details.optJSONObject(i)?.let(::evolutionRequirement)?.takeIf{it.isNotBlank()}?.let(::add)
                    }
                }
            }.distinct()
            val requirement=mergeEvolutionRequirements(
                pokemonId=pokemonId,
                apiRequirements=apiRequirements
            )
            result+=EvolutionStage(
                pokemonId,
                species.getString("name").toDisplayName(),
                requirement
            )
            val evolvesTo=node.getJSONArray("evolves_to")
            for(i in 0 until evolvesTo.length()) walk(evolvesTo.getJSONObject(i))
        }
        walk(root)
        return result
    }
    fun loadEncounters(id:Int):List<EncounterLocation>{val array=getJsonArray(encountersUrl(id));return buildList(array.length()){for(i in 0 until array.length()){val item=array.getJSONObject(i);val versionDetails=item.getJSONArray("version_details");val versionNames=mutableListOf<String>();val encounterDetails=mutableListOf<EncounterDetail>();for(j in 0 until versionDetails.length()){val versionDetail=versionDetails.getJSONObject(j);val versionName=versionDetail.getJSONObject("version").getString("name").toDisplayName();versionNames+=versionName;val encounters=versionDetail.optJSONArray("encounter_details")?:JSONArray();for(k in 0 until encounters.length()){val encounter=encounters.getJSONObject(k);val conditionsJson=encounter.optJSONArray("condition_values")?:JSONArray();val conditions=buildList(conditionsJson.length()){for(c in 0 until conditionsJson.length())add(conditionsJson.getJSONObject(c).getString("name").toDisplayName())};encounterDetails+=EncounterDetail(versionName,encounter.optJSONObject("method")?.optString("name")?.toDisplayName().orEmpty().ifBlank{"Encontro"},encounter.optInt("min_level",0),encounter.optInt("max_level",0),encounter.optInt("chance",0),conditions)}};add(EncounterLocation(item.getJSONObject("location_area").getString("name").toDisplayName(),versionNames.distinct(),encounterDetails.distinct()))}}.distinctBy{it.location}}
    private fun evolutionRequirement(detail:JSONObject):String{
        val pieces=mutableListOf<String>()
        val trigger=detail.optJSONObject("trigger")?.optString("name").orEmpty()
        val minLevel=detail.optInt("min_level").takeIf{it>0}

        when(trigger){
            "level-up" -> if(minLevel!=null) pieces+="Subir ao nível $minLevel" else pieces+="Subir de nível"
            "trade" -> pieces+="Troca"
            "use-item" -> pieces+="Usar item"
            "shed" -> pieces+="Condição especial após evolução"
            "spin" -> pieces+="Girar o personagem"
            "tower-of-darkness" -> pieces+="Concluir a Tower of Darkness"
            "tower-of-waters" -> pieces+="Concluir a Tower of Waters"
            "three-critical-hits" -> pieces+="Acertar 3 golpes críticos na mesma batalha"
            "take-damage" -> pieces+="Receber dano sem desmaiar"
            "other" -> pieces+="Método especial"
            else -> trigger.takeIf{it.isNotBlank()}?.let{pieces+=it.toDisplayName()}
        }

        if(trigger!="level-up"){
            minLevel?.let{pieces+="Nível mínimo $it"}
        }

        detail.optJSONObject("item")?.optString("name")
            ?.takeIf{it.isNotBlank()}?.let{
                val item=it.toDisplayName()
                pieces.remove("Usar item")
                pieces+="Usar $item"
            }
        detail.optJSONObject("held_item")?.optString("name")
            ?.takeIf{it.isNotBlank()}?.let{pieces+="Segurando ${it.toDisplayName()}"}
        detail.optJSONObject("known_move")?.optString("name")
            ?.takeIf{it.isNotBlank()}?.let{pieces+="Conhecendo ${it.toDisplayName()}"}
        detail.optJSONObject("known_move_type")?.optString("name")
            ?.takeIf{it.isNotBlank()}?.let{pieces+="Conhecendo um golpe do tipo ${it.toDisplayName()}"}
        detail.optJSONObject("location")?.optString("name")
            ?.takeIf{it.isNotBlank()}?.let{pieces+="Em ${it.toDisplayName()}"}
        detail.optJSONObject("party_species")?.optString("name")
            ?.takeIf{it.isNotBlank()}?.let{pieces+="Com ${it.toDisplayName()} no time"}
        detail.optJSONObject("party_type")?.optString("name")
            ?.takeIf{it.isNotBlank()}?.let{pieces+="Com um Pokémon do tipo ${it.toDisplayName()} no time"}
        detail.optJSONObject("trade_species")?.optString("name")
            ?.takeIf{it.isNotBlank()}?.let{pieces+="Trocar por ${it.toDisplayName()}"}

        detail.optInt("min_happiness").takeIf{it>0}?.let{pieces+="Amizade ≥ $it"}
        detail.optInt("min_affection").takeIf{it>0}?.let{pieces+="Afeição ≥ $it"}
        detail.optInt("min_beauty").takeIf{it>0}?.let{pieces+="Beleza ≥ $it"}

        detail.optString("time_of_day").takeIf{it.isNotBlank()}?.let{
            pieces+=when(it.lowercase()){
                "day" -> "Durante o dia"
                "night" -> "Durante a noite"
                "dusk" -> "Ao entardecer"
                else -> "Período: ${it.toDisplayName()}"
            }
        }
        if(detail.optBoolean("needs_overworld_rain",false)) pieces+="Com chuva no mundo"
        if(detail.optBoolean("turn_upside_down",false)) pieces+="Com o console virado de cabeça para baixo"

        detail.optInt("gender").takeIf{it>0}?.let{
            pieces+=when(it){
                1 -> "Somente fêmea"
                2 -> "Somente macho"
                else -> "Gênero específico"
            }
        }

        if(detail.has("relative_physical_stats") && !detail.isNull("relative_physical_stats")){
            when(detail.optInt("relative_physical_stats")){
                1 -> pieces+="Ataque maior que Defesa"
                0 -> pieces+="Ataque igual à Defesa"
                -1 -> pieces+="Ataque menor que Defesa"
            }
        }

        return pieces.distinct().joinToString(" • ").ifBlank{"Método especial"}
    }

    private fun mergeEvolutionRequirements(
        pokemonId:Int,
        apiRequirements:List<String>
    ):String?{
        if(apiRequirements.isEmpty() && pokemonId !in specialEvolutionRequirements) return null
        val api=apiRequirements.filterNot{it=="Método especial"}
        val special=specialEvolutionRequirements[pokemonId]
        val alternatives=buildList{
            if(api.isNotEmpty()) addAll(api)
            if(!special.isNullOrBlank() && special !in api) add(special)
        }.distinct()
        return alternatives.takeIf{it.isNotEmpty()}?.joinToString("  OU  ")
    }

    /**
     * Mecânicas especiais que o evolution_details genérico não descreve por completo.
     */
    private val specialEvolutionRequirements=mapOf(
        266 to "Subir ao nível 7 • Resultado entre Silcoon/Cascoon depende do valor de personalidade interno",
        268 to "Subir ao nível 7 • Resultado entre Silcoon/Cascoon depende do valor de personalidade interno",
        292 to "Nincada sobe ao nível 20 • Ter um espaço vazio no time • Ter uma Poké Ball na bolsa",
        687 to "Subir de nível a partir do nível 30 • Virar o console de cabeça para baixo",
        745 to "Subir de nível a partir do nível 25 • Forma depende do horário; Dusk exige Rockruff com Own Tempo no período correto",
        849 to "Subir ao nível 30 • Forma Amped ou Low Key depende da Nature do Toxel",
        865 to "Acertar 3 golpes críticos na mesma batalha com Galarian Farfetch'd",
        867 to "Galarian Yamask deve perder pelo menos 49 HP sem desmaiar • Passar sob o arco de pedra em Dusty Bowl",
        869 to "Milcery segurando um Sweet • Girar o personagem; forma e decoração dependem do Sweet, direção, duração e horário",
        892 to "Kubfu: interagir com o Scroll of Darkness ou Scroll of Waters após concluir a torre correspondente",
        899 to "Usar Psyshield Bash em Agile Style 20 vezes • Depois subir de nível",
        901 to "Usar Peat Block em Ursaring durante lua cheia",
        902 to "Basculin (White-Striped) deve acumular pelo menos 294 de dano de recoil sem desmaiar",
        904 to "Usar Barb Barrage em Strong Style 20 vezes • Depois subir de nível",
        923 to "Caminhar 1.000 passos com Pawmo no modo Let's Go • Depois subir de nível",
        947 to "Caminhar 1.000 passos com Bramblin no modo Let's Go • Depois subir de nível",
        954 to "Caminhar 1.000 passos com Rellor no modo Let's Go • Depois subir de nível",
        964 to "Subir Finizen ao nível 38 ou mais enquanto estiver em uma sessão multiplayer/Union Circle",
        979 to "Usar Rage Fist 20 vezes • Depois subir de nível",
        983 to "Bisharp segurando Leader's Crest • Derrotar 3 Bisharp líderes que também seguram Leader's Crest • Depois subir de nível",
        925 to "Tandemaus evolui a partir do nível 25 após participar de uma batalha; a forma Family of Three é rara",
        982 to "Dunsparce evolui ao subir de nível conhecendo Hyper Drill; a forma Three-Segment é rara",
        1000 to "Coletar 999 Gimmighoul Coins • Depois subir Gimmighoul de nível",
        1019 to "Dipplin evolui ao subir de nível conhecendo Dragon Cheer"
    )
    private fun getJson(url:String)=JSONObject(getText(url));private fun getJsonArray(url:String)=JSONArray(getText(url));private fun getText(url:String):String = PersistentApiCache.getOrFetch(url) { val connection=URL(url).openConnection() as HttpURLConnection;connection.connectTimeout=12_000;connection.readTimeout=12_000;connection.requestMethod="GET";connection.setRequestProperty("Accept","application/json");connection.connect();try{if(connection.responseCode !in 200..299)error("HTTP ${connection.responseCode} while loading $url");connection.inputStream.bufferedReader().use{it.readText()}}finally{connection.disconnect()}}
    private fun idFromUrl(url:String)=url.trimEnd('/').substringAfterLast('/').toInt()
}
fun generationForNationalDexId(id:Int):Int=when(id){in 1..151->1;in 152..251->2;in 252..386->3;in 387..493->4;in 494..649->5;in 650..721->6;in 722..809->7;in 810..905->8;in 906..1025->9;else->0}
private fun JSONArray.namesFromNamedResources():List<String> = buildList(length()){for(i in 0 until length())add(getJSONObject(i).getString("name").toDisplayName())}
private fun String.toDisplayName():String=split('-',' ').joinToString(" "){part->part.replaceFirstChar{it.uppercase()}}
