package com.otaviobarreto.pokedex.data

/**
 * Reverse encounter index used when the primary API does not expose modern
 * Scarlet/Violet reverse location data. The UI intentionally presents these
 * entries as a curated community index rather than an official/exhaustive table.
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
        CommunityAreaEncounter(
            "Poco Path",
            listOf("Lechonk", "Pawmi", "Tarountula", "Hoppip", "Fletchling", "Scatterbug", "Wingull", "Buizel", "Magikarp", "Arrokuda"),
            "2–8"
        ),
        CommunityAreaEncounter(
            "South Province (Area One)",
            listOf(
                "Hoppip", "Wooper", "Wingull", "Ralts", "Combee", "Sunkern", "Buizel", "Pawmi", "Gastly",
                "Fletchling", "Scatterbug", "Spewpa", "Oricorio", "Lechonk", "Tarountula", "Fidough", "Happiny",
                "Pichu", "Bonsly", "Skwovet", "Shroodle", "Bounsweet", "Igglybuff", "Drowzee", "Wiglett",
                "Pachirisu", "Flamigo", "Gimmighoul", "Magikarp", "Arrokuda", "Azurill", "Chewtle", "Psyduck", "Surskit"
            ),
            "2–8"
        ),
        CommunityAreaEncounter(
            "South Province (Area Two)",
            listOf(
                "Pikachu", "Jigglypuff", "Eevee", "Mareep", "Hoppip", "Starly", "Fletchling", "Smoliv", "Fidough",
                "Maschiff", "Happiny", "Pichu", "Bonsly", "Bounsweet", "Skwovet", "Shroodle", "Applin", "Igglybuff",
                "Rockruff", "Misdreavus", "Makuhita", "Skiddo", "Yungoos", "Nacli", "Sunkern", "Combee", "Flabébé",
                "Kricketot", "Diglett", "Gastly", "Drowzee", "Bronzor", "Tinkatink", "Squawkabilly", "Staravia",
                "Vespiquen", "Gimmighoul", "Psyduck", "Magikarp", "Azurill", "Buizel", "Chewtle", "Arrokuda", "Tadbulb"
            ),
            "5–12"
        ),
        CommunityAreaEncounter(
            "South Province (Area Three)",
            listOf(
                "Growlithe", "Gulpin", "Spoink", "Shuppet", "Shinx", "Oricorio", "Rookidee", "Nymble", "Pawmi",
                "Klawf", "Murkrow", "Dunsparce", "Happiny", "Tandemaus", "Squawkabilly", "Drifloon", "Makuhita",
                "Yungoos", "Skiddo", "Nacli", "Gastly", "Drowzee", "Bronzor", "Tinkatink", "Talonflame", "Staraptor",
                "Gimmighoul"
            ),
            "8–16",
            "Algumas espécies/encounters variam por versão ou são encontros fixos"
        ),
        CommunityAreaEncounter(
            "South Province (Area Four)",
            listOf(
                "Murkrow", "Dunsparce", "Grumpig", "Starly", "Staravia", "Pachirisu", "Riolu", "Deerling", "Fletchinder",
                "Toxel", "Lechonk", "Tarountula", "Pawmo", "Maschiff", "Toedscool", "Pikachu", "Pineco", "Komala",
                "Applin", "Charcadet", "Shroodle", "Gastly", "Misdreavus", "Phanpy", "Meditite", "Swablu", "Drifloon",
                "Rufflet", "Skiddo", "Rockruff", "Mudbray", "Flittle", "Psyduck", "Scyther", "Magikarp", "Marill",
                "Wooper", "Barboach", "Basculin", "Goomy", "Chewtle", "Hatenna", "Dreepy"
            ),
            "16–23",
            "Misdreavus/Drifloon/Dreepy têm disponibilidade dependente da versão"
        ),
        CommunityAreaEncounter(
            "South Province (Area Five)",
            listOf(
                "Mankey", "Skiploom", "Murkrow", "Dunsparce", "Stantler", "Shroomish", "Slakoth", "Vigoroth", "Zangoose",
                "Seviper", "Luxio", "Pachirisu", "Stunky", "Deerling", "Fletchinder", "Litleo", "Rookidee", "Corvisquire",
                "Lechonk", "Oinkologne", "Tarountula", "Pawmo", "Toedscool", "Gastly", "Misdreavus", "Larvitar", "Swablu",
                "Bagon", "Tinkatink", "Bronzor", "Charcadet", "Flamigo", "Naclstack", "Gimmighoul"
            ),
            "16–23",
            "Larvitar/Bagon/Stunky/Misdreavus variam entre Scarlet e Violet"
        ),
        CommunityAreaEncounter(
            "South Province (Area Six)",
            listOf(
                "Murkrow", "Banette", "Drifblim", "Gothorita", "Gothitelle", "Sylveon", "Klefki", "Sinistea", "Dachsbun",
                "Bombirdier", "Chansey", "Flareon", "Flaaffy", "Ampharos", "Sableye", "Meditite", "Medicham", "Tinkatuff",
                "Tinkaton", "Glimmet", "Glimmora", "Gabite", "Gible", "Salandit", "Salazzle"
            ),
            "37–43",
            "Alguns encontros noturnos e formas variam conforme condição/versão"
        ),
        CommunityAreaEncounter(
            "Tagtree Thicket",
            listOf(
                "Venonat", "Venomoth", "Murkrow", "Pineco", "Dunsparce", "Zorua", "Foongus", "Oranguru", "Passimian",
                "Komala", "Mimikyu", "Greedent", "Applin", "Impidimp", "Morgrem", "Oinkologne", "Spidops", "Charcadet",
                "Shroodle", "Grafaiai", "Toedscool", "Psyduck", "Magikarp", "Misdreavus", "Barboach", "Whiscash",
                "Basculin", "Drednaw", "Dreepy", "Combee", "Bellibolt"
            ),
            "25–32",
            "Oranguru/Passimian/Misdreavus/Dreepy incluem diferenças entre Scarlet e Violet"
        ),
        CommunityAreaEncounter(
            "East Province (Area One)",
            listOf("Buizel", "Psyduck", "Wiglett", "Arrokuda", "Magnemite", "Pawmo", "Nacli", "Shinx", "Makuhita", "Growlithe", "Charcadet", "Tadbulb"),
            "10–20"
        ),
        CommunityAreaEncounter(
            "East Province (Area Two)",
            listOf("Tauros", "Girafarig", "Tadbulb", "Bellibolt", "Pawmo", "Growlithe", "Murkrow", "Salandit", "Voltorb", "Magnemite", "Mareep", "Flaaffy"),
            "18–28"
        ),
        CommunityAreaEncounter(
            "East Province (Area Three)",
            listOf("Flittle", "Rellor", "Espathra", "Orthworm", "Varoom", "Revavroom", "Cufant", "Copperajah", "Tinkatuff", "Klefki", "Rookidee", "Corvisquire"),
            "22–35"
        ),
        CommunityAreaEncounter(
            "West Province (Area One)",
            listOf("Rockruff", "Nacli", "Fidough", "Azurill", "Bombirdier", "Mankey", "Phanpy", "Mudbray", "Toxel", "Pawmo", "Swablu", "Rufflet"),
            "12–24"
        ),
        CommunityAreaEncounter(
            "West Province (Area Two)",
            listOf("Gothita", "Solosis", "Sableye", "Spoink", "Girafarig", "Tauros", "Dugtrio", "Krookodile", "Toxtricity", "Houndour", "Houndoom", "Cyclizar"),
            "18–30",
            "Gothita/Solosis variam por versão"
        ),
        CommunityAreaEncounter(
            "West Province (Area Three)",
            listOf("Varoom", "Shroodle", "Grafaiai", "Tropius", "Ditto", "Zoroark", "Sawsbuck", "Dachsbun", "Persian", "Cyclizar", "Falinks", "Tinkatuff"),
            "25–36"
        ),
        CommunityAreaEncounter(
            "North Province (Area One)",
            listOf("Frigibax", "Cetoddle", "Orthworm", "Hawlucha", "Bisharp", "Scyther", "Heracross", "Pawniard", "Gogoat", "Sneasel", "Gabite", "Froslass"),
            "32–45"
        ),
        CommunityAreaEncounter(
            "North Province (Area Two)",
            listOf("Gogoat", "Passimian", "Oranguru", "Bisharp", "Pawniard", "Scyther", "Heracross", "Lokix", "Falinks", "Tropius", "Hawlucha", "Komala"),
            "35–48",
            "Passimian/Oranguru variam por versão"
        ),
        CommunityAreaEncounter(
            "North Province (Area Three)",
            listOf("Arctibax", "Dondozo", "Baxcalibur", "Cetitan", "Froslass", "Glalie", "Sneasel", "Beartic", "Cryogonal", "Delibird", "Bergmite", "Avalugg"),
            "45–55"
        ),
        CommunityAreaEncounter(
            "Asado Desert",
            listOf("Sandile", "Hippopotas", "Gabite", "Palossand", "Rellor", "Capsakid", "Cacnea", "Bronzor", "Orthworm", "Flittle", "Larvesta", "Rufflet"),
            "20–32"
        ),
        CommunityAreaEncounter(
            "Glaseado Mountain",
            listOf("Frigibax", "Sneasel", "Delibird", "Cetoddle", "Cetitan", "Cubchoo", "Beartic", "Snorunt", "Glalie", "Froslass", "Cryogonal", "Bergmite", "Avalugg", "Snom", "Frosmoth"),
            "36–52"
        ),
        CommunityAreaEncounter(
            "Casseroya Lake",
            listOf("Dondozo", "Tatsugiri", "Golduck", "Dratini", "Dragonair", "Veluza", "Gyarados", "Slowpoke", "Slowbro", "Chewtle", "Drednaw", "Basculin"),
            "45–58"
        ),
        CommunityAreaEncounter(
            "Area Zero",
            listOf(
                "Great Tusk", "Sandy Shocks", "Brute Bonnet", "Slither Wing", "Roaring Moon", "Scream Tail", "Flutter Mane",
                "Iron Treads", "Iron Bundle", "Iron Hands", "Iron Jugulis", "Iron Moth", "Iron Thorns", "Iron Valiant",
                "Glimmora", "Dudunsparce", "Raichu", "Corviknight", "Garganacl", "Farigiraf"
            ),
            "52–66",
            "Pokémon Paradoxo variam entre Scarlet e Violet"
        )
    )

    private val kitakami = listOf(
        CommunityAreaEncounter("Kitakami Road", listOf("Deerling", "Lotad", "Applin", "Poochyena", "Sentret", "Hoothoot", "Spinarak", "Volbeat", "Illumise", "Ekans"), "50–56"),
        CommunityAreaEncounter("Apple Hills", listOf("Applin", "Dipplin", "Sewaddle", "Swadloon", "Poochyena", "Mightyena", "Poltchageist", "Vulpix", "Seedot", "Hoothoot"), "50–60"),
        CommunityAreaEncounter("Reveler's Road", listOf("Vulpix", "Poochyena", "Hoothoot", "Spinarak", "Sewaddle", "Cutiefly", "Volbeat", "Illumise", "Poltchageist", "Mienfoo"), "52–60"),
        CommunityAreaEncounter("Wistful Fields", listOf("Geodude", "Ekans", "Koffing", "Poochyena", "Yanma", "Corphish", "Mienfoo", "Vullaby", "Munchlax", "Poltchageist"), "54–62"),
        CommunityAreaEncounter("Paradise Barrens", listOf("Geodude", "Graveler", "Koffing", "Magcargo", "Slugma", "Vullaby", "Mandibuzz", "Phantump", "Trevenant", "Dusclops"), "56–64"),
        CommunityAreaEncounter("Oni Mountain", listOf("Mankey", "Primeape", "Geodude", "Graveler", "Gligar", "Timburr", "Gurdurr", "Mienfoo", "Mienshao", "Annihilape"), "55–65"),
        CommunityAreaEncounter("Timeless Woods", listOf("Ursaring", "Stantler", "Noctowl", "Mightyena", "Trevenant", "Dusclops", "Munchlax", "Snorlax", "Basculin", "Duskull"), "58–68"),
        CommunityAreaEncounter("Fellhorn Gorge", listOf("Gligar", "Geodude", "Graveler", "Timburr", "Gurdurr", "Mienfoo", "Mienshao", "Corphish", "Crawdaunt", "Basculin"), "58–66"),
        CommunityAreaEncounter("Mossfell Confluence", listOf("Poliwag", "Poliwhirl", "Corphish", "Crawdaunt", "Lotad", "Lombre", "Basculin", "Feebas", "Carbink", "Ducklett"), "56–66"),
        CommunityAreaEncounter("Chilling Waterhead", listOf("Poliwag", "Poliwhirl", "Basculin", "Feebas", "Ducklett", "Swanna", "Corphish", "Crawdaunt", "Duskull", "Dusclops"), "58–68"),
        CommunityAreaEncounter("Crystal Pool", listOf("Carbink", "Feebas", "Duskull", "Dusclops", "Ogerpon", "Okidogi", "Munkidori", "Fezandipiti"), "60–70", "Ogerpon e os Loyal Three são encontros especiais ligados à progressão")
    )

    private val blueberry = listOf(
        CommunityAreaEncounter("Terarium — Savanna Biome", listOf("Litleo", "Hippopotas", "Sandile", "Doduo", "Rhyhorn", "Exeggcute", "Girafarig", "Blitzle", "Deerling", "Scyther", "Tauros", "Trapinch"), "60–68"),
        CommunityAreaEncounter("Terarium — Polar Biome", listOf("Frigibax", "Cetoddle", "Snom", "Seel", "Dewgong", "Lapras", "Delibird", "Cubchoo", "Beartic", "Snorunt", "Cryogonal", "Beldum"), "62–70"),
        CommunityAreaEncounter("Terarium — Canyon Biome", listOf("Larvitar", "Axew", "Dratini", "Rhyhorn", "Electabuzz", "Magmar", "Golett", "Minior", "Trapinch", "Skarmory", "Kleavor", "Archaludon"), "60–70"),
        CommunityAreaEncounter("Terarium — Coastal Biome", listOf("Dratini", "Finizen", "Palafin", "Exeggcute", "Exeggutor", "Slowpoke", "Lapras", "Alomomola", "Inkay", "Malamar", "Dewpider", "Araquanid"), "62–70"),
        CommunityAreaEncounter("Central Plaza / Terarium", listOf("Ditto", "Minior", "Porygon", "Rotom"), "60–70", "Amostra de encontros/espécies associadas ao Terarium; não exaustiva")
    )
}
