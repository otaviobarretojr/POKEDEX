package com.otaviobarreto.pokedex.data

data class CuratedEvolutionRule(
    val targetPokemonId:Int,
    val requirement:String,
    val sourceFormKey:String?=null,
    val targetFormKey:String?=null
)

object EvolutionCuratedCatalog {
    fun ruleFor(targetPokemonId:Int,context:GameContext?):CuratedEvolutionRule? {
        val game=context?.label.orEmpty()
        return when(targetPokemonId){
            865 -> when(game){
                "Sword / Shield" -> CuratedEvolutionRule(865,"Acertar 3 golpes críticos na mesma batalha com Galarian Farfetch'd","farfetchd-galar",null)
                else -> null
            }
            867 -> when(game){
                "Sword / Shield" -> CuratedEvolutionRule(867,"Galarian Yamask deve perder pelo menos 49 HP sem desmaiar • Passar sob o arco de pedra em Dusty Bowl","yamask-galar",null)
                "Pokémon Legends: Z-A" -> CuratedEvolutionRule(867,"Galarian Yamask deve perder pelo menos 49 HP sem desmaiar • Passar sob uma das pontes do Coulant Waterway","yamask-galar",null)
                else -> null
            }
            899 -> when(game){
                "Legends Arceus" -> CuratedEvolutionRule(899,"Usar Psyshield Bash em Agile Style 20 vezes • Depois evoluir",null,null)
                "" -> CuratedEvolutionRule(899,"Usar Psyshield Bash em Agile Style 20 vezes • Depois evoluir")
                else -> CuratedEvolutionRule(899,"Evolução indisponível neste jogo; evolua Stantler em Pokémon Legends: Arceus e transfira pelo Pokémon HOME")
            }
            900 -> when(game){
                "Legends Arceus" -> CuratedEvolutionRule(900,"Usar Black Augurite em Scyther",null,null)
                "" -> CuratedEvolutionRule(900,"Usar Black Augurite em Scyther")
                else -> CuratedEvolutionRule(900,"Evolução indisponível neste jogo; evolua Scyther em Pokémon Legends: Arceus e transfira pelo Pokémon HOME")
            }
            901 -> when(game){
                "Legends Arceus" -> CuratedEvolutionRule(901,"Usar Peat Block em Ursaring durante lua cheia")
                "" -> CuratedEvolutionRule(901,"Usar Peat Block em Ursaring durante lua cheia")
                else -> CuratedEvolutionRule(901,"Evolução indisponível neste jogo; evolua Ursaring em Pokémon Legends: Arceus e transfira pelo Pokémon HOME")
            }
            902 -> when(game){
                "Legends Arceus" -> CuratedEvolutionRule(902,"White-Striped Basculin deve acumular pelo menos 294 de dano de recoil sem desmaiar","basculin-white-striped",null)
                "Scarlet / Violet" -> CuratedEvolutionRule(902,"White-Striped Basculin deve acumular pelo menos 294 de dano de recoil sem desmaiar • Depois subir de nível","basculin-white-striped",null)
                "" -> CuratedEvolutionRule(902,"White-Striped Basculin deve acumular pelo menos 294 de dano de recoil sem desmaiar","basculin-white-striped",null)
                else -> null
            }
            903 -> when(game){
                "Legends Arceus" -> CuratedEvolutionRule(903,"Usar Razor Claw em Hisuian Sneasel durante o dia","sneasel-hisui",null)
                else -> null
            }
            904 -> when(game){
                "Legends Arceus" -> CuratedEvolutionRule(904,"Usar Barb Barrage em Strong Style 20 vezes • Depois evoluir","qwilfish-hisui",null)
                "Scarlet / Violet" -> CuratedEvolutionRule(904,"Subir de nível conhecendo Barb Barrage","qwilfish-hisui",null)
                "Pokémon Legends: Z-A" -> CuratedEvolutionRule(904,"Acertar 20 alvos com Barb Barrage","qwilfish-hisui",null)
                "" -> CuratedEvolutionRule(904,"Usar Barb Barrage em Strong Style 20 vezes • Depois evoluir","qwilfish-hisui",null)
                else -> null
            }
            923 -> CuratedEvolutionRule(923,"Caminhar 1.000 passos com Pawmo no modo Let's Go • Depois subir de nível")
            947 -> CuratedEvolutionRule(947,"Caminhar 1.000 passos com Bramblin no modo Let's Go • Depois subir de nível")
            954 -> CuratedEvolutionRule(954,"Caminhar 1.000 passos com Rellor no modo Let's Go • Depois subir de nível")
            964 -> CuratedEvolutionRule(964,"Subir Finizen ao nível 38 ou mais enquanto estiver em uma sessão multiplayer/Union Circle")
            979 -> CuratedEvolutionRule(979,"Usar Rage Fist 20 vezes • Depois subir de nível")
            983 -> CuratedEvolutionRule(983,"Bisharp segurando Leader's Crest • Derrotar 3 Bisharp líderes que também seguram Leader's Crest • Depois subir de nível")
            1000 -> CuratedEvolutionRule(1000,"Coletar 999 Gimmighoul Coins • Depois subir Gimmighoul de nível")
            1019 -> CuratedEvolutionRule(1019,"Dipplin evolui ao subir de nível conhecendo Dragon Cheer")
            else -> null
        }
    }

    fun formKeysFor(targetPokemonId:Int,context:GameContext?):Pair<String?,String?> {
        val rule=ruleFor(targetPokemonId,context)
        return rule?.sourceFormKey to rule?.targetFormKey
    }
}
