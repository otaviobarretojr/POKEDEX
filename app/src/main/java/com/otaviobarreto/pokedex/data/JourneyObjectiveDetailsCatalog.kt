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
        ),
        "sv-pg-01" to d("Fechamento da Victory Road: avaliação da Elite Four e batalha de Campeã.",listOf(p("Elite Four","Nv. 57–61"),p("Geeta","Nv. 61–62","Top Champion")),listOf("Cobertura variada"),"Leve seis Pokémon próximos do nível 62 com cobertura ampla e itens de cura.","Conclusão da Victory Road e avanço para o final das três rotas."),
        "sv-pg-02" to d("Arven encerra Path of Legends em uma batalha completa.",listOf(p("Equipe de Arven","Nv. 58–63","6 Pokémon")),listOf("Cobertura variada"),"Prepare respostas para uma equipe diversificada e preserve seu núcleo mais forte.","Conclusão de Path of Legends."),
        "sv-pg-03" to d("Fechamento de Starfall Street envolvendo Clavell e Cassiopeia.",listOf(p("Director Clavell","Nv. 60–61"),p("Cassiopeia","Nv. 62–63")),listOf("Cobertura variada"),"Equipe equilibrada na faixa de 63 facilita as duas batalhas.","Conclusão de Starfall Street."),
        "sv-pg-04" to d("The Way Home leva o grupo à Area Zero e encerra a história principal.",listOf(p("Area Zero","Nv. 55–65"),p("Batalha final","Nv. 66–67")),listOf("Cobertura variada"),"Leve o time principal curado, com respostas para Paradox Pokémon e uma vaga flexível.","Créditos, acesso completo ao pós-jogo e exploração ampliada de Area Zero."),
        "sv-pg-05" to d("Após os créditos, revisite os oito ginásios para as revanche de avaliação.",listOf(p("8 Gym Leaders","Nv. 65–66","Equipes reforçadas")),listOf("Cobertura dos 8 tipos"),"Use um time de nível 66+ e troque cobertura conforme cada ginásio.","Libera a etapa necessária para o Academy Ace Tournament."),
        "sv-pg-06" to d("O Academy Ace Tournament reúne treinadores fortes da Academy em sequência.",listOf(p("4 treinadores","Nv. 65–70","Chave aleatória/variável")),listOf("Cobertura geral"),"Equipe 68–70, itens de cura e golpes consistentes reduzem o risco entre rodadas.","Conclusão do primeiro torneio e avanço para raids de alto nível."),
        "sv-pg-07" to d("Black Crystal Tera Raids são o conteúdo de raid 6★ liberado no pós-jogo.",listOf(p("Tera Raid 6★","Nv. 75","Build específica por raid")),listOf("Depende do Tera Type"),"Monte Pokémon dedicados para raid; sobrevivência, STAB e sinergia importam mais que cobertura genérica.","Acesso regular às raids 6★ e preparação para eventos 7★."),
        "sv-pg-08" to d("Checklist aberto do endgame de Paldea: Pokédex, Treasures of Ruin e exploração.",listOf(p("Wo-Chien / Chien-Pao / Ting-Lu / Chi-Yu","Nv. 60","Lendários opcionais")),listOf("Captura e exploração"),"Leve False Swipe, status e muitas Poké Balls para a etapa de coleção.","Fechamento do conteúdo base antes ou em paralelo aos DLCs."),
        "sv-dlc-01" to d("A excursão escolar leva você a Kitakami e apresenta Carmine e Kieran.",listOf(p("Carmine","≈ Nv. 60+","Batalhas de história")),listOf("Cobertura variada"),"Use seu time pós-game; os níveis escalam conforme o progresso da campanha base.","Acesso à história de The Teal Mask."),
        "sv-dlc-02" to d("O festival revela a conexão de Ogerpon com a Teal Mask.",listOf(p("Ogerpon","Encontro de história","Ainda não é a captura final")),listOf("História / exploração"),"Avance pelo festival e mantenha o time pronto para batalhas curtas.","Descoberta central da lenda de Kitakami."),
        "sv-dlc-03" to d("A ida ao Crystal Pool aprofunda o mistério da máscara e a tensão com Kieran.",listOf(p("Encontros de Oni Mountain","≈ Nv. 62+")),listOf("Cobertura variada"),"Leve mobilidade e cura para explorar Oni Mountain.","Cristal necessário para reparar a Teal Mask."),
        "sv-dlc-04" to d("Kieran exige a Teal Mask em Loyalty Plaza e o confronto desperta os Loyal Three.",listOf(p("Kieran","≈ Nv. 62+","Batalha de história")),listOf("Cobertura variada"),"Prepare-se para uma equipe balanceada; preserve recursos para a sequência.","Desbloqueia a caçada aos Loyal Three."),
        "sv-dlc-05" to d("Okidogi, Munkidori e Fezandipiti precisam ser derrotados para recuperar as máscaras.",listOf(p("Okidogi","≈ Nv. 70","Veneno / Lutador"),p("Munkidori","≈ Nv. 70","Veneno / Psíquico"),p("Fezandipiti","≈ Nv. 70","Veneno / Fada")),listOf("Terra","Psíquico","Aço"),"Monte respostas diferentes para cada membro; não trate o trio como um único matchup.","Recupera as máscaras de Ogerpon."),
        "sv-dlc-06" to d("Kieran desafia você novamente antes da batalha decisiva com Ogerpon.",listOf(p("Kieran","≈ Nv. 69–70"),p("Ogerpon","≈ Nv. 70","Quatro fases / máscaras")),listOf("Cobertura adaptável"),"Entre com seis membros saudáveis e golpes variados para acompanhar as mudanças de Ogerpon.","Captura de Ogerpon e conclusão de The Teal Mask."),
        "sv-dlc-07" to d("Perrin conduz uma missão fotográfica que culmina no encontro com Bloodmoon Ursaluna.",listOf(p("Bloodmoon Ursaluna","≈ Nv. 70","Terra / Normal")),listOf("Água","Planta","Lutador"),"Complete o requisito da Pokédex de Kitakami antes de iniciar a etapa final da missão.","Captura única de Bloodmoon Ursaluna."),
        "sv-dlc-08" to d("Como aluno de intercâmbio, você chega à Blueberry Academy e entra na BB League.",listOf(p("Treinadores do Terarium","Nv. 70+","Foco em batalhas duplas")),listOf("Sinergia de dupla"),"A partir daqui, pense em dupla: Protect, speed control e sinergia importam muito.","Acesso à BB League e ao Terarium."),
        "sv-dlc-09" to d("Crispin é o membro de Fogo da BB Elite Four.",listOf(p("Crispin","Nv. 77–78","Batalha dupla")),listOf("Água","Terra","Pedra"),"Controle o clima e evite deixar dois atacantes de Fogo livres ao mesmo tempo.","Avanço na BB League."),
        "sv-dlc-10" to d("Amarys usa equipes de Aço e exige um Flying Time Trial antes da luta.",listOf(p("Amarys","Nv. 78–79","Batalha dupla")),listOf("Fogo","Lutador","Terra"),"Tenha respostas para Steel defensivo e preserve dano especial ou golpes super efetivos.","Avanço na BB League e progressão ligada ao voo."),
        "sv-dlc-11" to d("Lacey representa o tipo Fada entre a BB Elite Four.",listOf(p("Lacey","Nv. 78–79","Batalha dupla")),listOf("Aço","Veneno"),"Aço é a resposta mais segura; cuidado com cobertura contra Steel.","Avanço na BB League."),
        "sv-dlc-12" to d("Drayton usa Dragão e exige um Elite Trial com Pokémon capturados no Terarium.",listOf(p("Drayton","Nv. 79–80","Batalha dupla")),listOf("Fada","Gelo","Dragão"),"Prepare o time do trial separadamente e depois volte ao seu núcleo principal para a luta.","Conclusão dos quatro membros da BB Elite Four."),
        "sv-dlc-13" to d("Kieran é o Champion da BB League e usa uma equipe ofensiva de alto nível.",listOf(p("Kieran","Nv. 80–82","Campeão da BB League")),listOf("Cobertura ampla","Controle de velocidade"),"Entre com o time mais forte do save e planejamento de dupla.","Título da BB League e avanço para Area Zero."),
        "sv-dlc-14" to d("Briar conduz o grupo às Underdepths de Area Zero em busca da origem do fenômeno Terastal.",listOf(p("Pokémon selvagens / treinadores","Nv. 80+")),listOf("Cobertura ampla"),"Leve recursos de cura e espaço para capturas; a sequência é longa.","Acesso ao confronto final de The Indigo Disk."),
        "sv-dlc-15" to d("Terapagos é o clímax do Hidden Treasure of Area Zero.",listOf(p("Terapagos","Nv. 85","Stellar / múltiplas fases")),listOf("Dano consistente","Sobrevivência"),"Priorize estabilidade e preserve seus melhores recursos para a fase final.","Conclusão de The Indigo Disk e captura de Terapagos."),
        "sv-dlc-16" to d("O endgame de Blueberry abre atividades do League Club e encontros lendários via Snacksworth.",listOf(p("Lendários retornantes","Variável")),listOf("Captura","Exploração"),"Use builds de captura e complete BBQs para liberar mais encontros.","Conteúdo opcional pós-DLC e caça a lendários."),
        "sv-epi-01" to d("Mochi Mayhem começa em Peachy's após usar a Mythical Pecha Berry.",listOf(p("Pecharunt","Presença de história")),listOf("Exploração"),"Leve seu time de endgame e siga os eventos em Mossui Town.","Início do epílogo."),
        "sv-epi-02" to d("O mochi misterioso coloca vários amigos sob controle.",listOf(p("Treinadores possuídos","Nv. 80+")),listOf("Cobertura ampla"),"Espere batalhas consecutivas; mantenha cura suficiente.","Avanço até Loyalty Plaza."),
        "sv-epi-03" to d("Nemona enfrenta você sob a influência do mochi.",listOf(p("Nemona","Nv. 80+","Equipe completa")),listOf("Cobertura ampla"),"É um duelo de endgame: use o núcleo mais forte e não economize recursos.","Acesso ao confronto com Pecharunt."),
        "sv-epi-04" to d("Pecharunt encerra Mochi Mayhem e deve ser capturado após a batalha.",listOf(p("Pecharunt","Nv. 88","Veneno / Fantasma")),listOf("Terra","Psíquico","Fantasma","Sombrio"),"Cuidado com Poison Puppeteer; status e confusão podem quebrar seu ritmo.","Captura garantida de Pecharunt e encerramento do epílogo."),
        "za-06" to d("Yvon é o primeiro grande Promotion Match da subida de ranks.",listOf(p("Spritzee","Nv. 15"),p("Swirlix","Nv. 15"),p("Vivillon","Nv. 16")),listOf("Aço","Veneno","Fogo"),"Aço/Veneno resolvem as Fadas; use Fogo ou Pedra contra Vivillon.","Promoção para Rank X."),
        "za-07" to d("Xavi usa quatro Pokémon variados no Promotion Match seguinte.",listOf(p("Venipede","Nv. 20"),p("Kadabra","Nv. 21"),p("Roselia","Nv. 20"),p("Furfrou","Nv. 21")),listOf("Fogo","Sombrio","Psíquico","Lutador"),"Leve cobertura variada; não tente resolver tudo com um único tipo.","Promoção para Rank W."),
        "za-08" to d("Rintaro usa o trio de macacos elementais.",listOf(p("Simisage","Nv. 24"),p("Simipour","Nv. 24"),p("Simisear","Nv. 24")),listOf("Fogo","Elétrico","Planta","Água"),"Alterne a cobertura conforme cada membro; o trio pune times mono-tipo.","Promoção para Rank V."),
        "za-10" to d("Vinnie é o primeiro oponente de promoção que usa Mega Evolução.",listOf(p("Houndoom","Nv. 30"),p("Sharpedo","Nv. 30"),p("Buneary","Nv. 30"),p("Drampa","Nv. 32","Mega")),listOf("Lutador"),"Todos os quatro são vulneráveis a Lutador; dois atacantes confiáveis simplificam muito a luta.","Promoção para Rank F, TM Safeguard e acesso pleno à Mega Evolução."),
        "za-11" to d("Primeiro grupo de Rogue Megas.",listOf(p("Slowbro","≈ Nv. 33–35","Mega")),listOf("Elétrico","Planta","Fantasma","Sombrio"),"Mantenha distância dos golpes de área e ataque nas janelas de cooldown.","Slowbronite."),
        "za-12" to d("Rogue Mega Camerupt.",listOf(p("Camerupt","≈ Nv. 33–35","Mega")),listOf("Água","Terra"),"Água é a resposta mais forte; evite permanecer perto durante ataques de área.","Cameruptite."),
        "za-13" to d("Rogue Mega Victreebel.",listOf(p("Victreebel","≈ Nv. 33–35","Mega")),listOf("Fogo","Gelo","Voador","Psíquico"),"Use ataques à distância e saia da área dos golpes de Planta/Veneno.","Victreebelite."),
        "za-14" to d("Canari fecha o arco do Rank F com uma equipe Elétrica.",listOf(p("Heliolisk","Nv. 37"),p("Ampharos","Nv. 38"),p("Stunfisk","Nv. 38"),p("Eelektross","Nv. 39","Mega")),listOf("Terra"),"Terra é o eixo principal; leve cura para os golpes de cobertura.","Promoção para Rank E."),
        "za-16" to d("Rogue Mega Beedrill.",listOf(p("Beedrill","≈ Nv. 40–44","Mega")),listOf("Fogo","Psíquico","Voador","Pedra"),"Priorize mobilidade e ataques rápidos; Beedrill pune posicionamento ruim.","Beedrillite."),
        "za-17" to d("Rogue Mega Hawlucha.",listOf(p("Hawlucha","≈ Nv. 40–44","Mega")),listOf("Elétrico","Gelo","Psíquico","Fada"),"Golpes à distância ajudam bastante contra sua mobilidade.","Hawluchanite."),
        "za-18" to d("Rogue Mega Banette.",listOf(p("Banette","≈ Nv. 40–44","Mega")),listOf("Fantasma","Sombrio"),"Use Sombrio/Fantasma e evite ficar exposto durante ataques de área.","Banettite."),
        "za-19" to d("Ivor usa uma equipe inteira focada em Lutador.",listOf(p("Heracross","Nv. 45"),p("Machamp","Nv. 46"),p("Medicham","Nv. 46"),p("Falinks","Nv. 47","Mega")),listOf("Voador","Psíquico","Fada"),"Fada é muito segura; Fantasma também oferece imunidade parcial, mas cuidado com cobertura.","Promoção para Rank D e TM Bulk Up."),
        "za-21" to d("Rogue Mega Mawile.",listOf(p("Mawile","≈ Nv. 48–50","Mega")),listOf("Fogo","Terra"),"Fogo/Terra são as melhores respostas; não subestime seu dano físico.","Mawilite."),
        "za-22" to d("Rogue Mega Barbaracle.",listOf(p("Barbaracle","≈ Nv. 48–50","Mega")),listOf("Planta","Água","Lutador","Terra","Psíquico","Aço","Fada"),"Planta oferece uma resposta especialmente forte.","Barbaraclenite."),
        "za-23" to d("Rogue Mega Ampharos.",listOf(p("Ampharos","≈ Nv. 48–50","Mega")),listOf("Terra","Gelo","Dragão","Fada"),"Terra é simples antes da Mega; mantenha uma segunda cobertura para a forma Mega.","Ampharosite."),
        "za-24" to d("Corbeau lidera o Rust Syndicate e usa forte presença de Veneno.",listOf(p("Arbok","Nv. 50"),p("Gyarados","Nv. 51"),p("Roserade","Nv. 51"),p("Scolipede","Nv. 52","Mega")),listOf("Psíquico","Fogo","Elétrico"),"Psíquico cobre a maior parte da equipe; guarde Elétrico para Gyarados.","Promoção para Rank C e TM Gunk Shot."),
        "za-25" to d("Lida e Naveen fazem duas batalhas consecutivas no Battle Court.",listOf(p("Clawitzer","Nv. 46"),p("Vanillish","Nv. 46"),p("Emolga","Nv. 47"),p("Staryu","Nv. 48"),p("Ariados","Nv. 48"),p("Sableye","Nv. 48"),p("Krookodile","Nv. 49"),p("Scrafty","Nv. 50","Mega")),listOf("Elétrico","Pedra","Fada"),"Leve cobertura Elétrica/Pedra para Lida e Fada para o núcleo Sombrio de Naveen.","Avanço da história da Team MZ."),
        "za-26" to d("Lebanne encerra o incidente da SBC após os Rogue Megas.",listOf(p("Noivern","Nv. 53"),p("Tyrantrum","Nv. 53"),p("Garchomp","Nv. 53"),p("Dragalge","Nv. 54","Mega")),listOf("Gelo","Dragão","Fada"),"Gelo e Fada têm enorme valor contra a equipe de Dragões.","Avanço para o torneio de Jacinthe."),
        "za-27" to d("Rogue Mega Froslass.",listOf(p("Froslass","Nv. 52–53","Mega")),listOf("Fogo","Pedra","Fantasma","Sombrio"),"Fogo é a resposta mais direta.","Froslassite."),
        "za-28" to d("Rogue Mega Altaria.",listOf(p("Altaria","Nv. 52–53","Mega")),listOf("Gelo","Veneno","Aço","Fada"),"Aço/Fada funcionam muito bem e ajudam a resistir aos golpes de Dragão.","Altarianite."),
        "za-29" to d("Rogue Mega Venusaur.",listOf(p("Venusaur","Nv. 53","Mega")),listOf("Fogo","Gelo","Voador","Psíquico"),"Use dano super efetivo à distância e evite ficar preso em área.","Venusaurite."),
        "za-30" to d("Jacinthe fecha o torneio da SBC e promove você ao Rank B.",listOf(p("Carbink","Nv. 57"),p("Aurorus","Nv. 58"),p("Mawile","Nv. 58"),p("Gardevoir","Nv. 58"),p("Clefable","Nv. 59","Mega")),listOf("Aço"),"Aço é excelente contra quase toda a equipe; cuidado com cobertura de Fogo.","Promoção para Rank B e TM Play Rough."),
        "za-32" to d("Rogue Mega Dragonite.",listOf(p("Dragonite","≈ Nv. 60+","Mega")),listOf("Gelo","Fada","Dragão"),"Gelo é a melhor cobertura geral; mantenha distância dos golpes físicos.","Mega Stone correspondente."),
        "za-33" to d("Rogue Mega Tyranitar.",listOf(p("Tyranitar","≈ Nv. 60+","Mega")),listOf("Lutador","Água","Planta","Terra","Aço","Fada"),"Lutador causa enorme pressão e simplifica o encontro.","Tyranitarite."),
        "za-34" to d("Rogue Mega Starmie.",listOf(p("Starmie","≈ Nv. 60+","Mega")),listOf("Elétrico","Planta","Inseto","Fantasma","Sombrio"),"Elétrico/Sombrio são respostas consistentes.","Starmie Mega Stone."),
        "za-35" to d("A missão de promoção ao Rank A começa com a investigação da identidade de Grisham e inclui batalhas preparatórias antes do Promotion Match final.",listOf(p("Emma","Nv. 57–58","Ampharos · Mawile · Lopunny · Lucario · Malamar"),p("Grisham","Promotion Match","Batalha final da missão")),listOf("Fada","Terra","Fogo"),"Não trate esta etapa como uma única luta: preserve recursos durante a investigação e ajuste o time antes do Promotion Match de Grisham.","Promoção para Rank A."),
        "za-dlc-01" to d("Primeiro grande survey de Hyperspace Lumiose.",listOf(p("Absol","Nv. 105","Mega Absol Z")),listOf("Fada"),"Use uma Mega de Fada e colete as esferas negras para preservar o tempo do donut.","Absolite Z e progresso no Hyperspace."),
        "za-dlc-02" to d("Lebanne e Gwynn enfrentam você em dupla.",listOf(p("Tyrantrum","Nv. 73"),p("Noivern","Nv. 73"),p("Dragalge","Nv. 74","Mega"),p("Banette","Nv. 73"),p("Gourgeist","Nv. 73"),p("Chandelure","Nv. 74","Mega")),listOf("Gelo","Fantasma","Sombrio"),"Gelo resolve o núcleo Dragão; Fantasma/Sombrio ajudam contra a equipe de Gwynn.","Avanço da Mega Dimension."),
        "za-dlc-03" to d("Segundo survey culmina em Rogue Mega Staraptor.",listOf(p("Staraptor","Nv. 120","Mega")),listOf("Elétrico","Gelo","Voador","Psíquico","Fada"),"Use ataques rápidos e dodge quando ele iniciar a perseguição.","Staraptite."),
        "za-dlc-06" to d("Terceiro survey culmina em Mega Tatsugiri.",listOf(p("Tatsugiri","Nv. 140","Mega")),listOf("Fada"),"Fada é a resposta principal; corra em vez de depender só de dodge no ataque carregado.","Tatsugirinite."),
        "za-dlc-09" to d("Canari e Ivor aparecem juntos antes do Survey No. 5.",listOf(p("Ampharos","Nv. 74"),p("Stunfisk","Nv. 74"),p("Eelektross","Nv. 75","Mega"),p("Heracross","Nv. 73"),p("Machamp","Nv. 74"),p("Falinks","Nv. 75","Mega")),listOf("Terra","Fada","Psíquico"),"Terra cobre Canari; Fada/Psíquico cobrem Ivor sem expor um Voador ao Elétrico.","Acesso ao Survey No. 5."),
        "za-dlc-12" to d("A reta final da Mega Dimension leva aos três lendários de Hoenn.",listOf(p("Groudon","Nv. 200","Primal"),p("Kyogre","Nv. 200","Primal"),p("Rayquaza","Nv. 200","Mega")),listOf("Água","Planta","Gelo","Fada","Pedra"),"Monte respostas específicas para cada lendário e prepare donuts/berries suficientes antes de entrar.","Capturas de Groudon, Kyogre e Rayquaza."),
        "za-dlc-13" to d("Primal Groudon exige duas fases.",listOf(p("Groudon","Nv. 200","Primal Groudon")),listOf("Água","Planta","Gelo","Terra"),"Na forma Primal, Água fica bloqueada até atordoá-lo; use Terra para abrir a janela e depois Água.","Groudon + Red Orb."),
        "za-dlc-14" to d("Primal Kyogre exige duas fases.",listOf(p("Kyogre","Nv. 200","Primal Kyogre")),listOf("Elétrico","Planta"),"Recolha seu Pokémon durante os feixes de água e volte ao ataque quando a rajada terminar.","Kyogre + Blue Orb.")
    )
}
