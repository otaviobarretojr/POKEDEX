package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class EvolutionHisuiLocationAlternativesTest {

    @Test
    fun magnezoneAndProbopassKeepStoneAndCoronetRoutesInHisui() {
        val hisui=GameContext.fromSource("Legends Arceus · Hisui")

        listOf(462,476).forEach{target->
            val routes=EvolutionCuratedCatalog.rulesFor(target,hisui)
            assertEquals("#$target",2,routes.size)
            assertTrue(routes.any{it.requirement.contains("Thunder Stone")})
            assertTrue(routes.any{it.requirement.contains("Coronet Highlands")})
            routes.forEach{route->
                val methods=PokeApiService.auditFallbackMethods(route.requirement)
                if(route.requirement.contains("Thunder Stone")){
                    assertTrue(PokeApiService.EvolutionMethod.ITEM in methods)
                }
                if(route.requirement.contains("Coronet Highlands")){
                    assertTrue(PokeApiService.EvolutionMethod.LOCATION in methods)
                }
            }
        }
    }

    @Test
    fun leafeonAndGlaceonKeepStoneAndRockRoutesInHisui() {
        val hisui=GameContext.fromSource("Legends Arceus · Hisui")

        val leafeon=EvolutionCuratedCatalog.rulesFor(470,hisui)
        assertEquals(2,leafeon.size)
        assertTrue(leafeon.any{it.requirement.contains("Leaf Stone")})
        assertTrue(leafeon.any{it.requirement.contains("Moss Rock")})

        val glaceon=EvolutionCuratedCatalog.rulesFor(471,hisui)
        assertEquals(2,glaceon.size)
        assertTrue(glaceon.any{it.requirement.contains("Ice Stone")})
        assertTrue(glaceon.any{it.requirement.contains("Ice Rock")})

        (leafeon+glaceon).forEach{route->
            val methods=PokeApiService.auditFallbackMethods(route.requirement)
            if(route.requirement.contains("Stone")){
                assertTrue(route.requirement,PokeApiService.EvolutionMethod.ITEM in methods)
            }
            if(route.requirement.contains("Rock")){
                assertTrue(route.requirement,PokeApiService.EvolutionMethod.LOCATION in methods)
            }
        }
    }

    @Test
    fun bdspGlaceonDoesNotExposeUnavailableIceStoneRoute() {
        val bdsp=GameContext.fromSource("Brilliant Diamond / Shining Pearl · Sinnoh")
        val routes=EvolutionCuratedCatalog.rulesFor(471,bdsp)
        assertEquals(1,routes.size)
        assertTrue(routes.single().requirement.contains("Icy Rock"))
        assertFalse(routes.single().requirement.contains("Ice Stone"))
    }
}
