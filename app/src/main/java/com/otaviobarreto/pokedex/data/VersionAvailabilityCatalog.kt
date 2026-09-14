package com.otaviobarreto.pokedex.data

enum class VersionAvailabilityKind { SHARED, EXCLUSIVE, SPLIT_FORMS, UNAVAILABLE }

data class VersionAvailability(
    val kind: VersionAvailabilityKind,
    val title: String,
    val subtitle: String,
    val versionA: String,
    val versionB: String,
    val exclusiveVersion: String? = null
)

object VersionAvailabilityCatalog {

    fun forPokemon(pokemonId:Int, context:GameContext?):VersionAvailability? {
        val label=context?.label ?: return null
        val pair=pairFor(label) ?: return null
        splitForms[label]?.get(pokemonId)?.let { note ->
            return VersionAvailability(
                kind=VersionAvailabilityKind.SPLIT_FORMS,
                title="Exclusividade por forma",
                subtitle=note,
                versionA=pair.first,
                versionB=pair.second
            )
        }
        val exclusives=exclusiveIds[label] ?: return null
        if(!belongsToRegionalDex(pokemonId,context)) return VersionAvailability(
            kind=VersionAvailabilityKind.UNAVAILABLE,
            title="Fora desta Pokédex regional",
            subtitle="Esta espécie não pertence à Pokédex "+context.regionLabel+". Consulte outra região/DLC ou a compatibilidade por transferência.",
            versionA=pair.first,
            versionB=pair.second
        )
        val exclusiveVersion=when {
            pokemonId in exclusives.first -> pair.first
            pokemonId in exclusives.second -> pair.second
            else -> null
        }
        return if(exclusiveVersion!=null){
            VersionAvailability(
                kind=VersionAvailabilityKind.EXCLUSIVE,
                title="Exclusivo "+exclusiveVersion,
                subtitle="Para obter na outra versão, use troca, multiplayer ou transferência quando compatível.",
                versionA=pair.first,
                versionB=pair.second,
                exclusiveVersion=exclusiveVersion
            )
        }else{
            VersionAvailability(
                kind=VersionAvailabilityKind.SHARED,
                title="Sem exclusividade de versão",
                subtitle="Não há exclusividade catalogada desta espécie entre "+pair.first+" e "+pair.second+".",
                versionA=pair.first,
                versionB=pair.second
            )
        }
    }

    private fun pairFor(label:String):Pair<String,String>? = when(label){
        "Scarlet / Violet" -> "Scarlet" to "Violet"
        "Sword / Shield" -> "Sword" to "Shield"
        "Let's Go Pikachu / Eevee" -> "Let's Go Pikachu" to "Let's Go Eevee"
        "Brilliant Diamond / Shining Pearl" -> "Brilliant Diamond" to "Shining Pearl"
        "FireRed / LeafGreen" -> "FireRed" to "LeafGreen"
        else -> null
    }

    private fun belongsToRegionalDex(id:Int,context:GameContext):Boolean = when(context.pokedexSlug){
        "paldea" -> id !in scarletVioletDlcOnly
        "kitakami" -> id !in scarletVioletPaldeaOnly && id !in scarletVioletBlueberryOnly
        "blueberry" -> id !in scarletVioletPaldeaOnly && id !in scarletVioletKitakamiOnly
        "galar" -> id !in swordShieldArmorOnly && id !in swordShieldCrownOnly
        "isle-of-armor" -> id !in swordShieldGalarOnly && id !in swordShieldCrownOnly
        "crown-tundra" -> id !in swordShieldGalarOnly && id !in swordShieldArmorOnly
        else -> true
    }

    // Region-specific exclusive species. These guards prevent a DLC exclusive from being
    // presented as if it belonged to the base regional Pokédex.
    private val scarletVioletKitakamiOnly=setOf(37,38,190,207,472)
    private val scarletVioletBlueberryOnly=setOf(408,409,410,411,1020,1021,1022,1023)
    private val scarletVioletDlcOnly=scarletVioletKitakamiOnly+scarletVioletBlueberryOnly
    private val scarletVioletPaldeaOnly=setOf(246,247,248,316,317,371,372,373,425,426,434,435,633,634,635,690,691,692,693,765,766,845,874,875,877,885,886,887,936,937,984,985,986,987,988,989,990,991,992,993,994,995,1005,1006,1007,1008,1009,1010)
    private val swordShieldArmorOnly=setOf(127,214,690,691,692,693)
    private val swordShieldCrownOnly=setOf(138,139,140,141,250,371,372,373,381,383,483,484,641,642,643,644)
    private val swordShieldGalarOnly=setOf(83,77,78,222,246,247,248,273,274,275,302,303,337,338,380,443,444,445,453,454,554,555,574,575,576,577,578,579,629,630,633,634,635,682,683,684,685,704,705,706,716,717,765,766,776,780,782,783,784,791,792,841,842,865,874,875,888,889)

    private val splitForms=mapOf(
        "Scarlet / Violet" to mapOf(
            128 to "Paldean Tauros: Blaze Breed é de Scarlet; Aqua Breed é de Violet."
        ),
        "Sword / Shield" to mapOf(
            876 to "Indeedee macho é associado a Sword; Indeedee fêmea a Shield."
        ),
        "Let's Go Pikachu / Eevee" to mapOf(
            25 to "Partner Pikachu é exclusivo de Let's Go Pikachu; Pikachu comum existe nas duas versões.",
            133 to "Partner Eevee é exclusivo de Let's Go Eevee; Eevee comum existe nas duas versões."
        ),
        "FireRed / LeafGreen" to mapOf(
            386 to "Deoxys assume Attack Forme em FireRed e Defense Forme em LeafGreen."
        )
    )

    private val exclusiveIds=mapOf(
        "Scarlet / Violet" to (
            setOf(
                37,38,207,246,247,248,408,409,425,426,434,435,472,
                633,634,635,690,691,765,845,874,936,
                984,985,986,987,988,989,1005,1007,1009,1020,1021
            ) to
            setOf(
                27,28,190,200,316,317,371,372,373,410,411,424,429,
                692,693,766,875,877,885,886,887,937,
                990,991,992,993,994,995,1006,1008,1010,1022,1023
            )
        ),
        "Sword / Shield" to (
            setOf(
                83,127,138,139,250,273,274,275,303,338,371,372,373,
                381,383,483,554,555,574,575,576,627,628,633,634,635,
                641,643,684,685,692,693,716,766,776,782,783,784,791,
                841,865,874,888
            ) to
            setOf(
                77,78,140,141,214,222,246,247,248,249,270,271,272,
                302,337,380,382,443,444,445,453,454,484,577,578,579,
                629,630,642,644,682,683,690,691,704,705,706,717,765,
                780,792,842,875,889
            )
        ),
        "Let's Go Pikachu / Eevee" to (
            setOf(27,28,43,44,45,56,57,58,88,89,123) to
            setOf(23,24,37,38,52,53,69,70,71,109,110,127)
        ),
        "Brilliant Diamond / Shining Pearl" to (
            setOf(
                10,11,12,23,24,58,59,86,87,123,125,198,207,212,239,
                243,244,245,246,247,248,273,274,275,303,335,338,352,
                408,409,430,434,435,466,472,483
            ) to
            setOf(
                13,14,15,27,28,37,38,79,80,126,127,144,145,146,199,
                200,216,217,234,240,249,270,271,272,302,336,337,371,
                372,373,410,411,429,431,432,467,484
            )
        ),
        "FireRed / LeafGreen" to (
            setOf(
                23,24,43,44,45,54,55,58,59,90,91,123,125,182,194,
                195,198,211,212,225,227,239
            ) to
            setOf(
                27,28,37,38,69,70,71,79,80,120,121,126,127,183,184,199,200,
                215,223,224,226,240,298
            )
        )
    )
}
