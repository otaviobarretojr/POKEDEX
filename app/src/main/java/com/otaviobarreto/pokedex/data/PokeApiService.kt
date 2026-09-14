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
    data class RemoteMove(val name:String,val methods:List<String>,val learnDetails:List<MoveLearnDetail> = emptyList(),val resourceUrl:String="")
    data class RemotePokemonDetail(val id:Int,val name:String,val heightDecimeters:Int,val weightHectograms:Int,val types:List<String>,val stats:PokemonStats,val abilities:List<String>,val moves:List<RemoteMove>){val spriteUrl:String get()="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"}
    data class SpeciesInfo(val captureRate:Int,val baseHappiness:Int,val habitat:String?,val growthRate:String?,val eggGroups:List<String>,val flavorText:String?,val evolutionChainUrl:String?,val genus:String?=null)
    data class EvolutionStage(val pokemonId:Int,val name:String,val requirement:String?)
    enum class EvolutionMethod(val label:String){
        LEVEL("Nível"),
        TRADE("Troca"),
        ITEM("Item"),
        FRIENDSHIP("Amizade"),
        TIME("Horário"),
        LEVEL_CONDITION("Nível + condição"),
        MOVE("Golpe / movimento"),
        LOCATION("Local / clima"),
        ACTION("Ação especial"),
        MULTIPLAYER("Multiplayer"),
        OTHER("Outro método")
    }
    data class EvolutionSourceMethod(val sourcePokemonId:Int,val targetPokemonId:Int,val method:EvolutionMethod,val requirement:String)
    data class EncounterDetail(val version:String,val method:String,val minLevel:Int,val maxLevel:Int,val chance:Int,val conditions:List<String>)
    data class EncounterLocation(val location:String,val versions:List<String>,val details:List<EncounterDetail> = emptyList())

    fun loadNationalDex(limit:Int=MAX_NATIONAL_DEX_ID):List<DexIndexEntry>{val json=getJson("$API/pokemon?limit=$limit&offset=0");val results=json.getJSONArray("results");return buildList(results.length()){for(i in 0 until results.length()){val item=results.getJSONObject(i);val id=idFromUrl(item.getString("url"));if(id in 1..limit)add(DexIndexEntry(id,item.getString("name").toDisplayName(),generationForNationalDexId(id)))}}.sortedBy{it.id}}
    fun loadPokemonIdsForType(type:String):Set<Int>{val json=getJson("$API/type/${type.lowercase()}");val array=json.getJSONArray("pokemon");return buildSet{for(i in 0 until array.length()){val id=idFromUrl(array.getJSONObject(i).getJSONObject("pokemon").getString("url"));if(id in 1..MAX_NATIONAL_DEX_ID)add(id)}}}

    fun loadPokemon(id:Int):RemotePokemonDetail{
        val json=getJson(pokemonUrl(id));val typesJson=json.getJSONArray("types");val types=buildList(typesJson.length()){for(i in 0 until typesJson.length())add(typesJson.getJSONObject(i).getJSONObject("type").getString("name").toDisplayName())}
        val statMap=mutableMapOf<String,Int>();val statsJson=json.getJSONArray("stats");for(i in 0 until statsJson.length()){val stat=statsJson.getJSONObject(i);statMap[stat.getJSONObject("stat").getString("name")]=stat.getInt("base_stat")}
        val abilitiesJson=json.getJSONArray("abilities");val abilities=buildList(abilitiesJson.length()){for(i in 0 until abilitiesJson.length()){val item=abilitiesJson.getJSONObject(i);val name=item.getJSONObject("ability").getString("name").toDisplayName();add(if(item.optBoolean("is_hidden"))"$name (Oculta)" else name)}}
        val movesJson=json.getJSONArray("moves");val moves=buildList(movesJson.length()){for(i in 0 until movesJson.length()){val move=movesJson.getJSONObject(i);val methods=linkedSetOf<String>();val learnDetails=mutableListOf<MoveLearnDetail>();val details=move.getJSONArray("version_group_details");for(j in 0 until details.length()){val detail=details.getJSONObject(j);val method=detail.getJSONObject("move_learn_method").getString("name").toDisplayName();methods+=method;learnDetails+=MoveLearnDetail(detail.getJSONObject("version_group").getString("name").toDisplayName(),method,detail.optInt("level_learned_at",0))};add(RemoteMove(move.getJSONObject("move").getString("name").toDisplayName(),methods.toList(),learnDetails.distinct(),move.getJSONObject("move").optString("url")))}}.sortedBy{it.name}
        return RemotePokemonDetail(json.getInt("id"),json.getString("name").toDisplayName(),json.getInt("height"),json.getInt("weight"),types,PokemonStats(statMap["hp"]?:0,statMap["attack"]?:0,statMap["defense"]?:0,statMap["special-attack"]?:0,statMap["special-defense"]?:0,statMap["speed"]?:0),abilities,moves)
    }

    fun loadSpecies(id:Int):SpeciesInfo{
        val json=getJson(speciesUrl(id));val flavorEntries=json.getJSONArray("flavor_text_entries");var flavor:String?=null
        for(i in 0 until flavorEntries.length()){val entry=flavorEntries.getJSONObject(i);if(entry.getJSONObject("language").getString("name")=="en"){flavor=entry.getString("flavor_text").replace('\n',' ').replace('\u000c',' ').replace(Regex("\\s+")," ").trim();break}}
        var genus:String?=null;val genera=json.optJSONArray("genera")?:JSONArray();for(i in 0 until genera.length()){val g=genera.getJSONObject(i);if(g.optJSONObject("language")?.optString("name")=="en"){genus=g.optString("genus").takeIf{it.isNotBlank()};break}}
        return SpeciesInfo(json.optInt("capture_rate"),json.optInt("base_happiness"),json.optJSONObject("habitat")?.optString("name")?.toDisplayName(),json.optJSONObject("growth_rate")?.optString("name")?.toDisplayName(),json.getJSONArray("egg_groups").namesFromNamedResources(),flavor,json.optJSONObject("evolution_chain")?.optString("url"),genus)
    }

    fun loadEvolutionChain(url:String,context:GameContext?=null):List<EvolutionStage>{
        val root=getJson(url).getJSONObject("chain")
        val result=mutableListOf<EvolutionStage>()
        fun walk(node:JSONObject){
            val species=node.getJSONObject("species")
            val pokemonId=idFromUrl(species.getString("url"))
            val details=node.optJSONArray("evolution_details")
            val apiRequirements=buildList{
                if(details!=null){
                    for(i in 0 until details.length()){
                        details.optJSONObject(i)?.takeIf{detailAppliesToContext(it,context)}?.let(::evolutionRequirement)?.takeIf{it.isNotBlank()}?.let(::add)
                    }
                }
            }.distinct()
            val special=specialRequirementFor(pokemonId,context)
            val requirement=when{
                !special.isNullOrBlank() -> special
                else -> mergeEvolutionRequirements(
                    pokemonId=pokemonId,
                    apiRequirements=apiRequirements,
                    special=null
                )
            }
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
    fun loadEvolutionSourceMethods(url:String,context:GameContext?=null):List<EvolutionSourceMethod>{
        val root=getJson(url).getJSONObject("chain")
        val result=mutableListOf<EvolutionSourceMethod>()
        fun walk(node:JSONObject){
            val parentId=idFromUrl(node.getJSONObject("species").getString("url"))
            val children=node.getJSONArray("evolves_to")
            for(i in 0 until children.length()){
                val child=children.getJSONObject(i)
                val childId=idFromUrl(child.getJSONObject("species").getString("url"))
                val details=child.optJSONArray("evolution_details")
                val special=specialRequirementFor(childId,context)
                if(!special.isNullOrBlank()){
                    fallbackMethods(special).forEach{method->
                        result+=EvolutionSourceMethod(parentId,childId,method,special)
                    }
                }else{
                    if(details!=null){
                        for(j in 0 until details.length()){
                            val detail=details.optJSONObject(j)?:continue
                            if(!detailAppliesToContext(detail,context)) continue
                            val requirement=evolutionRequirement(detail)
                            evolutionMethods(detail,requirement).forEach{method->
                                result+=EvolutionSourceMethod(parentId,childId,method,requirement)
                            }
                        }
                    }
                }
                walk(child)
            }
        }
        walk(root)
        return result.distinct()
    }

    fun loadSpecialEvolutionSourceIds(url:String):Set<Int>{
        val root=getJson(url).getJSONObject("chain")
        val result=mutableSetOf<Int>()
        fun walk(node:JSONObject){
            val parentId=idFromUrl(node.getJSONObject("species").getString("url"))
            val children=node.getJSONArray("evolves_to")
            for(i in 0 until children.length()){
                val child=children.getJSONObject(i)
                val childId=idFromUrl(child.getJSONObject("species").getString("url"))
                val details=child.optJSONArray("evolution_details")
                val requirements=buildList{
                    if(details!=null) for(j in 0 until details.length()){
                        details.optJSONObject(j)?.let(::evolutionRequirement)?.takeIf{it.isNotBlank()}?.let(::add)
                    }
                }.distinct()
                val merged=mergeEvolutionRequirements(childId,requirements,specialRequirementFor(childId,null))
                if(isSpecialEvolutionRequirement(merged)) result+=parentId
                walk(child)
            }
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
        if(detail.optBoolean("near_special_rock",false)) pieces+="Próximo a uma pedra especial"
        if(detail.optBoolean("needs_multiplayer",false)) pieces+="Em modo multiplayer"
        detail.optJSONObject("region")?.optString("name")?.takeIf{it.isNotBlank()}?.let{pieces+="Na região de ${it.toDisplayName()}"}
        detail.optJSONObject("base_form")?.optString("name")?.takeIf{it.isNotBlank()}?.let{pieces+="Forma base: ${it.toDisplayName()}"}
        detail.optJSONObject("evolved_form")?.optString("name")?.takeIf{it.isNotBlank()}?.let{pieces+="Evolui para: ${it.toDisplayName()}"}
        detail.optJSONObject("used_move")?.optString("name")?.takeIf{it.isNotBlank()}?.let{move->
            val count=detail.optInt("min_move_count").takeIf{it>0}
            pieces+="Usar ${move.toDisplayName()}"+(count?.let{" ${it} vezes"}?:"")
        }
        detail.optInt("min_steps").takeIf{it>0}?.let{pieces+="Caminhar ${it} passos"}
        detail.optInt("min_damage_taken").takeIf{it>0}?.let{pieces+="Receber pelo menos ${it} de dano sem desmaiar"}
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

    fun isSpecialEvolutionRequirement(requirement:String?):Boolean{
        val value=requirement?.trim().orEmpty()
        if(value.isBlank()) return false
        return !Regex("^Subir (ao nível \\d+|de nível)$",RegexOption.IGNORE_CASE).matches(value)
    }

    private fun mergeEvolutionRequirements(
        pokemonId:Int,
        apiRequirements:List<String>,
        special:String?
    ):String?{
        if(apiRequirements.isEmpty() && special.isNullOrBlank()) return null
        val api=apiRequirements.filterNot{it=="Método especial"}
        val alternatives=buildList{
            if(api.isNotEmpty()) addAll(api)
            if(!special.isNullOrBlank() && special !in api) add(special)
        }.distinct()
        return alternatives.takeIf{it.isNotEmpty()}?.joinToString("  OU  ")
    }

    private fun detailAppliesToContext(detail:JSONObject,context:GameContext?):Boolean{
        // version_group_id marks when a rule was introduced, not necessarily the only
        // game where it remains valid. Game-specific changes are handled by overrides.
        return true
    }

    private fun evolutionMethods(detail:JSONObject,requirement:String):Set<EvolutionMethod>{
        val result=linkedSetOf<EvolutionMethod>()
        val trigger=detail.optJSONObject("trigger")?.optString("name").orEmpty()
        if(trigger=="trade" || detail.optJSONObject("trade_species")!=null) result+=EvolutionMethod.TRADE
        if(trigger=="use-item" || detail.optJSONObject("item")!=null || detail.optJSONObject("held_item")!=null) result+=EvolutionMethod.ITEM
        if(detail.optInt("min_happiness")>0 || detail.optInt("min_affection")>0 || detail.optInt("min_beauty")>0) result+=EvolutionMethod.FRIENDSHIP
        if(detail.optString("time_of_day").isNotBlank()) result+=EvolutionMethod.TIME
        if(detail.optJSONObject("known_move")!=null || detail.optJSONObject("known_move_type")!=null || detail.optJSONObject("used_move")!=null) result+=EvolutionMethod.MOVE
        if(detail.optJSONObject("location")!=null || detail.optJSONObject("region")!=null || detail.optBoolean("needs_overworld_rain",false) || detail.optBoolean("near_special_rock",false)) result+=EvolutionMethod.LOCATION
        if(detail.optBoolean("needs_multiplayer",false)) result+=EvolutionMethod.MULTIPLAYER
        if(detail.optInt("min_steps")>0 || detail.optInt("min_damage_taken")>0 || detail.optInt("min_move_count")>0 || detail.optBoolean("turn_upside_down",false) || trigger in setOf("shed","spin","tower-of-darkness","tower-of-waters","three-critical-hits","take-damage","other")) result+=EvolutionMethod.ACTION
        val plainLevel=Regex("^Subir (ao nível \\d+|de nível)$",RegexOption.IGNORE_CASE).matches(requirement.trim())
        if(plainLevel) result+=EvolutionMethod.LEVEL
        else if((trigger=="level-up" || detail.optInt("min_level")>0) && result.isEmpty()) result+=EvolutionMethod.LEVEL_CONDITION
        if(result.isEmpty() && isSpecialEvolutionRequirement(requirement)) result+=EvolutionMethod.OTHER
        return result
    }

    private fun fallbackMethods(requirement:String):Set<EvolutionMethod>{
        if(requirement.startsWith("Evolução indisponível",ignoreCase=true)) return emptySet()
        val r=requirement.lowercase()
        val result=linkedSetOf<EvolutionMethod>()
        if("troca" in r) result+=EvolutionMethod.TRADE
        if(listOf("segurando","peat block","leader's crest","scroll of darkness","scroll of waters","sweet").any{it in r}) result+=EvolutionMethod.ITEM
        if("amizade" in r || "afeição" in r || "beleza" in r) result+=EvolutionMethod.FRIENDSHIP
        if(listOf("durante o dia","durante a noite","entardecer","horário","lua cheia").any{it in r}) result+=EvolutionMethod.TIME
        if(listOf("rage fist","psyshield bash","barb barrage","hyper drill","dragon cheer","conhecendo").any{it in r}) result+=EvolutionMethod.MOVE
        if(listOf("dusty bowl","chuva","região de").any{it in r}) result+=EvolutionMethod.LOCATION
        if("union circle" in r || "multiplayer" in r) result+=EvolutionMethod.MULTIPLAYER
        if(listOf("passos","girar","virar o console","golpes críticos","dano","recoil","batalha","coins","vezes","tower of").any{it in r}) result+=EvolutionMethod.ACTION
        val plainLevel=Regex("^subir (ao nível \\d+|de nível)$",RegexOption.IGNORE_CASE).matches(requirement.trim())
        if(result.isEmpty() && plainLevel) result+=EvolutionMethod.LEVEL
        else if(result.isEmpty() && ("nível" in r || "subir " in r)) result+=EvolutionMethod.LEVEL_CONDITION
        if(result.isEmpty()) result+=EvolutionMethod.OTHER
        return result
    }

    internal fun auditFallbackMethods(requirement:String):Set<EvolutionMethod> = fallbackMethods(requirement)
    internal fun auditSpecialRequirement(pokemonId:Int,context:GameContext?):String? = specialRequirementFor(pokemonId,context)

    private fun hasContextualOverride(pokemonId:Int,context:GameContext?):Boolean =
        context!=null && pokemonId in setOf(899,904,892)

    private fun specialRequirementFor(pokemonId:Int,context:GameContext?):String?{
        EvolutionCuratedCatalog.ruleFor(pokemonId,context)?.let{return it.requirement}
        val game=context?.label.orEmpty()
        return when(pokemonId){
            899 -> when(game){
                "" -> specialEvolutionRequirements[pokemonId]
                "Legends Arceus" -> "Usar Psyshield Bash em Agile Style 20 vezes • Depois subir de nível"
                else -> "Evolução indisponível neste jogo; evolua Stantler em Pokémon Legends: Arceus e transfira pelo Pokémon HOME"
            }
            904 -> when(game){
                "" -> specialEvolutionRequirements[pokemonId]
                "Legends Arceus" -> "Usar Barb Barrage em Strong Style 20 vezes • Depois subir de nível"
                "Scarlet / Violet" -> "Subir de nível conhecendo Barb Barrage"
                "Pokémon Legends: Z-A" -> "Acertar 20 alvos com Barb Barrage"
                else -> specialEvolutionRequirements[pokemonId]
            }
            892 -> when(game){
                "" -> specialEvolutionRequirements[pokemonId]
                "Sword / Shield" -> "Concluir a torre correspondente e interagir com o Scroll of Darkness ou Scroll of Waters"
                "Scarlet / Violet" -> "Usar Scroll of Darkness ou Scroll of Waters"
                else -> specialEvolutionRequirements[pokemonId]
            }
            else -> specialEvolutionRequirements[pokemonId]
        }
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
