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
        387->listOf(387,388,389)
        390->listOf(390,391,392)
        393->listOf(393,394,395)
        25->listOf(25)
        133->listOf(133)
        1->listOf(1,2,3)
        4->listOf(4,5,6)
        7->listOf(7,8,9)
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

    fun starterMemberForProgress(starterId:Int,step:JourneyStep?):Int =
        progressMemberFor(starterLine(starterId).first(),step)

    fun isStarterLinePokemon(starterId:Int,pokemonId:Int):Boolean =
        pokemonId in starterLine(starterId)

    private val knownFamilies=listOf(
        listOf(906,907,908),listOf(909,910,911),listOf(912,913,914),
        listOf(821,822,823),listOf(940,941),listOf(194,980),listOf(928,929,930),
        listOf(935,936,937),listOf(129,130),listOf(296,297),listOf(280,281,282,475),
        listOf(328,329,330),listOf(56,57,979),
        listOf(810,811,812),listOf(813,814,815),listOf(816,817,818),
        listOf(835,836),listOf(850,851),listOf(829,830),listOf(859,860,861),
        listOf(848,849),listOf(529,530),listOf(679,680,681),listOf(885,886,887),
        listOf(722,723,724),listOf(155,156,157),listOf(501,502,503),
        listOf(403,404,405),listOf(396,397,398),listOf(418,419),listOf(390,391,392),
        listOf(216,217,901),listOf(215,903),listOf(443,444,445),listOf(447,448),
        listOf(387,388,389),listOf(393,394,395),listOf(406,315,407),listOf(459,460),
        listOf(152,153,154),listOf(498,499,500),listOf(158,159,160),
        listOf(214),listOf(659,660),listOf(661,662,663),listOf(25,26),listOf(92,93,94),
        listOf(1,2,3),listOf(4,5,6),listOf(7,8,9),listOf(16,17,18),listOf(29,30,31),
        listOf(32,33,34),listOf(43,44,45),listOf(63,64,65),listOf(131),listOf(143)
    )

    fun familyKey(pokemonId:Int):Int =
        knownFamilies.firstOrNull{pokemonId in it}?.first() ?: pokemonId

    fun sameEvolutionFamily(a:Int,b:Int):Boolean = familyKey(a)==familyKey(b)

    fun hasFamilyDuplicate(ids:List<Int>):Boolean =
        ids.map(::familyKey).let{keys->keys.size!=keys.distinct().size}

    private data class LevelEvolution(val family:List<Int>,val thresholds:List<Int>)

    private val levelEvolutions=listOf(
        LevelEvolution(listOf(906,907,908),listOf(16,36)),
        LevelEvolution(listOf(909,910,911),listOf(16,36)),
        LevelEvolution(listOf(912,913,914),listOf(16,36)),
        LevelEvolution(listOf(821,822,823),listOf(18,38)),
        LevelEvolution(listOf(940,941),listOf(25)),
        LevelEvolution(listOf(194,980),listOf(20)),
        LevelEvolution(listOf(928,929,930),listOf(25,35)),
        LevelEvolution(listOf(129,130),listOf(20)),
        LevelEvolution(listOf(296,297),listOf(24)),
        LevelEvolution(listOf(280,281,282),listOf(20,30)),
        LevelEvolution(listOf(328,329,330),listOf(35,45)),
        LevelEvolution(listOf(56,57),listOf(28)),
        LevelEvolution(listOf(810,811,812),listOf(16,35)),
        LevelEvolution(listOf(813,814,815),listOf(16,35)),
        LevelEvolution(listOf(816,817,818),listOf(16,35)),
        LevelEvolution(listOf(835,836),listOf(25)),
        LevelEvolution(listOf(850,851),listOf(28)),
        LevelEvolution(listOf(829,830),listOf(20)),
        LevelEvolution(listOf(859,860,861),listOf(32,42)),
        LevelEvolution(listOf(848,849),listOf(30)),
        LevelEvolution(listOf(529,530),listOf(31)),
        LevelEvolution(listOf(679,680),listOf(35)),
        LevelEvolution(listOf(607,608),listOf(41)),
        LevelEvolution(listOf(885,886,887),listOf(50,60)),
        LevelEvolution(listOf(722,723,724),listOf(17,36)),
        LevelEvolution(listOf(155,156,157),listOf(17,36)),
        LevelEvolution(listOf(501,502,503),listOf(17,36)),
        LevelEvolution(listOf(403,404,405),listOf(15,30)),
        LevelEvolution(listOf(396,397,398),listOf(14,34)),
        LevelEvolution(listOf(418,419),listOf(26)),
        LevelEvolution(listOf(390,391,392),listOf(14,36)),
        LevelEvolution(listOf(216,217),listOf(30)),
        LevelEvolution(listOf(443,444,445),listOf(24,48)),
        LevelEvolution(listOf(387,388,389),listOf(18,32)),
        LevelEvolution(listOf(393,394,395),listOf(16,36)),
        LevelEvolution(listOf(459,460),listOf(40)),
        LevelEvolution(listOf(152,153,154),listOf(16,32)),
        LevelEvolution(listOf(498,499,500),listOf(17,36)),
        LevelEvolution(listOf(158,159,160),listOf(18,30)),
        LevelEvolution(listOf(659,660),listOf(20)),
        LevelEvolution(listOf(661,662,663),listOf(17,35)),
        LevelEvolution(listOf(92,93),listOf(25)),
        LevelEvolution(listOf(1,2,3),listOf(16,32)),
        LevelEvolution(listOf(4,5,6),listOf(16,36)),
        LevelEvolution(listOf(7,8,9),listOf(16,36)),
        LevelEvolution(listOf(16,17,18),listOf(18,36)),
        LevelEvolution(listOf(19,20),listOf(20)),
        LevelEvolution(listOf(29,30),listOf(16)),
        LevelEvolution(listOf(32,33),listOf(16)),
        LevelEvolution(listOf(43,44),listOf(21)),
        LevelEvolution(listOf(63,64),listOf(16))
    )

    private fun targetLevel(step:JourneyStep?):Int? {
        val raw=step?.levelLabel ?: return null
        val nums=Regex("""\d+""").findAll(raw).mapNotNull{it.value.toIntOrNull()}.toList()
        return nums.maxOrNull()
    }

    fun progressMemberFor(pokemonId:Int,step:JourneyStep?):Int {
        val level=targetLevel(step) ?: return pokemonId
        val rule=levelEvolutions.firstOrNull{pokemonId in it.family} ?: return pokemonId
        var stage=0
        rule.thresholds.forEachIndexed{index,threshold->
            if(level>=threshold)stage=index+1
        }
        return rule.family.getOrElse(stage){rule.family.last()}
    }

    fun normalizedForProgress(ids:List<Int>,step:JourneyStep?):List<Int> =
        ids.map{progressMemberFor(it,step)}

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
        stepId.startsWith("bdsp-") -> sinnohCatchPlan
        stepId.startsWith("lgpe-") -> letsGoCatchPlan
        stepId.startsWith("frlg-") -> frlgCatchPlan
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
        stepId in setOf("bdsp-01","bdsp-02","bdsp-g1","bdsp-04","bdsp-g2","bdsp-06") -> "SINNOH · PRIMEIRAS BADGES"
        stepId in setOf("bdsp-g3","bdsp-08","bdsp-g4","bdsp-10","bdsp-g5","bdsp-g6") -> "SINNOH · MEIO DA CAMPANHA"
        stepId in setOf("bdsp-13","bdsp-14","bdsp-g7","bdsp-16","bdsp-17","bdsp-g8") -> "TEAM GALACTIC · SPEAR PILLAR"
        stepId=="bdsp-19" || stepId.startsWith("bdsp-e4-") || stepId=="bdsp-24" -> "LIGA POKÉMON · CYNTHIA"
        stepId.startsWith("bdsp-pg-") -> "SINNOH · PÓS-GAME"
        stepId in setOf("lgpe-01","lgpe-02","lgpe-g1","lgpe-04","lgpe-g2","lgpe-06","lgpe-g3") -> "KANTO · PRIMEIRAS BADGES"
        stepId in setOf("lgpe-08","lgpe-09","lgpe-g4","lgpe-11","lgpe-12","lgpe-g5","lgpe-g6") -> "KANTO · TEAM ROCKET"
        stepId in setOf("lgpe-g7","lgpe-g8","lgpe-17") || stepId.startsWith("lgpe-e4-") || stepId=="lgpe-22" -> "INDIGO PLATEAU · RETA FINAL"
        stepId.startsWith("lgpe-pg-") -> "KANTO · PÓS-GAME"
        stepId in setOf("frlg-01","frlg-02","frlg-g1","frlg-04","frlg-g2","frlg-06","frlg-g3") -> "KANTO · PRIMEIRAS BADGES"
        stepId in setOf("frlg-08","frlg-g4","frlg-10","frlg-11","frlg-12","frlg-g5","frlg-g6") -> "KANTO · TEAM ROCKET"
        stepId in setOf("frlg-g7","frlg-sevii-1","frlg-g8","frlg-18") || stepId.startsWith("frlg-e4-") || stepId=="frlg-23" -> "INDIGO PLATEAU · RETA FINAL"
        stepId.startsWith("frlg-pg-") -> "SEVII ISLANDS · PÓS-GAME"
        else -> "JORNADA"
    }

    private val frlgCatchPlan=listOf(
        JourneyCatchRecommendation(16,2,"Route 1 / Viridian Forest","Pidgey evolui para Pidgeot e oferece cobertura Voador útil contra Erika e Bruno."),
        JourneyCatchRecommendation(25,2,"Viridian Forest","Pikachu ajuda muito contra Misty, Lorelei e vários Pokémon de Água/Voador."),
        JourneyCatchRecommendation(56,4,"Route 3","Mankey é excelente contra Brock se você escolheu Charmander e continua útil contra Normal/Pedra."),
        JourneyCatchRecommendation(29,4,"Route 3","Nidoran♀ evolui para Nidoqueen e oferece cobertura ampla por TMs."),
        JourneyCatchRecommendation(32,4,"Route 3","Nidoran♂ evolui para Nidoking e é um dos melhores coringas ofensivos da campanha."),
        JourneyCatchRecommendation(63,6,"Route 24 / 25","Abra evolui para Kadabra/Alakazam e domina muitos confrontos de Team Rocket e Koga."),
        JourneyCatchRecommendation(129,7,"Old Rod / águas de Kanto","Magikarp evolui para Gyarados e vira um dos melhores atacantes físicos da campanha."),
        JourneyCatchRecommendation(92,11,"Pokémon Tower","Gastly oferece Fantasma/Veneno para Sabrina e Agatha."),
        JourneyCatchRecommendation(131,12,"Silph Co. gift","Lapras dá Água/Gelo excelente para Blaine, Giovanni, Lance e a Liga."),
        JourneyCatchRecommendation(143,13,"Routes 12 / 16","Snorlax oferece enorme bulk e cobertura por TMs para a reta final.")
    )

    private val letsGoCatchPlan=listOf(
        JourneyCatchRecommendation(1,2,"Viridian Forest","Bulbasaur ajuda muito contra Brock e Misty e pode ser recebido gratuitamente em Cerulean após cumprir requisito de capturas."),
        JourneyCatchRecommendation(16,2,"Routes 1–2","Pidgey/Pidgeotto fornece Voador cedo e cobertura útil contra Erika."),
        JourneyCatchRecommendation(29,3,"Route 22 / Route 3","Nidoran oferece linha versátil com boa cobertura por TMs."),
        JourneyCatchRecommendation(32,3,"Route 22 / Route 3","Nidoran macho é uma alternativa física forte para evoluir até Nidoking."),
        JourneyCatchRecommendation(56,4,"Route 3","Mankey é especialmente útil contra Brock e ameaças Normais/Pedra."),
        JourneyCatchRecommendation(37,6,"Routes 5–6","Vulpix em Eevee ou Growlithe em Pikachu dão opção de Fogo confiável para Erika."),
        JourneyCatchRecommendation(58,6,"Routes 5–6","Growlithe em Pikachu ou Vulpix em Eevee cobrem Planta/Gelo e ajudam no mid game."),
        JourneyCatchRecommendation(63,8,"Routes 5–6 / Saffron","Abra evolui para Kadabra/Alakazam e entrega enorme pressão especial contra Koga e Bruno."),
        JourneyCatchRecommendation(92,9,"Pokémon Tower","Gastly oferece Fantasma/Veneno para Sabrina e Agatha."),
        JourneyCatchRecommendation(131,15,"Silph Co. gift / pós-Saffron","Lapras dá Água/Gelo excelente para Blaine, Giovanni e principalmente Lance.")
    )

    private val sinnohCatchPlan=listOf(
        JourneyCatchRecommendation(396,2,"Route 201 / Route 202","Starly evolui para Staraptor e entrega Voador/Lutador com excelente valor por toda a campanha."),
        JourneyCatchRecommendation(403,2,"Route 202","Shinx evolui para Luxray e fornece cobertura Elétrica essencial contra Água e Voador."),
        JourneyCatchRecommendation(406,3,"Route 204 / Eterna Forest","Budew evolui para Roserade, ótimo atacante especial de Planta/Veneno."),
        JourneyCatchRecommendation(74,3,"Oreburgh Mine","Geodude oferece Pedra/Terra cedo e ajuda a estabilizar o início."),
        JourneyCatchRecommendation(418,5,"Route 205 / Valley Windworks","Buizel evolui para Floatzel, Água rápido para quem não escolheu Piplup."),
        JourneyCatchRecommendation(434,7,"Route 206","Stunky/Skuntank em Brilliant Diamond dá Sombrio/Veneno útil contra Fantasma e Psíquico."),
        JourneyCatchRecommendation(200,7,"Eterna Forest / Lost Tower","Misdreavus em Shining Pearl oferece alternativa Fantasma exclusiva da versão."),
        JourneyCatchRecommendation(443,10,"Wayward Cave","Gible evolui para Garchomp e é um dos maiores upgrades possíveis para a Liga."),
        JourneyCatchRecommendation(447,12,"Iron Island","Riolu/Lucario oferece Lutador/Aço e excelente cobertura para o fim da campanha."),
        JourneyCatchRecommendation(459,14,"Route 216 / 217","Snover/Abomasnow fornece Gelo para Dragões, incluindo Garchomp de Cynthia.")
    )

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
