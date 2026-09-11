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
        val nextId=next?.id.orEmpty()
        val phase=when{
            nextId.startsWith("sv-pg-") || nextId.startsWith("sv-dlc-") || nextId.startsWith("sv-epi-") -> CampaignPhase.LATE
            ratio < .34f -> CampaignPhase.EARLY
            ratio < .72f -> CampaignPhase.MID
            else -> CampaignPhase.LATE
        }
        val label=when{
            nextId.startsWith("sv-pg-") -> "Pós-jogo de Paldea"
            nextId in listOf("sv-dlc-01","sv-dlc-02","sv-dlc-03","sv-dlc-04","sv-dlc-05","sv-dlc-06","sv-dlc-07") -> "The Teal Mask"
            nextId.startsWith("sv-dlc-") -> "The Indigo Disk"
            nextId.startsWith("sv-epi-") -> "Mochi Mayhem"
            phase==CampaignPhase.EARLY -> "Início da campanha"
            phase==CampaignPhase.MID -> "Meio da campanha"
            else -> "Reta final"
        }
        val recommendation=when{
            steps.isEmpty() -> "A rota inteligente deste jogo ainda não foi catalogada."
            next==null -> "Jornada completa: campanha, pós-jogo, DLC e epílogo concluídos."
            else -> "Próximo alvo: "+next.title+" · "+next.levelLabel+". Etapa atual: "+label+"."
        }
        return JourneySmartContext(count,steps.size,next,phase,label,recommendation)
    }
}
