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
    fun forGame(game:String):List<JourneyStarterRecommendation> =
        if(game=="Scarlet / Violet") paldea else emptyList()

    fun bestForGame(game:String):JourneyStarterRecommendation? = forGame(game).maxByOrNull{
        it.rating.early*3 + it.rating.mid*2 + it.rating.late
    }

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
