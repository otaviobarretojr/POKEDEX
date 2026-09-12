package com.otaviobarreto.pokedex.data

enum class JourneyTeamActionType { KEEP, EVOLVE, CATCH, SWAP }

data class JourneyTeamAction(
    val type:JourneyTeamActionType,
    val fromPokemonId:Int?=null,
    val toPokemonId:Int,
    val title:String,
    val reason:String
)

data class JourneyCatchRecommendation(
    val pokemonId:Int,
    val availableBeforeStepOrder:Int,
    val area:String,
    val reason:String
)

object JourneyTeamProgressCatalog {
    fun starterLine(starterId:Int):List<Int> = when(starterId){
        906->listOf(906,907,908)
        909->listOf(909,910,911)
        912->listOf(912,913,914)
        else->listOf(starterId)
    }

    fun starterMemberForPhase(starterId:Int,phase:CampaignPhase):Int {
        val line=starterLine(starterId)
        return when(phase){
            CampaignPhase.EARLY->line.first()
            CampaignPhase.MID->line.getOrElse(1){line.first()}
            CampaignPhase.LATE->line.last()
        }
    }

    fun isStarterLinePokemon(starterId:Int,pokemonId:Int):Boolean =
        pokemonId in starterLine(starterId)

    fun catchRecommendationsBefore(step:JourneyStep?):List<JourneyCatchRecommendation> {
        val order=step?.order ?: return emptyList()
        return paldeaCatchPlan.filter{it.availableBeforeStepOrder<=order}
    }

    fun newlyRelevantCatches(step:JourneyStep?):List<JourneyCatchRecommendation> {
        val order=step?.order ?: return emptyList()
        return paldeaCatchPlan.filter{it.availableBeforeStepOrder==order}
    }

    fun chapterFor(stepId:String):String = when{
        stepId.matches(Regex("sv-(0[1-9]|1[0-8])")) -> "PALDEA · 18 OBJETIVOS"
        stepId in setOf("sv-pg-01","sv-pg-02","sv-pg-03") -> "FINAIS DAS 3 HISTÓRIAS"
        stepId=="sv-pg-04" -> "AREA ZERO · THE WAY HOME"
        stepId.startsWith("sv-pg-") -> "PÓS-GAME · PALDEA"
        stepId in setOf("sv-dlc-01","sv-dlc-02","sv-dlc-03","sv-dlc-04","sv-dlc-05","sv-dlc-06","sv-dlc-07") -> "DLC · THE TEAL MASK"
        stepId.startsWith("sv-dlc-") -> "DLC · THE INDIGO DISK"
        stepId.startsWith("sv-epi-") -> "EPÍLOGO · MOCHI MAYHEM"
        else -> "JORNADA"
    }

    private val paldeaCatchPlan=listOf(
        JourneyCatchRecommendation(940,2,"South Province / East Province","Wattrel entra cedo como cobertura Elétrica/Voadora e continua útil até Kilowattrel."),
        JourneyCatchRecommendation(194,2,"South Province","Wooper de Paldea é fácil de obter cedo e oferece ótima cobertura de Terra/Veneno."),
        JourneyCatchRecommendation(935,3,"East Province (Area One)","Charcadet oferece excelente rota de evolução para Armarouge/Ceruledge."),
        JourneyCatchRecommendation(129,4,"Áreas costeiras e lagos","Magikarp evolui cedo para Gyarados, um dos melhores upgrades de campanha."),
        JourneyCatchRecommendation(928,6,"Olive fields / South Province","Smoliv cobre Água/Terra e evolui para Arboliva no mid game."),
        JourneyCatchRecommendation(296,9,"South Province","Makuhita dá acesso barato a cobertura Lutador antes de Larry."),
        JourneyCatchRecommendation(280,10,"South Province","Ralts pode evoluir para Gardevoir e dá cobertura Psíquica/Fada."),
        JourneyCatchRecommendation(328,10,"Asado Desert","Trapinch vira opção forte de Terra quando a rota entra no deserto."),
        JourneyCatchRecommendation(823,14,"Evolução de Rookidee","Corviknight agrega resistência, imunidade a Terra e boa utilidade defensiva."),
        JourneyCatchRecommendation(979,16,"Evolução de Primeape","Annihilape é um upgrade forte para a reta final e pós-game.")
    )
}
