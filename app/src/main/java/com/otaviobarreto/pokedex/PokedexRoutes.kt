package com.otaviobarreto.pokedex

object PokedexRoutes {
    const val HOME = "home"
    const val POKEDEX = "pokedex"
    const val COLLECTION = "collection"
    const val BOXES = "boxes"
    const val CENTRAL = "central"

    const val POKEMON = "pokemon/{id}?source={source}"
    const val FORM_DETAIL = "formDetail/{id}?name={name}&shiny={shiny}"
    const val LOCATION = "location/{id}?source={source}"
    const val REGION_MAP = "regionMap/{id}?source={source}"
    const val REFERENCE = "reference?kind={kind}&name={name}&source={source}"
    const val CAMPAIGN_GUIDE = "campaignGuide?game={game}&phase={phase}&step={step}"

    val secondary = setOf(POKEMON, FORM_DETAIL, LOCATION, REGION_MAP, REFERENCE, CAMPAIGN_GUIDE)
    val main = setOf(HOME, POKEDEX, BOXES, CENTRAL)

    fun isSecondary(route:String?):Boolean = route in secondary
}
