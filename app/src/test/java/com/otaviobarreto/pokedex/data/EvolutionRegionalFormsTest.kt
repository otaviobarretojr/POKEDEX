package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class EvolutionRegionalFormsTest {

    @Test
    fun galarRegionalEvolutionTargetsCarryCorrectFormKeys() {
        val galar=GameContext.fromSource("Sword / Shield · Galar")

        assertEquals(null to "weezing-galar",EvolutionCuratedCatalog.formKeysFor(110,galar))
        assertEquals("mime-jr" to "mr-mime-galar",EvolutionCuratedCatalog.formKeysFor(122,galar))
        assertEquals("zigzagoon-galar" to "linoone-galar",EvolutionCuratedCatalog.formKeysFor(264,galar))
        assertEquals("darumaka-galar" to "darmanitan-galar",EvolutionCuratedCatalog.formKeysFor(555,galar))
        assertEquals("linoone-galar" to null,EvolutionCuratedCatalog.formKeysFor(862,galar))
        assertEquals("meowth-galar" to null,EvolutionCuratedCatalog.formKeysFor(863,galar))
        assertEquals("corsola-galar" to null,EvolutionCuratedCatalog.formKeysFor(864,galar))
        assertEquals("mr-mime-galar" to null,EvolutionCuratedCatalog.formKeysFor(866,galar))
    }

    @Test
    fun hisuiRegionalEvolutionTargetsCarryCorrectFormKeys() {
        val hisui=GameContext.fromSource("Legends Arceus · Hisui")

        assertEquals(null to "typhlosion-hisui",EvolutionCuratedCatalog.formKeysFor(157,hisui))
        assertEquals(null to "samurott-hisui",EvolutionCuratedCatalog.formKeysFor(503,hisui))
        assertEquals(null to "lilligant-hisui",EvolutionCuratedCatalog.formKeysFor(549,hisui))
        assertEquals(null to "braviary-hisui",EvolutionCuratedCatalog.formKeysFor(628,hisui))
        assertEquals("goomy" to "sliggoo-hisui",EvolutionCuratedCatalog.formKeysFor(705,hisui))
        assertEquals("sliggoo-hisui" to "goodra-hisui",EvolutionCuratedCatalog.formKeysFor(706,hisui))
        assertEquals("bergmite" to "avalugg-hisui",EvolutionCuratedCatalog.formKeysFor(713,hisui))
        assertEquals(null to "decidueye-hisui",EvolutionCuratedCatalog.formKeysFor(724,hisui))
    }

    @Test
    fun sameSpeciesOutsideRegionalContextDoesNotInheritRegionalTarget() {
        val sv=GameContext.fromSource("Scarlet / Violet · Paldea")
        assertEquals(null to null,EvolutionCuratedCatalog.formKeysFor(110,sv))
        assertEquals(null to null,EvolutionCuratedCatalog.formKeysFor(157,sv))
    }
}
