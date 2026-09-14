package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class CollectionSection(val label:String){
    OVERVIEW("Resumo"), LIVING("Living"), SHINY("Shiny"), FORMS("Formas")
}

@Composable
fun CollectionScreen(onPokemonClick:(Int)->Unit,onOpenBoxes:(String?,String?)->Unit){
    val captured=CollectionStore.capturedIds
    val variants=VariantCollectionStore.ownedVariants
    val plan=remember(captured,variants){LivingDexPlanner.current(PokeApiService.MAX_NATIONAL_DEX_ID)}
    val insights=remember(captured,variants,CollectionStore.boxes){CollectionInsightsService.current()}
    var section by rememberSaveable{mutableStateOf(CollectionSection.OVERVIEW)}
    var advisorReady by remember{mutableStateOf(CollectionAdvisor.isWarm())}

    LaunchedEffect(Unit){
        if(!advisorReady){
            withContext(Dispatchers.IO){CollectionAdvisor.warmAllGames()}
            advisorReady=true
        }
    }

    DexAppBackground{
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal=PokedexDesignTokens.Spacing.Lg),
            contentPadding=PaddingValues(top=PokedexDesignTokens.Spacing.Lg,bottom=PokedexDesignTokens.Spacing.Xxl),
            verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Lg)
        ){
            item{
                Column{
                    DexSectionEyebrow("Seu progresso")
                    Text("Coleção",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
                    Text("Living Dex, Shinies, formas e progresso de coleção ficam exclusivamente aqui.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item{
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){
                    CollectionSection.entries.forEachIndexed{index,item->
                        SegmentedButton(selected=section==item,onClick={section=item},shape=SegmentedButtonDefaults.itemShape(index,CollectionSection.entries.size),label={Text(item.label,maxLines=1)})
                    }
                }
            }
            when(section){
                CollectionSection.OVERVIEW -> overviewItems(plan,insights,advisorReady,onPokemonClick,onOpenBoxes){section=it}
                CollectionSection.LIVING -> livingItems(plan,onPokemonClick)
                CollectionSection.SHINY -> shinyItems(plan,variants,onPokemonClick)
                CollectionSection.FORMS -> formItems(plan,variants,onPokemonClick)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.overviewItems(
    plan:LivingDexPlan,insights:CollectionInsights,advisorReady:Boolean,
    onPokemonClick:(Int)->Unit,onOpenBoxes:(String?,String?)->Unit,openSection:(CollectionSection)->Unit
){
    item{
        DexGlassSurface(Modifier.fillMaxWidth().clickable{openSection(CollectionSection.LIVING)}){
            Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Column{Text("Living Dex",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black);Text("National Dex",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    Text("${(plan.speciesRatio*100).toInt()}%",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)
                }
                Text("${plan.capturedSpecies} / ${plan.totalSpecies}",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black,modifier=Modifier.padding(top=8.dp))
                LinearProgressIndicator(progress={plan.speciesRatio},modifier=Modifier.fillMaxWidth().padding(vertical=10.dp))
                Text("Faltam ${(plan.totalSpecies-plan.capturedSpecies).coerceAtLeast(0)} espécies",color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    item{
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
            CollectionMetricCard("Shiny Dex",plan.shinySpecies.toString(),"${percent(plan.shinySpecies,plan.totalSpecies)}%",Icons.Default.AutoAwesome,Modifier.weight(1f)){openSection(CollectionSection.SHINY)}
            CollectionMetricCard("Form Dex",plan.formRegistrations.toString(),"registros",Icons.Default.Extension,Modifier.weight(1f)){openSection(CollectionSection.FORMS)}
        }
    }
    item{
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
            CollectionMetricCard("Jogos",insights.gamesWithProgress.toString(),"com coleção",Icons.Default.SportsEsports,Modifier.weight(1f),null)
            CollectionMetricCard("Espécies",plan.capturedSpecies.toString(),"únicas",Icons.Default.CatchingPokemon,Modifier.weight(1f),null)
        }
    }
    item{
        val action=if(advisorReady) CollectionAdvisor.nextAction(plan.missingSpecies) else null
        Card(shape=MaterialTheme.shapes.large){
            Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg)){
                DexSectionEyebrow("Próxima captura")
                Text(action?.title ?: "Preparando recomendações…",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium)
                Text(action?.subtitle ?: "Analisando os jogos e regiões disponíveis na sua base.",color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=4.dp))
                if(action?.game!=null){
                    Button(onClick={onOpenBoxes(action.game,action.source)},modifier=Modifier.fillMaxWidth().padding(top=12.dp)){Text("Abrir "+action.game)}
                }
                if(action!=null && action.targetIds.isNotEmpty()){
                    TextButton(onClick={onPokemonClick(action.targetIds.first())},modifier=Modifier.fillMaxWidth()){Text("Ver primeiro Pokémon sugerido")}
                }
            }
        }
    }
    item{Text("Por geração",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)}
    items(plan.byGeneration,key={it.generation}){gen->
        val ratio=if(gen.total==0)0f else gen.captured.toFloat()/gen.total
        Card{
            Column(Modifier.fillMaxWidth().padding(14.dp)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text("Geração "+roman(gen.generation),fontWeight=FontWeight.Bold)
                    Text("${gen.captured}/${gen.total}")
                }
                LinearProgressIndicator(progress={ratio},modifier=Modifier.fillMaxWidth().padding(vertical=8.dp))
                Text("${gen.shiny} Shiny · ${percent(gen.shiny,gen.total)}%",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    item{Text("Por jogo",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)}
    items(insights.byGame,key={it.game}){game->
        Card(Modifier.fillMaxWidth().clickable{onOpenBoxes(game.game,null)}){
            Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){
                Column(Modifier.weight(1f)){
                    Text(game.game,fontWeight=FontWeight.Bold)
                    Text("${game.regionsWithProgress}/${game.totalRegions} região(ões) com coleção",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(game.captured.toString(),fontWeight=FontWeight.Black)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.livingItems(plan:LivingDexPlan,onPokemonClick:(Int)->Unit){
    item{CollectionSectionHeader("Living Dex","${plan.capturedSpecies}/${plan.totalSpecies}","${(plan.speciesRatio*100).toInt()}% completo")}
    if(plan.missingSpecies.isEmpty()){
        item{CollectionEmptyState("Living Dex completa","Todas as espécies da National Dex estão registradas na sua coleção.")}
    }else{
        item{Text("Espécies que faltam",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)}
        items(plan.missingSpecies,key={it}){id->
            Card(Modifier.fillMaxWidth().clickable{onPokemonClick(id)}){
                Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){
                    Text("#"+id.toString().padStart(4,'0'),fontWeight=FontWeight.Bold)
                    Text("Ver Pokémon",color=MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.shinyItems(plan:LivingDexPlan,variants:List<OwnedPokemonVariant>,onPokemonClick:(Int)->Unit){
    val shinySpecies=variants.asSequence().filter{it.shiny}.map{it.speciesId}.distinct().sorted().toList()
    item{CollectionSectionHeader("Shiny Dex","${plan.shinySpecies}/${plan.totalSpecies}","${percent(plan.shinySpecies,plan.totalSpecies)}% da National Dex em Shiny")}
    if(shinySpecies.isEmpty()){
        item{CollectionEmptyState("Nenhum Shiny registrado","Quando você registrar uma variante Shiny, ela aparecerá aqui.")}
    }else items(shinySpecies,key={it}){id->
        Card(Modifier.fillMaxWidth().clickable{onPokemonClick(id)}){
            Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){
                Text("#"+id.toString().padStart(4,'0'),fontWeight=FontWeight.Bold)
                Text("Shiny registrado",color=MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.formItems(plan:LivingDexPlan,variants:List<OwnedPokemonVariant>,onPokemonClick:(Int)->Unit){
    val forms=variants.filter{!it.shiny && !it.isDefault}.distinctBy{listOf(it.source,it.speciesId.toString(),it.formPokemonId.toString(),it.formKey.lowercase())}
    item{CollectionSectionHeader("Form Dex",plan.formRegistrations.toString(),"formas alternativas registradas")}
    if(forms.isEmpty()){
        item{CollectionEmptyState("Nenhuma forma alternativa registrada","Formas regionais e especiais registradas aparecerão aqui.")}
    }else items(forms,key={it.key}){form->
        Card(Modifier.fillMaxWidth().clickable{onPokemonClick(form.speciesId)}){
            Column(Modifier.fillMaxWidth().padding(14.dp)){
                Text(form.formName,fontWeight=FontWeight.Bold)
                Text("#"+form.speciesId.toString().padStart(4,'0')+" · "+form.source,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CollectionMetricCard(title:String,value:String,subtitle:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier,onClick:(()->Unit)?){
    Card(modifier.then(if(onClick!=null)Modifier.clickable(onClick=onClick) else Modifier)){
        Column(Modifier.fillMaxWidth().padding(14.dp)){
            Icon(icon,null,tint=MaterialTheme.colorScheme.primary)
            Text(value,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black,modifier=Modifier.padding(top=8.dp))
            Text(title,fontWeight=FontWeight.Bold)
            Text(subtitle,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CollectionSectionHeader(title:String,value:String,subtitle:String){
    DexGlassSurface(Modifier.fillMaxWidth()){
        Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg)){
            DexSectionEyebrow("Coleção")
            Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
            Text(value,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black)
            Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CollectionEmptyState(title:String,subtitle:String){
    Card{Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg)){Text(title,fontWeight=FontWeight.Black);Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=4.dp))}}
}

private fun percent(value:Int,total:Int):Int=if(total<=0)0 else ((value.toFloat()/total)*100).toInt().coerceIn(0,100)
private fun roman(value:Int)=when(value){1->"I";2->"II";3->"III";4->"IV";5->"V";6->"VI";7->"VII";8->"VIII";9->"IX";else->value.toString()}
