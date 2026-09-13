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
            nextId.startsWith("sv-pg-") || nextId.startsWith("sv-dlc-") || nextId.startsWith("sv-epi-") ||
                nextId.startsWith("za-dlc-") || nextId in setOf("za-38","za-39","za-40","za-41","za-42") ||
                nextId in setOf("la-19","la-20","la-21","la-22","la-23","la-24","la-25","la-26","la-27") || nextId.startsWith("la-db-") ||
                nextId in setOf("swsh-13","swsh-14","swsh-15","swsh-16") || nextId.startsWith("swsh-pg-") || nextId.startsWith("swsh-ioa-") || nextId.startsWith("swsh-ct-") ||
                nextId=="lgpe-17" || nextId.startsWith("lgpe-e4-") || nextId=="lgpe-22" || nextId.startsWith("lgpe-pg-") ||
                nextId=="frlg-18" || nextId.startsWith("frlg-e4-") || nextId=="frlg-23" || nextId.startsWith("frlg-pg-") -> CampaignPhase.LATE
            ratio < .34f -> CampaignPhase.EARLY
            ratio < .72f -> CampaignPhase.MID
            else -> CampaignPhase.LATE
        }
        val label=when{
            nextId.startsWith("sv-pg-") -> "Pós-jogo de Paldea"
            nextId in listOf("sv-dlc-01","sv-dlc-02","sv-dlc-03","sv-dlc-04","sv-dlc-05","sv-dlc-06","sv-dlc-07") -> "The Teal Mask"
            nextId.startsWith("sv-dlc-") -> "The Indigo Disk"
            nextId.startsWith("sv-epi-") -> "Mochi Mayhem"
            nextId.startsWith("za-dlc-") -> "Mega Dimension"
            nextId in setOf("za-38","za-39","za-40","za-41","za-42") -> "Pós-game de Lumiose"
            nextId.startsWith("za-") -> JourneyTeamProgressCatalog.chapterFor(nextId)
            nextId.startsWith("la-") -> JourneyTeamProgressCatalog.chapterFor(nextId)
            nextId.startsWith("swsh-") -> JourneyTeamProgressCatalog.chapterFor(nextId)
            nextId.startsWith("lgpe-") -> JourneyTeamProgressCatalog.chapterFor(nextId)
            nextId.startsWith("frlg-") -> JourneyTeamProgressCatalog.chapterFor(nextId)
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
