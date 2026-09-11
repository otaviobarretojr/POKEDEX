package com.otaviobarreto.pokedex.data

data class DynamicTeamSuggestion(
    val preset:CampaignTeamPreset?,
    val focusStep:JourneyStep?,
    val recommendedPokemonIds:List<Int>,
    val adjustedSlots:List<CampaignSlot>,
    val reason:String
)

object JourneyDynamicTeamCatalog {
    fun suggestion(game:String,starterId:Int):DynamicTeamSuggestion{
        val smart=JourneySmartProgress.context(game)
        val preset=TeamCampaignCatalog.preset(game,starterId,smart.phase)
        val prep=smart.nextStep?.let{JourneyPreparationCatalog.forStep(it.id)}
        val recommended=prep?.pokemonIds.orEmpty()
        val adjusted=adjustSlots(preset?.slots.orEmpty(),recommended,starterId)
        val reason=when{
            smart.nextStep==null -> "Campanha principal concluída: use o time de reta final e priorize cobertura geral."
            prep==null -> "Time ajustado para a fase atual da Jornada."
            else -> "Para "+smart.nextStep.title+", priorize "+prep.counters.joinToString(" / ")+" e mantenha o núcleo do time da fase "+smart.phase.label+"."
        }
        return DynamicTeamSuggestion(preset,smart.nextStep,recommended,adjusted,reason)
    }

    private fun adjustSlots(base:List<CampaignSlot>,recommended:List<Int>,starterId:Int):List<CampaignSlot>{
        if(base.isEmpty() || recommended.isEmpty())return base
        val out=base.toMutableList()
        val protectedIndex=out.indexOfFirst{it.pokemonId==starterId}.takeIf{it>=0} ?: 0
        recommended.take(2).forEach{candidate->
            if(out.any{it.pokemonId==candidate})return@forEach
            val replaceIndex=out.indices.lastOrNull{it!=protectedIndex && out[it].pokemonId !in recommended} ?: return@forEach
            out[replaceIndex]=CampaignSlot(candidate)
        }
        return out
    }
}
