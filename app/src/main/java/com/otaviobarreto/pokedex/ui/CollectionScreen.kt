package com.otaviobarreto.pokedex.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class CollectionArea { HOME, LIVING, SHINY, FORMS }

@Composable
fun CollectionScreen(onPokemonClick:(Int)->Unit,onOpenBoxes:(String?,String?)->Unit){
    val captured=CollectionStore.capturedIds
    val variants=VariantCollectionStore.ownedVariants
    val plan=remember(captured,variants){LivingDexPlanner.current(PokeApiService.MAX_NATIONAL_DEX_ID)}
    val insights=remember(captured,variants,CollectionStore.boxes){CollectionInsightsService.current()}
    var areaName by rememberSaveable{mutableStateOf(CollectionArea.HOME.name)}
    var generation by rememberSaveable{mutableStateOf<Int?>(null)}
    var advisorReady by remember{mutableStateOf(CollectionAdvisor.isWarm())}
    val area=CollectionArea.valueOf(areaName)

    LaunchedEffect(Unit){
        if(!advisorReady){
            withContext(Dispatchers.IO){CollectionAdvisor.warmAllGames()}
            advisorReady=true
        }
    }

    BackHandler(enabled=area!=CollectionArea.HOME || generation!=null){
        if(generation!=null) generation=null else areaName=CollectionArea.HOME.name
    }

    DexAppBackground{
        when{
            area==CollectionArea.HOME -> CollectionHome(plan,insights,advisorReady,{areaName=it.name},onOpenBoxes,onPokemonClick)
            area==CollectionArea.FORMS -> FormsAlbum(variants,{areaName=CollectionArea.HOME.name},onPokemonClick)
            generation==null -> GenerationShelf(
                if(area==CollectionArea.LIVING)"Living Dex" else "Shiny Dex",
                if(area==CollectionArea.LIVING)"Complete cada geração da National Dex." else "Sua coleção Shiny organizada por geração.",
                plan,
                area==CollectionArea.SHINY,
                {areaName=CollectionArea.HOME.name},
                {generation=it}
            )
            else -> PokemonAlbumGrid(generation!!,area==CollectionArea.SHINY,captured,variants,{generation=null},onPokemonClick)
        }
    }
}

@Composable
private fun CollectionHome(
    plan:LivingDexPlan,
    insights:CollectionInsights,
    advisorReady:Boolean,
    onOpenArea:(CollectionArea)->Unit,
    onOpenBoxes:(String?,String?)->Unit,
    onPokemonClick:(Int)->Unit
){
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal=PokedexDesignTokens.Spacing.Lg),
        contentPadding=PaddingValues(top=PokedexDesignTokens.Spacing.Lg,bottom=PokedexDesignTokens.Spacing.Xxl),
        verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Lg)
    ){
        item{
            CompanionContextHeader(
                title="Coleção",
                eyebrow="Sua coleção",
                subtitle="Espécies, Shinies e formas que você já registrou.",
                progress={
                    Text(
                        "${plan.capturedSpecies} de ${plan.totalSpecies} espécies",
                        style=MaterialTheme.typography.labelLarge,
                        fontWeight=FontWeight.Bold
                    )
                }
            )
        }
        item{CollectionHero(plan)}
        item{AlbumPortalCard("Living Dex","${plan.capturedSpecies} de ${plan.totalSpecies} espécies",plan.speciesRatio,listOf(1,4,7),false,Icons.Default.CatchingPokemon){onOpenArea(CollectionArea.LIVING)}}
        item{AlbumPortalCard("Shiny Dex","${plan.shinySpecies} espécies Shiny registradas",if(plan.totalSpecies==0)0f else plan.shinySpecies.toFloat()/plan.totalSpecies,listOf(25,94,448),true,Icons.Default.AutoAwesome){onOpenArea(CollectionArea.SHINY)}}
        item{AlbumPortalCard("Form Dex","${plan.formRegistrations} formas alternativas registradas",null,listOf(26,157,724),false,Icons.Default.Extension){onOpenArea(CollectionArea.FORMS)}}
        item{Text("${insights.gamesWithProgress} jogo(s) com coleção registrada",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}

@Composable
private fun CollectionHero(plan:LivingDexPlan){
    DexGlassSurface(Modifier.fillMaxWidth()){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
            Column(Modifier.weight(1f)){
                DexSectionEyebrow("National Dex")
                Text("${(plan.speciesRatio*100).toInt()}%",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)
                Text("${plan.capturedSpecies} / ${plan.totalSpecies}",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                Text("Faltam ${(plan.totalSpecies-plan.capturedSpecies).coerceAtLeast(0)} espécies",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PokemonArtwork(model=artwork(25,false),contentDescription="Pikachu",pokemonId=25,modifier=Modifier.size(112.dp))
        }
        LinearProgressIndicator(progress={plan.speciesRatio},modifier=Modifier.fillMaxWidth().padding(top=12.dp))
    }
}

@Composable
private fun AlbumPortalCard(
    title:String,subtitle:String,progress:Float?,ids:List<Int>,shiny:Boolean,
    icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit
){
    Surface(
        modifier=Modifier.fillMaxWidth().clickable(onClick=onClick),
        shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg),
        color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.30f)
    ){
        Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Icon(icon,null,tint=MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)){
                    Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
                    Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                Modifier.fillMaxWidth().padding(top=PokedexDesignTokens.Spacing.Md),
                horizontalArrangement=Arrangement.SpaceEvenly,
                verticalAlignment=Alignment.CenterVertically
            ){
                ids.forEach{id->
                    PokemonArtwork(
                        model=artwork(id,shiny),
                        contentDescription=null,
                        pokemonId=id,
                        modifier=Modifier.size(76.dp)
                    )
                }
            }
            if(progress!=null) LinearProgressIndicator(progress={progress.coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().padding(top=8.dp))
        }
    }
}

@Composable
private fun GenerationShelf(
    title:String,subtitle:String,plan:LivingDexPlan,shiny:Boolean,onBack:()->Unit,onGeneration:(Int)->Unit
){
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal=PokedexDesignTokens.Spacing.Lg),
        contentPadding=PaddingValues(top=PokedexDesignTokens.Spacing.Lg,bottom=PokedexDesignTokens.Spacing.Xxl),
        verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Md)
    ){
        item{CollectionPageHeader(title,subtitle,onBack)}
        items(plan.byGeneration,key={it.generation}){gen->
            val value=if(shiny)gen.shiny else gen.captured
            val ratio=if(gen.total==0)0f else value.toFloat()/gen.total
            val list=NationalDexCatalog.all.filter{it.generation==gen.generation}
            val representatives=if(list.size<3) list.map{it.id} else listOf(list.first().id,list[list.size/2].id,list.last().id)
            Surface(
                modifier=Modifier.fillMaxWidth().clickable{onGeneration(gen.generation)},
                shape=RoundedCornerShape(PokedexDesignTokens.Radius.Lg),
                color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.28f)
            ){
                Column(Modifier.fillMaxWidth().padding(PokedexDesignTokens.Spacing.Lg)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            DexSectionEyebrow("Geração ${roman(gen.generation)}")
                            Text(generationRegion(gen.generation),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
                            Text("$value de ${gen.total} registrados",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement=Arrangement.spacedBy(2.dp)){
                            representatives.forEach{id->PokemonArtwork(model=artwork(id,shiny),contentDescription=null,pokemonId=id,modifier=Modifier.size(50.dp))}
                        }
                    }
                    LinearProgressIndicator(progress={ratio},modifier=Modifier.fillMaxWidth().padding(top=PokedexDesignTokens.Spacing.Md))
                }
            }
        }
    }
}

@Composable
private fun PokemonAlbumGrid(
    generation:Int,shiny:Boolean,captured:Set<Int>,variants:List<OwnedPokemonVariant>,
    onBack:()->Unit,onPokemonClick:(Int)->Unit
){
    val ids=remember(generation){NationalDexCatalog.all.filter{it.generation==generation}}
    val shinyIds=remember(variants){variants.asSequence().filter{it.shiny}.map{it.speciesId}.toSet()}
    val ownedCount=ids.count{if(shiny)it.id in shinyIds else it.id in captured}
    Column(Modifier.fillMaxSize()){
        CollectionPageHeader(
            if(shiny)"${generationRegion(generation)} Shiny" else generationRegion(generation),
            "$ownedCount / ${ids.size} registrados",
            onBack,
            Modifier.padding(horizontal=PokedexDesignTokens.Spacing.Lg,vertical=PokedexDesignTokens.Spacing.Md)
        )
        LazyVerticalGrid(
            columns=GridCells.Fixed(4),
            modifier=Modifier.fillMaxSize().padding(horizontal=PokedexDesignTokens.Spacing.Md),
            contentPadding=PaddingValues(bottom=PokedexDesignTokens.Spacing.Xxl),
            verticalArrangement=Arrangement.spacedBy(8.dp),
            horizontalArrangement=Arrangement.spacedBy(8.dp)
        ){
            gridItems(ids,key={it.id}){species->
                val owned=if(shiny)species.id in shinyIds else species.id in captured
                PokemonAlbumTile(species.id,species.displayName,owned,shiny){onPokemonClick(species.id)}
            }
        }
    }
}

@Composable
private fun PokemonAlbumTile(id:Int,name:String,owned:Boolean,shiny:Boolean,onClick:()->Unit){
    val grayscale=remember{ColorMatrix().apply{setToSaturation(0f)}}
    Card(Modifier.aspectRatio(.82f).clickable(onClick=onClick),shape=RoundedCornerShape(PokedexDesignTokens.Radius.Md)){
        Column(Modifier.fillMaxSize().padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
            PokemonArtwork(
                model=artwork(id,shiny),
                contentDescription=name,
                pokemonId=id,
                modifier=Modifier.weight(1f).fillMaxWidth().alpha(if(owned)1f else .20f),
                colorFilter=if(owned)null else ColorFilter.colorMatrix(grayscale)
            )
            Text("#"+id.toString().padStart(4,'0'),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if(owned)name else "???",style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FormsAlbum(variants:List<OwnedPokemonVariant>,onBack:()->Unit,onPokemonClick:(Int)->Unit){
    val forms=variants.filter{it.countsForFormDex()}.distinctBy{listOf(it.speciesId,it.formPokemonId,it.formKey.lowercase())}
    val groups=remember(forms){forms.groupBy(::formCategory)}
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal=PokedexDesignTokens.Spacing.Lg),
        contentPadding=PaddingValues(top=PokedexDesignTokens.Spacing.Lg,bottom=PokedexDesignTokens.Spacing.Xxl),
        verticalArrangement=Arrangement.spacedBy(PokedexDesignTokens.Spacing.Lg)
    ){
        item{CollectionPageHeader("Form Dex","Formas alternativas organizadas por tipo.",onBack)}
        if(forms.isEmpty()){
            item{DexGlassSurface(Modifier.fillMaxWidth()){Text("Nenhuma forma alternativa registrada",fontWeight=FontWeight.Black);Text("Registre formas regionais e especiais para montar este álbum.",color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        }else{
            formCategoryOrder.forEach{category->
                val entries=groups[category].orEmpty()
                if(entries.isNotEmpty()){
                    item{
                        Column{
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                                Text(category,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
                                Text("${entries.size}",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
                            }
                            Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                entries.take(4).forEach{form->
                                    Card(Modifier.weight(1f).aspectRatio(.82f).clickable{onPokemonClick(form.speciesId)}){
                                        Column(Modifier.fillMaxSize().padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally){
                                            PokemonArtwork(model=form.artworkUrl,contentDescription=form.formName,pokemonId=form.formPokemonId.takeIf{it>0} ?: form.speciesId,modifier=Modifier.weight(1f).fillMaxWidth())
                                            Text(form.formName,style=MaterialTheme.typography.labelSmall,maxLines=2,overflow=TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                                repeat((4-entries.take(4).size).coerceAtLeast(0)){Spacer(Modifier.weight(1f))}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionPageHeader(title:String,subtitle:String,onBack:()->Unit,modifier:Modifier=Modifier){
    Row(modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null)}
        Column(Modifier.weight(1f)){
            Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun artwork(id:Int,shiny:Boolean)=
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"+(if(shiny)"shiny/" else "")+id+".png"

private fun generationRegion(generation:Int)=when(generation){
    1->"Kanto";2->"Johto";3->"Hoenn";4->"Sinnoh";5->"Unova";6->"Kalos";7->"Alola";8->"Galar";9->"Paldea";else->"Geração $generation"
}

private val formCategoryOrder=listOf("Formas regionais","Mega / batalha","Formas especiais","Cosméticas / gênero","Outras formas")

private fun formCategory(form:OwnedPokemonVariant):String{
    val n=(form.formName+" "+form.formKey).lowercase()
    return when{
        listOf("alola","galar","hisui","paldea").any{it in n}->"Formas regionais"
        listOf("mega","gmax","gigantamax","primal","eternamax").any{it in n}->"Mega / batalha"
        listOf("female","male","cap","cosplay","pattern","trim","cream","sweet","unown").any{it in n}->"Cosméticas / gênero"
        listOf("origin","sky","therian","black","white","crowned","hero","dusk","midnight","midday","mask","bloodmoon","roaming","chest").any{it in n}->"Formas especiais"
        else->"Outras formas"
    }
}

private fun roman(value:Int)=when(value){1->"I";2->"II";3->"III";4->"IV";5->"V";6->"VI";7->"VII";8->"VIII";9->"IX";else->value.toString()}
