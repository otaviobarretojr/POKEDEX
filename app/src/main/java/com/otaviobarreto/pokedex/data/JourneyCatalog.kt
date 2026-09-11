package com.otaviobarreto.pokedex.data

enum class JourneyChallengeKind(val label:String){ GYM("Ginásio"), TITAN("Titã"), STAR("Team Star"), STORY("História") }

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
        "Scarlet / Violet" -> "18 objetivos · ordem revisada por nível · sem level scaling"
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
        JourneyStep("sv-18",18,"Eri","Caph Squad","Nv. 55–56",JourneyChallengeKind.STAR,"Lutador","North Province (Area Two)","Fechamento recomendado dos 18 objetivos principais.")
    )
}
