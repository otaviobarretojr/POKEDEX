package com.otaviobarreto.pokedex.data

data class JourneyBossMember(
    val name:String,
    val level:String,
    val detail:String = ""
)

data class JourneyObjectiveDetail(
    val summary:String,
    val opponents:List<JourneyBossMember>,
    val weakTo:List<String>,
    val recommended:String,
    val reward:String
)

object JourneyObjectiveDetailsCatalog {
    fun detail(stepId:String):JourneyObjectiveDetail? = details[stepId]

    private fun d(
        summary:String,
        opponents:List<JourneyBossMember>,
        weakTo:List<String>,
        recommended:String,
        reward:String
    )=JourneyObjectiveDetail(summary,opponents,weakTo,recommended,reward)

    private fun p(name:String,level:String,detail:String="")=JourneyBossMember(name,level,detail)

    private val details=mapOf(
        "sv-01" to d(
            "Katy é a líder de Cortondo e usa Pokémon do tipo Inseto. O último Pokémon Terastaliza para Inseto.",
            listOf(p("Nymble","Nv. 14"),p("Tarountula","Nv. 14"),p("Teddiursa","Nv. 15","Tera Inseto")),
            listOf("Fogo","Voador","Pedra"),
            "Chegue por volta do nível 15. Um atacante de Fogo ou Voador resolve a luta rapidamente.",
            "Insígnia de Ginásio e avanço da Victory Road."
        ),
        "sv-02" to d(
            "Klawf é o primeiro Titã recomendado pela curva de nível da campanha.",
            listOf(p("Klawf","Nv. 16","Pedra")),
            listOf("Água","Planta","Lutador","Terra","Aço"),
            "Água ou Planta são as respostas mais simples. Evite depender de golpes Normais ou de Fogo.",
            "Herba Mystica e melhoria de mobilidade de Koraidon/Miraidon."
        ),
        "sv-03" to d(
            "Brassius lidera o ginásio de Artazon. Seu Sudowoodo Terastaliza para Planta.",
            listOf(p("Petilil","Nv. 16"),p("Smoliv","Nv. 16"),p("Sudowoodo","Nv. 17","Tera Planta")),
            listOf("Fogo","Gelo","Veneno","Voador","Inseto"),
            "Fogo e Voador funcionam muito bem. Leve o time para aproximadamente nível 17–18.",
            "Insígnia de Ginásio e avanço da Victory Road."
        ),
        "sv-04" to d(
            "Bombirdier é o Open Sky Titan e combina Voador com Sombrio.",
            listOf(p("Bombirdier","Nv. 19","Voador / Sombrio")),
            listOf("Elétrico","Gelo","Pedra","Fada"),
            "Elétrico é uma resposta excelente. Pedra também causa dano forte, mas cuidado com cobertura do Titã.",
            "Herba Mystica e nova melhoria de mobilidade."
        ),
        "sv-05" to d(
            "Giacomo comanda a Segin Squad, equipe Sombria da Team Star.",
            listOf(p("Pawniard","Nv. 21"),p("Segin Starmobile","Nv. 20","Revavroom da base")),
            listOf("Lutador","Inseto","Fada"),
            "Lutador é o melhor atalho para esta base. Entre com o time próximo do nível 21.",
            "Conclusão da base Segin e avanço da Starfall Street."
        ),
        "sv-06" to d(
            "Iono é a líder elétrica de Levincia. Mismagius Terastaliza para Elétrico e possui Levitate, anulando a resposta óbvia de Terra.",
            listOf(p("Wattrel","Nv. 23"),p("Bellibolt","Nv. 23"),p("Luxio","Nv. 23"),p("Mismagius","Nv. 24","Tera Elétrico · Levitate")),
            listOf("Terra","Cobertura neutra forte"),
            "Use Terra contra os três primeiros, mas guarde um atacante neutro forte para Mismagius por causa de Levitate.",
            "Insígnia de Ginásio e avanço da Victory Road."
        ),
        "sv-07" to d(
            "Mela lidera a Schedar Squad, especializada em Fogo.",
            listOf(p("Torkoal","Nv. 27"),p("Schedar Starmobile","Nv. 26","Revavroom da base")),
            listOf("Água","Terra","Pedra"),
            "Água é a opção mais segura. Entre com a equipe na faixa de nível 27.",
            "Conclusão da base Schedar e avanço da Starfall Street."
        ),
        "sv-08" to d(
            "Orthworm é o Lurking Steel Titan.",
            listOf(p("Orthworm","Nv. 28","Aço")),
            listOf("Fogo","Lutador","Terra"),
            "Fogo e Lutador são escolhas consistentes. Não dependa só de golpes de Terra se a habilidade do alvo estiver ativa.",
            "Herba Mystica e nova melhoria de mobilidade."
        ),
        "sv-09" to d(
            "Kofu comanda o ginásio de Cascarrafa e usa Água. Crabominable Terastaliza para Água.",
            listOf(p("Veluza","Nv. 29"),p("Wugtrio","Nv. 29"),p("Crabominable","Nv. 30","Tera Água")),
            listOf("Elétrico","Planta"),
            "Planta e Elétrico resolvem praticamente toda a luta. Mire em nível 30–31.",
            "Insígnia de Ginásio e avanço da Victory Road."
        ),
        "sv-10" to d(
            "Atticus lidera a Navi Squad, especializada em Veneno.",
            listOf(p("Skuntank","Nv. 32"),p("Muk","Nv. 32"),p("Revavroom","Nv. 33"),p("Navi Starmobile","Nv. 32")),
            listOf("Terra","Psíquico"),
            "Terra é a resposta geral mais segura; Psíquico também funciona bem quando não há combinação defensiva desfavorável.",
            "Conclusão da base Navi e avanço da Starfall Street."
        ),
        "sv-11" to d(
            "Larry é o líder de Medali e usa Normal. Staraptor Terastaliza para Normal.",
            listOf(p("Komala","Nv. 35"),p("Dudunsparce","Nv. 35"),p("Staraptor","Nv. 36","Tera Normal")),
            listOf("Lutador"),
            "Lutador é a fraqueza direta do tipo Normal. Chegue aproximadamente no nível 36–37.",
            "Insígnia de Ginásio e avanço da Victory Road."
        ),
        "sv-12" to d(
            "Ryme lidera Montenevera em uma batalha dupla de tipo Fantasma.",
            listOf(p("Banette","Nv. 41"),p("Mimikyu","Nv. 41"),p("Houndstone","Nv. 41"),p("Toxtricity","Nv. 42","Tera Fantasma")),
            listOf("Fantasma","Sombrio"),
            "Leve dois atacantes confiáveis porque a luta é em dupla. Sombrio costuma ser a abordagem mais confortável.",
            "Insígnia de Ginásio e avanço da Victory Road."
        ),
        "sv-13" to d(
            "O Quaking Earth Titan muda conforme a versão: Great Tusk em Scarlet ou Iron Treads em Violet.",
            listOf(p("Great Tusk","Nv. 44","Scarlet · Terra / Lutador"),p("Iron Treads","Nv. 44","Violet · Terra / Aço")),
            listOf("Água","Planta","Gelo"),
            "Água funciona bem nas duas versões. Ajuste cobertura adicional conforme Great Tusk ou Iron Treads.",
            "Herba Mystica e nova melhoria de mobilidade."
        ),
        "sv-14" to d(
            "Tulip lidera o ginásio de Alfornada com foco em Psíquico. Florges Terastaliza para Psíquico.",
            listOf(p("Farigiraf","Nv. 44"),p("Gardevoir","Nv. 44"),p("Espathra","Nv. 44"),p("Florges","Nv. 45","Tera Psíquico")),
            listOf("Inseto","Fantasma","Sombrio"),
            "Sombrio e Fantasma oferecem ótima pressão. Mire em nível 45–46.",
            "Insígnia de Ginásio e avanço da Victory Road."
        ),
        "sv-15" to d(
            "Grusha é o líder de Glaseado e usa Gelo. Altaria Terastaliza para Gelo.",
            listOf(p("Frosmoth","Nv. 47"),p("Beartic","Nv. 47"),p("Cetitan","Nv. 47"),p("Altaria","Nv. 48","Tera Gelo")),
            listOf("Fogo","Lutador","Pedra","Aço"),
            "Fogo ou Lutador oferecem uma rota direta. Entre perto do nível 48–49.",
            "Insígnia de Ginásio e fechamento dos ginásios principais."
        ),
        "sv-16" to d(
            "Ortega lidera a Ruchbah Squad, especializada em Fada.",
            listOf(p("Azumarill","Nv. 50"),p("Wigglytuff","Nv. 50"),p("Dachsbun","Nv. 51"),p("Ruchbah Starmobile","Nv. 50")),
            listOf("Veneno","Aço"),
            "Aço é a cobertura mais segura para a maior parte da equipe. Entre na faixa de nível 51.",
            "Conclusão da base Ruchbah e avanço da Starfall Street."
        ),
        "sv-17" to d(
            "O False Dragon Titan é uma sequência envolvendo Dondozo e Tatsugiri em Casseroya Lake.",
            listOf(p("Dondozo","Nv. 55","Água"),p("Tatsugiri","Nv. 55","Dragão / Água")),
            listOf("Elétrico","Planta","Fada","Dragão"),
            "Elétrico ou Planta ajudam contra Dondozo; tenha cobertura de Fada ou Dragão para Tatsugiri.",
            "Última Herba Mystica da rota dos Titãs e progressão final de Path of Legends."
        ),
        "sv-18" to d(
            "Eri lidera a Caph Squad e é o desafio mais forte entre as bases principais da Team Star.",
            listOf(p("Toxicroak","Nv. 55"),p("Passimian","Nv. 55"),p("Lucario","Nv. 55"),p("Annihilape","Nv. 56"),p("Caph Starmobile","Nv. 56")),
            listOf("Psíquico","Voador","Fada"),
            "Psíquico e Voador têm ótimo valor, mas leve cobertura para Lucario e Annihilape. Recomenda-se nível 56 ou mais.",
            "Conclusão da última base principal e fechamento da rota Starfall Street."
        ,
        "sv-pg-01" to d("Fechamento da Victory Road: avaliação da Elite Four e batalha de Campeã.",listOf(p("Elite Four","Nv. 57–61"),p("Geeta","Nv. 61–62","Top Champion")),listOf("Cobertura variada"),"Leve seis Pokémon próximos do nível 62 com cobertura ampla e itens de cura.","Conclusão da Victory Road e avanço para o final das três rotas."),
        "sv-pg-02" to d("Arven encerra Path of Legends em uma batalha completa.",listOf(p("Equipe de Arven","Nv. 58–63","6 Pokémon")),listOf("Cobertura variada"),"Prepare respostas para uma equipe diversificada e preserve seu núcleo mais forte.","Conclusão de Path of Legends."),
        "sv-pg-03" to d("Fechamento de Starfall Street envolvendo Clavell e Cassiopeia.",listOf(p("Director Clavell","Nv. 60–61"),p("Cassiopeia","Nv. 62–63")),listOf("Cobertura variada"),"Equipe equilibrada na faixa de 63 facilita as duas batalhas.","Conclusão de Starfall Street."),
        "sv-pg-04" to d("The Way Home leva o grupo à Area Zero e encerra a história principal.",listOf(p("Area Zero","Nv. 55–65"),p("Batalha final","Nv. 66–67")),listOf("Cobertura variada"),"Leve o time principal curado, com respostas para Paradox Pokémon e uma vaga flexível.","Créditos, acesso completo ao pós-jogo e exploração ampliada de Area Zero."),
        "sv-pg-05" to d("Após os créditos, revisite os oito ginásios para as revanche de avaliação.",listOf(p("8 Gym Leaders","Nv. 65–66","Equipes reforçadas")),listOf("Cobertura dos 8 tipos"),"Use um time de nível 66+ e troque cobertura conforme cada ginásio.","Libera a etapa necessária para o Academy Ace Tournament."),
        "sv-pg-06" to d("O Academy Ace Tournament reúne treinadores fortes da Academy em sequência.",listOf(p("4 treinadores","Nv. 65–70","Chave aleatória/variável")),listOf("Cobertura geral"),"Equipe 68–70, itens de cura e golpes consistentes reduzem o risco entre rodadas.","Conclusão do primeiro torneio e avanço para raids de alto nível."),
        "sv-pg-07" to d("Black Crystal Tera Raids são o conteúdo de raid 6★ liberado no pós-jogo.",listOf(p("Tera Raid 6★","Nv. 75","Build específica por raid")),listOf("Depende do Tera Type"),"Monte Pokémon dedicados para raid; sobrevivência, STAB e sinergia importam mais que cobertura genérica.","Acesso regular às raids 6★ e preparação para eventos 7★."),
        "sv-pg-08" to d("Checklist aberto do endgame de Paldea: Pokédex, Treasures of Ruin e exploração.",listOf(p("Wo-Chien / Chien-Pao / Ting-Lu / Chi-Yu","Nv. 60","Lendários opcionais")),listOf("Captura e exploração"),"Leve False Swipe, status e muitas Poké Balls para a etapa de coleção.","Fechamento do conteúdo base antes ou em paralelo aos DLCs.")
    )
}
