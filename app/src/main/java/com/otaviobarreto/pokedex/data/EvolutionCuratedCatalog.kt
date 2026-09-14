package com.otaviobarreto.pokedex.data

data class CuratedEvolutionRule(
    val targetPokemonId:Int,
    val requirement:String,
    val sourceFormKey:String?=null,
    val targetFormKey:String?=null
)

object EvolutionCuratedCatalog {
    fun rulesFor(targetPokemonId:Int,context:GameContext?):List<CuratedEvolutionRule> {
        val game=context?.label.orEmpty()
        return when(targetPokemonId){
            65 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(65,"Usar Linking Cord em Kadabra"))
                else -> emptyList()
            }
            68 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(68,"Usar Linking Cord em Machoke"))
                else -> emptyList()
            }
            76 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(76,"Usar Linking Cord em Graveler"))
                else -> emptyList()
            }
            94 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(94,"Usar Linking Cord em Haunter"))
                else -> emptyList()
            }
            208 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(208,"Usar Metal Coat em Onix"))
                else -> emptyList()
            }
            212 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(212,"Usar Metal Coat em Scyther"))
                else -> emptyList()
            }
            233 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(233,"Usar Upgrade em Porygon"))
                else -> emptyList()
            }
            464 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(464,"Usar Protector em Rhydon"))
                else -> emptyList()
            }
            466 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(466,"Usar Electirizer em Electabuzz"))
                else -> emptyList()
            }
            467 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(467,"Usar Magmarizer em Magmar"))
                else -> emptyList()
            }
            474 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(474,"Usar Dubious Disc em Porygon2"))
                else -> emptyList()
            }
            477 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(477,"Usar Reaper Cloth em Dusclops"))
                else -> emptyList()
            }
            113 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(113,"Usar Oval Stone em Happiny durante o dia"))
                "Brilliant Diamond / Shining Pearl","Sword / Shield","Scarlet / Violet" ->
                    listOf(CuratedEvolutionRule(113,"Subir de nível durante o dia segurando Oval Stone"))
                else -> emptyList()
            }
            413 -> listOf(CuratedEvolutionRule(413,"Burmy fêmea sobe ao nível 20 • A forma de Wormadam depende do Cloak atual","burmy",null))
            414 -> listOf(CuratedEvolutionRule(414,"Burmy macho sobe ao nível 20","burmy",null))
            416 -> listOf(CuratedEvolutionRule(416,"Combee fêmea sobe ao nível 21"))
            461 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(461,"Usar Razor Claw em Sneasel durante a noite"))
                else -> listOf(CuratedEvolutionRule(461,"Subir de nível durante a noite segurando Razor Claw"))
            }
            472 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(472,"Usar Razor Fang em Gligar durante a noite"))
                else -> listOf(CuratedEvolutionRule(472,"Subir de nível durante a noite segurando Razor Fang"))
            }
            475 -> listOf(CuratedEvolutionRule(475,"Usar Dawn Stone em Kirlia macho"))
            478 -> listOf(CuratedEvolutionRule(478,"Usar Dawn Stone em Snorunt fêmea"))
            745 -> when(game){
                "Sun / Moon","Ultra Sun / Ultra Moon","Sword / Shield","Scarlet / Violet" -> listOf(
                    CuratedEvolutionRule(745,"Rockruff sobe ao nível 25 ou mais durante o dia",targetFormKey="lycanroc-midday"),
                    CuratedEvolutionRule(745,"Rockruff sobe ao nível 25 ou mais durante a noite",targetFormKey="lycanroc-midnight"),
                    CuratedEvolutionRule(745,"Rockruff com Own Tempo sobe ao nível 25 ou mais ao entardecer",sourceFormKey="rockruff-own-tempo",targetFormKey="lycanroc-dusk")
                )
                else -> emptyList()
            }
            758 -> listOf(CuratedEvolutionRule(758,"Salandit fêmea sobe ao nível 33"))
            849 -> listOf(
                CuratedEvolutionRule(849,"Toxel sobe ao nível 30 • Nature compatível com Amped Form",targetFormKey="toxtricity-amped"),
                CuratedEvolutionRule(849,"Toxel sobe ao nível 30 • Nature compatível com Low Key Form",targetFormKey="toxtricity-low-key")
            )
            841 -> when(game){
                "Sword / Shield","Scarlet / Violet" -> listOf(CuratedEvolutionRule(841,"Usar Tart Apple em Applin"))
                else -> emptyList()
            }
            842 -> when(game){
                "Sword / Shield","Scarlet / Violet" -> listOf(CuratedEvolutionRule(842,"Usar Sweet Apple em Applin"))
                else -> emptyList()
            }
            855 -> when(game){
                "Sword / Shield","Scarlet / Violet" -> listOf(
                    CuratedEvolutionRule(855,"Usar Cracked Pot em Sinistea Phony Form"),
                    CuratedEvolutionRule(855,"Usar Chipped Pot em Sinistea Antique Form")
                )
                else -> emptyList()
            }
            936 -> when(game){
                "Scarlet / Violet" -> listOf(CuratedEvolutionRule(936,"Usar Auspicious Armor em Charcadet"))
                else -> emptyList()
            }
            937 -> when(game){
                "Scarlet / Violet" -> listOf(CuratedEvolutionRule(937,"Usar Malicious Armor em Charcadet"))
                else -> emptyList()
            }
            1011 -> when(game){
                "Scarlet / Violet" -> listOf(CuratedEvolutionRule(1011,"Usar Syrupy Apple em Applin"))
                else -> emptyList()
            }
            1013 -> when(game){
                "Scarlet / Violet" -> listOf(
                    CuratedEvolutionRule(1013,"Usar Unremarkable Teacup em Poltchageist Counterfeit Form"),
                    CuratedEvolutionRule(1013,"Usar Masterpiece Teacup em Poltchageist Artisan Form")
                )
                else -> emptyList()
            }
            1018 -> when(game){
                "Scarlet / Violet" -> listOf(CuratedEvolutionRule(1018,"Usar Metal Alloy em Duraludon"))
                else -> emptyList()
            }
            350 -> when(game){
                "Brilliant Diamond / Shining Pearl" -> listOf(
                    CuratedEvolutionRule(350,"Subir de nível com Beauty 170 ou mais")
                )
                "Omega Ruby / Alpha Sapphire" -> listOf(
                    CuratedEvolutionRule(350,"Subir de nível com Beauty 170 ou mais"),
                    CuratedEvolutionRule(350,"Trocar Feebas segurando Prism Scale")
                )
                "Black / White","X / Y","Sword / Shield","Scarlet / Violet" -> listOf(
                    CuratedEvolutionRule(350,"Trocar Feebas segurando Prism Scale")
                )
                else -> emptyList()
            }
            462 -> when(game){
                "Sword / Shield" -> listOf(CuratedEvolutionRule(462,"Usar Thunder Stone"))
                "Brilliant Diamond / Shining Pearl" -> listOf(
                    CuratedEvolutionRule(462,"Usar Thunder Stone"),
                    CuratedEvolutionRule(462,"Subir de nível em Mt. Coronet")
                )
                else -> emptyList()
            }
            470 -> when(game){
                "Sword / Shield" -> listOf(CuratedEvolutionRule(470,"Usar Leaf Stone"))
                "Brilliant Diamond / Shining Pearl" -> listOf(
                    CuratedEvolutionRule(470,"Usar Leaf Stone"),
                    CuratedEvolutionRule(470,"Subir de nível próximo à Mossy Rock em Eterna Forest")
                )
                else -> emptyList()
            }
            471 -> when(game){
                "Sword / Shield" -> listOf(CuratedEvolutionRule(471,"Usar Ice Stone"))
                "Brilliant Diamond / Shining Pearl" -> listOf(
                    CuratedEvolutionRule(471,"Usar Ice Stone"),
                    CuratedEvolutionRule(471,"Subir de nível próximo à Icy Rock na Route 217")
                )
                else -> emptyList()
            }
            687 -> when(game){
                "X / Y" -> listOf(CuratedEvolutionRule(687,"Subir ao nível 30 ou mais • Virar o console de cabeça para baixo ao subir de nível"))
                else -> emptyList()
            }
            700 -> when(game){
                "X / Y" -> listOf(CuratedEvolutionRule(700,"Subir de nível conhecendo um golpe do tipo Fairy • Ter pelo menos 2 corações de Affection no Pokémon-Amie"))
                "Sword / Shield","Scarlet / Violet" -> listOf(CuratedEvolutionRule(700,"Alta amizade • Subir de nível conhecendo um golpe do tipo Fairy"))
                else -> emptyList()
            }
            706 -> when(game){
                "X / Y","Omega Ruby / Alpha Sapphire","Sword / Shield","Scarlet / Violet" ->
                    listOf(CuratedEvolutionRule(706,"Subir ao nível 50 ou mais enquanto estiver chovendo"))
                else -> emptyList()
            }
            738 -> when(game){
                "Sword / Shield","Scarlet / Violet" -> listOf(CuratedEvolutionRule(738,"Usar Thunder Stone"))
                else -> emptyList()
            }
            865 -> when(game){
                "Sword / Shield" -> listOf(CuratedEvolutionRule(865,"Acertar 3 golpes críticos na mesma batalha com Galarian Farfetch'd","farfetchd-galar",null))
                else -> emptyList()
            }
            867 -> when(game){
                "Sword / Shield" -> listOf(CuratedEvolutionRule(867,"Galarian Yamask deve perder pelo menos 49 HP sem desmaiar • Passar sob o arco de pedra em Dusty Bowl","yamask-galar",null))
                "Pokémon Legends: Z-A" -> listOf(CuratedEvolutionRule(867,"Galarian Yamask deve perder pelo menos 49 HP sem desmaiar • Passar sob uma das pontes do Coulant Waterway","yamask-galar",null))
                else -> emptyList()
            }
            892 -> when(game){
                "Sword / Shield" -> listOf(
                    CuratedEvolutionRule(892,"Concluir a Tower of Darkness e interagir com o Scroll of Darkness"),
                    CuratedEvolutionRule(892,"Concluir a Tower of Waters e interagir com o Scroll of Waters")
                )
                "Scarlet / Violet" -> listOf(
                    CuratedEvolutionRule(892,"Usar Scroll of Darkness"),
                    CuratedEvolutionRule(892,"Usar Scroll of Waters")
                )
                else -> emptyList()
            }
            899 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(899,"Usar Psyshield Bash em Agile Style 20 vezes • Depois evoluir"))
                "" -> listOf(CuratedEvolutionRule(899,"Usar Psyshield Bash em Agile Style 20 vezes • Depois evoluir"))
                else -> listOf(CuratedEvolutionRule(899,"Evolução indisponível neste jogo; evolua Stantler em Pokémon Legends: Arceus e transfira pelo Pokémon HOME"))
            }
            900 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(900,"Usar Black Augurite em Scyther"))
                "" -> listOf(CuratedEvolutionRule(900,"Usar Black Augurite em Scyther"))
                else -> listOf(CuratedEvolutionRule(900,"Evolução indisponível neste jogo; evolua Scyther em Pokémon Legends: Arceus e transfira pelo Pokémon HOME"))
            }
            901 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(901,"Usar Peat Block em Ursaring durante lua cheia"))
                "" -> listOf(CuratedEvolutionRule(901,"Usar Peat Block em Ursaring durante lua cheia"))
                else -> listOf(CuratedEvolutionRule(901,"Evolução indisponível neste jogo; evolua Ursaring em Pokémon Legends: Arceus e transfira pelo Pokémon HOME"))
            }
            902 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(902,"White-Striped Basculin deve acumular pelo menos 294 de dano de recoil sem desmaiar","basculin-white-striped",null))
                "Scarlet / Violet" -> listOf(CuratedEvolutionRule(902,"White-Striped Basculin deve acumular pelo menos 294 de dano de recoil sem desmaiar • Depois subir de nível","basculin-white-striped",null))
                "" -> listOf(CuratedEvolutionRule(902,"White-Striped Basculin deve acumular pelo menos 294 de dano de recoil sem desmaiar","basculin-white-striped",null))
                else -> emptyList()
            }
            903 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(903,"Usar Razor Claw em Hisuian Sneasel durante o dia","sneasel-hisui",null))
                else -> emptyList()
            }
            904 -> when(game){
                "Legends Arceus" -> listOf(CuratedEvolutionRule(904,"Usar Barb Barrage em Strong Style 20 vezes • Depois evoluir","qwilfish-hisui",null))
                "Scarlet / Violet" -> listOf(CuratedEvolutionRule(904,"Subir de nível conhecendo Barb Barrage","qwilfish-hisui",null))
                "Pokémon Legends: Z-A" -> listOf(CuratedEvolutionRule(904,"Usar Barb Barrage 20 vezes","qwilfish-hisui",null))
                "" -> listOf(CuratedEvolutionRule(904,"Usar Barb Barrage em Strong Style 20 vezes • Depois evoluir","qwilfish-hisui",null))
                else -> emptyList()
            }
            923 -> listOf(CuratedEvolutionRule(923,"Caminhar 1.000 passos com Pawmo no modo Let's Go • Depois subir de nível"))
            947 -> listOf(CuratedEvolutionRule(947,"Caminhar 1.000 passos com Bramblin no modo Let's Go • Depois subir de nível"))
            954 -> listOf(CuratedEvolutionRule(954,"Caminhar 1.000 passos com Rellor no modo Let's Go • Depois subir de nível"))
            964 -> listOf(CuratedEvolutionRule(964,"Subir Finizen ao nível 38 ou mais enquanto estiver em uma sessão multiplayer/Union Circle"))
            979 -> listOf(CuratedEvolutionRule(979,"Usar Rage Fist 20 vezes • Depois subir de nível"))
            983 -> listOf(CuratedEvolutionRule(983,"Bisharp segurando Leader's Crest • Derrotar 3 Bisharp líderes que também seguram Leader's Crest • Depois subir de nível"))
            1000 -> listOf(CuratedEvolutionRule(1000,"Coletar 999 Gimmighoul Coins • Depois subir Gimmighoul de nível"))
            1019 -> listOf(CuratedEvolutionRule(1019,"Dipplin evolui ao subir de nível conhecendo Dragon Cheer"))
            else -> emptyList()
        }
    }

    fun ruleFor(targetPokemonId:Int,context:GameContext?):CuratedEvolutionRule? =
        rulesFor(targetPokemonId,context).firstOrNull()

    fun formKeysFor(targetPokemonId:Int,context:GameContext?):Pair<String?,String?> {
        val rule=ruleFor(targetPokemonId,context)
        if(rule?.sourceFormKey!=null || rule?.targetFormKey!=null){
            return rule.sourceFormKey to rule.targetFormKey
        }
        return contextualFormKeys(targetPokemonId,context)
    }

    private fun contextualFormKeys(targetPokemonId:Int,context:GameContext?):Pair<String?,String?> {
        return when(context?.label){
            "Sword / Shield" -> when(targetPokemonId){
                78 -> "ponyta-galar" to "rapidash-galar"
                80 -> "slowpoke-galar" to "slowbro-galar"
                110 -> null to "weezing-galar"
                122 -> "mime-jr" to "mr-mime-galar"
                199 -> "slowpoke-galar" to "slowking-galar"
                264 -> "zigzagoon-galar" to "linoone-galar"
                555 -> "darumaka-galar" to "darmanitan-galar"
                862 -> "linoone-galar" to null
                863 -> "meowth-galar" to null
                864 -> "corsola-galar" to null
                866 -> "mr-mime-galar" to null
                else -> null to null
            }
            "Legends Arceus" -> when(targetPokemonId){
                157 -> null to "typhlosion-hisui"
                503 -> null to "samurott-hisui"
                549 -> null to "lilligant-hisui"
                628 -> null to "braviary-hisui"
                705 -> "goomy" to "sliggoo-hisui"
                706 -> "sliggoo-hisui" to "goodra-hisui"
                713 -> "bergmite" to "avalugg-hisui"
                724 -> null to "decidueye-hisui"
                else -> null to null
            }
            else -> null to null
        }
    }
}
