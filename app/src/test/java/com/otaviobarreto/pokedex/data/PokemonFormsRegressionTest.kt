package com.otaviobarreto.pokedex.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PokemonFormsRegressionTest {

    @Test
    fun knownRegionalFormsAreClassifiedAsRegional() {
        val cases = listOf(
            "Raichu Alola",
            "Meowth Galar",
            "Zoroark Hisui",
            "Tauros Paldea Combat Breed",
            "Tauros Paldea Blaze Breed",
            "Tauros Paldea Aqua Breed"
        )
        cases.forEach { name ->
            assertEquals(name, PokemonFormKind.REGIONAL, PokemonFormsService.classify(name, false))
        }
    }

    @Test
    fun megaFormsAreClassifiedAsBattleForms() {
        val cases = listOf(
            "Charizard Mega X",
            "Charizard Mega Y",
            "Mewtwo Mega X",
            "Mewtwo Mega Y"
        )
        cases.forEach { name ->
            assertEquals(name, PokemonFormKind.BATTLE, PokemonFormsService.classify(name, false))
        }
    }

    @Test
    fun defaultFormRemainsDefault() {
        assertEquals(
            PokemonFormKind.DEFAULT,
            PokemonFormsService.classify("Charizard", true)
        )
    }

    @Test
    fun shinyDoesNotChangeUnderlyingFormKind() {
        assertEquals(
            PokemonFormKind.BATTLE,
            PokemonFormsService.classify("Charizard Mega X Shiny", false)
        )
        assertEquals(
            PokemonFormKind.REGIONAL,
            PokemonFormsService.classify("Raichu Alola Shiny", false)
        )
    }
}
