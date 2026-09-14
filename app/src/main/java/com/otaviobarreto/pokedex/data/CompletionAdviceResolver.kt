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
    val directAcquisition:Boolean=false
)

object CompletionAdviceResolver {

    fun resolve(
        pokemonId:Int,
        context:GameContext,
        routes:List<EvolutionRoute>,
        inRegionalDex:Boolean=true
    ):CompletionAdvice {
        val version=VersionAvailabilityCatalog.forPokemon(pokemonId,context,inRegionalDex)
        val targetRoutes=EvolutionResolutionEngine.routesForTarget(routes,pokemonId)
        val preferred=EvolutionResolutionEngine.preferredRoute(targetRoutes)

        if(preferred!=null){
            val method=when {
                preferred.availability==EvolutionAvailability.TRANSFER_ONLY -> CompletionMethodKind.TRANSFER
                PokeApiService.EvolutionMethod.TRADE in preferred.methods -> CompletionMethodKind.TRADE
                PokeApiService.EvolutionMethod.ITEM in preferred.methods -> CompletionMethodKind.ITEM
                PokeApiService.EvolutionMethod.MULTIPLAYER in preferred.methods -> CompletionMethodKind.MULTIPLAYER
                PokeApiService.EvolutionMethod.FRIENDSHIP in preferred.methods -> CompletionMethodKind.FRIENDSHIP
                PokeApiService.EvolutionMethod.TIME in preferred.methods -> CompletionMethodKind.TIME
                PokeApiService.EvolutionMethod.MOVE in preferred.methods -> CompletionMethodKind.MOVE
                PokeApiService.EvolutionMethod.LOCATION in preferred.methods -> CompletionMethodKind.LOCATION
                PokeApiService.EvolutionMethod.ACTION in preferred.methods -> CompletionMethodKind.ACTION
                PokeApiService.EvolutionMethod.LEVEL in preferred.methods -> CompletionMethodKind.LEVEL
                else -> CompletionMethodKind.CONDITION
            }
            return CompletionAdvice(
                pokemonId=pokemonId,
                method=method,
                title=preferred.summary,
                detail=when(preferred.availability){
                    EvolutionAvailability.TRANSFER_ONLY -> "Obtenha no jogo compatível e transfira pelo Pokémon HOME."
                    EvolutionAvailability.NOT_AVAILABLE -> "Esta evolução não pode ser feita no jogo atual."
                    else -> null
                },
                sourcePokemonId=preferred.sourcePokemonId,
                versionAvailability=version
            )
        }

        val special=SpecialAcquisitionCatalog.lookup(pokemonId,context)
        if(special!=null){
            return CompletionAdvice(
                pokemonId=pokemonId,
                method=when(special.kind){
                    CanonicalAcquisitionKind.TRADE -> CompletionMethodKind.TRADE
                    CanonicalAcquisitionKind.HOME_TRANSFER -> CompletionMethodKind.TRANSFER
                    CanonicalAcquisitionKind.EVOLUTION -> CompletionMethodKind.CONDITION
                    else -> CompletionMethodKind.SPECIAL
                },
                title=special.label,
                detail=special.requirement,
                versionAvailability=version,
                directAcquisition=true
            )
        }

        val exclusiveDetail=when(version?.kind){
            VersionAvailabilityKind.EXCLUSIVE ->
                "Exclusivo de "+version.exclusiveVersion+". Na outra versão, use troca, multiplayer ou HOME quando compatível."
            VersionAvailabilityKind.SPLIT_FORMS -> version.subtitle
            else -> null
        }

        return CompletionAdvice(
            pokemonId=pokemonId,
            method=CompletionMethodKind.DIRECT,
            title="Captura / obtenção direta",
            detail=exclusiveDetail,
            versionAvailability=version,
            directAcquisition=true
        )
    }

    fun priority(advice:CompletionAdvice):Int = when(advice.method){
        CompletionMethodKind.LEVEL -> 0
        CompletionMethodKind.DIRECT -> 1
        CompletionMethodKind.ITEM -> 2
        CompletionMethodKind.FRIENDSHIP -> 3
        CompletionMethodKind.TIME -> 4
        CompletionMethodKind.MOVE -> 5
        CompletionMethodKind.LOCATION -> 6
        CompletionMethodKind.MULTIPLAYER -> 7
        CompletionMethodKind.TRADE -> 8
        CompletionMethodKind.ACTION -> 9
        CompletionMethodKind.CONDITION -> 10
        CompletionMethodKind.SPECIAL -> 11
        CompletionMethodKind.TRANSFER -> 12
    }
}