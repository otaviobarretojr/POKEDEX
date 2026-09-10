package com.otaviobarreto.pokedex.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otaviobarreto.pokedex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class BootState(val progress: Float, val label: String)

@Composable
fun BootExperienceScreen(onReady: () -> Unit) {
    var state by remember { mutableStateOf(BootState(.04f, "Preparando sua Pokédex")) }
    var finished by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(state.progress, tween(420), label = "bootProgress")
    val infinite = rememberInfiniteTransition(label = "bootMotion")
    val rotation by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "orbit")
    val pulse by infinite.animateFloat(.96f, 1.04f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val glow by infinite.animateFloat(.18f, .42f, infiniteRepeatable(tween(1900), RepeatMode.Reverse), label = "glow")

    LaunchedEffect(Unit) {
        state = BootState(.10f, "Preparando dados locais")
        runCatching { withContext(Dispatchers.IO) { PokedexDataStore.nationalDex() } }
        state = BootState(.34f, "Sincronizando regiões e jogos")
        runCatching { withContext(Dispatchers.IO) {
            coroutineScope {
                AppGameCatalog.games.flatMap { it.regions }.map { region ->
                    async { GameContext.fromSource(region.source)?.let { GameDexService.loadGameDex(it) } }
                }.forEach { it.await() }
            }
        } }
        state = BootState(.67f, "Atualizando dados de referência")
        runCatching { withContext(Dispatchers.IO) {
            coroutineScope {
                listOf("move", "ability", "item").map { kind -> async { ReferenceCatalogService.load(kind) } }.forEach { it.await() }
            }
        } }
        state = BootState(.88f, "Finalizando Boxes e Times")
        delay(260)
        state = BootState(1f, "Tudo pronto")
        delay(420)
        finished = true
        onReady()
    }

    val mint = Color(0xFFEAFBF3)
    val teal = Color(0xFF159D9B)
    val green = Color(0xFF4ACB8A)
    Box(Modifier.fillMaxSize().background(mint)) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width * .5f, size.height * .43f)
            drawCircle(Color.White.copy(alpha=.58f), size.minDimension*.46f, center)
            drawCircle(teal.copy(alpha=.08f), size.minDimension*.34f, center)
            val radii = listOf(.31f,.43f,.57f)
            radii.forEachIndexed { index, r ->
                drawArc(
                    color = if(index%2==0) teal.copy(alpha=.22f) else green.copy(alpha=.18f),
                    startAngle = rotation + index*71f,
                    sweepAngle = 245f,
                    useCenter = false,
                    topLeft = Offset(center.x-size.minDimension*r, center.y-size.minDimension*r*.58f),
                    size = Size(size.minDimension*r*2, size.minDimension*r*1.16f),
                    style = Stroke(width=2.2f, cap=StrokeCap.Round)
                )
            }
            repeat(14) { i ->
                val x = size.width * (((i*37)%100)/100f)
                val y = size.height * (.10f + (((i*53)%78)/100f))
                val s = 5f + (i%4)*4f
                drawCircle(if(i%2==0) teal.copy(alpha=.13f) else green.copy(alpha=.16f), s, Offset(x,y))
            }
        }

        Column(Modifier.fillMaxSize().padding(horizontal=30.dp), horizontalAlignment=Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(.22f))
            Box(Modifier.size(224.dp), contentAlignment=Alignment.Center) {
                Surface(Modifier.size(148.dp).scale(pulse), shape=CircleShape, color=Color.White.copy(alpha=.92f), shadowElevation=10.dp) {
                    Box(contentAlignment=Alignment.Center) { Icon(Icons.Default.CatchingPokemon, null, Modifier.size(94.dp), tint=teal) }
                }
                Surface(Modifier.align(Alignment.BottomCenter).offset(y=10.dp), shape=RoundedCornerShape(20.dp), color=teal.copy(alpha=.94f)) {
                    Text("POKEDEX", Modifier.padding(horizontal=20.dp,vertical=7.dp), color=Color.White, fontWeight=FontWeight.Black, letterSpacing=2.sp)
                }
            }
            Spacer(Modifier.height(42.dp))
            Text("SUA JORNADA, ORGANIZADA.", fontWeight=FontWeight.Bold, color=teal, letterSpacing=1.4.sp, fontSize=13.sp)
            Spacer(Modifier.weight(.28f))
            Text(state.label, style=MaterialTheme.typography.titleMedium, fontWeight=FontWeight.SemiBold, color=Color(0xFF356B68), textAlign=TextAlign.Center)
            Spacer(Modifier.height(13.dp))
            LinearProgressIndicator(progress={animatedProgress}, Modifier.fillMaxWidth().height(6.dp), color=teal, trackColor=teal.copy(alpha=.12f), strokeCap=StrokeCap.Round)
            Spacer(Modifier.height(9.dp))
            Text("${(animatedProgress*100).toInt().coerceIn(0,100)}%", style=MaterialTheme.typography.labelMedium, color=teal.copy(alpha=.75f))
            Spacer(Modifier.height(38.dp))
            Text("POKEDEX  ·  v0.71", style=MaterialTheme.typography.labelSmall, color=Color(0xFF4B7D78).copy(alpha=.62f), letterSpacing=1.sp)
            Spacer(Modifier.height(24.dp))
        }
        if(finished) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha=glow*.15f)))
    }
}
