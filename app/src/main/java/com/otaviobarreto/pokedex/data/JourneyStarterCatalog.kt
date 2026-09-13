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
        "Legends Arceus" -> hisui
        "Sword / Shield" -> galar
        else -> emptyList()
    }

    fun bestForGame(game:String):JourneyStarterRecommendation? = forGame(game).maxByOrNull{
        it.rating.early*3 + it.rating.mid*2 + it.rating.late
    }

    private val galar=listOf(
        JourneyStarterRecommendation(
            813,"Scorbunny","Cinderace","Fogo",
            "RECOMENDADO · campanha rápida e ofensiva",
            StarterStageRating(5,5,5),
            listOf(
                "Early game: excelente contra Milo e vários encontros iniciais de Galar.",
                "Mid game: Raboot mantém velocidade e pressão física enquanto o time ganha cobertura.",
                "Late game: Cinderace continua rápido e confiável no Champion Cup e nas DLCs.",
                "É a escolha mais direta para uma Jornada com pouco grind."
            ),
            listOf("Nessa exige resposta de Planta/Elétrico.","Pedra, Terra e Água devem ser cobertos pelo restante da equipe.")
        ),
        JourneyStarterRecommendation(
            810,"Grookey","Rillaboom","Planta",
            "ÓTIMO · força física e segurança",
            StarterStageRating(4,5,5),
            listOf(
                "Early game: vantagem imediata contra Nessa e boa estabilidade na Wild Area.",
                "Mid game: Thwackey/Rillaboom entregam dano físico consistente.",
                "Late game: Rillaboom permanece excelente contra Água, Terra e Pedra nas rotas e DLCs.",
                "Boa opção para quem prefere resistência e dano físico."
            ),
            listOf("Kabu e outros usuários de Fogo exigem troca.","Voador, Gelo, Veneno e Inseto pedem cobertura.")
        ),
        JourneyStarterRecommendation(
            816,"Sobble","Inteleon","Água",
            "TÁTICO · velocidade e dano especial",
            StarterStageRating(4,4,5),
            listOf(
                "Early game: Sobble facilita Kabu e encontros de Pedra/Terra.",
                "Mid game: Drizzile evolui para um atacante especial veloz.",
                "Late game: Inteleon oferece dano especial preciso contra Champion Cup e exploração.",
                "Funciona melhor em times que já possuem resposta sólida para Planta e Elétrico."
            ),
            listOf("Milo é um matchup ruim no começo.","A fragilidade defensiva recompensa trocas e posicionamento mais cuidadosos.")
        )
    )

    private val hisui=listOf(
        JourneyStarterRecommendation(
            155,"Cyndaquil","Hisuian Typhlosion","Fogo → Fogo/Fantasma",
            "RECOMENDADO · campanha muito consistente",
            StarterStageRating(5,5,5),
            listOf(
                "Early game: Fogo facilita vários encontros de Obsidian Fieldlands e Crimson Mirelands.",
                "Mid game: Quilava mantém boa velocidade e pressão enquanto a equipe ainda está se formando.",
                "Late game: Hisuian Typhlosion combina Fogo/Fantasma e funciona muito bem contra o conteúdo de história e pós-game.",
                "É a opção mais simples para manter como núcleo ofensivo durante toda a Jornada."
            ),
            listOf("Precisa de apoio contra Água, Terra, Pedra, Fantasma e Sombrio.","Em lutas de Nobres, posicionamento e esquiva continuam tão importantes quanto o matchup.")
        ),
        JourneyStarterRecommendation(
            501,"Oshawott","Hisuian Samurott","Água → Água/Sombrio",
            "ÓTIMO · cobertura física e segurança",
            StarterStageRating(4,5,5),
            listOf(
                "Early game: Água é segura contra Pedra e ajuda na exploração inicial.",
                "Mid game: Dewott tem boa cobertura física e cresce bem com moves aprendidos/tutor.",
                "Late game: Hisuian Samurott adiciona Sombrio e oferece excelente cobertura contra Psíquico e Fantasma.",
                "Boa opção para quem prefere um atacante físico equilibrado."
            ),
            listOf("Elétrico e Planta exigem cobertura do restante do time.","Alguns Nobres não são resolvidos apenas com vantagem de tipo.")
        ),
        JourneyStarterRecommendation(
            722,"Rowlet","Hisuian Decidueye","Planta/Voador → Planta/Lutador",
            "TÁTICO · ótima cobertura no meio/fim",
            StarterStageRating(3,5,5),
            listOf(
                "Early game: Planta ajuda contra Água, Terra e Pedra presentes nas primeiras áreas.",
                "Mid game: Dartrix mantém utilidade e boa cobertura enquanto Hisui abre novas zonas.",
                "Late game: Hisuian Decidueye ganha Lutador e amplia muito a cobertura física.",
                "Ótima escolha para quem aceita um começo mais técnico em troca de muita versatilidade depois."
            ),
            listOf("O começo exige mais cuidado contra voadores e gelo.","A forma final acumula fraquezas que pedem trocas inteligentes.")
        )
    )

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
