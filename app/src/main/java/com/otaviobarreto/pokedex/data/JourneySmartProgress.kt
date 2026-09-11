package com.otaviobarreto.pokedex.data

data class JourneySmartContext(
    val completedCount:Int,
    val totalCount:Int,
    val nextStep:JourneyStep?,
    val phase:CampaignPhase,
    val phaseLabel:String,
    val recommendation:String
)

object JourneySmartProgress {
    fun context(game:String):JourneySmartContext{
        val steps=JourneyCatalog.steps(game)
        val completed=JourneyProgressStore.completed(game)
        val next=steps.firstOrNull{it.id !in completed}
        val count=completed.count{done->steps.any{it.id==done}}.coerceAtMost(steps.size)
        val ratio=if(steps.isEmpty())0f else count.toFloat()/steps.size
        val phase=when{
            ratio < .34f -> CampaignPhase.EARLY
            ratio < .72f -> CampaignPhase.MID
            else -> CampaignPhase.LATE
        }
        val label=when(phase){
            CampaignPhase.EARLY -> "Início da campanha"
            CampaignPhase.MID -> "Meio da campanha"
            CampaignPhase.LATE -> "Reta final"
        }
        val recommendation=when{
            steps.isEmpty() -> "A rota inteligente deste jogo ainda não foi catalogada."
            next==null -> "Rota principal concluída. Seu time deve estar preparado para o fechamento da campanha."
            else -> "Próximo alvo: "+next.title+" · "+next.levelLabel+". O Time Ideal será aberto já na fase "+phase.label+"."
        }
        return JourneySmartContext(count,steps.size,next,phase,label,recommendation)
    }
}
