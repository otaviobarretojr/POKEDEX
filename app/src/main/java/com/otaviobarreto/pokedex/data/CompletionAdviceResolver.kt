package com.otaviobarreto.pokedex.data

enum class CompletionDifficulty(val label:String){
    VERY_EASY("Muito fácil"),
    EASY("Fácil"),
    MODERATE("Moderado"),
    HARD("Difícil"),
    EXTERNAL("Externo")
}

enum class CompletionMethodKind(val label:String){
    DIRECT("Captura / obtenção"),
    LEVEL("Nível"),
    CONDITION("Condição"),
    TRADE("Troca"),
    ITEM("Item"),
    FRIENDSHIP("Amizade"),
    TIME("Horário"),
    MOVE("Golpe"),
    LOCATION("Local / clima"),
    MULTIPLAYER("Multiplayer"),
    ACTION("Ação especial"),
    TRANSFER("HOME / transferência"),
    SPECIAL("Método especial")
}

data class CompletionAlternative(
    val method:CompletionMethodKind,
    val title:String,
    val detail:String?=null,
    val score:Int,
    val difficulty:CompletionDifficulty
)

data class CompletionAdvice(
    val pokemonId:Int,
    val method:CompletionMethodKind,
    val title:String,
    val detail:String?=null,
    val sourcePokemonId:Int?=null,
    val versionAvailability:VersionAvailability?=null,
    val directAcquisition:Boolean=false,
    val selectedVersion:String?=null,
    val availableInSelectedVersion:Boolean?=null,
    val score:Int=100,
    val difficulty:CompletionDifficulty=CompletionDifficulty.EXTERNAL,
    val alternative:CompletionAlternative?=null
)

object CompletionAdviceResolver {

    private data class Candidate(val advice:CompletionAdvice)

    fun resolve(
        pokemonId:Int,
        context:GameContext,
        routes:List<EvolutionRoute>,
        owned:Set<Int> = emptySet(),
        names:Map<Int,String> = emptyMap(),
        selectedVersion:String? = null,
        canonical:CanonicalAvailability? = null,
        inRegionalDex:Boolean=true
    ):CompletionAdvice {
        val version=canonical?.version ?: VersionAvailabilityCatalog.forPokemon(pokemonId,context,inRegionalDex)
        val versionOk=when {
            selectedVersion==null -> null
            version?.kind==VersionAvailabilityKind.EXCLUSIVE -> version.exclusiveVersion==selectedVersion
            version?.kind==VersionAvailabilityKind.UNAVAILABLE -> false
            else -> true
        }

        val candidates=mutableListOf<Candidate>()

        EvolutionResolutionEngine.routesForTarget(routes,pokemonId).forEach{route->
            val method=methodFor(route)
            val sourceOwned=route.sourcePokemonId in owned
            val sourceName=names[route.sourcePokemonId] ?: "Pokémon #"+route.sourcePokemonId
            val baseScore=routeScore(
                method=method,
                availability=route.availability,
                requirement=route.summary
            )
            val sourcePenalty=if(sourceOwned || route.availability==EvolutionAvailability.TRANSFER_ONLY) 0 else 7
            val detail=when {
                route.availability==EvolutionAvailability.TRANSFER_ONLY ->
                    "Obtenha no jogo compatível e transfira pelo Pokémon HOME."
                !sourceOwned ->
                    "Obtenha "+sourceName+" primeiro. Depois: "+route.summary+"."
                else ->
                    "Você já possui "+sourceName+"."
            }
            candidates+=Candidate(
                CompletionAdvice(
                    pokemonId=pokemonId,
                    method=method,
                    title=when{
                        route.availability==EvolutionAvailability.TRANSFER_ONLY -> route.summary
                        sourceOwned -> route.summary
                        else -> "Obtenha "+sourceName+" primeiro"
                    },
                    detail=detail,
                    sourcePokemonId=route.sourcePokemonId,
                    versionAvailability=version,
                    selectedVersion=selectedVersion,
                    availableInSelectedVersion=versionOk,
                    score=baseScore+sourcePenalty
                )
            )
        }

        canonical?.takeIf{it.inRegionalDex}?.let{availability->
            canonicalCandidate(
                availability=availability,
                selectedVersion=selectedVersion,
                versionOk=versionOk
            )?.let{candidates+=Candidate(it)}
        }

        if(canonical==null){
            SpecialAcquisitionCatalog.lookup(pokemonId,context)?.let{special->
                candidates+=Candidate(
                    CompletionAdvice(
                        pokemonId=pokemonId,
                        method=kindForCanonical(special.kind),
                        title=special.label,
                        detail=special.requirement,
                        versionAvailability=version,
                        directAcquisition=true,
                        selectedVersion=selectedVersion,
                        availableInSelectedVersion=versionOk,
                        score=canonicalScore(special.kind,false)
                    )
                )
            }
        }

        if(
            selectedVersion!=null &&
            version?.kind==VersionAvailabilityKind.EXCLUSIVE &&
            version.exclusiveVersion!=selectedVersion
        ){
            candidates+=Candidate(
                CompletionAdvice(
                    pokemonId=pokemonId,
                    method=CompletionMethodKind.TRADE,
                    title="Troca / HOME",
                    detail="Exclusivo de "+version.exclusiveVersion+". Obtenha por troca, multiplayer ou transfira pelo Pokémon HOME quando compatível.",
                    versionAvailability=version,
                    directAcquisition=true,
                    selectedVersion=selectedVersion,
                    availableInSelectedVersion=false,
                    score=10
                )
            )
        }

        if(candidates.isEmpty()){
            // No confirmed acquisition/evolution/location data: keep the Box card intentionally blank.
            // This avoids presenting absence of data as if it were an acquisition method.
            candidates+=Candidate(
                CompletionAdvice(
                    pokemonId=pokemonId,
                    method=CompletionMethodKind.DIRECT,
                    title="",
                    detail=null,
                    versionAvailability=null,
                    directAcquisition=false,
                    selectedVersion=selectedVersion,
                    availableInSelectedVersion=null,
                    score=Int.MAX_VALUE
                )
            )
        }

        val ranked=candidates
            .map{it.advice.copy(score=effectiveScore(it.advice))}
            .sortedBy{it.score}

        val bestBase=ranked.first()
        val best=bestBase.copy(difficulty=difficultyFor(bestBase.score))
        val alternative=ranked.drop(1)
            .firstOrNull{
                (it.method!=best.method || it.title!=best.title) &&
                    difficultyFor(it.score)==best.difficulty
            }
            ?.let{
                CompletionAlternative(
                    method=it.method,
                    title=alternativeTitle(it),
                    detail=it.detail,
                    score=it.score,
                    difficulty=difficultyFor(it.score)
                )
            }

        return best.copy(
            detail=mergeDetails(
                best.detail,
                versionDetail(version,selectedVersion,versionOk)
            ),
            alternative=alternative
        )
    }

    private fun canonicalCandidate(
        availability:CanonicalAvailability,
        selectedVersion:String?,
        versionOk:Boolean?
    ):CompletionAdvice? {
        if(availability.acquisitionKind==CanonicalAcquisitionKind.UNAVAILABLE) return null
        return CompletionAdvice(
            pokemonId=availability.pokemonId,
            method=kindForCanonical(availability.acquisitionKind),
            title=availability.acquisitionLabel,
            detail=buildList{
                availability.requirement?.takeIf{it.isNotBlank()}?.let(::add)
                availability.locations.firstOrNull()?.let{add("Local: "+it)}
            }.joinToString(" • ").ifBlank{null},
            versionAvailability=availability.version,
            directAcquisition=availability.acquisitionKind!=CanonicalAcquisitionKind.EVOLUTION,
            selectedVersion=selectedVersion,
            availableInSelectedVersion=versionOk,
            score=canonicalScore(
                availability.acquisitionKind,
                availability.confidence==AvailabilityConfidence.PARTIAL
            )
        )
    }

    private fun methodFor(route:EvolutionRoute):CompletionMethodKind = when {
        route.availability==EvolutionAvailability.TRANSFER_ONLY -> CompletionMethodKind.TRANSFER
        PokeApiService.EvolutionMethod.TRADE in route.methods -> CompletionMethodKind.TRADE
        PokeApiService.EvolutionMethod.ITEM in route.methods -> CompletionMethodKind.ITEM
        PokeApiService.EvolutionMethod.MULTIPLAYER in route.methods -> CompletionMethodKind.MULTIPLAYER
        PokeApiService.EvolutionMethod.FRIENDSHIP in route.methods -> CompletionMethodKind.FRIENDSHIP
        PokeApiService.EvolutionMethod.TIME in route.methods -> CompletionMethodKind.TIME
        PokeApiService.EvolutionMethod.MOVE in route.methods -> CompletionMethodKind.MOVE
        PokeApiService.EvolutionMethod.LOCATION in route.methods -> CompletionMethodKind.LOCATION
        PokeApiService.EvolutionMethod.ACTION in route.methods -> CompletionMethodKind.ACTION
        PokeApiService.EvolutionMethod.LEVEL in route.methods -> CompletionMethodKind.LEVEL
        else -> CompletionMethodKind.CONDITION
    }

    private fun kindForCanonical(kind:CanonicalAcquisitionKind):CompletionMethodKind = when(kind){
        CanonicalAcquisitionKind.WILD -> CompletionMethodKind.DIRECT
        CanonicalAcquisitionKind.EVOLUTION -> CompletionMethodKind.CONDITION
        CanonicalAcquisitionKind.TRADE -> CompletionMethodKind.TRADE
        CanonicalAcquisitionKind.GIFT_STARTER -> CompletionMethodKind.SPECIAL
        CanonicalAcquisitionKind.RAID -> CompletionMethodKind.SPECIAL
        CanonicalAcquisitionKind.EVENT_SPECIAL -> CompletionMethodKind.SPECIAL
        CanonicalAcquisitionKind.HOME_TRANSFER -> CompletionMethodKind.TRANSFER
        CanonicalAcquisitionKind.OTHER_METHOD -> CompletionMethodKind.SPECIAL
        CanonicalAcquisitionKind.UNAVAILABLE -> CompletionMethodKind.TRANSFER
    }

    private fun routeScore(
        method:CompletionMethodKind,
        availability:EvolutionAvailability,
        requirement:String
    ):Int {
        if(availability==EvolutionAvailability.TRANSFER_ONLY) return 22

        val r=requirement.lowercase()
        val level=Regex("""(?:nível|nivel|level)\s*(\d{1,3})""")
            .find(r)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val levelPenalty=when{
            level==null -> 0
            level<=20 -> 0
            level<=35 -> 1
            level<=50 -> 3
            else -> 6
        }
        val complexityPenalty=
            (if("1.000 passos" in r || "1000 passos" in r) 3 else 0) +
            (if("20 vezes" in r || "3 golpes críticos" in r) 3 else 0) +
            (if("999" in r || "union circle" in r) 4 else 0) +
            (if("lua cheia" in r || "durante a noite" in r || "durante o dia" in r) 1 else 0)

        val base=when(method){
            CompletionMethodKind.LEVEL -> 1
            CompletionMethodKind.DIRECT -> 2
            CompletionMethodKind.ITEM -> 3
            CompletionMethodKind.FRIENDSHIP -> 5
            CompletionMethodKind.TIME -> 5
            CompletionMethodKind.MOVE -> 5
            CompletionMethodKind.LOCATION -> 6
            CompletionMethodKind.ACTION -> 7
            CompletionMethodKind.CONDITION -> 7
            CompletionMethodKind.MULTIPLAYER -> 9
            CompletionMethodKind.TRADE -> 10
            CompletionMethodKind.SPECIAL -> 11
            CompletionMethodKind.TRANSFER -> 22
        }
        return base+levelPenalty+complexityPenalty
    }

    private fun canonicalScore(kind:CanonicalAcquisitionKind,partial:Boolean):Int {
        val base=when(kind){
            CanonicalAcquisitionKind.WILD -> 2
            CanonicalAcquisitionKind.GIFT_STARTER -> 1
            CanonicalAcquisitionKind.RAID -> 7
            CanonicalAcquisitionKind.EVOLUTION -> 8
            CanonicalAcquisitionKind.TRADE -> 11
            CanonicalAcquisitionKind.EVENT_SPECIAL -> 13
            CanonicalAcquisitionKind.OTHER_METHOD -> 14
            CanonicalAcquisitionKind.HOME_TRANSFER -> 22
            CanonicalAcquisitionKind.UNAVAILABLE -> 35
        }
        return base+if(partial)3 else 0
    }

    private fun effectiveScore(advice:CompletionAdvice):Int {
        val versionPenalty=
            if(
                advice.availableInSelectedVersion==false &&
                advice.method!=CompletionMethodKind.TRADE &&
                advice.method!=CompletionMethodKind.TRANSFER
            ) 14 else 0
        return advice.score+versionPenalty
    }

    fun difficultyFor(score:Int):CompletionDifficulty = when {
        score<=2 -> CompletionDifficulty.VERY_EASY
        score<=5 -> CompletionDifficulty.EASY
        score<=9 -> CompletionDifficulty.MODERATE
        score<=15 -> CompletionDifficulty.HARD
        else -> CompletionDifficulty.EXTERNAL
    }

    private fun alternativeTitle(advice:CompletionAdvice):String {
        val local=advice.detail
            ?.substringAfter("Local:",missingDelimiterValue="")
            ?.substringBefore("•")
            ?.trim()
            .orEmpty()
        return when {
            advice.method==CompletionMethodKind.DIRECT && local.isNotBlank() ->
                "Capturar em "+local
            advice.method==CompletionMethodKind.TRADE ->
                "Troca / HOME"
            else -> advice.title
        }
    }

    private fun versionDetail(
        version:VersionAvailability?,
        selectedVersion:String?,
        available:Boolean?
    ):String? = when {
        version?.kind==VersionAvailabilityKind.SPLIT_FORMS -> version.subtitle
        version?.kind==VersionAvailabilityKind.EXCLUSIVE && selectedVersion==null ->
            "Exclusivo de "+version.exclusiveVersion+". Se você joga a outra versão, use troca, multiplayer ou HOME quando compatível."
        version?.kind==VersionAvailabilityKind.EXCLUSIVE && available==true ->
            "Disponível na sua versão ("+selectedVersion+")."
        version?.kind==VersionAvailabilityKind.EXCLUSIVE && available==false ->
            "Você joga "+selectedVersion+". Este Pokémon é exclusivo de "+version.exclusiveVersion+"; use troca, multiplayer ou HOME quando compatível."
        else -> null
    }

    private fun mergeDetails(a:String?,b:String?):String? =
        listOfNotNull(a?.takeIf{it.isNotBlank()},b?.takeIf{it.isNotBlank()})
            .distinct()
            .joinToString(" • ")
            .ifBlank{null}

    fun priority(advice:CompletionAdvice):Int = advice.score
}
