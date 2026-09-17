from pathlib import Path
p=Path('app/src/main/java/com/otaviobarreto/pokedex/ui/JourneyScreen.kt')
s=p.read_text()

def rep(old,new,label):
    global s
    if old not in s:
        raise SystemExit('missing patch target: '+label)
    s=s.replace(old,new,1)

rep(
'    var confirmReset by rememberSaveable(game.label){mutableStateOf(false)}\n',
'    var confirmReset by rememberSaveable(game.label){mutableStateOf(false)}\n    var routeMenuExpanded by rememberSaveable(game.label){mutableStateOf(false)}\n',
'menu state'
)

rep(
'''                TextButton(onClick=onTeam){
                    Icon(Icons.Default.Groups,null,Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Meu time")
                }
''',
'''                TextButton(onClick=onTeam){
                    Icon(Icons.Default.Groups,null,Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Meu time")
                }
                Box{
                    IconButton(onClick={routeMenuExpanded=true}){Icon(Icons.Default.MoreVert,"Gerenciar Jornada")}
                    DropdownMenu(expanded=routeMenuExpanded,onDismissRequest={routeMenuExpanded=false}){
                        DropdownMenuItem(
                            text={Text("Reiniciar Jornada")},
                            leadingIcon={Icon(Icons.Default.RestartAlt,null)},
                            onClick={routeMenuExpanded=false;confirmReset=true}
                        )
                    }
                }
''',
'header menu'
)

rep(
'                Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg)){\n                    Row(verticalAlignment=Alignment.CenterVertically){\n',
'                Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Md)){\n                    Row(verticalAlignment=Alignment.CenterVertically){\n',
'compact progress'
)
rep('.padding(top=PokedexDesignTokens.Spacing.Md).height(8.dp)', '.padding(top=PokedexDesignTokens.Spacing.Sm).height(6.dp)', 'progress bar')
rep('Modifier.fillMaxWidth().padding(top=PokedexDesignTokens.Spacing.Md).horizontalScroll(rememberScrollState())', 'Modifier.fillMaxWidth().padding(top=PokedexDesignTokens.Spacing.Sm).horizontalScroll(rememberScrollState())', 'progress pills')

rep(
'''                        Text(
                            "OBJETIVO ATUAL · "+(ui.chapter ?: JourneyTeamProgressCatalog.chapterFor(step.id)),
''',
'''                        // OBJETIVO ATUAL remains the semantic current-step section.
                        Text(
                            "CONTINUE SUA JORNADA · "+(ui.chapter ?: JourneyTeamProgressCatalog.chapterFor(step.id)),
''',
'current heading'
)

old_tools='''        if(currentUi!=null){
            item(key="journey_context_tools",contentType="tools"){
                Card(
                    shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg),
                    colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier=Modifier.fillMaxWidth().padding(bottom=PokedexDesignTokens.Spacing.Md)
                ){
                    Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg)){
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){
                                Text("Prepare o próximo passo",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium)
                                Text(
                                    if(remainingCount==1)"Último objetivo da Jornada"
                                    else remainingCount.toString()+" objetivos restantes",
                                    style=MaterialTheme.typography.labelSmall,
                                    color=MaterialTheme.colorScheme.primary
                                )
                            }
                            nextAfterCurrent?.let{
                                AssistChip(
                                    onClick={onOpenStep(it.id)},
                                    label={Text("Depois: "+journeyDisplayTitle(it),maxLines=1,overflow=TextOverflow.Ellipsis)},
                                    leadingIcon={Icon(Icons.Default.ArrowForward,null,Modifier.size(15.dp))}
                                )
                            }
                        }
                        Text(
                            smart.recommendation ?: "Use a Pokédex do jogo e a Central de evolução para preparar sua próxima etapa.",
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier=Modifier.padding(top=PokedexDesignTokens.Spacing.Xs,bottom=PokedexDesignTokens.Spacing.Md)
                        )
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)){
                            FilledTonalButton(onClick=onOpenGameDex,modifier=Modifier.weight(1f)){
                                Icon(Icons.Default.MenuBook,null,Modifier.size(17.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Dex do jogo")
                            }
                            FilledTonalButton(onClick=onOpenEvolutionCenter,modifier=Modifier.weight(1f)){
                                Icon(Icons.Default.AutoAwesome,null,Modifier.size(17.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Evoluções")
                            }
                            IconButton(onClick=onOpenSearch){Icon(Icons.Default.Search,"Busca universal")}
                        }
                    }
                }
            }
        }
'''
new_tools='''        if(currentUi!=null){
            item(key="journey_context_tools",contentType="tools"){
                Column(Modifier.fillMaxWidth().padding(bottom=PokedexDesignTokens.Spacing.Md)){
                    nextAfterCurrent?.let{next->
                        Surface(
                            shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md),
                            color=MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier=Modifier.fillMaxWidth().clickable{onOpenStep(next.id)}
                        ){
                            Row(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Md),verticalAlignment=Alignment.CenterVertically){
                                Text("A seguir",style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(PokedexDesignTokens.Spacing.Md))
                                Text(journeyDisplayTitle(next),style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
                                Icon(Icons.Default.ArrowForward,"Abrir próximo objetivo",Modifier.size(18.dp))
                            }
                        }
                    }
                    Text("FERRAMENTAS DA JORNADA",style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=PokedexDesignTokens.Spacing.Md,bottom=PokedexDesignTokens.Spacing.Xs,start=PokedexDesignTokens.Spacing.Xs))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Sm)){
                        FilledTonalButton(onClick=onOpenGameDex,modifier=Modifier.weight(1f)){Icon(Icons.Default.MenuBook,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("Dex")}
                        FilledTonalButton(onClick=onOpenEvolutionCenter,modifier=Modifier.weight(1f)){Icon(Icons.Default.AutoAwesome,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("Evoluções")}
                        FilledTonalIconButton(onClick=onOpenSearch){Icon(Icons.Default.Search,"Buscar Pokémon")}
                    }
                }
            }
        }
'''
rep(old_tools,new_tools,'context tools')

rep(
'''                FilledTonalButton(
                    onClick={showUpcoming=!showUpcoming},
''',
'''                OutlinedButton(
                    onClick={showUpcoming=!showUpcoming},
''',
'upcoming button'
)
rep(
'''                FilledTonalButton(
                    onClick={showCompleted=!showCompleted},
''',
'''                OutlinedButton(
                    onClick={showCompleted=!showCompleted},
''',
'completed button'
)

old_restart='''        item{
            TextButton(
                onClick={confirmReset=true},
                modifier=Modifier.fillMaxWidth().padding(top=PokedexDesignTokens.Spacing.Md)
            ){
                Icon(Icons.Default.RestartAlt,null)
                Spacer(Modifier.width(6.dp))
                Text("Reiniciar Jornada")
            }
        }
'''
rep(old_restart,'','restart removal')

rep('JourneyVisualThumb(it,Modifier.size(62.dp))','JourneyVisualThumb(it,Modifier.size(if(isNext)82.dp else 62.dp))','hero art size')
rep(
'''                    style=MaterialTheme.typography.titleMedium,
                    fontWeight=FontWeight.Black,
''',
'''                    style=if(isNext)MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    fontWeight=FontWeight.Black,
''',
'hero title'
)
rep('detail?.opponents?.takeIf{it.isNotEmpty()}?.let{members->','detail?.opponents?.takeIf{it.isNotEmpty() && !isNext}?.let{members->','hide current opponents')

marker='''                Row(
                    Modifier.fillMaxWidth().padding(top=10.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
'''
replacement='''                if(isNext){
                    Button(onClick=onOpen,modifier=Modifier.fillMaxWidth().padding(top=PokedexDesignTokens.Spacing.Md)){
                        Icon(Icons.Default.PlayArrow,null,Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Continuar objetivo",fontWeight=FontWeight.Bold)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top=10.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
'''
rep(marker,replacement,'continue CTA')

s=s.replace('    val smart=remember(game.label,revision){JourneySmartProgress.context(game.label)}\n','',1)
s=s.replace('    val remainingCount=remember(steps,completed){steps.count{it.id !in completed}}\n','',1)

p.write_text(s)
