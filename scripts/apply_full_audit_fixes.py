from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


journey_path = Path("app/src/main/java/com/otaviobarreto/pokedex/ui/JourneyScreen.kt")
journey = journey_path.read_text(encoding="utf-8")

journey = replace_once(
    journey,
    "Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Md)){\n                    Row(verticalAlignment=Alignment.CenterVertically){\n                        Column(Modifier.weight(1f)){\n                            Text(\"Progresso da campanha\",style=MaterialTheme.typography.labelMedium)\n                            Text(\n                                completedCount.toString()+\" / \"+steps.size+\" objetivos\",\n                                fontWeight=FontWeight.Black,\n                                style=MaterialTheme.typography.titleLarge\n                            )",
    "Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Sm)){\n                    Row(verticalAlignment=Alignment.CenterVertically){\n                        Column(Modifier.weight(1f)){\n                            Text(\"Progresso da campanha\",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onPrimaryContainer)\n                            Text(\n                                completedCount.toString()+\" / \"+steps.size+\" objetivos\",\n                                fontWeight=FontWeight.Black,\n                                style=MaterialTheme.typography.titleMedium\n                            )",
    "compact Journey progress typography",
)
journey = replace_once(
    journey,
    "modifier=Modifier.padding(horizontal=PokedexDesignTokens.Spacing.Md,vertical=PokedexDesignTokens.Spacing.Sm),\n                                fontWeight=FontWeight.Bold",
    "modifier=Modifier.padding(horizontal=PokedexDesignTokens.Spacing.Sm,vertical=PokedexDesignTokens.Spacing.Xs),\n                                fontWeight=FontWeight.Bold,\n                                style=MaterialTheme.typography.labelLarge",
    "compact Journey progress percentage",
)
journey = replace_once(
    journey,
    "modifier=Modifier.fillMaxWidth().padding(top=PokedexDesignTokens.Spacing.Sm).height(6.dp)",
    "modifier=Modifier.fillMaxWidth().padding(top=PokedexDesignTokens.Spacing.Xs).height(4.dp)",
    "compact Journey progress bar",
)
journey = replace_once(
    journey,
    "Modifier.fillMaxWidth().padding(top=PokedexDesignTokens.Spacing.Sm).horizontalScroll(rememberScrollState()),\n                        horizontalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)",
    "Modifier.fillMaxWidth().padding(top=PokedexDesignTokens.Spacing.Xs).horizontalScroll(rememberScrollState()),\n                        horizontalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Xs)",
    "compact Journey milestone row",
)
journey = replace_once(
    journey,
    "isNext->MaterialTheme.colorScheme.primaryContainer.copy(alpha=.58f)",
    "isNext->MaterialTheme.colorScheme.surfaceContainerHigh",
    "neutral current objective card",
)
journey = replace_once(
    journey,
    "JourneyVisualThumb(it,Modifier.size(if(isNext)82.dp else 62.dp))",
    "JourneyVisualThumb(it,Modifier.size(if(isNext)96.dp else 62.dp))",
    "prominent current objective artwork",
)
journey = replace_once(
    journey,
    "                Text(\n                    step.subtitle,\n                    style=MaterialTheme.typography.bodyMedium,\n                    color=MaterialTheme.colorScheme.onSurfaceVariant\n                )",
    "                if(!displayTitle.contains(step.subtitle,ignoreCase=true)){\n                    Text(\n                        step.subtitle,\n                        style=MaterialTheme.typography.bodyMedium,\n                        color=MaterialTheme.colorScheme.onSurfaceVariant\n                    )\n                }",
    "remove duplicated objective subtitle",
)
journey = replace_once(
    journey,
    """                detail?.opponents?.takeIf{it.isNotEmpty() && !isNext}?.let{members->
                    HorizontalDivider(Modifier.padding(top=PokedexDesignTokens.Spacing.Md,bottom=PokedexDesignTokens.Spacing.Sm))
                    Text(
                        if(step.kind==JourneyChallengeKind.TITAN)\"ALVO\" else \"EQUIPE\",
                        style=MaterialTheme.typography.labelSmall,
                        fontWeight=FontWeight.Black,
                        color=MaterialTheme.colorScheme.primary
                    )
                    LazyRow(
                        Modifier.fillMaxWidth().padding(top=7.dp),
                        horizontalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)
                    ){
                        items(
                            items=members.take(6).mapIndexed { index, member -> member to opponentPokemonIds.getOrNull(index) },
                            key={it.first.name+\"_\"+it.first.level},
                            contentType={\"opponent\"}
                        ){(member,pokemonId)->
                            JourneyOpponentMiniCard(
                                name=member.name,
                                level=member.level,
                                pokemonId=pokemonId
                            )
                        }
                    }
                }""",
    """                val resolvedOpponents=detail?.opponents.orEmpty().mapIndexedNotNull{index,member->
                    opponentPokemonIds.getOrNull(index)?.let{pokemonId->member to pokemonId}
                }
                if(resolvedOpponents.isNotEmpty()){
                    HorizontalDivider(Modifier.padding(top=PokedexDesignTokens.Spacing.Md,bottom=PokedexDesignTokens.Spacing.Sm))
                    Text(
                        if(step.kind==JourneyChallengeKind.TITAN)\"ALVO\" else \"EQUIPE\",
                        style=MaterialTheme.typography.labelSmall,
                        fontWeight=FontWeight.Black,
                        color=MaterialTheme.colorScheme.primary
                    )
                    LazyRow(
                        Modifier.fillMaxWidth().padding(top=7.dp),
                        horizontalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)
                    ){
                        items(
                            items=resolvedOpponents.take(6),
                            key={it.first.name+\"_\"+it.first.level},
                            contentType={\"opponent\"}
                        ){(member,pokemonId)->
                            JourneyOpponentMiniCard(
                                name=member.name,
                                level=member.level,
                                pokemonId=pokemonId
                            )
                        }
                    }
                }""",
    "render only real opponent artwork and include current objective team",
)
journey = replace_once(
    journey,
    """private fun JourneyOpponentMiniCard(
    name:String,
    level:String,
    pokemonId:Int?
){""",
    """private fun JourneyOpponentMiniCard(
    name:String,
    level:String,
    pokemonId:Int
){""",
    "non-null opponent artwork id",
)
journey = replace_once(
    journey,
    """                if(pokemonId!=null){
                    PokemonArtwork(
                        model=\"https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/\"+pokemonId+\".png\",
                        contentDescription=name,
                        modifier=Modifier.fillMaxSize().padding(4.dp),
                        pokemonId=pokemonId
                    )
                }else{
                    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                        Icon(Icons.Default.CatchingPokemon,null,Modifier.size(24.dp))
                    }
                }""",
    """                PokemonArtwork(
                    model=\"https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/\"+pokemonId+\".png\",
                    contentDescription=name,
                    modifier=Modifier.fillMaxSize().padding(4.dp),
                    pokemonId=pokemonId
                )""",
    "remove generic opponent placeholder",
)
journey = replace_once(
    journey,
    "Modifier.padding(horizontal=8.dp,vertical=6.dp),\n            verticalAlignment=Alignment.CenterVertically\n        ){\n            Icon(icon,null,Modifier.size(15.dp))",
    "Modifier.padding(horizontal=7.dp,vertical=4.dp),\n            verticalAlignment=Alignment.CenterVertically\n        ){\n            Icon(icon,null,Modifier.size(13.dp))",
    "compact Journey milestone chips",
)
journey_path.write_text(journey, encoding="utf-8")

collection_path = Path("app/src/main/java/com/otaviobarreto/pokedex/ui/CollectionScreen.kt")
collection = collection_path.read_text(encoding="utf-8")
collection = replace_once(
    collection,
    'item{AlbumPortalCard("Shiny Dex","${plan.shinySpecies} espécies Shiny registradas",if(plan.totalSpecies==0)0f else plan.shinySpecies.toFloat()/plan.totalSpecies,listOf(25,94,448),true,Icons.Default.AutoAwesome,ownedIds=setOf(25,94,448)){onOpenArea(CollectionArea.SHINY)}}',
    'item{AlbumPortalCard("Shiny Dex","${plan.shinySpecies} espécies Shiny registradas",if(plan.totalSpecies==0)0f else plan.shinySpecies.toFloat()/plan.totalSpecies,listOf(25,94,448),true,Icons.Default.AutoAwesome,ownedIds=shinyIds){onOpenArea(CollectionArea.SHINY)}}',
    "Shiny Dex preview ownership",
)
collection_path.write_text(collection, encoding="utf-8")

print("Full audit UX fixes applied successfully")
