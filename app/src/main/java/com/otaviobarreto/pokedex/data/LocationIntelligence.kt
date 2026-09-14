package com.otaviobarreto.pokedex.data

object LocationIntelligence {
    enum class Coverage { RELIABLE, PARTIAL }

    private val kitakami = setOf(
        "kitakami road","mossui town","apple hills","loyalty plaza","revelers road",
        "kitakami wilds","wistful fields","paradise barrens","fellhorn gorge",
        "oni mountain","crystal pool","infernal pass","chilling waterhead","timeless woods"
    )
    private val blueberry = setOf(
        "savanna biome","coastal biome","canyon biome","polar biome","central plaza",
        "chargestone cavern","torchlit labyrinth","blueberry academy","terarium"
    )
    private val isleOfArmor = setOf(
        "fields of honor","soothing wetlands","forest of focus","challenge beach",
        "brawlers cave","challenge road","courageous cavern","loop lagoon",
        "training lowlands","warm up tunnel","potbottom desert","workout sea",
        "stepping stone sea","insular sea","honeycalm sea","honeycalm island"
    )
    private val crownTundra = setOf(
        "slippery slope","freezington","frostpoint field","giants bed","old cemetery",
        "snowslide slope","tunnel to the top","path to the peak","crown shrine",
        "giants foot","roaring sea caves","frigid sea","three point pass",
        "ballimere lake","dyna tree hill","lakeside cave","max lair"
    )

    fun coverage(context: GameContext?): Coverage = when(context?.label) {
        "FireRed / LeafGreen" -> Coverage.RELIABLE
        "Pokémon Champions" -> Coverage.UNSUPPORTED
        null -> Coverage.PARTIAL
        else -> Coverage.PARTIAL
    }

    fun isSupported(context: GameContext?): Boolean = coverage(context) != Coverage.UNSUPPORTED

    fun belongsToContext(location: String, context: GameContext?): Boolean {
        if(context==null) return true
        val n=normalize(location)
        return when(context.pokedexSlug) {
            "kitakami" -> containsAny(n,kitakami)
            "blueberry" -> containsAny(n,blueberry)
            "paldea" -> !containsAny(n,kitakami) && !containsAny(n,blueberry)
            "isle-of-armor" -> containsAny(n,isleOfArmor)
            "crown-tundra" -> containsAny(n,crownTundra)
            "galar" -> !containsAny(n,isleOfArmor) && !containsAny(n,crownTundra)
            else -> true
        }
    }

    fun filter(
        encounters: List<PokeApiService.EncounterLocation>,
        context: GameContext?
    ): List<PokeApiService.EncounterLocation> {
        val byVersion = if(context==null) encounters else encounters.mapNotNull { encounter ->
            val versions=encounter.versions.filter(context::matchesVersion)
            val details=encounter.details.filter { context.matchesVersion(it.version) }
            if(versions.isEmpty() && details.isEmpty()) null
            else encounter.copy(versions=versions,details=details)
        }
        return byVersion
            .filter { belongsToContext(it.location,context) }
            .distinctBy { normalize(it.location) }
            .sortedBy { displayName(it.location) }
    }

    fun displayName(raw: String): String {
        var value=raw.trim()
            .replace(Regex("\\s+")," ")
            .replace(" Of "," of ")
            .replace(" To "," to ")
        value=value
            .replace(Regex("(?i)\\b(South|East|West|North) Province Area (One|Two|Three|Four|Five|Six)\\b")) {
                "${it.groupValues[1]} Province (Area ${it.groupValues[2]})"
            }
            .replace("Warm Up Tunnel","Warm-Up Tunnel")
            .replace("Stepping Stone Sea","Stepping-Stone Sea")
            .replace("Roaring Sea Caves","Roaring-Sea Caves")
            .replace("Three Point Pass","Three-Point Pass")
        return value
    }

    fun emptyMessage(context: GameContext?, hasAnyRawEncounter: Boolean): String {
        return when(coverage(context)) {
            Coverage.PARTIAL ->
                if(hasAnyRawEncounter)
                    "Nenhum encontro desta sub-região foi confirmado pela fonte atual."
                else
                    "Dados de localização ainda não estão disponíveis de forma completa para este jogo."
            Coverage.RELIABLE ->
                "Este Pokémon não possui encontro selvagem registrado neste jogo. Ele pode ser obtido por evolução, troca, evento ou outro método."
        }
    }

    private fun containsAny(value:String,keywords:Set<String>)=keywords.any{it in value}

    private fun normalize(value:String)=value
        .lowercase()
        .replace("'", "")
        .replace("-", " ")
        .replace(Regex("[^a-z0-9 ]+")," ")
        .replace(Regex("\\s+")," ")
        .trim()
}
