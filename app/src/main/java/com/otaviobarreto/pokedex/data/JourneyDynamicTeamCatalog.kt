package com.otaviobarreto.pokedex.data

data class DynamicTeamSwap(
    val outPokemonId:Int?,
    val inPokemonId:Int,
    val reason:String
)

data class DynamicTeamSuggestion(
    val preset:CampaignTeamPreset?,
    val focusStep:JourneyStep?,
    val recommendedPokemonIds:List<Int>,
    val adjustedSlots:List<CampaignSlot>,
    val swaps:List<DynamicTeamSwap>,
    val reason:String
)

object JourneyDynamicTeamCatalog {
    fun suggestion(game:String,starterId:Int):DynamicTeamSuggestion{
        val smart=JourneySmartProgress.context(game)
        val preset=TeamCampaignCatalog.preset(game,starterId,smart.phase)
        val prep=smart.nextStep?.let{JourneyPreparationCatalog.forStep(it.id)}
        val rawRecommended=prep?.pokemonIds.orEmpty()
        val recommended=rawRecommended.sortedByDescending{it in CollectionStore.capturedIds}
        val result=adjustSlots(
            base=preset?.slots.orEmpty(),
            recommended=recommended,
            starterId=starterId,
            counters=prep?.counters.orEmpty(),
            focus=smart.nextStep
        )
        val reason=when{
            smart.nextStep==null -> "Campanha principal concluída: use o time de reta final e priorize cobertura geral."
            prep==null -> "Time ajustado para a fase atual da Jornada."
            result.swaps.isEmpty() -> "Seu time atual já cobre "+smart.nextStep.title+". Mantenha o núcleo e ajuste golpes/itens se necessário."
            else -> "Para "+smart.nextStep.title+", o app recomenda "+result.swaps.size+" ajuste(s) de time para ganhar cobertura em "+prep.counters.joinToString(" / ")+"."
        }
        return DynamicTeamSuggestion(preset,smart.nextStep,recommended,result.slots,result.swaps,reason)
    }

    private data class AdjustmentResult(
        val slots:List<CampaignSlot>,
        val swaps:List<DynamicTeamSwap>
    )

    private fun adjustSlots(
        base:List<CampaignSlot>,
        recommended:List<Int>,
        starterId:Int,
        counters:List<String>,
        focus:JourneyStep?
    ):AdjustmentResult{
        if(base.isEmpty() || recommended.isEmpty())return AdjustmentResult(base,emptyList())
        val out=base.toMutableList()
        val swaps=mutableListOf<DynamicTeamSwap>()
        val protectedIndex=out.indexOfFirst{it.pokemonId==starterId}.takeIf{it>=0} ?: 0

        recommended.take(2).forEach{candidate->
            if(out.any{it.pokemonId==candidate})return@forEach
            val replaceIndex=out.indices.lastOrNull{
                it!=protectedIndex && out[it].pokemonId !in recommended
            } ?: return@forEach
            val outgoing=out[replaceIndex].pokemonId
            out[replaceIndex]=CampaignSlot(candidate)
            swaps += DynamicTeamSwap(
                outPokemonId=outgoing,
                inPokemonId=candidate,
                reason=buildString{
                    append("Entrar para ")
                    append(focus?.title ?: "o próximo objetivo")
                    if(counters.isNotEmpty()) append(": cobertura ").append(counters.joinToString(" / "))
                    append(". O inicial é preservado como núcleo do time.")
                }
            )
        }
        return AdjustmentResult(out,swaps)
    }
}
