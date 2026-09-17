package com.otaviobarreto.pokedex

object PokedexRoutes {
    const val HOME = "home"
    const val GAMES = "games"
    const val POKEDEX = "pokedex"
    const val COLLECTION = "collection"
    const val BOXES = "boxes"
    const val CENTRAL = "central"

    const val POKEMON = "pokemon/{id}?source={source}"
    const val FORM_DETAIL = "formDetail/{id}?name={name}&shiny={shiny}"
    const val REFERENCE = "reference?kind={kind}&name={name}&source={source}"
    const val CAMPAIGN_GUIDE = "campaignGuide?game={game}&phase={phase}&step={step}"
    const val SEARCH = "search"
    const val EVOLUTION_CENTER = "evolutionCenter"
    const val GAME_DEX = "gameDex"

    val secondary = setOf(
        POKEMON, FORM_DETAIL, REFERENCE, CAMPAIGN_GUIDE,
        SEARCH, EVOLUTION_CENTER, GAME_DEX
    )
    val main = setOf(HOME, GAMES, POKEDEX, COLLECTION, BOXES, CENTRAL)

    fun isSecondary(route:String?):Boolean = route in secondary
}
