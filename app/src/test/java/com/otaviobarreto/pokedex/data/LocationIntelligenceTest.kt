package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class LocationIntelligenceTest {

    @Test fun scarletVioletSubregionsStaySeparated() {
        val paldea=GameContext.fromSource("Scarlet / Violet · Paldea")
        val kitakami=GameContext.fromSource("Scarlet / Violet · Kitakami")
        val blueberry=GameContext.fromSource("Scarlet / Violet · Blueberry")

        assertTrue(LocationIntelligence.belongsToContext("South Province Area Two",paldea))
        assertFalse(LocationIntelligence.belongsToContext("Kitakami Wilds",paldea))
        assertFalse(LocationIntelligence.belongsToContext("Savanna Biome",paldea))

        assertTrue(LocationIntelligence.belongsToContext("Kitakami Wilds",kitakami))
        assertFalse(LocationIntelligence.belongsToContext("South Province Area Two",kitakami))

        assertTrue(LocationIntelligence.belongsToContext("Savanna Biome",blueberry))
        assertFalse(LocationIntelligence.belongsToContext("Kitakami Wilds",blueberry))
    }

    @Test fun swordShieldSubregionsStaySeparated() {
        val galar=GameContext.fromSource("Sword / Shield · Galar")
        val armor=GameContext.fromSource("Sword / Shield · Isle of Armor")
        val crown=GameContext.fromSource("Sword / Shield · Crown Tundra")

        assertTrue(LocationIntelligence.belongsToContext("Galar Route 2",galar))
        assertFalse(LocationIntelligence.belongsToContext("Fields of Honor",galar))
        assertFalse(LocationIntelligence.belongsToContext("Slippery Slope",galar))

        assertTrue(LocationIntelligence.belongsToContext("Fields of Honor",armor))
        assertTrue(LocationIntelligence.belongsToContext("Slippery Slope",crown))
    }

    @Test fun locationNamesUseOfficialAreaStyle() {
        assertEquals(
            "South Province (Area Two)",
            LocationIntelligence.displayName("South Province Area Two")
        )
        assertEquals(
            "Warm-Up Tunnel",
            LocationIntelligence.displayName("Warm Up Tunnel")
        )
    }

    @Test fun modernGamesDoNotClaimAbsenceWhenSourceIsIncomplete() {
        val paldea=GameContext.fromSource("Scarlet / Violet · Paldea")
        val message=LocationIntelligence.emptyMessage(paldea,false)
        assertTrue(message.contains("não estão disponíveis"))
    }
}
