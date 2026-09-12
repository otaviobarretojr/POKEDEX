package com.otaviobarreto.pokedex.data

data class DynamicTeamSuggestion(
    val preset:CampaignTeamPreset?,
    val focusStep:JourneyStep?,
    val recommendedPokemonIds:List<Int>,
    val adjustedSlots:List<CampaignSlot>,
    val actions:List<JourneyTeamAction>,
    val reason:String
)

object JourneyDynamicTeamCatalog {
    fun suggestion(game:String,starterId:Int):DynamicTeamSuggestion{
        val smart=JourneySmartProgress.context(game)
        val preset=TeamCampaignCatalog.preset(game,starterId,smart.phase)
        val prep=smart.nextStep?.let{JourneyPreparationCatalog.forStep(it.id)}
        val availableCatchIds=JourneyTeamProgressCatalog
            .catchRecommendationsBefore(smart.nextStep)
            .map{it.pokemonId}
            .toSet()
        val rawRecommended=prep?.pokemonIds.orEmpty()
        val recommended=rawRecommended
            .filter { it in CollectionStore.capturedIds || it in availableCatchIds }
            .sortedByDescending{it in CollectionStore.capturedIds}
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
            result.actions.none{it.type==JourneyTeamActionType.SWAP || it.type==JourneyTeamActionType.CATCH} ->
                "Seu time atual já cobre "+smart.nextStep.title+". Mantenha o núcleo e ajuste golpes/itens se necessário."
            else -> "Para "+smart.nextStep.title+", o app propõe ajustes de equipe com base na cobertura necessária e no que já está disponível antes desta etapa."
        }
        return DynamicTeamSuggestion(preset,smart.nextStep,recommended,result.slots,result.actions,reason)
    }

    private data class AdjustmentResult(
        val slots:List<CampaignSlot>,
        val actions:List<JourneyTeamAction>
    )

    private fun adjustSlots(
        base:List<CampaignSlot>,
        recommended:List<Int>,
        starterId:Int,
        counters:List<String>,
        focus:JourneyStep?
    ):AdjustmentResult{
        if(base.isEmpty())return AdjustmentResult(base,emptyList())
        val out=base.toMutableList()
        val actions=mutableListOf<JourneyTeamAction>()
        val starterLine=JourneyTeamProgressCatalog.starterLine(starterId)
        val starterIndex=out.indexOfFirst{it.pokemonId in starterLine}
            .takeIf{it>=0} ?: 0

        val expectedStarter=JourneyTeamProgressCatalog.starterMemberForPhase(
            starterId,
            JourneySmartProgress.context("Scarlet / Violet").phase
        )
        val currentStarter=out[starterIndex].pokemonId
        if(currentStarter!=expectedStarter && expectedStarter in starterLine){
            actions += JourneyTeamAction(
                type=JourneyTeamActionType.EVOLVE,
                fromPokemonId=currentStarter,
                toPokemonId=expectedStarter,
                title="Evolua seu inicial",
                reason="A linha evolutiva do inicial é o núcleo do time e acompanha a fase atual da campanha."
            )
            out[starterIndex]=CampaignSlot(expectedStarter)
        }

        recommended.take(2).forEach{candidate->
            if(out.any{it.pokemonId==candidate})return@forEach
            val replaceIndex=out.indices.lastOrNull{
                it!=starterIndex &&
                    out[it].pokemonId !in recommended &&
                    out[it].pokemonId !in starterLine
            } ?: return@forEach

            val outgoing=out[replaceIndex].pokemonId
            val alreadyOwned=candidate in CollectionStore.capturedIds
            val actionType=if(alreadyOwned)JourneyTeamActionType.SWAP else JourneyTeamActionType.CATCH
            val title=if(alreadyOwned)"Troque um slot do time" else "Capture antes de seguir"
            out[replaceIndex]=CampaignSlot(candidate)
            actions += JourneyTeamAction(
                type=actionType,
                fromPokemonId=outgoing,
                toPokemonId=candidate,
                title=title,
                reason=buildString{
                    append(if(alreadyOwned)"Use este Pokémon para " else "Capture este Pokémon para ")
                    append(focus?.title ?: "o próximo objetivo")
                    if(counters.isNotEmpty()) append(": cobertura ").append(counters.joinToString(" / "))
                    append(". A linha do inicial permanece protegida.")
                }
            )
        }

        JourneyTeamProgressCatalog.newlyRelevantCatches(focus).forEach{catch->
            if(catch.pokemonId !in CollectionStore.capturedIds && actions.none{it.toPokemonId==catch.pokemonId}){
                actions += JourneyTeamAction(
                    type=JourneyTeamActionType.CATCH,
                    toPokemonId=catch.pokemonId,
                    title="Captura recomendada no caminho",
                    reason=catch.reason+" Área: "+catch.area+"."
                )
            }
        }
        return AdjustmentResult(out,actions)
    }
}
