package com.otaviobarreto.pokedex.data

data class StarterStageRating(val early:Int,val mid:Int,val late:Int)

data class JourneyStarterRecommendation(
    val pokemonId:Int,
    val name:String,
    val finalName:String,
    val types:String,
    val verdict:String,
    val rating:StarterStageRating,
    val advantages:List<String>,
    val cautions:List<String>
)

object JourneyStarterCatalog {
    fun forGame(game:String):List<JourneyStarterRecommendation> = when(game){
        "Scarlet / Violet" -> paldea
        "Pokémon Legends: Z-A" -> lumiose
        else -> emptyList()
    }

    fun bestForGame(game:String):JourneyStarterRecommendation? = forGame(game).maxByOrNull{
        it.rating.early*3 + it.rating.mid*2 + it.rating.late
    }

    private val lumiose=listOf(
        JourneyStarterRecommendation(
            498,"Tepig","Emboar","Fogo → Fogo/Lutador",
            "RECOMENDADO · melhor cobertura geral",
            StarterStageRating(5,5,5),
            listOf(
                "Early game: ótima pressão ofensiva e cobertura simples para a progressão inicial da Z-A Royale.",
                "Mid game: Pignite/Emboar combina Fogo e Lutador, cobrindo muitos confrontos e Rogue Megas.",
                "Late game: Mega Emboar mantém excelente poder físico e ganha bastante resistência especial.",
                "É a escolha mais fácil para seguir a rota principal sem depender de muitas trocas."
            ),
            listOf("É lento e alguns golpes fortes têm cooldown maior.","Cubra Água, Voador, Psíquico e Terra com o restante do time.")
        ),
        JourneyStarterRecommendation(
            158,"Totodile","Feraligatr","Água → Água / Mega Água-Dragão",
            "ÓTIMO · equilíbrio e Mega forte",
            StarterStageRating(4,5,5),
            listOf(
                "Early game: Água é um tipo simples e seguro para a campanha.",
                "Mid game: Feraligatr oferece bom Ataque e cobertura ampla com golpes físicos.",
                "Late game: Mega Feraligatr ganha Dragão e possui o maior Ataque entre as Mega Evoluções dos três iniciais.",
                "Excelente escolha para quem quer um atacante físico consistente do começo ao fim."
            ),
            listOf("Antes da Mega Evolução, precisa de apoio contra Planta e Elétrico.","A Mega passa a exigir atenção especial a Dragão e Fada.")
        ),
        JourneyStarterRecommendation(
            152,"Chikorita","Meganium","Planta → Planta / Mega Planta-Fada",
            "DEFENSIVO · segurança e suporte",
            StarterStageRating(3,4,5),
            listOf(
                "Early game: maior foco em resistência e utilidade do que dano bruto.",
                "Mid game: Bayleef/Meganium funciona bem como suporte durável para batalhas mais longas.",
                "Late game: Mega Meganium ganha Fada e melhora muito o Ataque Especial sem perder o perfil defensivo.",
                "Boa escolha para quem prefere consistência, cura e controle da luta."
            ),
            listOf("É o inicial com menor pressão ofensiva no começo.","Precisa mais do restante da equipe para cobrir Fogo, Gelo, Veneno, Voador e Inseto.")
        )
    )

    private val paldea=listOf(
        JourneyStarterRecommendation(
            909,"Fuecoco","Skeledirge","Fogo → Fogo/Fantasma",
            "RECOMENDADO · rota mais confortável",
            StarterStageRating(5,5,5),
            listOf(
                "Early game: vantagem direta nos Ginásios de Inseto (Katy) e Planta (Brassius).",
                "Mid game: Fogo ajuda muito contra Orthworm e a linha ganha ótima resistência e Ataque Especial.",
                "Late game: Skeledirge ganha tipo Fantasma, imunidade a Normal/Lutador e Torch Song aumenta o Ataque Especial enquanto causa dano.",
                "É a escolha mais simples para seguir a Melhor Rota com poucas trocas de protagonista."
            ),
            listOf("É o mais lento dos três.","Cubra Água, Terra e Pedra com o restante do time.")
        ),
        JourneyStarterRecommendation(
            906,"Sprigatito","Meowscarada","Planta → Planta/Sombrio",
            "ÓTIMO · velocidade e Titãs",
            StarterStageRating(3,4,5),
            listOf(
                "Early game: excelente contra Klawf, o Stony Cliff Titan.",
                "Mid game: Planta resolve Kofu e vários encontros de Água/Terra.",
                "Late game: Meowscarada é muito veloz; Planta/Sombrio oferece pressão ofensiva e Flower Trick é extremamente consistente.",
                "Boa escolha para quem prefere atacar primeiro e jogar de forma agressiva."
            ),
            listOf("O começo é menos confortável contra os primeiros Ginásios.","A forma final é frágil e tem várias fraquezas, incluindo fraqueza 4× a Inseto.")
        ),
        JourneyStarterRecommendation(
            912,"Quaxly","Quaquaval","Água → Água/Lutador",
            "EQUILIBRADO · atacante físico",
            StarterStageRating(3,5,5),
            listOf(
                "Early game: Água é excelente contra Klawf e ajuda em vários encontros de Pedra/Terra.",
                "Mid game: funciona muito bem contra Mela e ganha excelente cobertura física.",
                "Late game: Quaquaval ganha tipo Lutador; Aqua Step causa dano e aumenta a própria Velocidade.",
                "Boa escolha para quem quer um atacante físico que melhora conforme a batalha avança."
            ),
            listOf("Tem menos vantagem nos dois primeiros Ginásios.","A forma final precisa de cobertura contra Psíquico, Voador e Fada.")
        )
    )
}
