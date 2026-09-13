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
        val nextId=next?.id.orEmpty()
        val mainStory=steps.takeWhile{it.kind !in setOf(JourneyChallengeKind.POSTGAME,JourneyChallengeKind.DLC,JourneyChallengeKind.EPILOGUE)}
        val mainCompleted=completed.count{done->mainStory.any{it.id==done}}.coerceAtMost(mainStory.size)
        val mainRatio=when{
            mainStory.isEmpty() -> 0f
            next!=null && next !in mainStory -> 1f
            else -> mainCompleted.toFloat()/mainStory.size
        }
        val phase=when{
            next==null -> CampaignPhase.LATE
            next.kind in setOf(JourneyChallengeKind.POSTGAME,JourneyChallengeKind.DLC,JourneyChallengeKind.EPILOGUE) -> CampaignPhase.LATE
            mainRatio < .34f -> CampaignPhase.EARLY
            mainRatio < .72f -> CampaignPhase.MID
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
            nextId.startsWith("bdsp-") -> JourneyTeamProgressCatalog.chapterFor(nextId)
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
