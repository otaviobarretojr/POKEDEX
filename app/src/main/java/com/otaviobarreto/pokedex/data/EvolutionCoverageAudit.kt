package com.otaviobarreto.pokedex.data

data class EvolutionAuditIssue(
    val code:String,
    val message:String,
    val sourcePokemonId:Int?=null,
    val targetPokemonId:Int?=null
)

object EvolutionCoverageAudit {
    private val formSensitiveTargets=setOf(865,867,902,903,904)

    fun inspect(routes:List<EvolutionRoute>):List<EvolutionAuditIssue> = buildList {
        routes.forEach{route->
            if(route.sourcePokemonId==route.targetPokemonId){
                add(EvolutionAuditIssue("SELF_ROUTE","Origem e destino não podem ser iguais.",route.sourcePokemonId,route.targetPokemonId))
            }
            if(route.summary.isBlank() || route.detail.isBlank()){
                add(EvolutionAuditIssue("EMPTY_RULE","Rota sem regra legível.",route.sourcePokemonId,route.targetPokemonId))
            }
            if(route.availability==EvolutionAvailability.UNKNOWN){
                add(EvolutionAuditIssue("UNKNOWN_AVAILABILITY","Disponibilidade não resolvida.",route.sourcePokemonId,route.targetPokemonId))
            }
            if(EvolutionResolutionEngine.executable(route) && route.methods.isEmpty()){
                add(EvolutionAuditIssue("EXECUTABLE_WITHOUT_METHOD","Rota executável sem método classificado.",route.sourcePokemonId,route.targetPokemonId))
            }
            if(route.availability==EvolutionAvailability.TRANSFER_ONLY && !route.detail.contains("HOME",ignoreCase=true)){
                add(EvolutionAuditIssue("TRANSFER_WITHOUT_HOME","Transferência sem instrução de HOME.",route.sourcePokemonId,route.targetPokemonId))
            }
            if(route.targetPokemonId in formSensitiveTargets && route.sourceFormKey.isNullOrBlank()){
                add(EvolutionAuditIssue("MISSING_SOURCE_FORM","Evolução sensível a forma sem forma de origem.",route.sourcePokemonId,route.targetPokemonId))
            }
        }
    }

    fun inspectCuratedContexts():List<EvolutionAuditIssue> = buildList {
        AppGameCatalog.games.forEach{game->
            game.regions.forEach{region->
                val context=GameContext.fromSource(region.source)
                listOf(899,900,901,902,904).forEach{target->
                    val rule=EvolutionCuratedCatalog.ruleFor(target,context) ?: return@forEach
                    if(rule.requirement.isBlank()){
                        add(EvolutionAuditIssue("EMPTY_CURATED_RULE",region.source+" #"+target+" sem requisito.",targetPokemonId=target))
                    }
                    if(rule.requirement.contains("indisponível",ignoreCase=true) &&
                        !rule.requirement.contains("HOME",ignoreCase=true)){
                        add(EvolutionAuditIssue("UNSAFE_UNAVAILABLE_RULE",region.source+" #"+target+" indisponível sem orientação de transferência.",targetPokemonId=target))
                    }
                }
            }
        }
    }
}
