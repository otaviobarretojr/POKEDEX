package com.otaviobarreto.pokedex.data

enum class EvolutionAvailability {
    AVAILABLE,
    AVAILABLE_WITH_CONDITION,
    TRANSFER_ONLY,
    NOT_AVAILABLE,
    UNKNOWN
}

data class EvolutionRoute(
    val sourcePokemonId:Int,
    val targetPokemonId:Int,
    val methods:Set<PokeApiService.EvolutionMethod>,
    val summary:String,
    val detail:String,
    val availability:EvolutionAvailability,
    val contextLabel:String?,
    val regionLabel:String?,
    val sourceFormKey:String?=null,
    val targetFormKey:String?=null
){
    val primaryMethod:PokeApiService.EvolutionMethod
        get()=EvolutionRuleCatalog.primaryMethod(methods)
}

object EvolutionResolutionEngine {
    fun load(chainUrl:String,context:GameContext?):List<EvolutionRoute> =
        PokeApiService.loadEvolutionSourceMethods(chainUrl,context)
            .groupBy{Triple(it.sourcePokemonId,it.targetPokemonId,it.requirement)}
            .map{(key,items)->
                val detail=key.third
                val methods=items.mapTo(linkedSetOf()){it.method}
                val formKeys=EvolutionCuratedCatalog.formKeysFor(key.second,context)
                EvolutionRoute(
                    sourcePokemonId=key.first,
                    targetPokemonId=key.second,
                    methods=methods,
                    summary=EvolutionRuleCatalog.simplify(detail),
                    detail=detail,
                    availability=availabilityFor(detail,methods),
                    contextLabel=context?.label,
                    regionLabel=context?.regionLabel,
                    sourceFormKey=formKeys.first,
                    targetFormKey=formKeys.second
                )
            }
            .sortedWith(
                compareBy<EvolutionRoute>{it.sourcePokemonId}
                    .thenBy{it.targetPokemonId}
                    .thenBy{it.summary}
            )

    fun availabilityFor(
        requirement:String,
        methods:Set<PokeApiService.EvolutionMethod>
    ):EvolutionAvailability {
        val r=requirement.lowercase()
        return when {
            "evolução indisponível" in r && "home" in r -> EvolutionAvailability.TRANSFER_ONLY
            "evolução indisponível" in r -> EvolutionAvailability.NOT_AVAILABLE
            requirement.isBlank() -> EvolutionAvailability.UNKNOWN
            methods==setOf(PokeApiService.EvolutionMethod.LEVEL) -> EvolutionAvailability.AVAILABLE
            else -> EvolutionAvailability.AVAILABLE_WITH_CONDITION
        }
    }

    fun executable(route:EvolutionRoute):Boolean =
        route.availability in setOf(
            EvolutionAvailability.AVAILABLE,
            EvolutionAvailability.AVAILABLE_WITH_CONDITION
        )

    fun routesForTarget(routes:List<EvolutionRoute>,targetPokemonId:Int):List<EvolutionRoute> =
        routes.filter{it.targetPokemonId==targetPokemonId}

    fun preferredRoute(routes:List<EvolutionRoute>):EvolutionRoute? =
        routes.sortedWith(
            compareBy<EvolutionRoute>{
                when(it.availability){
                    EvolutionAvailability.AVAILABLE -> 0
                    EvolutionAvailability.AVAILABLE_WITH_CONDITION -> 1
                    EvolutionAvailability.TRANSFER_ONLY -> 2
                    EvolutionAvailability.NOT_AVAILABLE -> 3
                    EvolutionAvailability.UNKNOWN -> 4
                }
            }.thenBy{EvolutionRuleCatalog.primaryMethod(it.methods).ordinal}
        ).firstOrNull()
}
