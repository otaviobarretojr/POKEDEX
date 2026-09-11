package com.otaviobarreto.pokedex.data

enum class JourneyChallengeKind(val label:String){ GYM("Ginásio"), TITAN("Titã"), STAR("Team Star"), STORY("História"), POSTGAME("Pós-jogo"), DLC("DLC"), EPILOGUE("Epílogo") }

data class JourneyStep(
    val id:String,
    val order:Int,
    val title:String,
    val subtitle:String,
    val levelLabel:String,
    val kind:JourneyChallengeKind,
    val typeLabel:String,
    val location:String,
    val note:String = "",
    val imageUrl:String? = null
)

object JourneyCatalog {
    fun routeLabel(game:String):String = when(game){
        "Scarlet / Violet" -> "Campanha + pós-jogo + DLC · rota completa"
        else -> "Rota de campanha"
    }

    fun steps(game:String):List<JourneyStep> = when(game){
        "Scarlet / Violet" -> scarletViolet
        else -> emptyList()
    }

    private val scarletViolet = listOf(
        JourneyStep("sv-01",1,"Katy","Cortondo Gym","Nv. 14–15",JourneyChallengeKind.GYM,"Inseto","Cortondo","Primeiro ginásio recomendado; equipe simples e boa abertura de campanha."),
        JourneyStep("sv-02",2,"Klawf","Stony Cliff Titan","Nv. 16",JourneyChallengeKind.TITAN,"Pedra","South Province (Area Three)","Libera uma melhoria de mobilidade de Koraidon/Miraidon."),
        JourneyStep("sv-03",3,"Brassius","Artazon Gym","Nv. 16–17",JourneyChallengeKind.GYM,"Planta","Artazon"),
        JourneyStep("sv-04",4,"Bombirdier","Open Sky Titan","Nv. 19",JourneyChallengeKind.TITAN,"Voador / Sombrio","West Province (Area One)"),
        JourneyStep("sv-05",5,"Giacomo","Segin Squad","Nv. 20–21",JourneyChallengeKind.STAR,"Sombrio","West Province (Area One)"),
        JourneyStep("sv-06",6,"Iono","Levincia Gym","Nv. 23–24",JourneyChallengeKind.GYM,"Elétrico","Levincia"),
        JourneyStep("sv-07",7,"Mela","Schedar Squad","Nv. 26–27",JourneyChallengeKind.STAR,"Fogo","East Province (Area One)"),
        JourneyStep("sv-08",8,"Orthworm","Lurking Steel Titan","Nv. 28",JourneyChallengeKind.TITAN,"Aço","East Province (Area Three)"),
        JourneyStep("sv-09",9,"Kofu","Cascarrafa Gym","Nv. 29–30",JourneyChallengeKind.GYM,"Água","Cascarrafa"),
        JourneyStep("sv-10",10,"Atticus","Navi Squad","Nv. 32–33",JourneyChallengeKind.STAR,"Veneno","Tagtree Thicket"),
        JourneyStep("sv-11",11,"Larry","Medali Gym","Nv. 35–36",JourneyChallengeKind.GYM,"Normal","Medali"),
        JourneyStep("sv-12",12,"Ryme","Montenevera Gym","Nv. 41–42",JourneyChallengeKind.GYM,"Fantasma","Montenevera"),
        JourneyStep("sv-13",13,"Great Tusk / Iron Treads","Quaking Earth Titan","Nv. 44",JourneyChallengeKind.TITAN,"Terra","Asado Desert","Great Tusk em Scarlet; Iron Treads em Violet."),
        JourneyStep("sv-14",14,"Tulip","Alfornada Gym","Nv. 44–45",JourneyChallengeKind.GYM,"Psíquico","Alfornada"),
        JourneyStep("sv-15",15,"Grusha","Glaseado Gym","Nv. 47–48",JourneyChallengeKind.GYM,"Gelo","Glaseado"),
        JourneyStep("sv-16",16,"Ortega","Ruchbah Squad","Nv. 50–51",JourneyChallengeKind.STAR,"Fada","North Province (Area Three)"),
        JourneyStep("sv-17",17,"Dondozo / Tatsugiri","False Dragon Titan","Nv. 55",JourneyChallengeKind.TITAN,"Dragão","Casseroya Lake"),
        JourneyStep("sv-18",18,"Eri","Caph Squad","Nv. 55–56",JourneyChallengeKind.STAR,"Lutador","North Province (Area Two)","Fechamento recomendado dos 18 objetivos principais."),
        JourneyStep("sv-pg-01",19,"Victory Road Finale","Elite Four + Champion","Nv. 57–62",JourneyChallengeKind.POSTGAME,"Liga","Pokémon League","Conclua a avaliação da Liga e a sequência final da Victory Road."),
        JourneyStep("sv-pg-02",20,"Path of Legends Finale","Batalha final com Arven","Nv. 58–63",JourneyChallengeKind.POSTGAME,"História","Poco Path","Feche a história de Arven após os cinco Titãs."),
        JourneyStep("sv-pg-03",21,"Starfall Street Finale","Clavell + Cassiopeia","Nv. 60–63",JourneyChallengeKind.POSTGAME,"História","Uva / Naranja Academy","Conclua a identidade de Cassiopeia e o arco Team Star."),
        JourneyStep("sv-pg-04",22,"The Way Home","Area Zero","Nv. 62–67",JourneyChallengeKind.POSTGAME,"História","The Great Crater of Paldea","Reúna Nemona, Arven e Penny e conclua a campanha principal em Area Zero."),
        JourneyStep("sv-pg-05",23,"Gym Leader Rematches","Revanche dos 8 ginásios","Nv. 65–66",JourneyChallengeKind.POSTGAME,"Misto","Paldea","Revise os oito ginásios para liberar o Academy Ace Tournament."),
        JourneyStep("sv-pg-06",24,"Academy Ace Tournament","Primeiro torneio","Nv. 65–70",JourneyChallengeKind.POSTGAME,"Misto","Academy","Vença o primeiro Academy Ace Tournament e avance o pós-jogo."),
        JourneyStep("sv-pg-07",25,"Black Crystal Raids","6★ Tera Raids","Nv. 75+",JourneyChallengeKind.POSTGAME,"Tera","Paldea","Complete raids suficientes após o torneio até Jacq liberar as Black Crystal 6★ raids."),
        JourneyStep("sv-pg-08",26,"Paldea Endgame","Dex, lendários e exploração","Livre",JourneyChallengeKind.POSTGAME,"Exploração","Paldea","Complete a Pokédex, capture os Treasures of Ruin e finalize conteúdos opcionais antes ou junto dos DLCs."),
        JourneyStep("sv-dlc-01",27,"Chegada a Kitakami","The Teal Mask começa","≈ Nv. 60+",JourneyChallengeKind.DLC,"História","Mossui Town","Inicie a excursão escolar e conheça Carmine, Kieran e a lenda local."),
        JourneyStep("sv-dlc-02",28,"Festival das Máscaras","Primeiro encontro com Ogerpon","≈ Nv. 60+",JourneyChallengeKind.DLC,"História","Kitakami Hall","Avance pelo festival e descubra a figura misteriosa por trás da Teal Mask."),
        JourneyStep("sv-dlc-03",29,"Crystal Pool","Segredo de Ogerpon","≈ Nv. 62+",JourneyChallengeKind.DLC,"História","Oni Mountain","Busque o cristal necessário para reparar a máscara e avance a história de Kieran."),
        JourneyStep("sv-dlc-04",30,"Kieran e a Teal Mask","Confronto em Loyalty Plaza","≈ Nv. 62+",JourneyChallengeKind.DLC,"História","Loyalty Plaza","Derrote Kieran e testemunhe o retorno dos Loyal Three."),
        JourneyStep("sv-dlc-05",31,"The Loyal Three","Recupere as três máscaras","≈ Nv. 65+",JourneyChallengeKind.DLC,"Veneno","Kitakami","Derrote Okidogi, Munkidori e Fezandipiti espalhados por Kitakami."),
        JourneyStep("sv-dlc-06",32,"Ogerpon","Final de The Teal Mask","≈ Nv. 68–70",JourneyChallengeKind.DLC,"Lendário","Dreaded Den","Vença Kieran e conclua as quatro fases de Ogerpon para capturá-la."),
        JourneyStep("sv-dlc-07",33,"Bloodmoon Ursaluna","Missão de Perrin","≈ Nv. 70+",JourneyChallengeKind.DLC,"Opcional","Timeless Woods","Complete o requisito da Pokédex de Kitakami e finalize a missão fotográfica de Perrin."),
        JourneyStep("sv-dlc-08",34,"Blueberry Academy","The Indigo Disk começa","Nv. 70+",JourneyChallengeKind.DLC,"História","Blueberry Academy","Chegue ao Terarium e entre na BB League."),
        JourneyStep("sv-dlc-09",35,"Crispin","BB Elite Four · Fogo","Nv. 77–78",JourneyChallengeKind.DLC,"Fogo","Savanna Biome","Complete o Elite Trial e derrote Crispin em batalha dupla."),
        JourneyStep("sv-dlc-10",36,"Amarys","BB Elite Four · Aço","Nv. 78–79",JourneyChallengeKind.DLC,"Aço","Canyon Biome","Complete o Flying Time Trial e derrote Amarys."),
        JourneyStep("sv-dlc-11",37,"Lacey","BB Elite Four · Fada","Nv. 78–79",JourneyChallengeKind.DLC,"Fada","Coastal Biome","Passe pelo Elite Trial de Lacey e vença sua equipe em dupla."),
        JourneyStep("sv-dlc-12",38,"Drayton","BB Elite Four · Dragão","Nv. 79–80",JourneyChallengeKind.DLC,"Dragão","Polar Biome","Use Pokémon capturados no Terarium no Elite Trial e derrote Drayton."),
        JourneyStep("sv-dlc-13",39,"Kieran Champion","Final da BB League","Nv. 80–82",JourneyChallengeKind.DLC,"Campeão","Blueberry Academy","Derrote Kieran para assumir o topo da BB League."),
        JourneyStep("sv-dlc-14",40,"Area Zero Underdepths","Retorno a Paldea","Nv. 80+",JourneyChallengeKind.DLC,"História","Area Zero","Acompanhe Briar, Carmine e Kieran até as profundezas da Area Zero."),
        JourneyStep("sv-dlc-15",41,"Terapagos","Final de The Indigo Disk","Nv. 85",JourneyChallengeKind.DLC,"Lendário","Area Zero Underdepths","Conclua o confronto com Terapagos e feche a história principal do DLC."),
        JourneyStep("sv-dlc-16",42,"Blueberry Endgame","Lendários e League Club","Livre",JourneyChallengeKind.DLC,"Exploração","Blueberry Academy","Expanda o Terarium, conclua atividades do League Club e use Snacksworth para caçar lendários."),
        JourneyStep("sv-epi-01",43,"Mochi Mayhem","O epílogo começa","Nv. 80+",JourneyChallengeKind.EPILOGUE,"História","Mossui Town","Use a Mythical Pecha Berry em Peachy's e reúna seus amigos em Kitakami."),
        JourneyStep("sv-epi-02",44,"Mochi Mayhem","Amigos sob controle","Nv. 80+",JourneyChallengeKind.EPILOGUE,"História","Kitakami","Enfrente os personagens afetados pelo mochi misterioso e siga a origem do caos."),
        JourneyStep("sv-epi-03",45,"Nemona","Último grande duelo","Nv. 80+",JourneyChallengeKind.EPILOGUE,"Batalha","Loyalty Plaza","Derrote Nemona enquanto ela está sob influência do mochi."),
        JourneyStep("sv-epi-04",46,"Pecharunt","Final do epílogo","Nv. 88",JourneyChallengeKind.EPILOGUE,"Veneno / Fantasma","Loyalty Plaza","Derrote e capture Pecharunt para concluir Mochi Mayhem.")
    )
}
