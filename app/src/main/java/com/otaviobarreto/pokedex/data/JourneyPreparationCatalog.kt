package com.otaviobarreto.pokedex.data

data class JourneyPreparation(
    val stepId:String,
    val recommendedLevel:String,
    val counters:List<String>,
    val pokemonIds:List<Int>,
    val items:List<String>,
    val tip:String
)

object JourneyPreparationCatalog {
    fun forStep(stepId:String):JourneyPreparation? = items[stepId]

    private val items=mapOf(
        "sv-01" to JourneyPreparation("sv-01","15–17",listOf("Fogo","Voador","Pedra"),listOf(909,661,935),listOf("Potion","Antidote"),"Ataque primeiro e pressione com STAB super efetivo."),
        "sv-02" to JourneyPreparation("sv-02","16–18",listOf("Água","Planta","Lutador"),listOf(912,906,194),listOf("Potion"),"Água e Planta tornam Klawf um encontro bem seguro."),
        "sv-03" to JourneyPreparation("sv-03","17–19",listOf("Fogo","Voador"),listOf(909,661,935),listOf("Potion","Paralyze Heal"),"Guarde seu melhor atacante para o Tera Planta final."),
        "sv-04" to JourneyPreparation("sv-04","19–21",listOf("Elétrico","Pedra","Gelo"),listOf(940,25,935),listOf("Potion"),"Elétrico é a resposta mais direta para Bombirdier."),
        "sv-05" to JourneyPreparation("sv-05","21–23",listOf("Lutador","Fada"),listOf(296,926,280),listOf("Super Potion"),"Tenha ao menos um golpe Lutador confiável para o Starmobile."),
        "sv-06" to JourneyPreparation("sv-06","24–26",listOf("Terra","Dano neutro forte"),listOf(194,328,322),listOf("Super Potion","Paralyze Heal"),"Terra domina os primeiros membros, mas Mismagius exige plano neutro."),
        "sv-07" to JourneyPreparation("sv-07","27–29",listOf("Água","Terra","Pedra"),listOf(912,194,328),listOf("Super Potion"),"Água é a opção mais segura contra a equipe de Mela."),
        "sv-08" to JourneyPreparation("sv-08","28–30",listOf("Fogo","Lutador"),listOf(909,296,935),listOf("Super Potion"),"Evite depender apenas de golpes de Terra contra Orthworm."),
        "sv-09" to JourneyPreparation("sv-09","30–32",listOf("Elétrico","Planta"),listOf(940,906,928),listOf("Super Potion"),"Leve cobertura Elétrica ou Planta consistente."),
        "sv-10" to JourneyPreparation("sv-10","33–35",listOf("Terra","Psíquico"),listOf(980,328,282),listOf("Super Potion","Antidote"),"Terra oferece o plano mais simples contra a base Veneno."),
        "sv-11" to JourneyPreparation("sv-11","36–38",listOf("Lutador"),listOf(296,979,448),listOf("Hyper Potion"),"O tipo Normal abre espaço para um atacante Lutador dedicado."),
        "sv-12" to JourneyPreparation("sv-12","42–44",listOf("Sombrio","Fantasma"),listOf(908,936,937),listOf("Hyper Potion"),"Como é batalha dupla, leve dois atacantes úteis ao mesmo tempo."),
        "sv-13" to JourneyPreparation("sv-13","44–46",listOf("Água","Planta","Gelo"),listOf(130,908,131),listOf("Hyper Potion"),"Água funciona bem contra as duas versões do Titã."),
        "sv-14" to JourneyPreparation("sv-14","45–47",listOf("Sombrio","Fantasma","Inseto"),listOf(908,937,214),listOf("Hyper Potion"),"Sombrio e Fantasma são as opções mais consistentes."),
        "sv-15" to JourneyPreparation("sv-15","48–50",listOf("Fogo","Lutador","Aço"),listOf(911,448,937),listOf("Hyper Potion"),"Fogo, Lutador e Aço reduzem bastante o risco do ginásio de Gelo."),
        "sv-16" to JourneyPreparation("sv-16","51–53",listOf("Aço","Veneno"),listOf(448,980,823),listOf("Hyper Potion","Full Heal"),"Aço é o melhor eixo de cobertura contra a base Fada."),
        "sv-17" to JourneyPreparation("sv-17","55–57",listOf("Elétrico","Planta","Fada"),listOf(941,930,282),listOf("Max Potion"),"Prepare respostas separadas para Dondozo e Tatsugiri."),
        "sv-18" to JourneyPreparation("sv-18","56–58",listOf("Psíquico","Voador","Fada"),listOf(282,823,941),listOf("Max Potion","Full Restore"),"Entre com cobertura ampla; é o desafio mais pesado entre as bases."),
        "sv-pg-01" to JourneyPreparation("sv-pg-01","61–63",listOf("Cobertura ampla"),listOf(908,911,914,937,941,979),listOf("Full Restore","Revive"),"Evite um time mono-tipo; a Liga exige respostas diferentes em sequência."),
        "sv-pg-02" to JourneyPreparation("sv-pg-02","62–64",listOf("Cobertura ampla"),listOf(908,911,914,937,941,979),listOf("Full Restore"),"Arven usa seis funções diferentes; priorize consistência."),
        "sv-pg-03" to JourneyPreparation("sv-pg-03","63–65",listOf("Cobertura ampla"),listOf(908,911,914,937,941,979),listOf("Full Restore","Revive"),"Prepare-se para duas lutas importantes no fechamento da rota."),
        "sv-pg-04" to JourneyPreparation("sv-pg-04","66–68",listOf("Fada","Gelo","Terra","Elétrico"),listOf(282,937,980,941),listOf("Full Restore","Max Revive"),"Area Zero mistura Paradox Pokémon e a batalha final; entre com seis membros prontos."),
        "sv-pg-05" to JourneyPreparation("sv-pg-05","66–70",listOf("Cobertura dos 8 ginásios"),listOf(908,911,914,937,941,979),listOf("Full Restore"),"Troque golpes de cobertura entre as oito revanche quando necessário."),
        "sv-pg-06" to JourneyPreparation("sv-pg-06","68–72",listOf("Cobertura geral"),listOf(908,911,914,937,941,979),listOf("Full Restore","Max Revive"),"Use seu núcleo mais estável: o torneio exige várias batalhas consecutivas."),
        "sv-pg-07" to JourneyPreparation("sv-pg-07","75–100",listOf("Build por Tera Type"),listOf(979,1000,1002,1003),listOf("Shell Bell","Held item adequado"),"Para 6★, escolha o atacante depois de conferir espécie, Tera Type e moveset do raid boss."),
        "sv-pg-08" to JourneyPreparation("sv-pg-08","Livre",listOf("Captura","Exploração"),listOf(1001,1002,1003,1004),listOf("Ultra Ball","Timer Ball","Quick Ball"),"Use status + False Swipe para fechar lendários e Pokédex com segurança."),
        "sv-dlc-01" to JourneyPreparation("sv-dlc-01","60+",listOf("Cobertura ampla"),listOf(908,911,914),listOf("Full Restore"),"Seu time de pós-game já deve estar pronto para a abertura de Kitakami."),
        "sv-dlc-02" to JourneyPreparation("sv-dlc-02","60+",listOf("Exploração"),listOf(908,911,914),listOf("Full Restore"),"Mantenha uma vaga flexível para capturas enquanto avança no festival."),
        "sv-dlc-03" to JourneyPreparation("sv-dlc-03","62+",listOf("Cobertura ampla"),listOf(937,941,979),listOf("Full Restore"),"Oni Mountain mistura exploração e batalha; leve mobilidade e cura."),
        "sv-dlc-04" to JourneyPreparation("sv-dlc-04","62+",listOf("Cobertura ampla"),listOf(908,937,941),listOf("Full Restore"),"Kieran usa uma equipe variada; evite depender de um único sweeper."),
        "sv-dlc-05" to JourneyPreparation("sv-dlc-05","65–70",listOf("Terra","Psíquico","Aço"),listOf(980,282,937),listOf("Full Restore","Antidote"),"O trio compartilha Veneno, mas cada membro exige uma resposta diferente."),
        "sv-dlc-06" to JourneyPreparation("sv-dlc-06","68–70",listOf("Cobertura adaptável"),listOf(908,911,914,937),listOf("Full Restore","Max Revive"),"Ogerpon muda de máscara; use dano consistente e preserve respostas para as fases finais."),
        "sv-dlc-07" to JourneyPreparation("sv-dlc-07","70+",listOf("Água","Planta","Lutador"),listOf(914,908,979),listOf("Ultra Ball","Full Restore"),"Conclua o requisito da Dex de Kitakami antes da missão final de Perrin."),
        "sv-dlc-08" to JourneyPreparation("sv-dlc-08","70+",listOf("Sinergia de dupla"),listOf(937,941,979),listOf("Full Restore"),"A Blueberry Academy usa muitas batalhas duplas; ajuste moveset e posicionamento."),
        "sv-dlc-09" to JourneyPreparation("sv-dlc-09","77–79",listOf("Água","Terra","Pedra"),listOf(914,980,934),listOf("Full Restore"),"Contra Crispin, controle o campo e pressione os dois slots."),
        "sv-dlc-10" to JourneyPreparation("sv-dlc-10","78–80",listOf("Fogo","Lutador","Terra"),listOf(911,979,980),listOf("Full Restore"),"Leve dano super efetivo suficiente para quebrar a resistência do tipo Aço."),
        "sv-dlc-11" to JourneyPreparation("sv-dlc-11","78–80",listOf("Aço","Veneno"),listOf(937,980,823),listOf("Full Restore"),"Aço é o eixo principal contra Lacey; proteja-o de cobertura adversária."),
        "sv-dlc-12" to JourneyPreparation("sv-dlc-12","79–81",listOf("Fada","Gelo","Dragão"),listOf(282,998,1006),listOf("Full Restore"),"Prepare antes o time do Elite Trial usando apenas Pokémon capturados no Terarium."),
        "sv-dlc-13" to JourneyPreparation("sv-dlc-13","80–82",listOf("Controle de velocidade","Cobertura ampla"),listOf(908,911,914,937,941,979),listOf("Full Restore","Max Revive"),"Kieran é a batalha mais completa da BB League; use seu melhor núcleo de dupla."),
        "sv-dlc-14" to JourneyPreparation("sv-dlc-14","80+",listOf("Cobertura ampla"),listOf(937,941,979,1000),listOf("Full Restore","Max Revive"),"Entre nas Underdepths com recursos para uma sequência longa."),
        "sv-dlc-15" to JourneyPreparation("sv-dlc-15","85+",listOf("Dano consistente","Sobrevivência"),listOf(937,979,1000,1003),listOf("Full Restore","Max Revive"),"Reserve seus melhores recursos para Terapagos e priorize estabilidade."),
        "sv-dlc-16" to JourneyPreparation("sv-dlc-16","Livre",listOf("Captura","Exploração"),listOf(1000,1001,1002,1003),listOf("Quick Ball","Timer Ball","Ultra Ball"),"Complete BBQs e use builds de captura para os lendários de Snacksworth."),
        "sv-epi-01" to JourneyPreparation("sv-epi-01","80+",listOf("Cobertura ampla"),listOf(908,911,914,937),listOf("Full Restore"),"Mochi Mayhem é conteúdo de endgame; leve o time principal."),
        "sv-epi-02" to JourneyPreparation("sv-epi-02","80+",listOf("Cobertura ampla"),listOf(937,941,979),listOf("Full Restore"),"Espere várias batalhas em sequência e mantenha PP e cura."),
        "sv-epi-03" to JourneyPreparation("sv-epi-03","80+",listOf("Cobertura ampla"),listOf(908,911,914,937,941,979),listOf("Full Restore","Max Revive"),"Nemona usa uma equipe forte; trate como uma batalha de campeonato."),
        "sv-epi-04" to JourneyPreparation("sv-epi-04","88+",listOf("Terra","Psíquico","Fantasma","Sombrio"),listOf(980,282,937,908),listOf("Full Restore"),"Poison Puppeteer combina veneno e confusão; controle status e finalize com segurança.")
    )
}
