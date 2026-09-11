package com.otaviobarreto.pokedex.data

enum class CampaignPhase(val label:String){ EARLY("Início"), MID("Mid game"), LATE("Late game") }

data class CampaignSlot(
    val pokemonId:Int,
    val alternatives:List<Int> = emptyList()
)

data class CampaignTeamPreset(
    val game:String,
    val starter:String,
    val starterId:Int,
    val phase:CampaignPhase,
    val slots:List<CampaignSlot>,
    val rationale:String,
    val sourceLabel:String
)

data class CampaignBuild(
    val role:String,
    val moves:List<String>,
    val nature:String,
    val item:String,
    val notes:String
)

object TeamCampaignCatalog {
    val switchGames = listOf(
        "Let's Go Pikachu / Eevee",
        "Sword / Shield",
        "Brilliant Diamond / Shining Pearl",
        "Legends Arceus",
        "Scarlet / Violet",
        "Legends Z-A"
    )

    fun starters(game:String):List<Pair<String,Int>> = when(game){
        "Let's Go Pikachu / Eevee" -> listOf("Partner Pikachu" to 25, "Partner Eevee" to 133)
        "Sword / Shield" -> listOf("Grookey" to 810, "Scorbunny" to 813, "Sobble" to 816)
        "Brilliant Diamond / Shining Pearl" -> listOf("Turtwig" to 387, "Chimchar" to 390, "Piplup" to 393)
        "Legends Arceus" -> listOf("Rowlet" to 722, "Cyndaquil" to 155, "Oshawott" to 501)
        "Scarlet / Violet" -> listOf("Sprigatito" to 906, "Fuecoco" to 909, "Quaxly" to 912)
        "Legends Z-A" -> listOf("Chikorita" to 152, "Tepig" to 498, "Totodile" to 158)
        else -> emptyList()
    }

    fun preset(game:String,starterId:Int,phase:CampaignPhase):CampaignTeamPreset? {
        val starterName=starters(game).firstOrNull{it.second==starterId}?.first ?: return null
        val slots=when(game){
            "Let's Go Pikachu / Eevee" -> letsGo(starterId,phase)
            "Sword / Shield" -> swordShield(starterId,phase)
            "Brilliant Diamond / Shining Pearl" -> bdsp(starterId,phase)
            "Legends Arceus" -> arceus(starterId,phase)
            "Scarlet / Violet" -> scarletViolet(starterId,phase)
            "Legends Z-A" -> legendsZa(starterId,phase)
            else -> emptyList()
        }
        if(slots.isEmpty())return null
        return CampaignTeamPreset(
            game,starterName,starterId,phase,slots,
            rationale=when(phase){
                CampaignPhase.EARLY->"Prioriza Pokémon acessíveis cedo, cobertura simples e evolução barata para atravessar os primeiros chefes sem grind excessivo."
                CampaignPhase.MID->"Mantém o núcleo já treinado e troca slots por evoluções e coberturas que ficam disponíveis no meio da campanha."
                CampaignPhase.LATE->"Time pensado para fechar a história e os chefes finais com cobertura ampla, bons stats e pouca dependência de setup competitivo."
            },
            sourceLabel=when(game){
                "Let's Go Pikachu / Eevee"->"PokéBase · equipes in-game"
                "Legends Z-A"->"Game8 · Story Progression / Starter Teams"
                else->"Game8 · Best Team / Story Progression"
            }
        )
    }

    private fun s(id:Int,vararg alts:Int)=CampaignSlot(id,alts.toList())

    private fun letsGo(starter:Int,p:CampaignPhase)=when(p){
        CampaignPhase.EARLY->listOf(s(starter),s(29,32),s(4),s(129),s(35),s(1))
        CampaignPhase.MID->listOf(s(starter),s(33,34),s(5),s(130),s(36),s(2))
        CampaignPhase.LATE->listOf(s(starter),s(6),s(34),s(131),s(3),s(149))
    }

    private fun swordShield(starter:Int,p:CampaignPhase)=when(starter){
        810->when(p){
            CampaignPhase.EARLY->listOf(s(810),s(821),s(659),s(129),s(607),s(848))
            CampaignPhase.MID->listOf(s(811),s(130),s(849),s(608),s(857),s(853))
            CampaignPhase.LATE->listOf(s(812),s(130),s(849),s(609),s(858),s(890))
        }
        813->when(p){
            CampaignPhase.EARLY->listOf(s(813),s(821),s(659),s(129),s(848),s(829))
            CampaignPhase.MID->listOf(s(814),s(849),s(660),s(130),s(857),s(853))
            CampaignPhase.LATE->listOf(s(815),s(660),s(130),s(849),s(858),s(890))
        }
        else->when(p){
            CampaignPhase.EARLY->listOf(s(816),s(821),s(659),s(607),s(848),s(829))
            CampaignPhase.MID->listOf(s(817),s(822),s(849),s(608),s(857),s(853))
            CampaignPhase.LATE->listOf(s(818),s(823),s(849),s(609),s(858),s(890))
        }
    }

    private fun arceus(starter:Int,p:CampaignPhase)=when(starter){
        722->when(p){
            CampaignPhase.EARLY->listOf(s(722),s(403),s(418),s(390),s(216),s(133))
            CampaignPhase.MID->listOf(s(723),s(404),s(419),s(391),s(217),s(67))
            CampaignPhase.LATE->listOf(s(724),s(405),s(902),s(392),s(445),s(68))
        }
        155->when(p){
            CampaignPhase.EARLY->listOf(s(155),s(396),s(403),s(418),s(216),s(133))
            CampaignPhase.MID->listOf(s(156),s(397),s(404),s(419),s(217),s(67))
            CampaignPhase.LATE->listOf(s(157),s(398),s(405),s(902),s(445),s(68))
        }
        else->when(p){
            CampaignPhase.EARLY->listOf(s(501),s(403),s(396),s(390),s(216),s(133))
            CampaignPhase.MID->listOf(s(502),s(404),s(397),s(391),s(217),s(67))
            CampaignPhase.LATE->listOf(s(503),s(405),s(398),s(392),s(445),s(68))
        }
    }

    private fun scarletViolet(starter:Int,p:CampaignPhase)=when(starter){
        906->when(p){
            CampaignPhase.EARLY->listOf(s(906),s(129),s(935),s(821),s(940),s(194))
            CampaignPhase.MID->listOf(s(907),s(130),s(935),s(822),s(941),s(980))
            CampaignPhase.LATE->listOf(s(908),s(130),s(937,936),s(823),s(941),s(980))
        }
        909->when(p){
            CampaignPhase.EARLY->listOf(s(909),s(821),s(940),s(129),s(194),s(928))
            CampaignPhase.MID->listOf(s(910),s(822),s(941),s(130),s(980),s(929))
            CampaignPhase.LATE->listOf(s(911),s(823),s(941),s(130),s(980),s(930))
        }
        else->when(p){
            CampaignPhase.EARLY->listOf(s(912),s(935),s(821),s(940),s(194),s(928))
            CampaignPhase.MID->listOf(s(913),s(935),s(822),s(941),s(980),s(929))
            CampaignPhase.LATE->listOf(s(914),s(937,936),s(980),s(823),s(941),s(930))
        }
    }

    private fun bdsp(starter:Int,p:CampaignPhase)=when(starter){
        387->when(p){
            CampaignPhase.EARLY->listOf(s(387),s(396),s(403),s(129),s(77),s(74))
            CampaignPhase.MID->listOf(s(388),s(397),s(404),s(130),s(78),s(443))
            CampaignPhase.LATE->listOf(s(389),s(78),s(445),s(448),s(398),s(130))
        }
        390->when(p){
            CampaignPhase.EARLY->listOf(s(390),s(129),s(396),s(406),s(403),s(74))
            CampaignPhase.MID->listOf(s(391),s(130),s(397),s(315),s(404),s(443))
            CampaignPhase.LATE->listOf(s(392),s(130),s(445),s(398),s(448),s(407))
        }
        else->when(p){
            CampaignPhase.EARLY->listOf(s(393),s(77),s(396),s(406),s(403),s(74))
            CampaignPhase.MID->listOf(s(394),s(78),s(397),s(315),s(404),s(443))
            CampaignPhase.LATE->listOf(s(395),s(78),s(445),s(448),s(407),s(398))
        }
    }

    private fun legendsZa(starter:Int,p:CampaignPhase)=when(starter){
        152->when(p){
            CampaignPhase.EARLY->listOf(s(152),s(214),s(659),s(661),s(25),s(92))
            CampaignPhase.MID->listOf(s(153),s(80),s(359),s(660),s(282),s(663))
            CampaignPhase.LATE->listOf(s(154),s(530),s(282),s(448),s(149),s(229,6,214,359))
        }
        498->when(p){
            CampaignPhase.EARLY->listOf(s(498),s(214),s(659),s(661),s(25),s(92))
            CampaignPhase.MID->listOf(s(499),s(71),s(359),s(660),s(282),s(663))
            CampaignPhase.LATE->listOf(s(500),s(530),s(282),s(130),s(227),s(214))
        }
        else->when(p){
            CampaignPhase.EARLY->listOf(s(158),s(214),s(659),s(661),s(25),s(92))
            CampaignPhase.MID->listOf(s(159),s(323),s(359),s(660),s(282),s(663))
            CampaignPhase.LATE->listOf(s(160),s(530),s(282),s(214),s(6),s(227))
        }
    }

    fun buildFor(id:Int,game:String):CampaignBuild {
        val special=moves[id]
        val moves=special ?: listOf("STAB principal","STAB secundário","Cobertura","Utilidade")
        val nature=when(id){
            812,815,389,392,445,448,130,160,500,530,214,149,34->"Adamant / Jolly"
            818,911,858,609,282,930,154->"Modest / Timid"
            else->"Nature neutra é suficiente para campanha"
        }
        val item=when(game){
            "Let's Go Pikachu / Eevee","Legends Arceus"->"Sem held item obrigatório"
            else->"Item de dano ou sustain disponível no estágio"
        }
        return CampaignBuild(
            role=roles[id] ?: "Cobertura / flex",
            moves=moves,
            nature=nature,
            item=item,
            notes="Moves-alvo de campanha: use a versão disponível no seu estágio até liberar o golpe final."
        )
    }

    private val roles=mapOf(
        25 to "Speed / cobertura",133 to "Flex / cobertura",812 to "Atacante físico",815 to "Sweeper físico",
        818 to "Atacante especial",157 to "Atacante especial",724 to "Atacante físico",503 to "Atacante misto",
        908 to "Sweeper físico",911 to "Atacante especial",914 to "Sweeper físico",392 to "Atacante misto",
        389 to "Tanque físico",395 to "Tanque especial",154 to "Bulky Grass",500 to "Breaker físico",160 to "Atacante físico",
        130 to "Setup / físico",282 to "Especial / Fairy",448 to "Físico / Steel",530 to "Ground / Steel",823 to "Tanque / Flying"
    )

    private val moves=mapOf(
        25 to listOf("Thunderbolt","Zippy Zap","Splishy Splash","Floaty Fall"),
        133 to listOf("Bouncy Bubble","Buzzy Buzz","Sizzly Slide","Glitzy Glow"),
        812 to listOf("Drum Beating","Knock Off","High Horsepower","U-turn"),
        815 to listOf("Pyro Ball","High Jump Kick","U-turn","Bounce"),
        818 to listOf("Snipe Shot","Ice Beam","Dark Pulse","U-turn"),
        157 to listOf("Flamethrower","Shadow Ball","Calm Mind","Mystical Fire"),
        724 to listOf("Triple Arrows","Leaf Blade","Psycho Cut","Roost"),
        503 to listOf("Ceaseless Edge","Aqua Tail","Night Slash","Swords Dance"),
        908 to listOf("Flower Trick","Knock Off","Play Rough","U-turn"),
        911 to listOf("Torch Song","Shadow Ball","Earth Power","Snarl"),
        914 to listOf("Aqua Step","Close Combat","Ice Spinner","U-turn"),
        389 to listOf("Earthquake","Wood Hammer","Crunch","Stone Edge"),
        392 to listOf("Flare Blitz","Close Combat","Mach Punch","U-turn"),
        395 to listOf("Surf","Flash Cannon","Ice Beam","Grass Knot"),
        154 to listOf("Giga Drain","Body Slam","Earthquake","Synthesis"),
        500 to listOf("Flare Blitz","Close Combat","Wild Charge","Bulk Up"),
        160 to listOf("Liquidation","Ice Fang","Crunch","Aqua Jet"),
        130 to listOf("Waterfall","Crunch","Ice Fang","Dragon Dance"),
        282 to listOf("Psychic","Moonblast","Shadow Ball","Calm Mind"),
        448 to listOf("Aura Sphere","Flash Cannon","Extreme Speed","Swords Dance"),
        530 to listOf("Earthquake","Iron Head","Rock Slide","Swords Dance"),
        149 to listOf("Dragon Claw","Fly","Thunder Punch","Ice Punch"),
        229 to listOf("Flamethrower","Dark Pulse","Sludge Bomb","Nasty Plot"),
        823 to listOf("Brave Bird","Iron Head","U-turn","Roost"),
        849 to listOf("Overdrive","Sludge Bomb","Boomburst","Volt Switch"),
        609 to listOf("Flamethrower","Shadow Ball","Energy Ball","Will-O-Wisp"),
        858 to listOf("Psychic","Dazzling Gleam","Mystical Fire","Calm Mind"),
        890 to listOf("Dynamax Cannon","Dragon Pulse","Flamethrower","Sludge Bomb"),
        980 to listOf("Earthquake","Poison Jab","Megahorn","Yawn"),
        941 to listOf("Thunderbolt","Air Slash","Volt Switch","Roost"),
        930 to listOf("Giga Drain","Energy Ball","Earth Power","Pollen Puff"),
        937 to listOf("Bitter Blade","Shadow Claw","Psycho Cut","Swords Dance"),
        936 to listOf("Armor Cannon","Psychic","Energy Ball","Calm Mind"),
        407 to listOf("Giga Drain","Sludge Bomb","Dazzling Gleam","Toxic"),
        398 to listOf("Brave Bird","Close Combat","U-turn","Quick Attack"),
        445 to listOf("Earthquake","Dragon Claw","Rock Slide","Swords Dance"),
        78 to listOf("Flare Blitz","High Horsepower","Megahorn","Will-O-Wisp"),
        902 to listOf("Wave Crash","Shadow Ball","Crunch","Aqua Jet"),
        405 to listOf("Wild Charge","Crunch","Ice Fang","Thunder Wave"),
        68 to listOf("Close Combat","Rock Slide","Bullet Punch","Bulk Up"),
        6 to listOf("Flamethrower","Air Slash","Dragon Pulse","Roost"),
        34 to listOf("Earthquake","Poison Jab","Megahorn","Rock Slide"),
        131 to listOf("Surf","Ice Beam","Thunderbolt","Psychic"),
        3 to listOf("Mega Drain","Sludge Bomb","Sleep Powder","Leech Seed")
    )
}
