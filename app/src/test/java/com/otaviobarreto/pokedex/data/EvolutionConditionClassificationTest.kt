package com.otaviobarreto.pokedex.data

import org.junit.Assert.*
import org.junit.Test

class EvolutionConditionClassificationTest {

    @Test
    fun friendshipAndTimeRemainSeparateFilterDimensions() {
        val day=PokeApiService.auditFallbackMethods("Alta amizade • Subir de nível durante o dia")
        assertTrue(PokeApiService.EvolutionMethod.FRIENDSHIP in day)
        assertTrue(PokeApiService.EvolutionMethod.TIME in day)

        val night=PokeApiService.auditFallbackMethods("Alta amizade • Subir de nível durante a noite")
        assertTrue(PokeApiService.EvolutionMethod.FRIENDSHIP in night)
        assertTrue(PokeApiService.EvolutionMethod.TIME in night)
    }

    @Test
    fun pokemonAmieCountsAsAffectionNotGenericOther() {
        val methods=PokeApiService.auditFallbackMethods(
            "Subir de nível conhecendo um golpe do tipo Fairy • Ter pelo menos 2 corações de Affection no Pokémon-Amie"
        )
        assertTrue(PokeApiService.EvolutionMethod.FRIENDSHIP in methods)
        assertTrue(PokeApiService.EvolutionMethod.MOVE in methods)
        assertFalse(PokeApiService.EvolutionMethod.OTHER in methods)
    }

    @Test
    fun environmentalRoutesAreClassifiedAsLocation() {
        listOf(
            "Subir de nível em Mt. Coronet",
            "Subir de nível próximo à Mossy Rock em Eterna Forest",
            "Subir de nível próximo à Icy Rock na Route 217",
            "Galarian Yamask deve perder pelo menos 49 HP sem desmaiar • Passar sob o arco de pedra em Dusty Bowl",
            "Galarian Yamask deve perder pelo menos 49 HP sem desmaiar • Passar sob uma das pontes do Coulant Waterway",
            "Subir ao nível 50 ou mais enquanto estiver chovendo"
        ).forEach{requirement->
            val methods=PokeApiService.auditFallbackMethods(requirement)
            assertTrue(requirement,PokeApiService.EvolutionMethod.LOCATION in methods)
        }
    }

    @Test
    fun legendsArceusTradeReplacementsAreItemsNotTrades() {
        val hisui=GameContext.fromSource("Legends Arceus · Hisui")
        val targets=listOf(65,68,76,94,208,212,233,464,466,467,474,477)
        targets.forEach{target->
            val rule=EvolutionCuratedCatalog.ruleFor(target,hisui)
            assertNotNull("#$target",rule)
            val methods=PokeApiService.auditFallbackMethods(requireNotNull(rule).requirement)
            assertTrue("#$target "+rule.requirement,PokeApiService.EvolutionMethod.ITEM in methods)
            assertFalse("#$target should not require trade in Hisui",PokeApiService.EvolutionMethod.TRADE in methods)
        }
    }

    @Test
    fun genderSensitiveRequirementsStayGenderSensitive() {
        listOf(
            "Usar Dawn Stone em Kirlia macho",
            "Usar Dawn Stone em Snorunt fêmea",
            "Salandit fêmea sobe ao nível 33",
            "Burmy macho sobe ao nível 20"
        ).forEach{requirement->
            assertTrue(
                requirement,
                PokeApiService.EvolutionMethod.GENDER in PokeApiService.auditFallbackMethods(requirement)
            )
        }
    }

    @Test
    fun knownMoveAndSpecialActionRemainDistinct() {
        val move=PokeApiService.auditFallbackMethods("Subir de nível conhecendo Dragon Cheer")
        assertTrue(PokeApiService.EvolutionMethod.MOVE in move)

        val action=PokeApiService.auditFallbackMethods("Caminhar 1.000 passos com Pawmo no modo Let's Go • Depois subir de nível")
        assertTrue(PokeApiService.EvolutionMethod.ACTION in action)
    }
}
