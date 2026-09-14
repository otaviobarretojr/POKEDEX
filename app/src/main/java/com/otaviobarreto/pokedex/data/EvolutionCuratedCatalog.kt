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
            350 -> when(game){
                "Brilliant Diamond / Shining Pearl" -> listOf(
                    CuratedEvolutionRule(350,"Subir de nível com a condição Beauty maximizada")
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
        return rule?.sourceFormKey to rule?.targetFormKey
    }
}
