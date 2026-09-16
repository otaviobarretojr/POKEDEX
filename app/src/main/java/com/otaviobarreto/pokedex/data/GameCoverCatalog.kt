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
        val visualKey:String
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
            heroArtwork="https://www.nintendo.com/eu/media/images/assets/nintendo_switch_games/pokemonlegendsza/1x1_NSwitch2_PokemonLegendsZA_KV_DE_de_image500w.jpg",
            libraryCover="https://legends.pokemon.com/images/box-art/poke-legends-box-art-NS-UKV-2x.png",
            visualKey="za"
        ),
        "Legends Arceus" to GameArt(
            canonicalName="Pokémon Legends: Arceus",
            heroArtwork="https://www.nintendo.com/sg/switch/aw7k/img/og.jpg",
            libraryCover="https://legends.arceus.pokemon.com/assets/img/common/packshot/en-us/packshot.png",
            visualKey="arceus"
        ),
        "Brilliant Diamond / Shining Pearl" to GameArt(
            canonicalName="Pokémon Brilliant Diamond",
            heroArtwork="https://assets.nintendo.com/image/upload/ar_16%3A9%2Cb_auto%3Aborder%2Cc_lpad/b_white/f_auto/q_auto/dpr_1.5/store/software/switch/70010000039987/13d11f4a53d7c3d52f968cadf56b80e9e2545014e6426bcac3b554fcdeee21d7",
            libraryCover="https://diamondpearl.pokemon.com/en-us/assets/boxart_bd.png",
            visualKey="diamond"
        ),
        "Sword / Shield" to GameArt(
            canonicalName="Pokémon Shield",
            heroArtwork="https://assets.nintendo.com/image/upload/ar_16%3A9%2Cb_auto%3Aborder%2Cc_lpad/b_white/f_auto/q_auto/dpr_1.5/store/software/switch/70010000019931/7b0438776a68a56bc24027f06d68611e4ba057ea175ed3e65d20feca768b3e3d",
            libraryCover="https://swordshield.pokemon.com/assets/img/common/packshot/en-gb/packshot_shield.png",
            visualKey="shield"
        ),
        "Let's Go Pikachu / Eevee" to GameArt(
            canonicalName="Pokémon: Let's Go, Pikachu!",
            heroArtwork="https://assets.nintendo.com/image/upload/ar_16%3A9%2Cb_auto%3Aborder%2Cc_lpad/b_white/f_auto/q_auto/dpr_1.5/store/software/switch/70010000000433/ef9e13d3f6c17d1f98f4f64e753d2274e14cbb4ad570f87f9a11e86ad69c6a45",
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
    fun displayNameFor(gameLabel:String):String=artFor(gameLabel)?.canonicalName ?: gameLabel
    fun coversFor(gameLabel:String): List<String> = primaryCoverFor(gameLabel)?.let(::listOf).orEmpty()
}
