package com.otaviobarreto.pokedex.data

data class AvailabilityCoverageSummary(
    val totalDexEntries:Int,
    val resolved:Int,
    val confirmed:Int,
    val partial:Int,
    val unavailable:Int
){
    val complete:Boolean get()=resolved==totalDexEntries
    val confirmedRatio:Double get()=if(totalDexEntries==0)1.0 else confirmed.toDouble()/totalDexEntries
}

object AvailabilityCoverageAudit {
    fun summarize(
        dex:List<GameDexService.GameDexEntry>,
        records:List<CanonicalAvailability>
    ):AvailabilityCoverageSummary {
        val byId=records.associateBy{it.pokemonId}
        var confirmed=0
        var partial=0
        var unavailable=0
        var resolved=0
        dex.forEach{entry->
            val record=byId[entry.nationalId] ?: return@forEach
            resolved++
            if(record.acquisitionKind==CanonicalAcquisitionKind.UNAVAILABLE) unavailable++
            when(record.confidence){
                AvailabilityConfidence.CONFIRMED -> confirmed++
                AvailabilityConfidence.PARTIAL -> partial++
            }
        }
        return AvailabilityCoverageSummary(
            totalDexEntries=dex.size,
            resolved=resolved,
            confirmed=confirmed,
            partial=partial,
            unavailable=unavailable
        )
    }

    fun assertNoCoverageHoles(
        dex:List<GameDexService.GameDexEntry>,
        records:List<CanonicalAvailability>
    ){
        val summary=summarize(dex,records)
        check(summary.complete){"Cobertura incompleta: ${summary.resolved}/${summary.totalDexEntries}"}
        check(summary.unavailable==0){"Há espécies da própria Pokédex marcadas como indisponíveis: ${summary.unavailable}"}
    }
}

object AvailabilitySourceRegistry {
    val primary=listOf(
        "Pokédex regional / PokeAPI",
        "Encontros por versão / PokeAPI",
        "Cadeia evolutiva / PokeAPI"
    )
    val crossChecks=listOf(
        "Serebii / Pokéarth",
        "Bulbapedia",
        "Dados oficiais dos jogos Pokémon"
    )
}
