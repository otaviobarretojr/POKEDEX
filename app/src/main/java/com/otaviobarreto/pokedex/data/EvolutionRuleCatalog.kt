package com.otaviobarreto.pokedex.data

data class ContextualEvolutionRule(
    val sourcePokemonId:Int,
    val targetPokemonId:Int,
    val methods:Set<PokeApiService.EvolutionMethod>,
    val summary:String,
    val rawRequirement:String
){
    val primaryMethod:PokeApiService.EvolutionMethod
        get() = EvolutionRuleCatalog.primaryMethod(methods)
}

object EvolutionRuleCatalog {
    data class FilterOption(val key:String,val label:String)

    val filterOptions:List<FilterOption> = listOf(
        FilterOption("ALL","Todos que faltam"),
        FilterOption("LEVEL","Nível"),
        FilterOption("ITEM","Item"),
        FilterOption("TRADE","Troca"),
        FilterOption("FRIENDSHIP","Amizade"),
        FilterOption("TIME","Horário"),
        FilterOption("GENDER","Gênero"),
        FilterOption("MOVE","Golpe / movimento"),
        FilterOption("LOCATION","Local / clima"),
        FilterOption("MULTIPLAYER","Multiplayer"),
        FilterOption("ACTION","Ação especial"),
        FilterOption("CONDITION","Condição"),
        FilterOption("TRANSFER","Transferência")
    )

    fun filterLabel(key:String):String =
        filterOptions.firstOrNull{it.key==key}?.label ?: "Evolução"

    private val priority=listOf(
        PokeApiService.EvolutionMethod.LEVEL,
        PokeApiService.EvolutionMethod.TRADE,
        PokeApiService.EvolutionMethod.ITEM,
        PokeApiService.EvolutionMethod.MULTIPLAYER,
        PokeApiService.EvolutionMethod.FRIENDSHIP,
        PokeApiService.EvolutionMethod.TIME,
        PokeApiService.EvolutionMethod.GENDER,
        PokeApiService.EvolutionMethod.MOVE,
        PokeApiService.EvolutionMethod.LOCATION,
        PokeApiService.EvolutionMethod.ACTION,
        PokeApiService.EvolutionMethod.LEVEL_CONDITION,
        PokeApiService.EvolutionMethod.OTHER
    )

    fun load(chainUrl:String,context:GameContext?):List<ContextualEvolutionRule> =
        EvolutionResolutionEngine.load(chainUrl,context).map{route->
            ContextualEvolutionRule(
                sourcePokemonId=route.sourcePokemonId,
                targetPokemonId=route.targetPokemonId,
                methods=route.methods,
                summary=route.summary,
                rawRequirement=route.detail
            )
        }

    fun primaryMethod(methods:Set<PokeApiService.EvolutionMethod>):PokeApiService.EvolutionMethod =
        priority.firstOrNull{it in methods} ?: PokeApiService.EvolutionMethod.OTHER

    fun simplify(requirement:String?):String {
        var value=requirement?.trim().orEmpty()
        if(value.isBlank()) return "Forma inicial"

        value=value
            .replace(Regex("(?i)^Subir ao nível (\\d+)$")){"Nível "+it.groupValues[1]}
            .replace(Regex("(?i)^Subir de nível$"),"Subir de nível")
            .replace(Regex("(?i)Subir ao nível (\\d+) • Durante a noite"),"Nível $1 à noite")
            .replace(Regex("(?i)Subir ao nível (\\d+) • Durante o dia"),"Nível $1 durante o dia")
            .replace(Regex("(?i)Subir ao nível (\\d+) • Ao entardecer"),"Nível $1 ao entardecer")
            .replace(Regex("(?i)Subir de nível • Durante a noite"),"Subir de nível à noite")
            .replace(Regex("(?i)Subir de nível • Durante o dia"),"Subir de nível durante o dia")
            .replace(Regex("(?i)Subir de nível • Ao entardecer"),"Subir de nível ao entardecer")
            .replace(Regex("(?i)Subir de nível • Amizade ≥ \\d+ • Durante o dia"),"Alta amizade durante o dia")
            .replace(Regex("(?i)Subir de nível • Amizade ≥ \\d+ • Durante a noite"),"Alta amizade à noite")
            .replace(Regex("(?i)Subir de nível • Amizade ≥ \\d+"),"Alta amizade e subir de nível")
            .replace(Regex("(?i)Troca • Segurando ([^•]+)$")){"Trocar segurando "+it.groupValues[1].trim()}
            .replace(Regex("(?i)Subir de nível • Conhecendo ([^•]+)$")){"Subir de nível conhecendo "+it.groupValues[1].trim()}
            .replace(Regex("(?i)Caminhar 1[.,]?000 passos.*Depois subir de nível"),"Caminhar 1.000 passos e subir de nível")
            .replace(Regex("(?i)Subir Finizen ao nível 38 ou mais enquanto estiver em uma sessão multiplayer/Union Circle"),"Nível 38 no multiplayer")
            .replace(" • "," · ")
            .replace(Regex("\\s+OU\\s+")," ou ")
            .replace(Regex("\\s+")," ")
            .trim()

        return value
    }

    fun filterBucket(rule:ContextualEvolutionRule):String {
        if(rule.methods==setOf(PokeApiService.EvolutionMethod.LEVEL)) return "LEVEL"
        return when(primaryMethod(rule.methods)){
            PokeApiService.EvolutionMethod.LEVEL -> "LEVEL"
            PokeApiService.EvolutionMethod.TRADE -> "TRADE"
            PokeApiService.EvolutionMethod.ITEM -> "ITEM"
            PokeApiService.EvolutionMethod.MULTIPLAYER -> "MULTIPLAYER"
            PokeApiService.EvolutionMethod.FRIENDSHIP -> "FRIENDSHIP"
            PokeApiService.EvolutionMethod.TIME -> "TIME"
            PokeApiService.EvolutionMethod.GENDER -> "GENDER"
            PokeApiService.EvolutionMethod.MOVE -> "MOVE"
            PokeApiService.EvolutionMethod.LOCATION -> "LOCATION"
            PokeApiService.EvolutionMethod.ACTION -> "ACTION"
            PokeApiService.EvolutionMethod.LEVEL_CONDITION -> "CONDITION"
            PokeApiService.EvolutionMethod.OTHER -> "CONDITION"
        }
    }
}
