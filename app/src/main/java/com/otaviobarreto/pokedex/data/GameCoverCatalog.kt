package com.otaviobarreto.pokedex.data

/**
 * Curated official artwork for the game-first navigation.
 *
 * HERO is intentionally independent from COVER:
 * - heroArtwork: wide/key art used by the active Journey home.
 * - libraryCover: portrait/packshot used by the Games library.
 *
 * Keeping both roles separate prevents a portrait box from being shrunk into
 * a wide dashboard card (the visual regression that triggered this rework).
 */
object GameCoverCatalog {
    data class GameArt(
        val canonicalName:String,
        val heroArtwork:String,
        val libraryCover:String,
        val visualKey:String,
        val libraryAspectRatio:Float=16f/9f
    )

    private val art=mapOf(
        "Scarlet / Violet" to GameArt(
            canonicalName="Pokémon Scarlet",
            heroArtwork="https://assets.nintendo.com/image/upload/ar_16%3A9%2Cb_auto%3Aborder%2Cc_lpad/b_white/f_auto/q_auto/dpr_1.5/store/software/switch/70010000053966/849c234de8df7265201d26d9d72f88eed3f32438d3dca12fc135beb4c3befc85",
            libraryCover="https://scarletviolet.pokemon.com/_images/global/en-us/packshot-scarlet.png",
            visualKey="scarlet"
        ),
        "Pokémon Legends: Z-A" to GameArt(
            canonicalName="Pokémon Legends: Z-A",
            heroArtwork="https://images.squarespace-cdn.com/content/v1/5c95f8d416b640656eb7765a/3f050708-fd9d-4d91-8735-dab94f7e6212/Pokemon_Legends_Z-A_Key_Visual.jpg",
            libraryCover="https://legends.pokemon.com/images/box-art/poke-legends-box-art-NS-UKV-2x.png",
            visualKey="za",
            libraryAspectRatio=1.72f
        ),
        "Legends Arceus" to GameArt(
            canonicalName="Pokémon Legends: Arceus",
            heroArtwork="https://archives.bulbagarden.net/media/upload/e/e1/Legends_Arceus_artwork_1_horizontal.png",
            libraryCover="https://legends.arceus.pokemon.com/assets/img/common/packshot/en-us/packshot.png",
            visualKey="arceus",
            libraryAspectRatio=1.72f
        ),
        "Brilliant Diamond / Shining Pearl" to GameArt(
            canonicalName="Pokémon Brilliant Diamond",
            heroArtwork="https://assets.nintendo.eu/image/upload/f_auto%2Cc_limit%2Cw_1920%2Cq_75/MNS/Content%20Pages%20Assets/Category-List%20Pages/Franchises/Pokemon/16.9_BrandPage_Switch_PokemonBrilliantDiamondShiningPearl_KeyArt_enNOE",
            libraryCover="https://diamondpearl.pokemon.com/en-us/assets/boxart_bd.png",
            visualKey="diamond",
            libraryAspectRatio=1.72f
        ),
        "Sword / Shield" to GameArt(
            canonicalName="Pokémon Shield",
            heroArtwork="https://swordshield.pokemon.com/assets/icons/share_icon-fb_en-us.jpg",
            libraryCover="https://swordshield.pokemon.com/assets/img/common/packshot/en-gb/packshot_shield.png",
            visualKey="shield"
        ),
        "Let's Go Pikachu / Eevee" to GameArt(
            canonicalName="Pokémon: Let's Go, Pikachu!",
            heroArtwork="https://fs-prod-cdn.nintendo-europe.com/media/images/10_share_images/games_15/nintendo_switch_4/H2x1_NSwitch_PokemonLetsGo_Combo_enGB_image1280w.jpg",
            libraryCover="https://pokemonletsgo.pokemon.com/assets/img/en-us/packshot-pikachu.png",
            visualKey="letsgo-pikachu"
        ),
        "FireRed / LeafGreen" to GameArt(
            canonicalName="Pokémon FireRed",
            heroArtwork="https://assets.nintendo.com/image/upload/ar_16%3A9%2Cb_auto%3Aborder%2Cc_lpad/b_white/f_auto/q_auto/dpr_1.5/store/software/switch/70010000118613/86f3c6d4e129d185cbbc441169856733dea45ff231446a4edf5fec4624041849",
            libraryCover="https://assets.nintendo.com/image/upload/ar_16%3A9%2Cb_auto%3Aborder%2Cc_lpad/b_white/f_auto/q_auto/dpr_1.5/store/software/switch/70010000118613/86f3c6d4e129d185cbbc441169856733dea45ff231446a4edf5fec4624041849",
            visualKey="firered"
        )
    )

    fun artFor(gameLabel:String):GameArt?=art[gameLabel]
    fun primaryCoverFor(gameLabel:String):String?=artFor(gameLabel)?.libraryCover
    fun heroFor(gameLabel:String):String?=artFor(gameLabel)?.heroArtwork
    fun libraryAspectRatioFor(gameLabel:String):Float=artFor(gameLabel)?.libraryAspectRatio ?: 16f/9f
    fun displayNameFor(gameLabel:String):String=artFor(gameLabel)?.canonicalName ?: gameLabel
    fun coversFor(gameLabel:String): List<String> = primaryCoverFor(gameLabel)?.let(::listOf).orEmpty()
}
