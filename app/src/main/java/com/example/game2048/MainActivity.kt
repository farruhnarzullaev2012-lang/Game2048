package com.example.game2048

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val Bg = Color(0xFFFAF8EF)
private val BoardColor = Color(0xFFBBADA0)
private val EmptyCell = Color(0xFFCDC1B4)
private val DarkText = Color(0xFF776E65)

// Классические цвета плиток 2048
private fun tileColor(v: Int) = when (v) {
    0 -> EmptyCell
    2 -> Color(0xFFEEE4DA); 4 -> Color(0xFFEDE0C8); 8 -> Color(0xFFF2B179)
    16 -> Color(0xFFF59563); 32 -> Color(0xFFF67C5F); 64 -> Color(0xFFF65E3B)
    128 -> Color(0xFFEDCF72); 256 -> Color(0xFFEDCC61); 512 -> Color(0xFFEDC850)
    1024 -> Color(0xFFEDC53F); 2048 -> Color(0xFFEDC22E)
    else -> Color(0xFF3C3A32)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Тёмные значки в строке состояния на светлом фоне
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        setContent { MaterialTheme { GameScreen() } }
    }
}

@Composable
fun GameScreen() {
    val prefs = LocalContext.current.getSharedPreferences("game2048", Context.MODE_PRIVATE)
    var board by remember { mutableStateOf(Game2048.newGame()) }
    var best by remember { mutableIntStateOf(prefs.getInt("best", 0)) }
    var winShown by remember { mutableStateOf(false) }   // «Победа» показываем один раз
    var keepPlaying by remember { mutableStateOf(false) }

    fun doMove(m: Move) {
        if (board.isGameOver || (board.won && !keepPlaying)) return
        board = Game2048.play(board, m).board
        if (board.score > best) { best = board.score; prefs.edit().putInt("best", best).apply() }
    }
    fun restart() { board = Game2048.newGame(); winShown = false; keepPlaying = false }

    Box(
        Modifier.fillMaxSize().background(Bg).pointerInput(Unit) {
            var total = Offset.Zero
            detectDragGestures(
                onDragStart = { total = Offset.Zero },
                onDrag = { ch, d -> ch.consume(); total += d },
                onDragEnd = {
                    if (abs(total.x) > 40 || abs(total.y) > 40) doMove(
                        if (abs(total.x) > abs(total.y)) { if (total.x > 0) Move.RIGHT else Move.LEFT }
                        else { if (total.y > 0) Move.DOWN else Move.UP }
                    )
                }
            )
        }
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("2048", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = DarkText, modifier = Modifier.weight(1f))
                ScoreBox("СЧЁТ", board.score)
                Spacer(Modifier.width(8.dp))
                ScoreBox("РЕКОРД", best)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Собери плитку 2048!", color = DarkText, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Button(onClick = ::restart, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F7A66)),
                    shape = RoundedCornerShape(8.dp)) { Text("Новая игра", color = Color.White) }
            }
            Spacer(Modifier.height(16.dp))
            BoardView(board)
            Spacer(Modifier.height(20.dp))
            ArrowPad(::doMove)
        }

        when {
            board.won && !winShown && !keepPlaying -> Overlay(
                "Победа! 🎉", "Ты собрал 2048. Счёт: ${board.score}", Color(0xCCEDC22E),
                "Играть дальше" to { winShown = true; keepPlaying = true }, "Заново" to ::restart
            )
            board.isGameOver -> Overlay(
                "Игра окончена", "Счёт: ${board.score}" + if (board.score >= best && board.score > 0) "\nНовый рекорд! 🏆" else "",
                Color(0xCCEEE4DA), "Заново" to ::restart, null
            )
        }
    }
}

@Composable
private fun ScoreBox(label: String, value: Int) {
    Column(
        Modifier.clip(RoundedCornerShape(8.dp)).background(BoardColor).padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFFEEE4DA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("$value", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BoardView(board: Board) {
    Column(
        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(BoardColor).padding(6.dp)
    ) {
        board.cells.forEachIndexed { r, row ->
            Row(Modifier.weight(1f)) {
                row.forEachIndexed { c, v -> Tile(v, Modifier.weight(1f).fillMaxHeight().padding(6.dp)) }
            }
        }
    }
}

@Composable
private fun Tile(value: Int, modifier: Modifier) {
    val color by animateColorAsState(tileColor(value), tween(120), label = "tile")
    // Лёгкий «пульс» когда в клетке появляется или меняется плитка
    val pop = remember { Animatable(1f) }
    LaunchedEffect(value) {
        if (value != 0) { pop.snapTo(0.85f); pop.animateTo(1f, tween(120)) }
    }
    Box(
        modifier.scale(pop.value).clip(RoundedCornerShape(6.dp)).background(color),
        contentAlignment = Alignment.Center
    ) {
        if (value != 0) Text(
            "$value",
            color = if (value <= 4) DarkText else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = when { value < 100 -> 32.sp; value < 1000 -> 26.sp; else -> 20.sp }
        )
    }
}

@Composable
private fun ArrowPad(onMove: (Move) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Arrow("▲", "Вверх") { onMove(Move.UP) }
        Row {
            Arrow("◀", "Влево") { onMove(Move.LEFT) }
            Spacer(Modifier.width(60.dp))
            Arrow("▶", "Вправо") { onMove(Move.RIGHT) }
        }
        Arrow("▼", "Вниз") { onMove(Move.DOWN) }
    }
}

@Composable
private fun Arrow(symbol: String, label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick, modifier = Modifier.size(60.dp).padding(3.dp),
        shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = EmptyCell, contentColor = DarkText)
    ) { Text(symbol, fontSize = 20.sp, modifier = Modifier.semantics { contentDescription = label }) }
}

@Composable
private fun Overlay(
    title: String, subtitle: String, bg: Color,
    primary: Pair<String, () -> Unit>, secondary: Pair<String, () -> Unit>?
) {
    Box(Modifier.fillMaxSize().background(bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, fontSize = 18.sp, color = DarkText, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Row {
                Button(onClick = primary.second, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F7A66))) {
                    Text(primary.first, color = Color.White, fontSize = 17.sp)
                }
                if (secondary != null) {
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = secondary.second) { Text(secondary.first, color = DarkText, fontSize = 17.sp) }
                }
            }
        }
    }
}
