package com.otaviobarreto.pokedex.data

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
    val score:Int=100
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
            val baseScore=routeScore(method,route.availability)
            val sourcePenalty=if(sourceOwned || route.availability==EvolutionAvailability.TRANSFER_ONLY) 0 else 6
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
                    title=if(sourceOwned) route.summary else "Obtenha "+sourceName+" primeiro",
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

        if(candidates.isEmpty()){
            candidates+=Candidate(
                CompletionAdvice(
                    pokemonId=pokemonId,
                    method=CompletionMethodKind.DIRECT,
                    title="Método de obtenção a confirmar",
                    detail=versionDetail(version,selectedVersion,versionOk),
                    versionAvailability=version,
                    directAcquisition=true,
                    selectedVersion=selectedVersion,
                    availableInSelectedVersion=versionOk,
                    score=20
                )
            )
        }

        val best=candidates.minBy{candidate->
            candidate.advice.score + if(candidate.advice.availableInSelectedVersion==false) 10 else 0
        }.advice

        return best.copy(
            detail=mergeDetails(
                best.detail,
                versionDetail(version,selectedVersion,versionOk)
            )
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

    private fun routeScore(method:CompletionMethodKind,availability:EvolutionAvailability):Int {
        if(availability==EvolutionAvailability.TRANSFER_ONLY) return 18
        return when(method){
            CompletionMethodKind.LEVEL -> 0
            CompletionMethodKind.ITEM -> 2
            CompletionMethodKind.FRIENDSHIP -> 3
            CompletionMethodKind.TIME -> 4
            CompletionMethodKind.MOVE -> 4
            CompletionMethodKind.LOCATION -> 5
            CompletionMethodKind.MULTIPLAYER -> 7
            CompletionMethodKind.TRADE -> 8
            CompletionMethodKind.ACTION -> 6
            CompletionMethodKind.CONDITION -> 6
            CompletionMethodKind.DIRECT -> 1
            CompletionMethodKind.SPECIAL -> 9
            CompletionMethodKind.TRANSFER -> 18
        }
    }

    private fun canonicalScore(kind:CanonicalAcquisitionKind,partial:Boolean):Int {
        val base=when(kind){
            CanonicalAcquisitionKind.WILD -> 1
            CanonicalAcquisitionKind.GIFT_STARTER -> 1
            CanonicalAcquisitionKind.RAID -> 5
            CanonicalAcquisitionKind.EVOLUTION -> 6
            CanonicalAcquisitionKind.TRADE -> 9
            CanonicalAcquisitionKind.EVENT_SPECIAL -> 10
            CanonicalAcquisitionKind.OTHER_METHOD -> 11
            CanonicalAcquisitionKind.HOME_TRANSFER -> 18
            CanonicalAcquisitionKind.UNAVAILABLE -> 30
        }
        return base+if(partial)2 else 0
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
