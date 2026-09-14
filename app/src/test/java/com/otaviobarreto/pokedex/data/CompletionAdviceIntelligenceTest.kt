package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class CompletionAdviceIntelligenceTest {

    private fun route(
        source:Int,
        target:Int,
        methods:Set<PokeApiService.EvolutionMethod>,
        summary:String,
        availability:EvolutionAvailability=EvolutionAvailability.AVAILABLE_WITH_CONDITION
    )=EvolutionRoute(
        sourcePokemonId=source,
        targetPokemonId=target,
        methods=methods,
        summary=summary,
        detail=summary,
        availability=availability,
        contextLabel="Scarlet / Violet",
        regionLabel="Paldea"
    )

    private fun canonicalWild(
        id:Int,
        context:GameContext,
        version:VersionAvailability?
    )=CanonicalAvailability(
        pokemonId=id,
        context=context,
        inRegionalDex=true,
        regionalNumber=1,
        version=version,
        acquisitionKind=CanonicalAcquisitionKind.WILD,
        acquisitionLabel="Captura selvagem em Paldea",
        locations=listOf("South Province"),
        confidence=AvailabilityConfidence.CONFIRMED
    )

    @Test
    fun missingPreEvolutionIsExplicitlyCalledOut() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!
        val advice=CompletionAdviceResolver.resolve(
            pokemonId=907,
            context=context,
            routes=listOf(route(906,907,setOf(PokeApiService.EvolutionMethod.LEVEL),"Nível 16")),
            owned=emptySet(),
            names=mapOf(906 to "Sprigatito")
        )

        assertEquals(906,advice.sourcePokemonId)
        assertTrue(advice.title.contains("Sprigatito"))
        assertTrue(advice.detail?.contains("Obtenha Sprigatito primeiro")==true)
    }

    @Test
    fun ownedPreEvolutionMakesEvolutionRouteActionable() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!
        val advice=CompletionAdviceResolver.resolve(
            pokemonId=907,
            context=context,
            routes=listOf(route(906,907,setOf(PokeApiService.EvolutionMethod.LEVEL),"Nível 16")),
            owned=setOf(906),
            names=mapOf(906 to "Sprigatito")
        )

        assertEquals(CompletionMethodKind.LEVEL,advice.method)
        assertEquals("Nível 16",advice.title)
        assertTrue(advice.detail?.contains("Você já possui Sprigatito")==true)
    }

    @Test
    fun confirmedWildCaptureBeatsEvolutionWhenSourceIsMissing() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!
        val version=VersionAvailabilityCatalog.forPokemon(907,context,true)
        val advice=CompletionAdviceResolver.resolve(
            pokemonId=907,
            context=context,
            routes=listOf(route(906,907,setOf(PokeApiService.EvolutionMethod.LEVEL),"Nível 16")),
            owned=emptySet(),
            names=mapOf(906 to "Sprigatito"),
            canonical=canonicalWild(907,context,version)
        )

        assertEquals(CompletionMethodKind.DIRECT,advice.method)
        assertTrue(advice.title.contains("Captura selvagem"))
    }

    @Test
    fun ownedPreEvolutionCanBeatDirectCapture() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!
        val version=VersionAvailabilityCatalog.forPokemon(907,context,true)
        val advice=CompletionAdviceResolver.resolve(
            pokemonId=907,
            context=context,
            routes=listOf(route(906,907,setOf(PokeApiService.EvolutionMethod.LEVEL),"Nível 16")),
            owned=setOf(906),
            names=mapOf(906 to "Sprigatito"),
            canonical=canonicalWild(907,context,version)
        )

        assertEquals(CompletionMethodKind.LEVEL,advice.method)
    }

    @Test
    fun oppositeVersionExclusiveShowsTradeHomeGuidance() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!
        val version=VersionAvailabilityCatalog.forPokemon(936,context,true)
        val canonical=canonicalWild(936,context,version)

        val advice=CompletionAdviceResolver.resolve(
            pokemonId=936,
            context=context,
            routes=emptyList(),
            selectedVersion="Violet",
            canonical=canonical
        )

        assertEquals(CompletionMethodKind.TRADE,advice.method)
        assertFalse(advice.availableInSelectedVersion!!)
        assertEquals(CompletionDifficulty.HARD,advice.difficulty)
        assertTrue(advice.title.contains("Troca",true) || advice.title.contains("HOME",true))
        assertTrue(advice.detail?.contains("exclusivo de Scarlet",true)==true)
    }

    @Test
    fun sameVersionExclusiveIsMarkedAvailable() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!
        val version=VersionAvailabilityCatalog.forPokemon(936,context,true)
        val canonical=canonicalWild(936,context,version)

        val advice=CompletionAdviceResolver.resolve(
            pokemonId=936,
            context=context,
            routes=emptyList(),
            selectedVersion="Scarlet",
            canonical=canonical
        )

        assertEquals(true,advice.availableInSelectedVersion)
        assertTrue(advice.detail?.contains("Disponível na sua versão")==true)
    }
}
