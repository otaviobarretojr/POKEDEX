package com.otaviobarreto.pokedex.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class VariantCollectionIdentityRegressionTest {
    @Test
    fun formsSharingPokemonIdRemainIndependent() {
        val normal = OwnedPokemonVariant(
            source = "paldea",
            speciesId = 869,
            formPokemonId = 869,
            formName = "Alcremie Ruby Cream",
            shiny = false,
            formKey = "alcremie-ruby-cream"
        )
        val alternate = OwnedPokemonVariant(
            source = "paldea",
            speciesId = 869,
            formPokemonId = 869,
            formName = "Alcremie Matcha Cream",
            shiny = false,
            formKey = "alcremie-matcha-cream"
        )
        val shiny = normal.copy(shiny = true)

        assertNotEquals(normal.key, alternate.key)
        assertNotEquals(normal.key, shiny.key)
        assertEquals("alcremie-ruby-cream", normal.formKey)
    }
}
