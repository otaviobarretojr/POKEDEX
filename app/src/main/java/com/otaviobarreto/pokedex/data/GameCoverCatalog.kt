package com.otaviobarreto.pokedex.data

/**
 * Official game-cover artwork used only by the Journey game cards.
 *
 * Pair releases keep both original covers visible side by side. URLs point to
 * official Nintendo / Pokémon properties so the app does not depend on
 * fan-made artwork.
 */
object GameCoverCatalog {
    fun coversFor(gameLabel: String): List<String> = covers[gameLabel].orEmpty()

    private val covers = mapOf(
        "Pokémon Legends: Z-A" to listOf(
            "https://legends.pokemon.com/_next/image?q=75&url=%2Fimages%2Fbox-art%2Fpoke-legends-box-art-NS-UKV-2x.png&w=640"
        ),
        "Scarlet / Violet" to listOf(
            "https://assets.nintendo.com/image/upload/ar_16%3A9%2Cb_auto%3Aborder%2Cc_lpad/b_white/f_auto/q_auto/dpr_1.5/store/software/switch/70010000053966/849c234de8df7265201d26d9d72f88eed3f32438d3dca12fc135beb4c3befc85",
            "https://assets.nintendo.com/image/upload/ar_16%3A9%2Cb_auto%3Aborder%2Cc_lpad/b_white/f_auto/q_auto/dpr_1.5/store/software/switch/70010000053971/842b2784d91520d41a947dec17fac116fec889bb1f1db4023615af8429dae00d"
        ),
        "Sword / Shield" to listOf(
            "https://swordshield.pokemon.com/assets/img/common/packshot/en-gb/packshot_sword.png",
            "https://swordshield.pokemon.com/assets/img/common/packshot/en-gb/packshot_shield.png"
        ),
        "Let's Go Pikachu / Eevee" to listOf(
            "https://pokemonletsgo.pokemon.com/assets/img/en-us/packshot-pikachu.png",
            "https://pokemonletsgo.pokemon.com/assets/img/en-us/packshot-eevee.png"
        ),
        "Legends Arceus" to listOf(
            "https://www.nintendo.com/ph/switch/aw7k/img/hero_sp.jpg"
        ),
        "Brilliant Diamond / Shining Pearl" to listOf(
            "https://diamondpearl.pokemon.com/en-us/assets/boxart_bd.png",
            "https://diamondpearl.pokemon.com/en-us/assets/boxart_sp.png"
        ),
        "FireRed / LeafGreen" to listOf(
            "https://assets.nintendo.com/image/upload/ar_16%3A9%2Cb_auto%3Aborder%2Cc_lpad/b_white/f_auto/q_auto/dpr_1.5/store/software/switch/70010000118613/86f3c6d4e129d185cbbc441169856733dea45ff231446a4edf5fec4624041849",
            "https://assets.nintendo.com/image/upload/ar_16%3A9%2Cb_auto%3Aborder%2Cc_lpad/b_white/f_auto/q_auto/dpr_1.5/store/software/switch/70010000118618/af2c94d85c4b3bc671d0a88d3c0ea4b1eb96a8b5d45c758ceea10e29a0ffda44"
        )
    )
}
