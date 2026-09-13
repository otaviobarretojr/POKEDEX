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
        152->listOf(152,153,154)
        498->listOf(498,499,500)
        158->listOf(158,159,160)
        722->listOf(722,723,724)
        155->listOf(155,156,157)
        501->listOf(501,502,503)
        810->listOf(810,811,812)
        813->listOf(813,814,815)
        816->listOf(816,817,818)
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
        val plan=catchPlanFor(step.id)
        return plan.filter{it.availableBeforeStepOrder<=order}
    }

    fun newlyRelevantCatches(step:JourneyStep?):List<JourneyCatchRecommendation> {
        val order=step?.order ?: return emptyList()
        val plan=catchPlanFor(step.id)
        return plan.filter{it.availableBeforeStepOrder==order}
    }

    private fun catchPlanFor(stepId:String):List<JourneyCatchRecommendation> = when {
        stepId.startsWith("za-") -> lumioseCatchPlan
        stepId.startsWith("sv-") -> paldeaCatchPlan
        stepId.startsWith("la-") -> hisuiCatchPlan
        stepId.startsWith("swsh-") -> galarCatchPlan
        else -> emptyList()
    }

    fun chapterFor(stepId:String):String = when{
        stepId.matches(Regex("sv-(0[1-9]|1[0-8])")) -> "PALDEA · 18 OBJETIVOS"
        stepId in setOf("sv-pg-01","sv-pg-02","sv-pg-03") -> "FINAIS DAS 3 HISTÓRIAS"
        stepId=="sv-pg-04" -> "AREA ZERO · THE WAY HOME"
        stepId.startsWith("sv-pg-") -> "PÓS-GAME · PALDEA"
        stepId in setOf("sv-dlc-01","sv-dlc-02","sv-dlc-03","sv-dlc-04","sv-dlc-05","sv-dlc-06","sv-dlc-07") -> "DLC · THE TEAL MASK"
        stepId.startsWith("sv-dlc-") -> "DLC · THE INDIGO DISK"
        stepId.startsWith("sv-epi-") -> "EPÍLOGO · MOCHI MAYHEM"
        stepId in setOf("za-01","za-02","za-03","za-04","za-05") -> "LUMIOSE · PRÓLOGO / RANK Z"
        stepId in setOf("za-06","za-07","za-08","za-09") -> "Z-A ROYALE · RANKS Y → V"
        stepId in setOf("za-10","za-11","za-12","za-13","za-14") -> "MEGA EVOLUTION · RANK F"
        stepId in setOf("za-15","za-16","za-17","za-18","za-19") -> "TEAM MZ · RANK E"
        stepId in setOf("za-20","za-21","za-22","za-23","za-24") -> "RUST SYNDICATE · RANK D"
        stepId in setOf("za-25","za-26","za-27","za-28","za-29","za-30") -> "SBC · RANK C"
        stepId in setOf("za-31","za-32","za-33","za-34","za-35") -> "QUASARTICO · RANK B"
        stepId in setOf("za-36","za-37") -> "RANK A · FINAL DE LUMIOSE"
        stepId.startsWith("za-") && !stepId.startsWith("za-dlc-") -> "PÓS-GAME · LUMIOSE"
        stepId.startsWith("za-dlc-") -> "DLC · MEGA DIMENSION"
        stepId in setOf("la-01","la-02","la-03","la-04","la-05","la-06") -> "HISUI · SURVEY CORPS"
        stepId in setOf("la-07","la-08") -> "NOBRES · FIELDLANDS / MIRELANDS"
        stepId in setOf("la-09","la-10") -> "COBALT COASTLANDS"
        stepId in setOf("la-11","la-12") -> "CORONET / ALABASTER"
        stepId in setOf("la-13","la-14","la-15","la-16","la-17","la-18") -> "CRISE DO ESPAÇO-TEMPO"
        stepId in setOf("la-19","la-20","la-21","la-22","la-23","la-24","la-25","la-26","la-27") -> "PÓS-GAME · PLATES E ARCEUS"
        stepId.startsWith("la-db-") -> "DAYBREAK · MASSIVE MASS OUTBREAKS"
        stepId in setOf("swsh-01","swsh-02","swsh-03","swsh-04") -> "GALAR · INÍCIO DO GYM CHALLENGE"
        stepId in setOf("swsh-g1","swsh-g2","swsh-g3") -> "GYM CHALLENGE · PRIMEIRAS BADGES"
        stepId in setOf("swsh-g4","swsh-g5","swsh-g6") -> "GYM CHALLENGE · MEIO DA CAMPANHA"
        stepId in setOf("swsh-g7","swsh-g8","swsh-13","swsh-14","swsh-15","swsh-16") -> "WYNDON · RETA FINAL"
        stepId.startsWith("swsh-pg-") -> "PÓS-GAME · HEROES OF GALAR"
        stepId.startsWith("swsh-ioa-") -> "DLC · ISLE OF ARMOR"
        stepId.startsWith("swsh-ct-") -> "DLC · CROWN TUNDRA"
        else -> "JORNADA"
    }

    private val galarCatchPlan=listOf(
        JourneyCatchRecommendation(821,2,"Route 1 / Route 2","Rookidee evolui para Corviknight e oferece excelente utilidade defensiva e cobertura Voador/Aço."),
        JourneyCatchRecommendation(835,3,"Route 2","Yamper/Boltund dão cobertura Elétrica cedo, especialmente útil contra Nessa."),
        JourneyCatchRecommendation(850,3,"Route 3","Sizzlipede evolui para Centiskorch e ajuda muito se o inicial não for de Fogo."),
        JourneyCatchRecommendation(829,4,"Route 3","Gossifleur/Eldegoss são opções Planta seguras para Água e Terra."),
        JourneyCatchRecommendation(859,5,"Motostoke Outskirts / Glimwood Tangle","Impidimp evolui para Grimmsnarl e oferece cobertura Sombrio/Fada excelente para a reta final."),
        JourneyCatchRecommendation(848,6,"Route 7 / Wild Area","Toxel evolui para Toxtricity e combina Elétrico/Veneno para excelente cobertura ofensiva."),
        JourneyCatchRecommendation(529,7,"Galar Mine / Wild Area","Drilbur evolui para Excadrill e entrega Terra/Aço de altíssimo valor contra vários líderes."),
        JourneyCatchRecommendation(679,8,"Hammerlocke Hills","Honedge evolui para Aegislash e oferece enorme utilidade contra Fada, Psíquico e Gelo."),
        JourneyCatchRecommendation(447,10,"Giant's Cap","Riolu/Lucario oferece Lutador/Aço para Circhester, Champion Cup e pós-game."),
        JourneyCatchRecommendation(885,12,"Lake of Outrage","Dreepy evolui para Dragapult e é um excelente upgrade para Champion Cup e conteúdo final.")
    )

    private val hisuiCatchPlan=listOf(
        JourneyCatchRecommendation(403,2,"Obsidian Fieldlands","Shinx é capturado no teste de entrada e evolui para Luxray, excelente cobertura Elétrica para toda a campanha."),
        JourneyCatchRecommendation(396,2,"Obsidian Fieldlands","Starly aparece cedo e sua linha entrega velocidade, Voador e cobertura física muito útil."),
        JourneyCatchRecommendation(418,3,"Obsidian Fieldlands","Buizel é uma opção Água acessível cedo e ajuda bastante se o inicial não cobre Fogo/Pedra."),
        JourneyCatchRecommendation(74,5,"Obsidian Fieldlands","Geodude oferece Pedra/Terra cedo para estabilizar encontros contra Fogo, Voador e Elétrico."),
        JourneyCatchRecommendation(123,7,"Obsidian Fieldlands","Scyther pode evoluir para Kleavor e virar um atacante físico fortíssimo no mid game."),
        JourneyCatchRecommendation(280,8,"Crimson Mirelands","Ralts dá acesso a Gardevoir/Gallade e traz ótima cobertura Psíquica/Fada."),
        JourneyCatchRecommendation(133,10,"Horseshoe Plains / Space-time Distortions","Eevee é flexível e permite preencher a principal lacuna elemental do seu time."),
        JourneyCatchRecommendation(215,11,"Coronet Highlands / Alabaster Icelands","Hisuian Sneasel evolui para Sneasler e adiciona alta velocidade e cobertura Lutador/Veneno."),
        JourneyCatchRecommendation(443,12,"Coronet Highlands","Gible evolui para Garchomp e é um dos melhores upgrades de reta final para dano físico e cobertura Terra/Dragão."),
        JourneyCatchRecommendation(447,17,"Alabaster Icelands","Riolu evolui para Lucario e oferece excelente cobertura na crise final e no pós-game.")
    )

    private val lumioseCatchPlan=listOf(
        JourneyCatchRecommendation(214,3,"Side Mission cedo / Lumiose","Heracross é um atacante físico excelente e continua relevante após liberar Mega Evolução."),
        JourneyCatchRecommendation(129,4,"Wild Zone 2 / Wild Zone 6","Magikarp evolui para Gyarados e entrega ótima cobertura contra Fogo, Terra e Pedra."),
        JourneyCatchRecommendation(69,6,"Wild Zone 5","Bellsprout oferece Planta/Veneno cedo; o Alpha da área pode ajudar contra Alphas e chefes."),
        JourneyCatchRecommendation(280,7,"Vert Sector 4 à noite","Ralts evolui para Gardevoir e dá cobertura Psíquica/Fada muito útil na Z-A Royale."),
        JourneyCatchRecommendation(359,9,"Missão principal / história","Absol entra naturalmente na história e ganha valor enorme com Mega Evolução."),
        JourneyCatchRecommendation(529,14,"Wild Zone 8 / 14","Drilbur evolui para Excadrill, uma das melhores coberturas de Terra/Aço da campanha."),
        JourneyCatchRecommendation(478,18,"Evolução de Snorunt","Froslass ajuda contra Dragão, Voador, Terra e Planta e funciona bem contra vários Rogue Megas."),
        JourneyCatchRecommendation(445,24,"Áreas avançadas de Lumiose","Garchomp é um ótimo upgrade de reta final para dano físico e cobertura Terra/Dragão.")
    )

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
