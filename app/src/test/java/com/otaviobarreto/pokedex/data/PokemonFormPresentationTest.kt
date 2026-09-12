package com.otaviobarreto.pokedex.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PokemonFormPresentationTest {
    @Test
    fun regionalAndBattleLabelsAreLocalized() {
        assertEquals("Forma de Alola", PokemonFormPresentation.label("Raichu", "Raichu Alola"))
        assertEquals("Mega X · Shiny", PokemonFormPresentation.label("Charizard", "Charizard Mega X", true))
        assertEquals("Ataque", PokemonFormPresentation.label("Deoxys", "Deoxys Attack"))
    }

    @Test
    fun everyKindHasHumanReadableCategory() {
        PokemonFormKind.entries.forEach {
            assertTrue(PokemonFormPresentation.categoryLabel(it).isNotBlank())
            assertTrue(PokemonFormPresentation.behaviorLabel(it).isNotBlank())
        }
    }
}
