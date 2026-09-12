package com.otaviobarreto.pokedex.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NationalDexRegressionTest {

    @Test
    fun nationalDexContainsEverySpeciesInCanonicalOrder() {
        val all = NationalDexCatalog.all
        assertEquals(1025, all.size)
        assertEquals((1..1025).toList(), all.map { it.id })
        assertEquals(1025, all.map { it.identifier }.distinct().size)
    }

    @Test
    fun repositoryUsesFullNationalDexOrder() {
        val all = PokemonRepository.all()
        assertEquals(1025, all.size)
        assertEquals((1..1025).toList(), all.map { it.id })
        assertEquals("Bulbasaur", all.first().name)
        assertEquals("Pecharunt", all.last().name)
    }

    @Test
    fun generationBoundariesRemainCorrect() {
        val expectedStarts = mapOf(
            1 to 1,
            2 to 152,
            3 to 252,
            4 to 387,
            5 to 494,
            6 to 650,
            7 to 722,
            8 to 810,
            9 to 906
        )
        expectedStarts.forEach { (generation, firstId) ->
            assertEquals(
                "Generation $generation start",
                firstId,
                NationalDexCatalog.all.first { it.generation == generation }.id
            )
        }
    }

    @Test
    fun knownNamesWithPunctuationAreHumanReadable() {
        val expected = mapOf(
            29 to "Nidoran♀",
            32 to "Nidoran♂",
            83 to "Farfetch’d",
            122 to "Mr. Mime",
            250 to "Ho-Oh",
            474 to "Porygon-Z",
            772 to "Type: Null",
            865 to "Sirfetch’d",
            866 to "Mr. Rime"
        )
        expected.forEach { (id, name) ->
            assertEquals(name, NationalDexCatalog.find(id)?.displayName)
        }
    }

    @Test
    fun nationalDexArtworkUrlsAreStableAndAddressableById() {
        PokemonRepository.all().forEach { pokemon ->
            assertTrue(pokemon.spriteUrl.endsWith("/" + pokemon.id + ".png"))
        }
    }
}
