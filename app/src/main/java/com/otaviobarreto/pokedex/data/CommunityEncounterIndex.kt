package com.otaviobarreto.pokedex.data

/**
 * Small reverse encounter index used when the primary API does not expose
 * Scarlet/Violet reverse location data. This is deliberately presented in the
 * UI as a community sample, not as an exhaustive or official encounter table.
 */
data class CommunityAreaEncounter(
    val area: String,
    val pokemonNames: List<String>,
    val levels: String? = null,
    val versionNote: String? = null
)

object CommunityEncounterIndex {
    fun entriesFor(regionLabel: String): List<CommunityAreaEncounter> = when (regionLabel) {
        "Paldea" -> paldea
        "Kitakami" -> kitakami
        "Blueberry" -> blueberry
        else -> emptyList()
    }

    fun entriesForZone(regionLabel: String, zone: RegionMapZone): List<CommunityAreaEncounter> =
        entriesFor(regionLabel).filter { zone.matches(it.area) }

    private val paldea = listOf(
        CommunityAreaEncounter("South Province (Area One)", listOf("Lechonk", "Pawmi", "Tarountula", "Fletchling"), "2–5"),
        CommunityAreaEncounter("South Province (Area Two)", listOf("Bounsweet", "Smoliv", "Hoppip", "Flabébé"), "5–9"),
        CommunityAreaEncounter("South Province (Area Three)", listOf("Maschiff", "Nacli", "Capsakid", "Wooper"), "8–14"),
        CommunityAreaEncounter("South Province (Area Four)", listOf("Drowzee", "Tinkatink", "Shroodle"), "12–18"),
        CommunityAreaEncounter("South Province (Area Five)", listOf("Charcadet", "Klawf", "Sableye"), "18–25"),
        CommunityAreaEncounter("South Province (Area Six)", listOf("Klawf", "Zangoose", "Seviper", "Murkrow"), "25–30", "Zangoose/Seviper variam por versão"),
        CommunityAreaEncounter("East Province (Area One)", listOf("Buizel", "Psyduck", "Wiglett", "Arrokuda"), "10–15"),
        CommunityAreaEncounter("East Province (Area Two)", listOf("Larvitar", "Deino", "Bagon"), "18–25"),
        CommunityAreaEncounter("East Province (Area Three)", listOf("Flittle", "Rellor", "Espathra"), "22–28"),
        CommunityAreaEncounter("West Province (Area One)", listOf("Rockruff", "Nacli", "Fidough", "Azurill"), "12–18"),
        CommunityAreaEncounter("West Province (Area Two)", listOf("Gothita", "Solosis", "Sableye", "Spoink"), "18–25", "Gothita/Solosis variam por versão"),
        CommunityAreaEncounter("West Province (Area Three)", listOf("Varoom", "Shroodle", "Grafaiai"), "25–33"),
        CommunityAreaEncounter("North Province (Area One)", listOf("Frigibax", "Cetoddle", "Orthworm"), "32–40"),
        CommunityAreaEncounter("North Province (Area Two)", listOf("Gogoat", "Passimian", "Oranguru"), "35–43"),
        CommunityAreaEncounter("North Province (Area Three)", listOf("Arctibax", "Dondozo", "Baxcalibur"), "45–55"),
        CommunityAreaEncounter("Asado Desert", listOf("Sandile", "Hippopotas", "Gabite", "Palossand"), "20–28"),
        CommunityAreaEncounter("Glaseado Mountain", listOf("Frigibax", "Sneasel", "Delibird", "Cetoddle"), "40–52"),
        CommunityAreaEncounter("Casseroya Lake", listOf("Dondozo", "Tatsugiri", "Golduck"), "50–58"),
        CommunityAreaEncounter("Area Zero", listOf("Great Tusk", "Sandy Shocks", "Brute Bonnet", "Slither Wing", "Iron Treads", "Iron Bundle", "Iron Hands", "Iron Jugulis"), "55–65", "Paradoxos variam entre Scarlet e Violet")
    )

    private val kitakami = listOf(
        CommunityAreaEncounter("Kitakami Road", listOf("Deerling", "Lotad", "Applin"), "50–56"),
        CommunityAreaEncounter("Oni Mountain", listOf("Mankey", "Primeape", "Annihilape"), "55–60"),
        CommunityAreaEncounter("Crystal Pool", listOf("Ogerpon", "Okidogi", "Munkidori"), "70", "Encontros especiais; disponibilidade depende da progressão/versão")
    )

    private val blueberry = listOf(
        CommunityAreaEncounter("Terarium — Savanna Biome", listOf("Litleo", "Hippopotas", "Sandile"), "60–68"),
        CommunityAreaEncounter("Terarium — Polar Biome", listOf("Frigibax", "Cetoddle", "Snom"), "62–70"),
        CommunityAreaEncounter("Terarium — Canyon Biome", listOf("Larvitar", "Axew", "Dratini"), "60–68"),
        CommunityAreaEncounter("Terarium — Coastal Biome", listOf("Dratini", "Finizen", "Palafin"), "62–70")
    )
}
