package com.otaviobarreto.pokedex.data

data class DynamicTeamSuggestion(
    val preset:CampaignTeamPreset?,
    val focusStep:JourneyStep?,
    val recommendedPokemonIds:List<Int>,
    val reason:String
)

object JourneyDynamicTeamCatalog {
    fun suggestion(game:String,starterId:Int):DynamicTeamSuggestion{
        val smart=JourneySmartProgress.context(game)
        val preset=TeamCampaignCatalog.preset(game,starterId,smart.phase)
        val prep=smart.nextStep?.let{JourneyPreparationCatalog.forStep(it.id)}
        val recommended=prep?.pokemonIds.orEmpty()
        val reason=when{
            smart.nextStep==null -> "Campanha principal concluída: use o time de reta final e priorize cobertura geral."
            prep==null -> "Time ajustado para a fase atual da Jornada."
            else -> "Para "+smart.nextStep.title+", priorize "+prep.counters.joinToString(" / ")+" e mantenha o núcleo do time da fase "+smart.phase.label+"."
        }
        return DynamicTeamSuggestion(preset,smart.nextStep,recommended,reason)
    }
}
