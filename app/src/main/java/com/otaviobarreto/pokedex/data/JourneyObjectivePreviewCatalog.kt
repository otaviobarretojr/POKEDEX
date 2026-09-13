package com.otaviobarreto.pokedex.data

data class JourneyObjectiveMember(
    val pokemonId:Int,
    val level:String
)

data class JourneyObjectivePreview(
    val label:String,
    val subtitle:String,
    val members:List<JourneyObjectiveMember> = emptyList()
)

object JourneyObjectivePreviewCatalog {
    fun forStep(step:JourneyStep?):JourneyObjectivePreview? = step?.let { objective ->
        val members=when(objective.id){
            "sv-01"->listOf(m(919,14),m(917,14),m(216,15))
            "sv-02"->listOf(m(950,16))
            "sv-03"->listOf(m(548,16),m(928,16),m(185,17))
            "sv-04"->listOf(m(962,19))
            "sv-05"->listOf(m(624,21))
            "sv-06"->listOf(m(940,23),m(939,23),m(404,23),m(429,24))
            "sv-07"->listOf(m(324,27))
            "sv-08"->listOf(m(968,28))
            "sv-09"->listOf(m(976,29),m(961,29),m(740,30))
            "sv-10"->listOf(m(435,32),m(89,32),m(966,32))
            "sv-11"->listOf(m(775,35),m(982,35),m(398,36))
            "sv-12"->listOf(m(354,41),m(778,41),m(972,41),m(849,42))
            "sv-13"->listOf(m(984,44),m(990,44))
            "sv-14"->listOf(m(981,44),m(282,44),m(956,44),m(671,45))
            "sv-15"->listOf(m(873,47),m(614,47),m(975,47),m(334,48))
            "sv-16"->listOf(m(184,50),m(40,50),m(927,51))
            "sv-17"->listOf(m(977,55),m(978,55))
            "sv-18"->listOf(m(454,55),m(766,55),m(448,55),m(979,56))
            else->emptyList()
        }
        JourneyObjectivePreview(
            label=objective.title,
            subtitle=listOf(objective.kind.label,objective.typeLabel,objective.levelLabel)
                .filter{it.isNotBlank()}.joinToString(" · "),
            members=members
        )
    }

    private fun m(id:Int,level:Int)=JourneyObjectiveMember(id,"Nv. $level")
}
