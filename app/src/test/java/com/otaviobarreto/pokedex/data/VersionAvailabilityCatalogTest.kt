package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class VersionAvailabilityCatalogTest {

    @Test fun scarletVioletExclusivesResolve() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")
        assertEquals("Scarlet",VersionAvailabilityCatalog.forPokemon(246,context)?.exclusiveVersion)
        assertEquals("Violet",VersionAvailabilityCatalog.forPokemon(371,context)?.exclusiveVersion)
        assertEquals(VersionAvailabilityKind.SPLIT_FORMS,VersionAvailabilityCatalog.forPokemon(128,context)?.kind)
    }

    @Test fun swordShieldExclusivesResolve() {
        val context=GameContext.fromSource("Sword / Shield · Galar")
        assertEquals("Sword",VersionAvailabilityCatalog.forPokemon(865,context)?.exclusiveVersion)
        assertEquals("Shield",VersionAvailabilityCatalog.forPokemon(875,context)?.exclusiveVersion)
    }

    @Test fun letsGoExclusivesResolve() {
        val context=GameContext.fromSource("Let's Go Pikachu / Eevee · Kanto")
        assertEquals("Let's Go Pikachu",VersionAvailabilityCatalog.forPokemon(43,context)?.exclusiveVersion)
        assertEquals("Let's Go Eevee",VersionAvailabilityCatalog.forPokemon(69,context)?.exclusiveVersion)
    }

    @Test fun bdspExclusivesResolve() {
        val context=GameContext.fromSource("Brilliant Diamond / Shining Pearl · Sinnoh")
        assertEquals("Brilliant Diamond",VersionAvailabilityCatalog.forPokemon(483,context)?.exclusiveVersion)
        assertEquals("Shining Pearl",VersionAvailabilityCatalog.forPokemon(484,context)?.exclusiveVersion)
    }

    @Test fun fireRedLeafGreenExclusivesResolve() {
        val context=GameContext.fromSource("FireRed / LeafGreen · Kanto")
        assertEquals("FireRed",VersionAvailabilityCatalog.forPokemon(23,context)?.exclusiveVersion)
        assertEquals("LeafGreen",VersionAvailabilityCatalog.forPokemon(37,context)?.exclusiveVersion)
        assertEquals(VersionAvailabilityKind.SPLIT_FORMS,VersionAvailabilityCatalog.forPokemon(386,context)?.kind)
    }

    @Test fun dlcExclusiveIsNotMislabelledInPaldea() {
        val paldea=GameContext.fromSource("Scarlet / Violet · Paldea")
        val kitakami=GameContext.fromSource("Scarlet / Violet · Kitakami")
        assertEquals(VersionAvailabilityKind.UNAVAILABLE,VersionAvailabilityCatalog.forPokemon(37,paldea,false)?.kind)
        assertEquals("Scarlet",VersionAvailabilityCatalog.forPokemon(37,kitakami,true)?.exclusiveVersion)
    }

    @Test fun fireRedLeafGreenIncludesMarillLine() {
        val context=GameContext.fromSource("FireRed / LeafGreen · Kanto")
        assertEquals("LeafGreen",VersionAvailabilityCatalog.forPokemon(183,context)?.exclusiveVersion)
        assertEquals("LeafGreen",VersionAvailabilityCatalog.forPokemon(184,context)?.exclusiveVersion)
    }

    @Test fun sharedSpeciesAreNotMarkedExclusive() {
        val context=GameContext.fromSource("Scarlet / Violet · Paldea")
        assertEquals(VersionAvailabilityKind.SHARED,VersionAvailabilityCatalog.forPokemon(25,context)?.kind)
    }
}
