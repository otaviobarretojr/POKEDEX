package com.otaviobarreto.pokedex.data

data class JourneyWalkthrough(
    val title:String,
    val steps:List<String>,
    val tips:List<String> = emptyList()
)

object JourneyWalkthroughCatalog {
    fun forStep(stepId:String):JourneyWalkthrough? = walkthroughs[stepId]

    private fun w(title:String, vararg steps:String, tips:List<String> = emptyList()) =
        JourneyWalkthrough(title, steps.toList(), tips)

    private val walkthroughs = mapOf(
        "sv-01" to w(
            "Cortondo · prova da azeitona",
            "Entre no Ginásio de Cortondo e registre-se para o Gym Test.",
            "Vá ao campo cercado a noroeste da cidade e fale com o funcionário.",
            "Empurre a azeitona gigante pelo percurso até o gol. Você pode batalhar com treinadores para abrir atalhos, mas não é obrigatório.",
            "Volte ao Ginásio e desafie Katy com o time por volta do nível 15.",
            tips=listOf("Fogo, Voador e Pedra funcionam muito bem.", "Teddiursa vira Tera Inseto no final da luta.")
        ),
        "sv-02" to w(
            "Stony Cliff Titan · Klawf",
            "Siga para South Province (Area Three), a leste de Mesagoza, e aproxime-se do grande Klawf nas paredes rochosas.",
            "Persiga Klawf pela encosta e vença a primeira fase.",
            "Após ele comer a Herba Mystica, lute novamente com Arven como parceiro.",
            "Conclua a cena na caverna para receber a melhoria de montaria.",
            tips=listOf("Água e Planta são as opções mais simples.", "Faça este Titã cedo: a melhoria de mobilidade facilita bastante a exploração.")
        ),
        "sv-03" to w(
            "Artazon · Sunflora Hide-and-Seek",
            "Registre-se no Ginásio de Artazon.",
            "Fale com o responsável pelo teste próximo ao Sunflora Lawn.",
            "Encontre 10 Sunflora espalhados por Artazon e faça-os seguir você.",
            "Retorne com os 10 Sunflora ao ponto inicial e informe a conclusão.",
            "Volte ao Ginásio e enfrente Brassius por volta do nível 17.",
            tips=listOf("Fogo e Voador são excelentes aqui.", "Alguns Sunflora podem iniciar uma batalha antes de seguirem você.")
        ),
        "sv-04" to w(
            "Open Sky Titan · Bombirdier",
            "Vá para West Province (Area One) e suba a estrada onde pedras caem pela encosta.",
            "Desvie das pedras enquanto avança até Bombirdier.",
            "Vença a primeira fase; depois siga o Titã até a Herba Mystica.",
            "Lute novamente ao lado de Arven e conclua a cena na caverna.",
            tips=listOf("Elétrico, Gelo, Pedra e Fada causam bom dano.", "A recompensa de montaria desta etapa abre novas rotas de exploração.")
        ),
        "sv-05" to w(
            "Segin Squad · Giacomo",
            "Chegue ao portão da base Sombria da Team Star em West Province (Area One).",
            "Vença o guarda da entrada para iniciar o Star Barrage.",
            "Use Let's Go para derrotar 30 Pokémon da base dentro do limite de tempo; leve três Pokémon adequados nos primeiros slots.",
            "Derrote Giacomo e o Segin Starmobile.",
            tips=listOf("Lutador, Inseto e Fada são fortes contra Sombrio.", "Cure seus três Pokémon nas máquinas da base se necessário durante o Star Barrage.")
        ),
        "sv-06" to w(
            "Levincia · Find Mister Walksabout",
            "Registre-se no Ginásio de Levincia e encontre Iono.",
            "No teste, localize o diretor Clavell/Mr. Walksabout nas imagens mostradas por Iono.",
            "Entre as rodadas de busca, vença os treinadores indicados.",
            "Complete todas as buscas e volte ao Ginásio para enfrentar Iono no nível 24.",
            tips=listOf("Terra domina boa parte da equipe.", "Mismagius tem Levitate e vira Tera Elétrico: não dependa apenas de golpes de Terra.")
        ),
        "sv-07" to w(
            "Schedar Squad · Mela",
            "Vá à base de Fogo da Team Star em East Province (Area One).",
            "Vença o guarda e inicie o Star Barrage.",
            "Derrote 30 Pokémon usando Let's Go com seus três primeiros Pokémon.",
            "Enfrente Mela e depois o Schedar Starmobile.",
            tips=listOf("Água, Terra e Pedra são excelentes.", "Entre com o time perto do nível 27.")
        ),
        "sv-08" to w(
            "Lurking Steel Titan · Orthworm",
            "Vá para East Province (Area Three) e localize Orthworm saindo do solo.",
            "Persiga-o pelos túneis/aberturas até conseguir iniciar a primeira luta.",
            "Após a primeira fase, siga-o novamente até a Herba Mystica.",
            "Vença a segunda fase ao lado de Arven e conclua a caverna.",
            tips=listOf("Fogo e Lutador são respostas seguras.", "Earth Eater pode anular golpes de Terra, então não faça deles sua única opção.")
        ),
        "sv-09" to w(
            "Cascarrafa · carteira e leilão",
            "Entre no Ginásio de Cascarrafa. Você receberá a carteira esquecida de Kofu.",
            "Siga até o mercado de Porto Marinada e localize Kofu.",
            "Antes de entregar a carteira, vença o aprendiz dele, que usa Pokémon de Água.",
            "Entregue a carteira e participe do leilão de seaweed. Kofu fornece cerca de ₽50.000.",
            "Faça lances conservadores; é possível fechar o teste gastando cerca de ₽35.000.",
            "Retorne a Cascarrafa e enfrente Kofu no nível 30.",
            tips=listOf("Planta e Elétrico funcionam praticamente na luta inteira.", "O dinheiro que sobrar do valor fornecido por Kofu fica com você.")
        ),
        "sv-10" to w(
            "Navi Squad · Atticus",
            "Entre em Tagtree Thicket e vá até a base de Veneno da Team Star.",
            "Vença o guarda e comece o Star Barrage.",
            "Derrote 30 Pokémon com Let's Go usando seus três primeiros Pokémon.",
            "Enfrente Atticus, incluindo Revavroom e o Navi Starmobile.",
            tips=listOf("Terra e Psíquico são as melhores coberturas gerais.", "Leve Antidotes ou itens para status se quiser jogar com mais segurança.")
        ),
        "sv-11" to w(
            "Medali · pedido secreto",
            "Registre-se no Ginásio de Medali e receba a pista inicial.",
            "Você pode investigar as pistas batalhando com os outros participantes pela cidade ou ir direto ao restaurante se já souber a resposta.",
            "No Treasure Eatery, fale com o atendente e escolha: Grilled Rice Balls.",
            "Escolha o tamanho Medium.",
            "Escolha Fire Blast como estilo de preparo.",
            "Finalize com Lemon como tempero.",
            "Após acertar o pedido secreto, enfrente Larry no nível 36.",
            tips=listOf("Resposta completa: Grilled Rice Balls · Medium · Fire Blast · Lemon.", "Lutador é a fraqueza direta da equipe Normal de Larry.")
        ),
        "sv-12" to w(
            "Montenevera · palco e batalhas duplas",
            "Registre-se no Ginásio de Montenevera e vá ao palco.",
            "O Gym Test é uma sequência de Double Battles antes de enfrentar Ryme.",
            "Monte a dupla com sinergia: evite golpes que atinjam seu próprio parceiro e priorize ataques em área quando forem seguros.",
            "Vença as apresentações/batalhas e então desafie Ryme no nível 42.",
            tips=listOf("Sombrio é uma cobertura confortável contra Fantasma.", "A luta contra Ryme também é em dupla; prepare dois Pokémon principais, não apenas um.")
        ),
        "sv-13" to w(
            "Quaking Earth Titan",
            "Vá ao Asado Desert e encontre o Titã em movimento.",
            "Em Scarlet o alvo é Great Tusk; em Violet é Iron Treads.",
            "Vença a primeira fase e siga o Titã após ele recuar.",
            "Na segunda fase, lute ao lado de Arven.",
            "Conclua a caverna para receber a próxima melhoria da montaria.",
            tips=listOf("Água funciona bem nas duas versões.", "No Scarlet, prepare-se para Great Tusk; no Violet, para Iron Treads.")
        ),
        "sv-14" to w(
            "Alfornada · Emotional Spectrum Practice",
            "Registre-se no Ginásio de Alfornada e vá ao campo ao lado do prédio.",
            "Inicie o ESP com Dendra.",
            "Observe a emoção exibida e pressione o botão correspondente entre X, A, B e Y.",
            "Complete as rodadas de reação e vença as batalhas que aparecem entre elas.",
            "Volte à recepção do Ginásio e enfrente Tulip no nível 45.",
            tips=listOf("Sombrio, Fantasma e Inseto são boas respostas.", "O teste alterna minigame e batalha; mantenha o time curado antes de começar.")
        ),
        "sv-15" to w(
            "Glaseado · Snow Slope Run",
            "Registre-se no Ginásio de Glaseado.",
            "Vá ao ponto de largada da pista na montanha e inicie o Snow Slope Run.",
            "Desça montado em Koraidon/Miraidon passando pelos checkpoints dentro do tempo limite de aproximadamente 1min30s.",
            "Ao concluir o percurso, retorne e enfrente Grusha no nível 48.",
            tips=listOf("Fogo, Lutador, Pedra e Aço são fortes.", "Cetitan possui Thick Fat; golpes de Lutador evitam a redução de dano que afeta Fogo.")
        ),
        "sv-16" to w(
            "Ruchbah Squad · Ortega",
            "Vá à base de Fada da Team Star em North Province (Area Three).",
            "Vença o guarda e inicie o Star Barrage.",
            "Derrote 30 Pokémon da base com seus três primeiros Pokémon usando Let's Go.",
            "Enfrente Ortega e o Ruchbah Starmobile.",
            tips=listOf("Aço é a cobertura mais segura.", "Veneno também é forte, mas confira a combinação de tipos dos oponentes.")
        ),
        "sv-17" to w(
            "False Dragon Titan · Dondozo e Tatsugiri",
            "Vá a Casseroya Lake e procure o pequeno Tatsugiri que dispara a fala diferente dos demais.",
            "Interaja com ele para iniciar a sequência do False Dragon Titan.",
            "Enfrente Dondozo nas fases iniciais e siga o objetivo pelo lago.",
            "Depois da segunda luta contra Dondozo, prepare-se para o confronto com Tatsugiri.",
            "Conclua a caverna com Arven para fechar a rota das Herba Mystica.",
            tips=listOf("Elétrico ou Planta ajudam muito contra Dondozo.", "Tenha Fada ou Dragão para a etapa com Tatsugiri.")
        ),
        "sv-18" to w(
            "Caph Squad · Eri",
            "Vá à base de Lutador da Team Star em North Province (Area Two).",
            "Vença o confronto de entrada e inicie o Star Barrage.",
            "Derrote 30 Pokémon com Let's Go usando três membros preparados para Lutador.",
            "Enfrente Eri, sua equipe completa e o Caph Starmobile.",
            tips=listOf("Psíquico, Voador e Fada têm ótimo valor.", "É a base mais forte da campanha principal; entre no nível 56 ou acima.")
        )
    )
}
