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
        "Pokémon Legends: Z-A" -> "Z-A Royale + Rogue Megas + pós-jogo + Mega Dimension"
        else -> "Rota de campanha"
    }

    fun steps(game:String):List<JourneyStep> = when(game){
        "Scarlet / Violet" -> scarletViolet
        "Pokémon Legends: Z-A" -> legendsZa
        else -> emptyList()
    }


    private val legendsZa = listOf(
        JourneyStep("za-01",1,"Get Your Travel Bag Back","Escolha seu inicial e recupere a bolsa","Nv. 5–6",JourneyChallengeKind.STORY,"Tutorial","Vert District","Escolha entre Chikorita, Tepig e Totodile."),
        JourneyStep("za-02",2,"Escape from the Battle Zone","Primeira fuga da Battle Zone","Nv. 6–8",JourneyChallengeKind.STORY,"Tutorial","Lumiose City"),
        JourneyStep("za-03",3,"A New Life in Lumiose City","Hotel Z e Team MZ","Nv. 7–9",JourneyChallengeKind.STORY,"História","Hotel Z"),
        JourneyStep("za-04",4,"Battling in the Z-A Royale","Entrada oficial na Z-A Royale","Nv. 9+",JourneyChallengeKind.STORY,"Z-A Royale","Battle Zone"),
        JourneyStep("za-05",5,"The City in the Shadow of Prism Tower","Incidentes em Lumiose","Nv. 12–15",JourneyChallengeKind.STORY,"História","Prism Tower"),
        JourneyStep("za-06",6,"Reaching Rank X","Promotion Match · Yvon","Nv. 15–16",JourneyChallengeKind.STORY,"Rank Y → X","Bleu Plaza"),
        JourneyStep("za-07",7,"Reaching Rank W","Promotion Match · Xavi","Nv. 20–21",JourneyChallengeKind.STORY,"Rank X → W","Rouge District"),
        JourneyStep("za-08",8,"Reaching Rank V","Promotion Match · Rintaro","Nv. 24",JourneyChallengeKind.STORY,"Rank W → V","Restaurant Le Nah"),
        JourneyStep("za-09",9,"Chase That Mysterious Pokémon","Absol e o mistério de Lumiose","Nv. 24–25",JourneyChallengeKind.STORY,"História","Lumiose City"),
        JourneyStep("za-10",10,"The Secrets of Mega Evolution","Mega Evolução + Promotion Match Vinnie","Nv. 25–32",JourneyChallengeKind.STORY,"Rank V → F","Quasartico Inc.","Libera Mega Evolução e a Mega Stone do seu inicial."),
        JourneyStep("za-11",11,"A Rogue Mega Slowbro","Rogue Mega","Nv. 33–35",JourneyChallengeKind.STORY,"Mega","Lumiose City"),
        JourneyStep("za-12",12,"A Rogue Mega Camerupt","Rogue Mega","Nv. 33–35",JourneyChallengeKind.STORY,"Mega","Lumiose City"),
        JourneyStep("za-13",13,"A Rogue Mega Victreebel","Rogue Mega","Nv. 33–35",JourneyChallengeKind.STORY,"Mega","Lumiose City"),
        JourneyStep("za-14",14,"Reaching Rank E","Promotion Match · Canari","Nv. 37–39",JourneyChallengeKind.STORY,"Rank F → E","DYN4MO"),
        JourneyStep("za-15",15,"A Job for Team MZ","Novo incidente de Rogue Megas","Nv. 39–44",JourneyChallengeKind.STORY,"História","Hotel Z"),
        JourneyStep("za-16",16,"A Rogue Mega Beedrill","Rogue Mega","Nv. 40–44",JourneyChallengeKind.STORY,"Mega","Lumiose City"),
        JourneyStep("za-17",17,"A Rogue Mega Hawlucha","Rogue Mega","Nv. 40–44",JourneyChallengeKind.STORY,"Mega","Lumiose City"),
        JourneyStep("za-18",18,"A Rogue Mega Banette","Rogue Mega","Nv. 40–44",JourneyChallengeKind.STORY,"Mega","Lumiose City"),
        JourneyStep("za-19",19,"Reaching Rank D","Promotion Match · Ivor","Nv. 45–47",JourneyChallengeKind.STORY,"Rank E → D","Justice Dojo"),
        JourneyStep("za-20",20,"A Request from the Rust Syndicate","Corbeau e novos Rogue Megas","Nv. 47–50",JourneyChallengeKind.STORY,"História","Bleu District"),
        JourneyStep("za-21",21,"A Rogue Mega Mawile","Rogue Mega","Nv. 48–50",JourneyChallengeKind.STORY,"Mega","Lumiose City"),
        JourneyStep("za-22",22,"A Rogue Mega Barbaracle","Rogue Mega","Nv. 48–50",JourneyChallengeKind.STORY,"Mega","Bleu Sector 5"),
        JourneyStep("za-23",23,"A Rogue Mega Ampharos","Rogue Mega","Nv. 48–50",JourneyChallengeKind.STORY,"Mega","Lumiose City"),
        JourneyStep("za-24",24,"Reaching Rank C","Promotion Match · Corbeau","Nv. 50–52",JourneyChallengeKind.STORY,"Rank D → C","Rust Syndicate"),
        JourneyStep("za-25",25,"A Showdown on the Battle Court","Lida + Naveen","Nv. 46–50",JourneyChallengeKind.STORY,"Team MZ","Vert Sector 4"),
        JourneyStep("za-26",26,"An Invitation from the SBC","Society of Battle Connoisseurs","Nv. 52–54",JourneyChallengeKind.STORY,"História","Hotel Richissime"),
        JourneyStep("za-27",27,"A Rogue Mega Froslass","Rogue Mega","Nv. 52–53",JourneyChallengeKind.STORY,"Mega","Aymlis Park"),
        JourneyStep("za-28",28,"A Rogue Mega Altaria","Rogue Mega","Nv. 52–53",JourneyChallengeKind.STORY,"Mega","Magenta Sector 4"),
        JourneyStep("za-29",29,"A Rogue Mega Venusaur","Rogue Mega","Nv. 53",JourneyChallengeKind.STORY,"Mega","Jaune Sector 8"),
        JourneyStep("za-30",30,"Reaching Rank B","Promotion Match · Jacinthe","Nv. 57–59",JourneyChallengeKind.STORY,"Rank C → B","Hotel Richissime"),
        JourneyStep("za-31",31,"A Summons from Vinnie","Última série de Rogue Megas","Nv. 59–61",JourneyChallengeKind.STORY,"História","Quasartico Inc."),
        JourneyStep("za-32",32,"A Rogue Mega Dragonite","Rogue Mega","Nv. 60+",JourneyChallengeKind.STORY,"Mega","Lumiose City"),
        JourneyStep("za-33",33,"A Rogue Mega Tyranitar","Rogue Mega","Nv. 60+",JourneyChallengeKind.STORY,"Mega","Lumiose City"),
        JourneyStep("za-34",34,"A Rogue Mega Starmie","Rogue Mega","Nv. 60+",JourneyChallengeKind.STORY,"Mega","Lumiose City"),
        JourneyStep("za-35",35,"Reaching Rank A","Promotion Match · Grisham","Nv. 61–63",JourneyChallengeKind.STORY,"Rank B → A","Nouveau Cafe"),
        JourneyStep("za-36",36,"Prism Tower's Dark Turn","Crise final em Lumiose","Nv. 63–65",JourneyChallengeKind.STORY,"Final","Prism Tower"),
        JourneyStep("za-37",37,"Operation Protect Lumiose","Final da campanha principal","Nv. 64–66",JourneyChallengeKind.STORY,"Final","Lumiose City"),
        JourneyStep("za-38",38,"The Future of Lumiose City","Pós-créditos imediato","Nv. 70+",JourneyChallengeKind.POSTGAME,"Pós-jogo","Lumiose City","Necessário para iniciar Mega Dimension."),
        JourneyStep("za-39",39,"The Infinite Z-A Royale","Royale sem fim","Nv. 70+",JourneyChallengeKind.POSTGAME,"Z-A Royale","Battle Zones"),
        JourneyStep("za-40",40,"The One That Gives","Pós-game lendário","Nv. 70+",JourneyChallengeKind.POSTGAME,"Lendário","Lumiose City"),
        JourneyStep("za-41",41,"The One That Takes","Pós-game lendário","Nv. 70+",JourneyChallengeKind.POSTGAME,"Lendário","Lumiose City"),
        JourneyStep("za-42",42,"To Keep the World in Balance","Fechamento de Zygarde","Nv. 70+",JourneyChallengeKind.POSTGAME,"Lendário","Lumiose City"),
        JourneyStep("za-dlc-00",43,"Donuts of Unworldly Deliciousness!","Conheça Ansha e Hoopa","Nv. 70+",JourneyChallengeKind.DLC,"Mega Dimension","Hotel Z","Prólogo necessário para acessar Hyperspace Lumiose."),
        JourneyStep("za-dlc-01",44,"Hyperspace Lumiose Survey No. 1","7.000 pontos + Mega Absol Z","Nv. 100+",JourneyChallengeKind.DLC,"Hyperspace","Hyperspace Lumiose"),
        JourneyStep("za-dlc-02",45,"Lebanne Arrives with a Bang!","Lebanne + Gwynn","Nv. 73–74",JourneyChallengeKind.DLC,"Batalha","Hotel Z"),
        JourneyStep("za-dlc-03",46,"Hyperspace Lumiose Survey No. 2","10.000 pontos + Mega Staraptor","Nv. 120",JourneyChallengeKind.DLC,"Hyperspace","Hyperspace Lumiose"),
        JourneyStep("za-dlc-04",47,"Le Musee et Le Cafe","Ansha e Korrina exploram Lumiose","Livre",JourneyChallengeKind.DLC,"História","Lumiose City"),
        JourneyStep("za-dlc-05",48,"A Boom from the Strategy Room","Novo incidente no Hotel Z","Nv. 120+",JourneyChallengeKind.DLC,"História","Hotel Z"),
        JourneyStep("za-dlc-06",49,"Hyperspace Lumiose Survey No. 3","15.000 pontos + Mega Tatsugiri","Nv. 140",JourneyChallengeKind.DLC,"Hyperspace","Hyperspace Lumiose"),
        JourneyStep("za-dlc-07",50,"Naveen's Not OK","Incidente com Naveen","Nv. 140+",JourneyChallengeKind.DLC,"História","Hotel Z"),
        JourneyStep("za-dlc-08",51,"Hyperspace Lumiose Survey No. 4","Survey de 4 estrelas","Nv. 170",JourneyChallengeKind.DLC,"Hyperspace","Hyperspace Lumiose"),
        JourneyStep("za-dlc-09",52,"Hyperspace Lumiose Survey No. 5","28.000 pontos","Nv. 200",JourneyChallengeKind.DLC,"Hyperspace","Hyperspace Lumiose"),
        JourneyStep("za-dlc-10",53,"Mayhem at Midnight","Crise no Hotel Z","Nv. 170–200",JourneyChallengeKind.DLC,"História","Hotel Z"),
        JourneyStep("za-dlc-11",54,"Hyperspace Lumiose Survey No. 6","50.000 pontos + clímax do Hyperspace","Nv. 200",JourneyChallengeKind.DLC,"Hyperspace","Hyperspace Lumiose"),
        JourneyStep("za-dlc-12",55,"The Greatest Gift","Groudon, Kyogre e caminho para Rayquaza","Nv. 200",JourneyChallengeKind.DLC,"Lendários","Hyperspace Lumiose"),
        JourneyStep("za-dlc-13",56,"A Ruby-Red Legend","Primal Groudon","Nv. 200",JourneyChallengeKind.DLC,"Lendário","Hyperspace Lumiose"),
        JourneyStep("za-dlc-14",57,"A Sapphire-Blue Legend","Primal Kyogre","Nv. 200",JourneyChallengeKind.DLC,"Lendário","Hyperspace Lumiose")
    )

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
