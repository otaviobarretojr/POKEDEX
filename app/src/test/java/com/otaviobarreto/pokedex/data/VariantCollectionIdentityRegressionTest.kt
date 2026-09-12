package com.otaviobarreto.pokedex.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VariantCollectionIdentityRegressionTest {
    private fun variant(
        formName: String = "Alcremie Ruby Cream",
        shiny: Boolean = false,
        formKey: String = "alcremie-ruby-cream"
    ) = OwnedPokemonVariant(
        source = "paldea",
        speciesId = 869,
        formPokemonId = 869,
        formName = formName,
        shiny = shiny,
        formKey = formKey
    )

    @Test
    fun formsSharingPokemonIdRemainIndependent() {
        val normal = variant()
        val alternate = variant(
            formName = "Alcremie Matcha Cream",
            formKey = "alcremie-matcha-cream"
        )
        val shiny = normal.copy(shiny = true)

        assertNotEquals(normal.key, alternate.key)
        assertNotEquals(normal.key, shiny.key)
        assertEquals("alcremie-ruby-cream", normal.formKey)
    }

    @Test
    fun normalAndShinyNeverCollapseIntoSameIdentity() {
        val normal = variant(shiny = false)
        val shiny = variant(shiny = true)

        assertFalse(sameOwnedVariantIdentity(normal, shiny))
        assertFalse(sameOwnedVariantIdentity(shiny, normal))
    }

    @Test
    fun legacyIdentityCanMigrateToCanonicalFormKeyOnInteraction() {
        val legacy = variant(formKey = "alcremie ruby cream")
        val canonical = variant(formKey = "alcremie-ruby-cream")

        assertNotEquals(legacy.key, canonical.key)
        assertTrue(sameOwnedVariantIdentity(legacy, canonical))
    }

    @Test
    fun distinctCosmeticFormsSharingPokemonIdStayIndependent() {
        val ruby = variant(
            formName = "Alcremie Ruby Cream",
            formKey = "alcremie-ruby-cream"
        )
        val matcha = variant(
            formName = "Alcremie Matcha Cream",
            formKey = "alcremie-matcha-cream"
        )

        assertFalse(sameOwnedVariantIdentity(ruby, matcha))
    }
}
