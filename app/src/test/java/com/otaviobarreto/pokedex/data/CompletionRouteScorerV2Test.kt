package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class CompletionRouteScorerV2Test {

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

    private fun canonical(
        id:Int,
        context:GameContext,
        kind:CanonicalAcquisitionKind,
        label:String,
        confidence:AvailabilityConfidence=AvailabilityConfidence.CONFIRMED
    )=CanonicalAvailability(
        pokemonId=id,
        context=context,
        inRegionalDex=true,
        regionalNumber=1,
        version=null,
        acquisitionKind=kind,
        acquisitionLabel=label,
        confidence=confidence
    )

    @Test
    fun highLevelEvolutionCanLoseToConfirmedWildCapture() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!
        val advice=CompletionAdviceResolver.resolve(
            pokemonId=2,
            context=context,
            routes=listOf(route(1,2,setOf(PokeApiService.EvolutionMethod.LEVEL),"Nível 55")),
            owned=setOf(1),
            names=mapOf(1 to "Base"),
            canonical=canonical(2,context,CanonicalAcquisitionKind.WILD,"Captura selvagem")
        )

        assertEquals(CompletionMethodKind.DIRECT,advice.method)
        assertNotNull(advice.alternative)
        assertEquals(CompletionMethodKind.LEVEL,advice.alternative?.method)
    }

    @Test
    fun lowLevelEvolutionCanBeatWildCaptureWhenSourceOwned() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!
        val advice=CompletionAdviceResolver.resolve(
            pokemonId=2,
            context=context,
            routes=listOf(route(1,2,setOf(PokeApiService.EvolutionMethod.LEVEL),"Nível 16")),
            owned=setOf(1),
            names=mapOf(1 to "Base"),
            canonical=canonical(2,context,CanonicalAcquisitionKind.WILD,"Captura selvagem")
        )

        assertEquals(CompletionMethodKind.LEVEL,advice.method)
        assertEquals(CompletionMethodKind.DIRECT,advice.alternative?.method)
    }

    @Test
    fun partialWildEvidenceDoesNotAlwaysBeatSimpleEvolution() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!
        val advice=CompletionAdviceResolver.resolve(
            pokemonId=2,
            context=context,
            routes=listOf(route(1,2,setOf(PokeApiService.EvolutionMethod.LEVEL),"Nível 20")),
            owned=setOf(1),
            names=mapOf(1 to "Base"),
            canonical=canonical(
                2,context,CanonicalAcquisitionKind.WILD,"Captura selvagem",
                AvailabilityConfidence.PARTIAL
            )
        )

        assertEquals(CompletionMethodKind.LEVEL,advice.method)
    }

    @Test
    fun complexActionGetsHigherCostThanSimpleItem() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!
        val action=CompletionAdviceResolver.resolve(
            pokemonId=2,
            context=context,
            routes=listOf(route(1,2,setOf(PokeApiService.EvolutionMethod.ACTION),"Caminhar 1.000 passos • Depois subir de nível")),
            owned=setOf(1)
        )
        val item=CompletionAdviceResolver.resolve(
            pokemonId=4,
            context=context,
            routes=listOf(route(3,4,setOf(PokeApiService.EvolutionMethod.ITEM),"Usar Thunder Stone")),
            owned=setOf(3)
        )

        assertTrue(CompletionAdviceResolver.priority(item) < CompletionAdviceResolver.priority(action))
    }

    @Test
    fun distantAlternativesAreNotShownAsNoise() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!
        val advice=CompletionAdviceResolver.resolve(
            pokemonId=2,
            context=context,
            routes=listOf(route(1,2,setOf(PokeApiService.EvolutionMethod.LEVEL),"Nível 16")),
            owned=setOf(1),
            canonical=canonical(2,context,CanonicalAcquisitionKind.HOME_TRANSFER,"Transferência / HOME")
        )

        assertEquals(CompletionMethodKind.LEVEL,advice.method)
        assertNull(advice.alternative)
    }
}
