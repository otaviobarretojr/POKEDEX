package com.otaviobarreto.pokedex.data

object PokemonFormPresentation {
    fun categoryLabel(kind: PokemonFormKind): String = when(kind) {
        PokemonFormKind.DEFAULT -> "Padrão"
        PokemonFormKind.REGIONAL -> "Regional"
        PokemonFormKind.GENDER -> "Gênero"
        PokemonFormKind.BATTLE -> "Batalha"
        PokemonFormKind.SPECIAL -> "Especial"
        PokemonFormKind.COSMETIC -> "Cosmética"
        PokemonFormKind.OTHER -> "Outra"
    }

    fun label(baseName:String, rawName:String, shiny:Boolean=false):String {
        val raw = rawName
            .removePrefix(baseName)
            .trim()
            .ifBlank { "Padrão" }

        val translated = raw
            .replace("Alola", "Forma de Alola", ignoreCase=true)
            .replace("Galar", "Forma de Galar", ignoreCase=true)
            .replace("Hisui", "Forma de Hisui", ignoreCase=true)
            .replace("Paldea", "Forma de Paldea", ignoreCase=true)
            .replace("Mega X", "Mega X", ignoreCase=true)
            .replace("Mega Y", "Mega Y", ignoreCase=true)
            .replace("Mega", "Mega", ignoreCase=true)
            .replace("Gmax", "Gigantamax", ignoreCase=true)
            .replace("Gigantamax", "Gigantamax", ignoreCase=true)
            .replace("Primal", "Primal", ignoreCase=true)
            .replace("Attack", "Ataque", ignoreCase=true)
            .replace("Defense", "Defesa", ignoreCase=true)
            .replace("Speed", "Velocidade", ignoreCase=true)
            .replace("Sunny", "Ensolarado", ignoreCase=true)
            .replace("Rainy", "Chuvoso", ignoreCase=true)
            .replace("Snowy", "Nevado", ignoreCase=true)
            .replace("Female", "Fêmea", ignoreCase=true)
            .replace("Male", "Macho", ignoreCase=true)
            .replace("Midday", "Diurna", ignoreCase=true)
            .replace("Midnight", "Noturna", ignoreCase=true)
            .replace("Dusk", "Crepúsculo", ignoreCase=true)
            .replace("Amped", "Aguda", ignoreCase=true)
            .replace("Low Key", "Grave", ignoreCase=true)
            .replace("Family Of Three", "Família de Três", ignoreCase=true)
            .replace("Family Of Four", "Família de Quatro", ignoreCase=true)
            .replace("Three Segment", "Três Segmentos", ignoreCase=true)
            .replace("Two Segment", "Dois Segmentos", ignoreCase=true)
            .replace("Curly", "Curvada", ignoreCase=true)
            .replace("Droopy", "Caída", ignoreCase=true)
            .replace("Stretchy", "Alongada", ignoreCase=true)
            .replace("Roaming", "Errante", ignoreCase=true)
            .replace("Chest", "Baú", ignoreCase=true)
            .replace("Artisan", "Artesanal", ignoreCase=true)
            .replace("Counterfeit", "Falsificada", ignoreCase=true)
            .replace("Antique", "Antiga", ignoreCase=true)
            .replace("Phony", "Falsa", ignoreCase=true)
            .replace("Teal Mask", "Máscara Turquesa", ignoreCase=true)
            .replace("Wellspring", "Nascente", ignoreCase=true)
            .replace("Hearthflame", "Chama da Lareira", ignoreCase=true)
            .replace("Cornerstone", "Pedra Fundamental", ignoreCase=true)
            .replace("Terastal", "Terastal", ignoreCase=true)
            .replace("Stellar", "Estelar", ignoreCase=true)

        return translated + if(shiny) " · Shiny" else ""
    }

    fun behaviorLabel(kind: PokemonFormKind):String = when(kind) {
        PokemonFormKind.BATTLE -> "Altera dados de batalha"
        PokemonFormKind.REGIONAL -> "Forma regional"
        PokemonFormKind.COSMETIC -> "Variação visual"
        PokemonFormKind.GENDER -> "Diferença de gênero"
        PokemonFormKind.SPECIAL -> "Forma especial"
        PokemonFormKind.DEFAULT -> "Forma padrão"
        PokemonFormKind.OTHER -> "Variante"
    }
}
