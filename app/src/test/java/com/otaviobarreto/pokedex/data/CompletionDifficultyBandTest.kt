package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class CompletionDifficultyBandTest {

    @Test
    fun scoreBandsAreHumanReadable() {
        assertEquals(CompletionDifficulty.VERY_EASY,CompletionAdviceResolver.difficultyFor(1))
        assertEquals(CompletionDifficulty.EASY,CompletionAdviceResolver.difficultyFor(4))
        assertEquals(CompletionDifficulty.MODERATE,CompletionAdviceResolver.difficultyFor(8))
        assertEquals(CompletionDifficulty.HARD,CompletionAdviceResolver.difficultyFor(12))
        assertEquals(CompletionDifficulty.EXTERNAL,CompletionAdviceResolver.difficultyFor(20))
    }

    @Test
    fun alternativeOnlyAppearsInsideSameDifficultyBand() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!
        val level=EvolutionRoute(
            sourcePokemonId=1,
            targetPokemonId=2,
            methods=setOf(PokeApiService.EvolutionMethod.LEVEL),
            summary="Nível 16",
            detail="Nível 16",
            availability=EvolutionAvailability.AVAILABLE_WITH_CONDITION,
            contextLabel="Scarlet / Violet",
            regionLabel="Paldea"
        )
        val canonical=CanonicalAvailability(
            pokemonId=2,
            context=context,
            inRegionalDex=true,
            regionalNumber=1,
            version=null,
            acquisitionKind=CanonicalAcquisitionKind.HOME_TRANSFER,
            acquisitionLabel="Transferência / HOME",
            confidence=AvailabilityConfidence.CONFIRMED
        )

        val advice=CompletionAdviceResolver.resolve(
            pokemonId=2,
            context=context,
            routes=listOf(level),
            owned=setOf(1),
            canonical=canonical
        )

        assertEquals(CompletionDifficulty.VERY_EASY,advice.difficulty)
        assertNull(advice.alternative)
    }

    @Test
    fun sameBandAlternativeCanRemainVisible() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")!!
        val level=EvolutionRoute(
            sourcePokemonId=1,
            targetPokemonId=2,
            methods=setOf(PokeApiService.EvolutionMethod.LEVEL),
            summary="Nível 35",
            detail="Nível 35",
            availability=EvolutionAvailability.AVAILABLE_WITH_CONDITION,
            contextLabel="Scarlet / Violet",
            regionLabel="Paldea"
        )
        val canonical=CanonicalAvailability(
            pokemonId=2,
            context=context,
            inRegionalDex=true,
            regionalNumber=1,
            version=null,
            acquisitionKind=CanonicalAcquisitionKind.WILD,
            acquisitionLabel="Captura selvagem em Paldea",
            locations=listOf("South Province"),
            confidence=AvailabilityConfidence.PARTIAL
        )

        val advice=CompletionAdviceResolver.resolve(
            pokemonId=2,
            context=context,
            routes=listOf(level),
            owned=setOf(1),
            canonical=canonical
        )

        assertEquals(CompletionDifficulty.EASY,advice.difficulty)
        assertNotNull(advice.alternative)
        assertEquals(CompletionDifficulty.EASY,advice.alternative?.difficulty)
    }
}
