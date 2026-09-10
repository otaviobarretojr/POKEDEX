package com.otaviobarreto.pokedex.data

data class TypeMatchupResult(
    val quadrupleWeak: List<String>,
    val doubleWeak: List<String>,
    val neutral: List<String>,
    val halfResist: List<String>,
    val quarterResist: List<String>,
    val immune: List<String>
)

data class OffensiveCoverageResult(
    val covered: List<String>,
    val uncovered: List<String>,
    val coverageByAttackType: Map<String, List<String>>
)

object TypeMatchup {
    private val types = listOf("Normal","Fire","Water","Electric","Grass","Ice","Fighting","Poison","Ground","Flying","Psychic","Bug","Rock","Ghost","Dragon","Dark","Steel","Fairy")

    private val chart = mapOf(
        "Normal" to mapOf("Rock" to .5, "Ghost" to 0.0, "Steel" to .5),
        "Fire" to mapOf("Fire" to .5,"Water" to .5,"Grass" to 2.0,"Ice" to 2.0,"Bug" to 2.0,"Rock" to .5,"Dragon" to .5,"Steel" to 2.0),
        "Water" to mapOf("Fire" to 2.0,"Water" to .5,"Grass" to .5,"Ground" to 2.0,"Rock" to 2.0,"Dragon" to .5),
        "Electric" to mapOf("Water" to 2.0,"Electric" to .5,"Grass" to .5,"Ground" to 0.0,"Flying" to 2.0,"Dragon" to .5),
        "Grass" to mapOf("Fire" to .5,"Water" to 2.0,"Grass" to .5,"Poison" to .5,"Ground" to 2.0,"Flying" to .5,"Bug" to .5,"Rock" to 2.0,"Dragon" to .5,"Steel" to .5),
        "Ice" to mapOf("Fire" to .5,"Water" to .5,"Grass" to 2.0,"Ice" to .5,"Ground" to 2.0,"Flying" to 2.0,"Dragon" to 2.0,"Steel" to .5),
        "Fighting" to mapOf("Normal" to 2.0,"Ice" to 2.0,"Poison" to .5,"Flying" to .5,"Psychic" to .5,"Bug" to .5,"Rock" to 2.0,"Ghost" to 0.0,"Dark" to 2.0,"Steel" to 2.0,"Fairy" to .5),
        "Poison" to mapOf("Grass" to 2.0,"Poison" to .5,"Ground" to .5,"Rock" to .5,"Ghost" to .5,"Steel" to 0.0,"Fairy" to 2.0),
        "Ground" to mapOf("Fire" to 2.0,"Electric" to 2.0,"Grass" to .5,"Poison" to 2.0,"Flying" to 0.0,"Bug" to .5,"Rock" to 2.0,"Steel" to 2.0),
        "Flying" to mapOf("Electric" to .5,"Grass" to 2.0,"Fighting" to 2.0,"Bug" to 2.0,"Rock" to .5,"Steel" to .5),
        "Psychic" to mapOf("Fighting" to 2.0,"Poison" to 2.0,"Psychic" to .5,"Dark" to 0.0,"Steel" to .5),
        "Bug" to mapOf("Fire" to .5,"Grass" to 2.0,"Fighting" to .5,"Poison" to .5,"Flying" to .5,"Psychic" to 2.0,"Ghost" to .5,"Dark" to 2.0,"Steel" to .5,"Fairy" to .5),
        "Rock" to mapOf("Fire" to 2.0,"Ice" to 2.0,"Fighting" to .5,"Ground" to .5,"Flying" to 2.0,"Bug" to 2.0,"Steel" to .5),
        "Ghost" to mapOf("Normal" to 0.0,"Psychic" to 2.0,"Ghost" to 2.0,"Dark" to .5),
        "Dragon" to mapOf("Dragon" to 2.0,"Steel" to .5,"Fairy" to 0.0),
        "Dark" to mapOf("Fighting" to .5,"Psychic" to 2.0,"Ghost" to 2.0,"Dark" to .5,"Fairy" to .5),
        "Steel" to mapOf("Fire" to .5,"Water" to .5,"Electric" to .5,"Ice" to 2.0,"Rock" to 2.0,"Steel" to .5,"Fairy" to 2.0),
        "Fairy" to mapOf("Fire" to .5,"Fighting" to 2.0,"Poison" to .5,"Dragon" to 2.0,"Dark" to 2.0,"Steel" to .5)
    )

    fun defensiveFor(defendingTypes: List<String>): TypeMatchupResult {
        val normalized = defendingTypes.map { canonical(it) }.filter { it in types }
        val multipliers = types.associateWith { attacking ->
            normalized.fold(1.0) { acc, defender -> acc * (chart[attacking]?.get(defender) ?: 1.0) }
        }
        fun names(value: Double) = multipliers.filterValues { kotlin.math.abs(it - value) < 0.001 }.keys.sorted()
        return TypeMatchupResult(
            quadrupleWeak = names(4.0),
            doubleWeak = names(2.0),
            neutral = names(1.0),
            halfResist = names(.5),
            quarterResist = names(.25),
            immune = names(0.0)
        )
    }

    fun superEffectiveAgainst(attackingType: String): List<String> {
        val attack = canonical(attackingType)
        return types.filter { defender -> (chart[attack]?.get(defender) ?: 1.0) > 1.0 }
    }

    fun offensiveCoverageFor(attackingTypes: List<String>): OffensiveCoverageResult {
        val normalized = attackingTypes.map(::canonical).filter { it in types }.distinct()
        val byType = normalized.associateWith(::superEffectiveAgainst)
        val covered = byType.values.flatten().distinct().sorted()
        val uncovered = types.filterNot { it in covered }
        return OffensiveCoverageResult(covered, uncovered, byType)
    }

    fun allTypes(): List<String> = types

    private fun canonical(value:String):String = value.trim().replaceFirstChar { it.uppercase() }
}
