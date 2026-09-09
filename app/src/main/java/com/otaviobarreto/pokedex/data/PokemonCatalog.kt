package com.otaviobarreto.pokedex.data

data class PokemonSummary(
    val id: Int,
    val name: String,
    val generation: Int,
    val types: List<String>,
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val specialAttack: Int,
    val specialDefense: Int,
    val speed: Int,
    val abilities: List<String>
) {
    val spriteUrl: String
        get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
}

object PokemonCatalog {
    val all = listOf(
        PokemonSummary(1, "Bulbasaur", 1, listOf("Grass", "Poison"), 45, 49, 49, 65, 65, 45, listOf("Overgrow", "Chlorophyll")),
        PokemonSummary(2, "Ivysaur", 1, listOf("Grass", "Poison"), 60, 62, 63, 80, 80, 60, listOf("Overgrow", "Chlorophyll")),
        PokemonSummary(3, "Venusaur", 1, listOf("Grass", "Poison"), 80, 82, 83, 100, 100, 80, listOf("Overgrow", "Chlorophyll")),
        PokemonSummary(4, "Charmander", 1, listOf("Fire"), 39, 52, 43, 60, 50, 65, listOf("Blaze", "Solar Power")),
        PokemonSummary(5, "Charmeleon", 1, listOf("Fire"), 58, 64, 58, 80, 65, 80, listOf("Blaze", "Solar Power")),
        PokemonSummary(6, "Charizard", 1, listOf("Fire", "Flying"), 78, 84, 78, 109, 85, 100, listOf("Blaze", "Solar Power")),
        PokemonSummary(7, "Squirtle", 1, listOf("Water"), 44, 48, 65, 50, 64, 43, listOf("Torrent", "Rain Dish")),
        PokemonSummary(8, "Wartortle", 1, listOf("Water"), 59, 63, 80, 65, 80, 58, listOf("Torrent", "Rain Dish")),
        PokemonSummary(9, "Blastoise", 1, listOf("Water"), 79, 83, 100, 85, 105, 78, listOf("Torrent", "Rain Dish")),
        PokemonSummary(25, "Pikachu", 1, listOf("Electric"), 35, 55, 40, 50, 50, 90, listOf("Static", "Lightning Rod")),
        PokemonSummary(26, "Raichu", 1, listOf("Electric"), 60, 90, 55, 90, 80, 110, listOf("Static", "Lightning Rod")),
        PokemonSummary(39, "Jigglypuff", 1, listOf("Normal", "Fairy"), 115, 45, 20, 45, 25, 20, listOf("Cute Charm", "Competitive")),
        PokemonSummary(52, "Meowth", 1, listOf("Normal"), 40, 45, 35, 40, 40, 90, listOf("Pickup", "Technician")),
        PokemonSummary(94, "Gengar", 1, listOf("Ghost", "Poison"), 60, 65, 60, 130, 75, 110, listOf("Cursed Body")),
        PokemonSummary(130, "Gyarados", 1, listOf("Water", "Flying"), 95, 125, 79, 60, 100, 81, listOf("Intimidate", "Moxie")),
        PokemonSummary(131, "Lapras", 1, listOf("Water", "Ice"), 130, 85, 80, 85, 95, 60, listOf("Water Absorb", "Shell Armor")),
        PokemonSummary(133, "Eevee", 1, listOf("Normal"), 55, 55, 50, 45, 65, 55, listOf("Run Away", "Adaptability")),
        PokemonSummary(143, "Snorlax", 1, listOf("Normal"), 160, 110, 65, 65, 110, 30, listOf("Immunity", "Thick Fat")),
        PokemonSummary(149, "Dragonite", 1, listOf("Dragon", "Flying"), 91, 134, 95, 100, 100, 80, listOf("Inner Focus", "Multiscale")),
        PokemonSummary(150, "Mewtwo", 1, listOf("Psychic"), 106, 110, 90, 154, 90, 130, listOf("Pressure", "Unnerve")),
        PokemonSummary(151, "Mew", 1, listOf("Psychic"), 100, 100, 100, 100, 100, 100, listOf("Synchronize")),
        PokemonSummary(152, "Chikorita", 2, listOf("Grass"), 45, 49, 65, 49, 65, 45, listOf("Overgrow", "Leaf Guard")),
        PokemonSummary(155, "Cyndaquil", 2, listOf("Fire"), 39, 52, 43, 60, 50, 65, listOf("Blaze", "Flash Fire")),
        PokemonSummary(158, "Totodile", 2, listOf("Water"), 50, 65, 64, 44, 48, 43, listOf("Torrent", "Sheer Force")),
        PokemonSummary(252, "Treecko", 3, listOf("Grass"), 40, 45, 35, 65, 55, 70, listOf("Overgrow", "Unburden")),
        PokemonSummary(255, "Torchic", 3, listOf("Fire"), 45, 60, 40, 70, 50, 45, listOf("Blaze", "Speed Boost")),
        PokemonSummary(258, "Mudkip", 3, listOf("Water"), 50, 70, 50, 50, 50, 40, listOf("Torrent", "Damp")),
        PokemonSummary(387, "Turtwig", 4, listOf("Grass"), 55, 68, 64, 45, 55, 31, listOf("Overgrow", "Shell Armor")),
        PokemonSummary(390, "Chimchar", 4, listOf("Fire"), 44, 58, 44, 58, 44, 61, listOf("Blaze", "Iron Fist")),
        PokemonSummary(393, "Piplup", 4, listOf("Water"), 53, 51, 53, 61, 56, 40, listOf("Torrent", "Competitive")),
        PokemonSummary(495, "Snivy", 5, listOf("Grass"), 45, 45, 55, 45, 55, 63, listOf("Overgrow", "Contrary")),
        PokemonSummary(498, "Tepig", 5, listOf("Fire"), 65, 63, 45, 45, 45, 45, listOf("Blaze", "Thick Fat")),
        PokemonSummary(501, "Oshawott", 5, listOf("Water"), 55, 55, 45, 63, 45, 45, listOf("Torrent", "Shell Armor")),
        PokemonSummary(650, "Chespin", 6, listOf("Grass"), 56, 61, 65, 48, 45, 38, listOf("Overgrow", "Bulletproof")),
        PokemonSummary(653, "Fennekin", 6, listOf("Fire"), 40, 45, 40, 62, 60, 60, listOf("Blaze", "Magician")),
        PokemonSummary(656, "Froakie", 6, listOf("Water"), 41, 56, 40, 62, 44, 71, listOf("Torrent", "Protean")),
        PokemonSummary(722, "Rowlet", 7, listOf("Grass", "Flying"), 68, 55, 55, 50, 50, 42, listOf("Overgrow", "Long Reach")),
        PokemonSummary(725, "Litten", 7, listOf("Fire"), 45, 65, 40, 60, 40, 70, listOf("Blaze", "Intimidate")),
        PokemonSummary(728, "Popplio", 7, listOf("Water"), 50, 54, 54, 66, 56, 40, listOf("Torrent", "Liquid Voice")),
        PokemonSummary(810, "Grookey", 8, listOf("Grass"), 50, 65, 50, 40, 40, 65, listOf("Overgrow", "Grassy Surge")),
        PokemonSummary(813, "Scorbunny", 8, listOf("Fire"), 50, 71, 40, 40, 40, 69, listOf("Blaze", "Libero")),
        PokemonSummary(816, "Sobble", 8, listOf("Water"), 50, 40, 40, 70, 40, 70, listOf("Torrent", "Sniper")),
        PokemonSummary(906, "Sprigatito", 9, listOf("Grass"), 40, 61, 54, 45, 45, 65, listOf("Overgrow", "Protean")),
        PokemonSummary(909, "Fuecoco", 9, listOf("Fire"), 67, 45, 59, 63, 40, 36, listOf("Blaze", "Unaware")),
        PokemonSummary(912, "Quaxly", 9, listOf("Water"), 55, 65, 45, 50, 45, 50, listOf("Torrent", "Moxie"))
    )

    fun find(id: Int): PokemonSummary? = all.firstOrNull { it.id == id }
}
