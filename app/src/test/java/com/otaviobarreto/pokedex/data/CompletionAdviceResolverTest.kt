package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class CompletionAdviceResolverTest {

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
        contextLabel="Game",
        regionLabel="Region"
    )

    @Test
    fun evolutionAdviceUsesResolvedMethodAndRequirement() {
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
        assertEquals(906,advice.sourcePokemonId)
    }

    @Test
    fun unknownAcquisitionStaysBlankInsteadOfInventingFallbackCopy() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!
        val advice=CompletionAdviceResolver.resolve(
            pokemonId=936,
            context=context,
            routes=emptyList()
        )

        assertTrue(advice.title.isBlank())
        assertNull(advice.detail)
        assertNull(advice.versionAvailability)
        assertFalse(advice.directAcquisition)
        assertEquals(Int.MAX_VALUE,advice.score)
    }

    @Test
    fun transferOnlyRouteBecomesHomeAdvice() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!
        val advice=CompletionAdviceResolver.resolve(
            pokemonId=899,
            context=context,
            routes=listOf(
                route(
                    234,
                    899,
                    emptySet(),
                    "Evolua em Legends: Arceus e transfira pelo Pokémon HOME",
                    EvolutionAvailability.TRANSFER_ONLY
                )
            )
        )

        assertEquals(CompletionMethodKind.TRANSFER,advice.method)
        assertTrue(advice.title.contains("HOME"))
    }

    @Test
    fun bestRouteOrderingPrioritizesSimpleCompletionPaths() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!
        val level=CompletionAdviceResolver.resolve(
            2,context,listOf(route(1,2,setOf(PokeApiService.EvolutionMethod.LEVEL),"Nível 16"))
        )
        val trade=CompletionAdviceResolver.resolve(
            4,context,listOf(route(3,4,setOf(PokeApiService.EvolutionMethod.TRADE),"Troca"))
        )
        val transfer=CompletionAdviceResolver.resolve(
            6,context,listOf(route(5,6,emptySet(),"HOME",EvolutionAvailability.TRANSFER_ONLY))
        )

        assertTrue(CompletionAdviceResolver.priority(level) < CompletionAdviceResolver.priority(trade))
        assertTrue(CompletionAdviceResolver.priority(trade) < CompletionAdviceResolver.priority(transfer))
    }
}
