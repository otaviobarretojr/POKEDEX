package com.otaviobarreto.pokedex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

private data class DetailV2Bundle(val pokemon:PokeApiService.RemotePokemonDetail,val species:PokeApiService.SpeciesInfo,val evolutions:List<PokeApiService.EvolutionStage>,val encounters:List<PokeApiService.EncounterLocation>)
private data class MoveView(val move:PokeApiService.RemoteMove,val details:List<PokeApiService.MoveLearnDetail>)

private fun cachedDetailBundle(id:Int):DetailV2Bundle?{val p=PokedexDataStore.cachedPokemon(id)?:return null;val s=PokedexDataStore.cachedSpecies(id)?:return null;val e=s.evolutionChainUrl?.let{PokedexDataStore.cachedEvolutions(it)}?:emptyList();val l=PokedexDataStore.cachedEncounters(id).orEmpty();return DetailV2Bundle(p,s,e,l)}

@Composable
fun PokemonDetailV2Screen(
    id:Int,
    source:String?=null,
    onBack:()->Unit,
    onOpenLocation:(()->Unit)?=null,
    onOpenReference:((String,String)->Unit)?=null,
    onOpenPokemon:((Int)->Unit)?=null
){
    var bundle by remember(id){ mutableStateOf(cachedDetailBundle(id)) }
    var error by remember(id){ mutableStateOf<String?>(null) }
    var retry by remember{ mutableIntStateOf(0) }
    var tab by remember(id){ mutableIntStateOf(0) }
    val context=remember(source){ GameContext.fromSource(source) }

    LaunchedEffect(id,retry){
        error=null

        val core = runCatching {
            withContext(Dispatchers.IO) {
                coroutineScope {
                    val pJob=async { PokedexDataStore.pokemon(id) }
                    val sJob=async { PokedexDataStore.species(id) }
                    pJob.await() to sJob.await()
                }
            }
        }.getOrElse {
            if(bundle==null) error="Não foi possível carregar os dados deste Pokémon."
            return@LaunchedEffect
        }

        val (pokemon,species)=core
        // Render as soon as the two core payloads are ready. Evolution and encounter
        // data are secondary and must never block opening the detail screen.
        bundle=DetailV2Bundle(
            pokemon,
            species,
            PokedexDataStore.cachedEvolutions(species.evolutionChainUrl).orEmpty(),
            PokedexDataStore.cachedEncounters(id).orEmpty()
        )

        runCatching {
            withContext(Dispatchers.IO) {
                coroutineScope {
                    val eJob=async {
                        species.evolutionChainUrl?.let { PokedexDataStore.evolutions(it) } ?: emptyList()
                    }
                    val lJob=async { PokedexDataStore.encounters(id) }
                    eJob.await() to lJob.await()
                }
            }
        }.onSuccess { (evolutions,encounters) ->
            bundle=DetailV2Bundle(pokemon,species,evolutions,encounters)
        }
    }

    when{
        bundle!=null -> DetailV2Content(
            bundle!!,tab,{tab=it},context,onBack,onOpenLocation,onOpenReference,onOpenPokemon
        )
        error!=null -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
            Column(horizontalAlignment=Alignment.CenterHorizontally){
                Text(error!!)
                Button({retry++},Modifier.padding(top=12.dp)){Text("Tentar novamente")}
            }
        }
        else -> InstantDetailShell(id,onBack)
    }
}

@Composable private fun InstantDetailShell(id:Int,onBack:()->Unit){
    val entry=remember(id){PokedexDataStore.cachedIndexEntry(id)}
    val name=entry?.name ?: "Pokémon #$id"
    val image=entry?.spriteUrl ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
    Column(Modifier.fillMaxSize().background(Color(0xFFF8F8FC))){
        Box(Modifier.fillMaxWidth().height(338.dp).background(Brush.linearGradient(listOf(Color(0xFFEAE8FB),Color(0xFFF8FBFF),Color.White)))){
            IconButton(onBack,Modifier.padding(16.dp).size(46.dp).background(Color.White.copy(alpha=.82f),CircleShape)){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar")}
            Column(Modifier.align(Alignment.CenterStart).padding(start=28.dp,top=36.dp).width(185.dp)){
                Text("#${id.toString().padStart(4,'0')}",fontSize=15.sp,color=Color(0xFF56607A),fontWeight=FontWeight.SemiBold)
                Text(name,fontSize=34.sp,lineHeight=36.sp,fontWeight=FontWeight.Black,color=Color(0xFF11152A),maxLines=2)
                Text("Carregando detalhes…",fontSize=14.sp,color=Color(0xFF667085),modifier=Modifier.padding(top=8.dp))
            }
            AsyncImage(model=image,contentDescription=name,contentScale=ContentScale.Fit,modifier=Modifier.align(Alignment.CenterEnd).padding(end=20.dp,top=50.dp).size(width=190.dp,height=220.dp))
        }
        LinearProgressIndicator(modifier=Modifier.fillMaxWidth(),color=Color(0xFF5B55E7),trackColor=Color(0xFFEAE8FB))
        Text("A ficha já está aberta. Os dados complementares estão sendo preparados em segundo plano.",modifier=Modifier.padding(18.dp),style=MaterialTheme.typography.bodyMedium,color=Color(0xFF667085))
    }
}

@Composable private fun DetailV2Content(b:DetailV2Bundle,tab:Int,setTab:(Int)->Unit,context:GameContext?,back:()->Unit,openLocation:(()->Unit)?,openRef:((String,String)->Unit)?,openPokemon:((Int)->Unit)?){val primaryType=b.pokemon.types.firstOrNull().orEmpty();val accent=typeColor(primaryType);val boxes=CollectionStore.boxesForPokemon(b.pokemon.id);Column(Modifier.fillMaxSize().background(Color(0xFFF8F8FC))){HeroCard(b,context,accent,boxes,back);DetailTabs(tab,setTab);when(tab){0->InfoTab(b,accent,context,openRef);1->V2Stats(b.pokemon.stats);2->V2Evolution(b.evolutions,b.pokemon.id,openPokemon);3->V2Moves(b.pokemon.moves,context,openRef);else->V2Locations(b.encounters,context,openLocation)}}}

@Composable private fun HeroCard(b:DetailV2Bundle,context:GameContext?,accent:Color,boxes:List<String>,back:()->Unit){val savedBox=boxes.firstOrNull();val savedGame=savedBox?.let(::gameFromBox)?:context?.label;val savedAccent=gameColor(savedGame);val inCollection=savedBox!=null;Box(Modifier.fillMaxWidth().height(338.dp).background(Brush.linearGradient(listOf(accent.copy(alpha=.16f),Color(0xFFF8FBFF),Color.White)))){repeat(5){i->Box(Modifier.offset(x=(45+i*62).dp,y=(42+i*48).dp).size((30+i*6).dp).alpha(.08f).background(accent,CircleShape))};IconButton(back,Modifier.padding(16.dp).size(46.dp).background(Color.White.copy(alpha=.82f),CircleShape)){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar")};Column(Modifier.align(Alignment.TopEnd).padding(top=14.dp,end=16.dp).widthIn(min=72.dp,max=132.dp),horizontalAlignment=Alignment.CenterHorizontally){Surface(shape=RoundedCornerShape(16.dp),color=Color.White.copy(alpha=.92f),modifier=Modifier.size(56.dp)){Box(contentAlignment=Alignment.Center){Icon(Icons.Default.CatchingPokemon,null,tint=savedAccent,modifier=Modifier.size(37.dp).alpha(if(inCollection)1f else .20f))}};Text(if(inCollection)savedGame?:"Coleção" else "Não salvo",fontSize=10.sp,fontWeight=FontWeight.Bold,color=if(inCollection)savedAccent else Color(0xFF9AA0AE),modifier=Modifier.padding(top=5.dp),maxLines=1,overflow=TextOverflow.Ellipsis);Text(if(inCollection)compactBoxName(savedBox!!) else "Segure na Box",fontSize=9.sp,color=if(inCollection)Color(0xFF50566A) else Color(0xFFA4A8B2),maxLines=1,overflow=TextOverflow.Ellipsis);Box(Modifier.padding(top=4.dp).width(54.dp).height(4.dp).background(if(inCollection)savedAccent else Color(0xFFD5D7DE),RoundedCornerShape(50)))};Column(Modifier.align(Alignment.CenterStart).padding(start=28.dp,top=48.dp).width(185.dp)){Text("#${b.pokemon.id.toString().padStart(4,'0')}",fontSize=15.sp,color=Color(0xFF56607A),fontWeight=FontWeight.SemiBold);Text(b.pokemon.name,fontSize=34.sp,lineHeight=36.sp,fontWeight=FontWeight.Black,color=Color(0xFF11152A),maxLines=2);Text(b.species.genus?:"Pokémon",fontSize=15.sp,color=Color(0xFF667085),modifier=Modifier.padding(top=4.dp),maxLines=1,overflow=TextOverflow.Ellipsis);Row(Modifier.padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){b.pokemon.types.take(2).forEach{TypeBadge(it)}};Spacer(Modifier.height(14.dp));DetailMetric(Icons.Default.Height,"${String.format("%.1f",b.pokemon.heightDecimeters/10.0)} m");DetailMetric(Icons.Default.MonitorWeight,"${String.format("%.1f",b.pokemon.weightHectograms/10.0)} kg");DetailMetric(Icons.Default.LocationOn,context?.regionLabel?:"Nacional")};Box(Modifier.align(Alignment.CenterEnd).padding(end=20.dp,top=58.dp).size(width=190.dp,height=220.dp),contentAlignment=Alignment.Center){AsyncImage(model=b.pokemon.spriteUrl,contentDescription=b.pokemon.name,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize().padding(8.dp))}}}

@Composable private fun TypeBadge(type:String){Surface(shape=RoundedCornerShape(11.dp),color=typeColor(type),modifier=Modifier.wrapContentWidth()){Row(Modifier.padding(horizontal=10.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Eco,null,tint=Color.White,modifier=Modifier.size(15.dp));Spacer(Modifier.width(5.dp));Text(type.uppercase(),color=Color.White,fontWeight=FontWeight.Bold,fontSize=12.sp,maxLines=1)}}}
private fun gameFromBox(box:String):String=when{box.contains("Scarlet / Violet",true)->"Scarlet / Violet";box.contains("Sword / Shield",true)->"Sword / Shield";box.contains("Let's Go",true)->"Let's Go Pikachu / Eevee";box.contains("Arceus",true)->"Legends Arceus";box.contains("HOME",true)->"Pokémon HOME";else->box.substringBefore(" · Box").substringBefore(" Box ").trim()}
private fun compactBoxName(box:String):String=when{box.contains("· Box",true)->box.substringAfter("· ").trim();Regex("Box \\d+",RegexOption.IGNORE_CASE).containsMatchIn(box)->Regex("Box \\d+",RegexOption.IGNORE_CASE).find(box)?.value?:box;else->box}
@Composable private fun DetailMetric(icon:androidx.compose.ui.graphics.vector.ImageVector,text:String){Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(vertical=4.dp)){Icon(icon,null,tint=Color(0xFF243477),modifier=Modifier.size(19.dp));Spacer(Modifier.width(9.dp));Text(text,fontWeight=FontWeight.SemiBold,color=Color(0xFF2F3650),maxLines=1,overflow=TextOverflow.Ellipsis)}}
@Composable private fun DetailTabs(selected:Int,setSelected:(Int)->Unit){val tabs=listOf("Info" to Icons.Default.Info,"Stats" to Icons.Default.BarChart,"Evolução" to Icons.Default.AccountTree,"Golpes" to Icons.Default.AutoAwesome,"Localização" to Icons.Default.LocationOn);Surface(color=Color.White,shadowElevation=4.dp){Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=7.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){tabs.forEachIndexed{i,(label,icon)->val active=i==selected;Surface(Modifier.weight(1f).clickable{setSelected(i)},shape=RoundedCornerShape(24.dp),color=if(active)Color(0xFF5B55E7)else Color.Transparent){Column(Modifier.padding(vertical=7.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,null,tint=if(active)Color.White else Color(0xFF243477),modifier=Modifier.size(18.dp));Text(label,fontSize=10.sp,fontWeight=FontWeight.Bold,color=if(active)Color.White else Color(0xFF1E2A55),maxLines=1)}}}}}}
@Composable private fun InfoTab(b:DetailV2Bundle,accent:Color,context:GameContext?,openRef:((String,String)->Unit)?){val captured=CollectionStore.isCaptured(b.pokemon.id);LazyColumn(Modifier.fillMaxSize().padding(horizontal=14.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{SectionCard("Coleção",Icons.Default.CatchingPokemon){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(if(captured)"Capturado" else "Ainda não capturado",fontWeight=FontWeight.Bold);Text(if(captured)"Sincronizado com Living Dex e Boxes." else "Marque como capturado para refletir no Living Dex.",style=MaterialTheme.typography.bodySmall,color=Color(0xFF667085))};FilledTonalButton(onClick={CollectionStore.toggleCaptured(b.pokemon.id)}){Text(if(captured)"Remover" else "Capturar")}}}};if(context!=null)item{SectionCard("Contexto do jogo",Icons.Default.SportsEsports){Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text(context.label,fontWeight=FontWeight.Bold);Text("Região: " + context.regionLabel,style=MaterialTheme.typography.bodyMedium);Text("Golpes e localizações são filtrados para este jogo.",style=MaterialTheme.typography.bodySmall,color=Color(0xFF667085))}}};item{SectionCard("Informações gerais",Icons.Default.Info){Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){InfoMini("Taxa de captura",b.species.captureRate.toString(),Modifier.weight(1f));InfoMini("Felicidade base",b.species.baseHappiness.toString(),Modifier.weight(1f));InfoMini("Crescimento",b.species.growthRate?:"—",Modifier.weight(1f))};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){InfoMini("Habitat",b.species.habitat?:"—",Modifier.weight(1f));InfoMini("Grupos de ovo",b.species.eggGroups.joinToString().ifBlank{"—"},Modifier.weight(2f))}}}};item{TypeMatchupCard(b.pokemon.types)};item{SectionCard("Habilidades",Icons.Default.Bolt){Column(verticalArrangement=Arrangement.spacedBy(7.dp)){b.pokemon.abilities.forEach{ability->Surface(Modifier.fillMaxWidth().then(if(openRef!=null)Modifier.clickable{openRef("ability",ability)}else Modifier),shape=RoundedCornerShape(14.dp),color=accent.copy(alpha=.08f)){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Text(ability,Modifier.weight(1f),fontWeight=FontWeight.SemiBold);if(openRef!=null)Icon(Icons.Default.ChevronRight,null)}}}}}};item{Spacer(Modifier.height(12.dp))}}}
@Composable private fun SectionCard(title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,content:@Composable ColumnScope.()->Unit){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Color(0xFF263C8C));Spacer(Modifier.width(9.dp));Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)};content()}}}
@Composable private fun InfoMini(label:String,value:String,modifier:Modifier){Surface(modifier,shape=RoundedCornerShape(14.dp),color=Color(0xFFF4F5FA)){Column(Modifier.padding(11.dp)){Text(label,fontSize=10.sp,color=Color(0xFF6F7890));Text(value,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)}}}
@Composable private fun V2Stats(s:PokemonStats){val rows=listOf("HP" to s.hp,"Ataque" to s.attack,"Defesa" to s.defense,"Ataque Esp." to s.specialAttack,"Defesa Esp." to s.specialDefense,"Velocidade" to s.speed);LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(rows){(name,v)->Column{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(name,fontWeight=FontWeight.SemiBold);Text(v.toString())};LinearProgressIndicator(progress={(v/200f).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().padding(top=4.dp))}};item{HorizontalDivider();InfoMini("Total",rows.sumOf{it.second}.toString(),Modifier.fillMaxWidth())}}}
@Composable private fun V2Evolution(e:List<PokeApiService.EvolutionStage>,currentId:Int,openPokemon:((Int)->Unit)?){LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){item{Text("Família evolutiva",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)};if(e.isEmpty())item{Text("Nenhuma evolução encontrada.")}else items(e,key={it.pokemonId}){stage->val active=stage.pokemonId==currentId;Card(Modifier.fillMaxWidth().then(if(openPokemon!=null&&!active)Modifier.clickable{openPokemon(stage.pokemonId)}else Modifier)){Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/${stage.pokemonId}.png",stage.name,Modifier.size(70.dp),contentScale=ContentScale.Fit);Column(Modifier.weight(1f).padding(start=10.dp)){Text(stage.name,fontWeight=FontWeight.Bold);Text(stage.requirement?:"Forma inicial",style=MaterialTheme.typography.bodySmall);if(active)Text("Pokémon atual",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)};if(openPokemon!=null&&!active)Icon(Icons.Default.ChevronRight,null)}}}}}
@Composable private fun V2Moves(moves:List<PokeApiService.RemoteMove>,context:GameContext?,openRef:((String,String)->Unit)?){var query by remember(moves,context){mutableStateOf("")};var methodFilter by remember(moves,context){mutableStateOf("Todos")};val base=remember(moves,context){if(context==null)moves.map{MoveView(it,it.learnDetails)}else moves.mapNotNull{move->move.learnDetails.filter{context.matchesVersionGroup(it.versionGroup)}.takeIf{it.isNotEmpty()}?.let{MoveView(move,it)}}};val methods=remember(base){base.flatMap{it.details}.map{methodLabel(it.method)}.distinct().sorted()};val visible=remember(base,query,methodFilter){base.filter{v->(query.isBlank()||v.move.name.contains(query,true))&&(methodFilter=="Todos"||v.details.any{methodLabel(it.method)==methodFilter})}.sortedBy{it.move.name}};LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){item{Text("Golpes",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=14.dp));OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(top=8.dp),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},label={Text("Buscar golpe")});LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){item{FilterChip(methodFilter=="Todos",{methodFilter="Todos"},{Text("Todos")})};items(methods,key={it}){label->FilterChip(methodFilter==label,{methodFilter=label},{Text(label)})}}};items(visible,key={it.move.name}){view->Card(Modifier.fillMaxWidth().then(if(openRef!=null)Modifier.clickable{openRef("move",view.move.name)}else Modifier)){Row(Modifier.fillMaxWidth().padding(11.dp),verticalAlignment=Alignment.CenterVertically){Text(view.move.name,Modifier.weight(1f),fontWeight=FontWeight.SemiBold);if(openRef!=null)Icon(Icons.Default.ChevronRight,null)}}};item{Spacer(Modifier.height(16.dp))}}}
private fun methodLabel(method:String):String=when(method.lowercase()){"level up"->"Nível";"machine"->"TM";"egg"->"Ovo";"tutor"->"Tutor";else->method}
@Composable private fun V2Locations(encounters:List<PokeApiService.EncounterLocation>,context:GameContext?,openLocation:(()->Unit)?){val visible=if(context==null)encounters else encounters.mapNotNull{e->val versions=e.versions.filter(context::matchesVersion);val details=e.details.filter{context.matchesVersion(it.version)};if(versions.isEmpty()&&details.isEmpty())null else e.copy(versions=versions,details=details)};LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){item{if(openLocation!=null)Button(openLocation,Modifier.fillMaxWidth()){Icon(Icons.Default.LocationOn,null);Spacer(Modifier.width(8.dp));Text("Abrir localização / mapa")}};if(visible.isEmpty())item{Text("Nenhum encontro detalhado disponível para este contexto.")}else items(visible,key={it.location}){e->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(10.dp)){Text(e.location,fontWeight=FontWeight.Bold);e.details.take(4).forEach{d->Text(listOfNotNull(d.method,d.minLevel.takeIf{it>0}?.let{"Nv. $it"}).joinToString(" · "),style=MaterialTheme.typography.bodySmall)}}}}}}
private fun typeColor(type:String)=when(type.lowercase()){ "grass"->Color(0xFF38B84A);"fire"->Color(0xFFE85C43);"water"->Color(0xFF4E8FEA);"electric"->Color(0xFFE3B62F);"psychic"->Color(0xFFE8679A);"ice"->Color(0xFF6CC7D8);"dragon"->Color(0xFF6553C7);"dark"->Color(0xFF5B5363);"fairy"->Color(0xFFE484C4);"fighting"->Color(0xFFC65443);"poison"->Color(0xFF9B5BC6);"ground"->Color(0xFFC9A45D);"rock"->Color(0xFFAA9554);"bug"->Color(0xFF8AAE2D);"ghost"->Color(0xFF665F9A);"steel"->Color(0xFF7F9AA7);"flying"->Color(0xFF7E9AD8);else->Color(0xFF6D7180)}
private fun gameColor(label:String?)=when{label?.contains("Scarlet",true)==true->Color(0xFF7655E8);label?.contains("Sword",true)==true->Color(0xFF35A9C7);label?.contains("Let's Go",true)==true->Color(0xFFE0A929);label?.contains("Arceus",true)==true->Color(0xFF527F7C);label?.contains("HOME",true)==true->Color(0xFF5B55E7);else->Color(0xFF6C63E8)}