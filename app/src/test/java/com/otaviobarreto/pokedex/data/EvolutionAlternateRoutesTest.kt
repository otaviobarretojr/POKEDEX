package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class EvolutionAlternateRoutesTest {

    @Test
    fun bdspPreservesMultipleValidRoutes() {
        val context=GameContext.fromSource("Brilliant Diamond / Shining Pearl · Sinnoh")

        val magnezone=EvolutionCuratedCatalog.rulesFor(462,context)
        assertEquals(2,magnezone.size)
        assertTrue(magnezone.any{it.requirement.contains("Thunder Stone")})
        assertTrue(magnezone.any{it.requirement.contains("Mt. Coronet")})

        val leafeon=EvolutionCuratedCatalog.rulesFor(470,context)
        assertEquals(2,leafeon.size)
        assertTrue(leafeon.any{it.requirement.contains("Leaf Stone")})
        assertTrue(leafeon.any{it.requirement.contains("Mossy Rock")})

        val glaceon=EvolutionCuratedCatalog.rulesFor(471,context)
        assertEquals(2,glaceon.size)
        assertTrue(glaceon.any{it.requirement.contains("Ice Stone")})
        assertTrue(glaceon.any{it.requirement.contains("Icy Rock")})
    }

    @Test
    fun swordShieldUsesModernStoneRoutes() {
        val context=GameContext.fromSource("Sword / Shield · Galar")

        assertEquals(
            listOf("Usar Thunder Stone"),
            EvolutionCuratedCatalog.rulesFor(462,context).map{it.requirement}
        )
        assertEquals(
            listOf("Usar Leaf Stone"),
            EvolutionCuratedCatalog.rulesFor(470,context).map{it.requirement}
        )
        assertEquals(
            listOf("Usar Ice Stone"),
            EvolutionCuratedCatalog.rulesFor(471,context).map{it.requirement}
        )
    }

    @Test
    fun kubfuBranchesStayIndependentByGame() {
        val galar=GameContext.fromSource("Sword / Shield · Isle of Armor")
        val sv=GameContext.fromSource("Scarlet / Violet · Blueberry")

        val galarRoutes=EvolutionCuratedCatalog.rulesFor(892,galar)
        assertEquals(2,galarRoutes.size)
        assertTrue(galarRoutes.any{it.requirement.contains("Tower of Darkness")})
        assertTrue(galarRoutes.any{it.requirement.contains("Tower of Waters")})

        val svRoutes=EvolutionCuratedCatalog.rulesFor(892,sv)
        assertEquals(2,svRoutes.size)
        assertTrue(svRoutes.any{it.requirement=="Usar Scroll of Darkness"})
        assertTrue(svRoutes.any{it.requirement=="Usar Scroll of Waters"})
    }

    @Test
    fun zaAndSwordShieldRunerigusDoNotShareLocationText() {
        val za=GameContext.fromSource("Pokémon Legends: Z-A · Lumiose")
        val galar=GameContext.fromSource("Sword / Shield · Galar")

        assertTrue(EvolutionCuratedCatalog.ruleFor(867,za)?.requirement?.contains("Coulant Waterway")==true)
        assertTrue(EvolutionCuratedCatalog.ruleFor(867,galar)?.requirement?.contains("Dusty Bowl")==true)
    }

    @Test
    fun xYSylveonKeepsPokemonAmieRuleWhileModernGamesUseFriendship() {
        val xy=GameContext.fromSource("X / Y · Kalos Central")
        val swsh=GameContext.fromSource("Sword / Shield · Galar")

        assertTrue(EvolutionCuratedCatalog.ruleFor(700,xy)?.requirement?.contains("Pokémon-Amie")==true)
        assertTrue(EvolutionCuratedCatalog.ruleFor(700,swsh)?.requirement?.contains("Alta amizade")==true)
    }
}
