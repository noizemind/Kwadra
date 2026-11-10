package app.kwadra

import android.app.Activity
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val Manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.manrope, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.manrope, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)

private class DragLive(
    val slot: Int,
    val piece: Piece,
    var x: Float,
    var y: Float,
    val grabX: Float,
    val grabY: Float,
    val cell: Float,
    val gap: Float,
    val lift: Float,
)

@Composable
fun KwadraScreen(game: GameController) {
    val palette = paletteFor(game.settings.theme)
    val copy = game.copy
    val context = LocalContext.current
    val reduced = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val view = LocalView.current
    androidx.compose.runtime.SideEffect {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !palette.dark
        controller.isAppearanceLightNavigationBars = !palette.dark
    }
    BackHandler(game.settingsOpen || game.state.status != "menu") {
        when {
            game.settingsOpen -> game.settingsOpen = false
            else -> game.toMenu()
        }
    }
    LaunchedEffect(game.pending) {
        val result = game.pending ?: return@LaunchedEffect
        kotlinx.coroutines.delay(if (reduced) 60 else if (result.clears.count > 0) 280 else 90)
        if (game.pending === result) game.commit()
    }
    LaunchedEffect(game.banner) {
        if (game.banner != null) {
            kotlinx.coroutines.delay(900)
            game.banner = null
        }
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) withFrameMillis { now = System.currentTimeMillis() }
    }
    LaunchedEffect(now) {
        game.floaters.removeAll { now - it.born > 720 }
        game.sparks.removeAll { now - it.born > 460 }
    }
    var drag by remember { mutableStateOf<DragLive?>(null) }
    var boardBox by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var gridSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val gapPx = with(density) { 4.dp.toPx() }
    val padPx = with(density) { 12.dp.toPx() }
    val cellPx = ((gridSize.width - gapPx * 7f) / 8f).coerceAtLeast(1f)
    val step = cellPx + gapPx

    CompositionLocalProvider(LocalTextStyle provides TextStyle(fontFamily = Manrope)) {
    Box(
        Modifier
            .fillMaxSize()
            .background(palette.surfaceDim),
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .widthIn(max = 448.dp)
                .fillMaxWidth()
                .background(palette.surface)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            if (game.state.status != "menu") {
                Header(game, palette, copy)
                Board(
                    game, palette, copy, now, reduced, drag, cellPx, gapPx, step, padPx, boardBox,
                    onBoard = { boardBox = it },
                    onGrid = { gridSize = it },
                )
                if (game.showHint && game.state.status == "playing" && game.pending == null) {
                    Text(
                        copy.hint,
                        color = palette.muted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 16.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Tray(game, palette, drag, cellPx, gapPx, boardBox, step, padPx, now) { drag = it }
            } else {
                Menu(game, palette, copy, now, reduced)
            }
        }
        if (game.state.status == "gameover") GameOverSheet(game, palette, copy)
        if (game.settingsOpen) SettingsSheet(game, palette, copy)
        val live = drag
        if (live != null) {
            PiecePx(
                live.piece,
                live.cell,
                live.gap,
                palette.pieces[live.piece.color],
                Modifier.offset { IntOffset((live.x - live.grabX).roundToInt(), (live.y - live.grabY - live.lift).roundToInt()) },
            )
        }
    }
    }
}

@Composable
private fun Header(game: GameController, palette: Palette, copy: Copy) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("KWADRA", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.92.sp)
            Text(fmt(game.state.score, copy), color = palette.ink, fontSize = 30.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(
            Modifier.clip(CircleShape).background(palette.high).padding(horizontal = 12.dp).height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Trophy(palette.muted)
            Spacer(Modifier.width(6.dp))
            Text(fmt(game.state.best, copy), color = palette.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        CircleButton(palette, { game.patch(game.settings.copy(sound = !game.settings.sound)) }) {
            Speaker(palette.ink, game.settings.sound)
        }
        Spacer(Modifier.width(8.dp))
        CircleButton(palette, { game.settingsOpen = true }) { Sliders(palette.ink) }
    }
}

@Composable
private fun ColumnScope.Board(
    game: GameController,
    palette: Palette,
    copy: Copy,
    now: Long,
    reduced: Boolean,
    drag: DragLive?,
    cellPx: Float,
    gapPx: Float,
    step: Float,
    padPx: Float,
    boardBox: androidx.compose.ui.geometry.Rect,
    onBoard: (androidx.compose.ui.geometry.Rect) -> Unit,
    onGrid: (IntSize) -> Unit,
) {
    val shake = if (game.shake && !reduced) shakeOffset(now, game.floaters.lastOrNull()?.born ?: now) else Offset.Zero
    Box(
        Modifier
            .align(Alignment.CenterHorizontally)
            .padding(horizontal = 16.dp)
            .widthIn(max = 384.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .offset { IntOffset(shake.x.roundToInt(), shake.y.roundToInt()) }
            .clip(RoundedCornerShape(20.dp))
            .background(palette.low)
            .onGloballyPositioned { onBoard(it.boundsInRoot()) },
    ) {
        if (game.state.combo >= 2 && game.state.status == "playing") {
            Text(
                "×${game.state.combo}",
                color = palette.onPrimaryContainer,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-10).dp)
                    .clip(CircleShape)
                    .background(palette.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Box(Modifier.fillMaxSize().padding(12.dp).onGloballyPositioned { onGrid(it.size) }) {
            val density = LocalDensity.current
            val board = game.displayBoard()
            val pending = game.pending
            val placedAt = game.floaters.lastOrNull()?.born
            val ghost = ghostOf(drag, boardBox, padPx, step, game.state.board)
            val cellDp = with(density) { cellPx.toDp() }
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    val value = board[r][c]
                    val clearing = pending != null && pending.clears.hits(r, c) && value != 0
                    val justPlaced = pending?.placed?.any { it.r == r && it.c == c } == true
                    val motion = if (reduced || placedAt == null) 1f else ((now - placedAt) / 250f).coerceIn(0f, 1f)
                    val scale = when {
                        clearing -> 1f - 0.8f * motion
                        justPlaced -> 0.86f + 0.14f * motion
                        else -> 1f
                    }
                    val fade = when {
                        clearing -> 1f - motion
                        justPlaced -> 0.7f + 0.3f * motion
                        else -> 1f
                    }
                    val color = if (value == 0) palette.highest else palette.pieces[(value - 1).coerceIn(0, 5)]
                    Box(
                        Modifier
                            .offset { IntOffset((c * step).roundToInt(), (r * step).roundToInt()) }
                            .size(cellDp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                alpha = fade
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .background(color),
                    ) {
                        if (value != 0) {
                            Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.28f)))
                        }
                    }
                    if (value == 0 && ghost != null && ghost.cells.any { it.r == r && it.c == c }) {
                        Box(
                            Modifier
                                .offset { IntOffset((c * step).roundToInt(), (r * step).roundToInt()) }
                                .size(cellDp)
                                .clip(RoundedCornerShape(8.dp))
                                .background((if (ghost.valid) palette.pieces[ghost.color] else palette.error).copy(alpha = if (ghost.valid) 0.45f else 0.30f)),
                        )
                    }
                }
            }
            game.floaters.forEach { item ->
                val life = if (reduced) 1f else ((now - item.born) / 700f).coerceIn(0f, 1f)
                Text(
                    item.text,
                    color = palette.ink.copy(alpha = 1f - life),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (item.col * step + cellPx / 2).roundToInt(),
                                (item.row * step + cellPx * 0.2f - life * 18f).roundToInt(),
                            )
                        }
                        .wrapContentSize(Alignment.Center, unbounded = true),
                )
            }
            if (!reduced) game.sparks.forEach { spark ->
                val life = ((now - spark.born) / 420f).coerceIn(0f, 1f)
                val scaleSpark = 1f - 0.85f * life
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (spark.col * step + cellPx / 2 + spark.dx * life).roundToInt(),
                                (spark.row * step + cellPx / 2 + spark.dy * life).roundToInt(),
                            )
                        }
                        .size(8.dp)
                        .wrapContentSize(Alignment.Center, unbounded = true)
                        .graphicsLayer {
                            scaleX = scaleSpark
                            scaleY = scaleSpark
                            alpha = 0.95f * (1f - life)
                        }
                        .clip(RoundedCornerShape(2.dp))
                        .background(palette.pieces[spark.color.coerceIn(0, 5)]),
                )
            }
        }
        val banner = game.banner
        if (banner != null) {
            val bannerAt = remember(banner) { System.currentTimeMillis() }
            val pop = if (reduced) 1f else ((now - bannerAt) / 250f).coerceIn(0f, 1f)
            Text(
                banner,
                color = palette.onInverse,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val scale = 0.96f + 0.04f * pop
                        scaleX = scale
                        scaleY = scale
                        alpha = pop
                    }
                    .clip(CircleShape)
                    .background(palette.inverse)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
}

private class Ghost(val row: Int, val col: Int, val cells: List<Cell>, val valid: Boolean, val color: Int)

private fun shakeOffset(now: Long, born: Long): Offset {
    val t = ((now - born) / 280f).coerceIn(0f, 1f)
    val x = when {
        t < 0.25f -> -2f * (t / 0.25f)
        t < 0.5f -> -2f + 4f * ((t - 0.25f) / 0.25f)
        t < 0.75f -> 2f - 3f * ((t - 0.5f) / 0.25f)
        else -> -1f * (1f - (t - 0.75f) / 0.25f)
    }
    val y = when {
        t < 0.25f -> 1f * (t / 0.25f)
        t < 0.5f -> 1f - 2f * ((t - 0.25f) / 0.25f)
        else -> -1f * (1f - ((t - 0.5f) / 0.5f).coerceIn(0f, 1f))
    }
    return Offset(x, y)
}

private fun ghostOf(
    drag: DragLive?,
    board: androidx.compose.ui.geometry.Rect,
    pad: Float,
    step: Float,
    grid: List<List<Int>>,
): Ghost? {
    if (drag == null || step <= 0f || board.width <= 0f) return null
    val left = drag.x - drag.grabX
    val top = drag.y - drag.grabY - drag.lift
    val col = ((left - board.left - pad) / step).roundToInt()
    val row = ((top - board.top - pad) / step).roundToInt()
    if (row < -2 || col < -2 || row > 8 || col > 8) return null
    val cells = drag.piece.cells.map { Cell(row + it.r, col + it.c) }.filter { it.r in 0..7 && it.c in 0..7 }
    return Ghost(row, col, cells, Engine.canPlace(grid, drag.piece, row, col), drag.piece.color)
}

@Composable
private fun Tray(
    game: GameController,
    palette: Palette,
    drag: DragLive?,
    cellPx: Float,
    gapPx: Float,
    boardBox: androidx.compose.ui.geometry.Rect,
    step: Float,
    padPx: Float,
    now: Long,
    onDrag: (DragLive?) -> Unit,
) {
    val boardNow = rememberUpdatedState(boardBox)
    val gridNow = rememberUpdatedState(game.state.board)
    val cellNow = rememberUpdatedState(cellPx)
    val gapNow = rememberUpdatedState(gapPx)
    val stepNow = rememberUpdatedState(step)
    val padNow = rememberUpdatedState(padPx)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        game.displayTray().forEachIndexed { slot, piece ->
            val dim = piece != null && game.state.status == "playing" && !Engine.fitsAnywhere(game.fitBoard(), piece)
            val hidden = drag?.slot == slot
            val hintSlot = game.showHint && slot == 0 && piece != null && !dim && !hidden && game.state.status == "playing"
            val pulse = if (hintSlot) sin((now % 1600) / 1600.0 * Math.PI).toFloat().coerceIn(0f, 1f) else 0f
            var slotCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
            Box(
                Modifier
                    .weight(1f)
                    .height(112.dp)
                    .alpha(when {
                        hidden -> 0f
                        piece == null || dim -> 0.4f
                        else -> 1f
                    })
                    .drawBehind {
                        if (pulse > 0f) {
                            drawRoundRect(
                                palette.primary.copy(alpha = 0.22f * pulse),
                                style = Stroke(4.dp.toPx()),
                                cornerRadius = CornerRadius(16.dp.toPx()),
                            )
                        }
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.low)
                    .onGloballyPositioned { slotCoords = it }
                    .pointerInput(piece?.id, game.busy()) {
                        val held = piece ?: return@pointerInput
                        if (game.busy()) return@pointerInput
                        val threshold = 8.dp.toPx()
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val origin = down.position
                            var started = false
                            var live: DragLive? = null
                            var last = origin
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    if (started && change.changedToUp()) {
                                        val current = live
                                        val ghost = ghostOf(current, boardNow.value, padNow.value, stepNow.value, gridNow.value)
                                        onDrag(null)
                                        if (current == null || ghost == null || !ghost.valid) game.reject()
                                        else if (!game.drop(slot, ghost.row, ghost.col)) game.reject()
                                    } else onDrag(null)
                                    break
                                }
                                last = change.position
                                val travelled = last - origin
                                if (!started && travelled.x * travelled.x + travelled.y * travelled.y > threshold * threshold) {
                                    started = true
                                    val coords = slotCoords
                                    val rootPoint = coords?.localToRoot(last) ?: last
                                    val cell = cellNow.value
                                    val gap = gapNow.value
                                    val fracX = (origin.x / size.width).coerceIn(0f, 1f)
                                    val fracY = (origin.y / size.height).coerceIn(0f, 1f)
                                    val floatW = held.w * cell + (held.w - 1) * gap
                                    val floatH = held.h * cell + (held.h - 1) * gap
                                    val lift = minOf(120f, boardNow.value.height * 0.28f)
                                    live = DragLive(slot, held, rootPoint.x, rootPoint.y, fracX * floatW, fracY * floatH, cell, gap, lift)
                                    onDrag(live)
                                    game.dragBegan()
                                }
                                if (started && live != null) {
                                    change.consume()
                                    val rootPoint = slotCoords?.localToRoot(last) ?: last
                                    val current = live!!
                                    live = DragLive(current.slot, current.piece, rootPoint.x, rootPoint.y, current.grabX, current.grabY, current.cell, current.gap, current.lift)
                                    onDrag(live)
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (piece != null) {
                    PieceDp(piece, 16.dp, 3.dp, palette.pieces[piece.color], 1f)
                }
            }
        }
    }
}

@Composable
private fun Menu(game: GameController, palette: Palette, copy: Copy, now: Long, reduced: Boolean) {
    val born = remember { System.currentTimeMillis() }
    Box(Modifier.fillMaxSize()) {
        CircleButton(palette, { game.settingsOpen = true }, Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 24.dp)) {
            Sliders(palette.ink)
        }
        Column(Modifier.align(Alignment.CenterStart).padding(horizontal = 24.dp).padding(bottom = 120.dp)) {
            Mark(palette)
            Spacer(Modifier.height(20.dp))
            Stagger(now, born, 0, reduced) { Text(game.remark, color = palette.muted, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
            Spacer(Modifier.height(8.dp))
            Stagger(now, born, 1, reduced) {
                Text("Kwadra", color = palette.ink, fontSize = 48.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.44).sp)
            }
            Spacer(Modifier.height(12.dp))
            Stagger(now, born, 2, reduced) {
                Text(copy.howTo, color = palette.muted, fontSize = 16.sp, modifier = Modifier.widthIn(max = 320.dp))
            }
            Spacer(Modifier.height(32.dp))
            Stagger(now, born, 3, reduced) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Trophy(palette.muted)
                    Spacer(Modifier.width(12.dp))
                    Text(copy.record, color = palette.muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(12.dp))
                    Text(fmt(game.state.best, copy), color = palette.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp).padding(bottom = 32.dp).widthIn(max = 384.dp)) {
            if (game.hasContinue) {
                PrimaryButton(copy.resume, palette) { game.start(true) }
                Spacer(Modifier.height(12.dp))
                TonalButton(copy.newGame, palette) { game.start(false) }
            } else {
                PrimaryButton(copy.play, palette, icon = { PlayIcon(palette.onPrimary) }) { game.start(false) }
            }
        }
    }
}

@Composable
private fun Stagger(now: Long, born: Long, index: Int, reduced: Boolean, content: @Composable () -> Unit) {
    val t = if (reduced) 1f else ((now - born - index * 40L) / 400f).coerceIn(0f, 1f)
    Box(Modifier.graphicsLayer {
        alpha = t
        translationY = 8.dp.toPx() * (1f - t)
    }) { content() }
}

@Composable
private fun GameOverSheet(game: GameController, palette: Palette, copy: Copy) {
    val shown = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown.value = true }
    Box(Modifier.fillMaxSize().background(palette.inverse.copy(alpha = 0.35f)).swallow()) {
        AnimatedVisibility(
            shown.value,
            Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 32 },
        ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(palette.container)
                .swallow()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 28.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Text(copy.gameOver, color = palette.muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(fmt(game.state.score, copy), color = palette.ink, fontSize = 48.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            Text(
                if (game.freshRecord()) copy.newRecord else copy.recordValue(fmt(game.state.best, copy)),
                color = palette.muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            PrimaryButton(copy.again, palette, icon = { AgainIcon(palette.onPrimary) }) { game.restart() }
            Spacer(Modifier.height(12.dp))
            TonalButton(copy.menu, palette) { game.toMenu() }
        }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsSheet(game: GameController, palette: Palette, copy: Copy) {
    val shown = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown.value = true }
    Box(Modifier.fillMaxSize().background(palette.inverse.copy(alpha = 0.4f)).clickable { game.settingsOpen = false }) {
        AnimatedVisibility(
            shown.value,
            Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 32 },
        ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(palette.container)
                .swallow()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(CircleShape).background(palette.outlineVariant))
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(copy.settings, color = palette.ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                CircleButton(palette, { game.settingsOpen = false }, size = 40.dp) { Cross(palette.ink) }
            }
            Text(copy.language.uppercase(), color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp, modifier = Modifier.padding(bottom = 12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice(copy.languageSystem, game.settings.locale == "system", palette, Modifier.fillMaxWidth()) { game.patch(game.settings.copy(locale = "system")) }
                Texts.native.forEach { (id, name) ->
                    Choice(name, game.settings.locale == id, palette, Modifier.fillMaxWidth(0.48f)) { game.patch(game.settings.copy(locale = id)) }
                }
            }
            Text(copy.theme.uppercase(), color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 20.dp, bottom = 12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(palette.highest).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ThemeChoice(copy.themeSystem, game.settings.theme == "system", palette) { game.patch(game.settings.copy(theme = "system")) }
                ThemeChoice(copy.themeLight, game.settings.theme == "light", palette) { game.patch(game.settings.copy(theme = "light")) }
                ThemeChoice(copy.themeDark, game.settings.theme == "dark", palette) { game.patch(game.settings.copy(theme = "dark")) }
            }
            Text(copy.themeHint, color = palette.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
            Column(Modifier.clip(RoundedCornerShape(16.dp)).background(palette.high).padding(horizontal = 16.dp)) {
                SettingSwitch(copy.sound, game.settings.sound, palette) { game.patch(game.settings.copy(sound = it)) }
                Box(Modifier.fillMaxWidth().height(1.dp).background(palette.highest))
                SettingSwitch(copy.haptics, game.settings.haptics, palette) { game.patch(game.settings.copy(haptics = it)) }
            }
        }
        }
    }
}

@Composable
private fun Modifier.swallow(): Modifier {
    val source = remember { MutableInteractionSource() }
    return clickable(interactionSource = source, indication = null) {}
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, palette: Palette, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = palette.ink, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = palette.primary,
                checkedThumbColor = palette.onPrimary,
                uncheckedTrackColor = palette.highest,
                uncheckedThumbColor = Color.White,
            ),
        )
    }
}

@Composable
private fun Choice(label: String, on: Boolean, palette: Palette, modifier: Modifier = Modifier, click: () -> Unit) {
    Text(
        label,
        color = if (on) palette.onSecondaryContainer else palette.muted,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(CircleShape)
            .background(if (on) palette.secondaryContainer else palette.highest)
            .clickable(onClick = click)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
private fun RowScope.ThemeChoice(label: String, on: Boolean, palette: Palette, click: () -> Unit) {
    Box(
        Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (on) palette.secondaryContainer else Color.Transparent).clickable(onClick = click),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (on) palette.onSecondaryContainer else palette.muted, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
private fun PrimaryButton(label: String, palette: Palette, icon: (@Composable () -> Unit)? = null, click: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                val scale = if (pressed) 0.96f else 1f
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(palette.primary)
            .clickable(interaction, null, onClick = click),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = palette.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TonalButton(label: String, palette: Palette, click: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                val scale = if (pressed) 0.96f else 1f
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(palette.secondaryContainer)
            .clickable(interaction, null, onClick = click),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = palette.onSecondaryContainer, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CircleButton(
    palette: Palette,
    click: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier.size(size).clip(CircleShape).background(palette.high).clickable(onClick = click),
        contentAlignment = Alignment.Center,
    ) { icon() }
}

@Composable
private fun Mark(palette: Palette) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(palette.primary).padding(7.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.size(13.dp).clip(RoundedCornerShape(3.dp)).background(palette.pieces[0]))
                Box(Modifier.size(13.dp).clip(RoundedCornerShape(3.dp)).background(palette.pieces[1]))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.size(13.dp).clip(RoundedCornerShape(3.dp)).background(palette.pieces[2]))
                Box(Modifier.size(13.dp).clip(RoundedCornerShape(3.dp)).background(palette.pieces[3]))
            }
        }
    }
}

@Composable
private fun PieceDp(piece: Piece, cell: androidx.compose.ui.unit.Dp, gap: androidx.compose.ui.unit.Dp, color: Color, alpha: Float) {
    Box(Modifier.size(cell * piece.w + gap * (piece.w - 1), cell * piece.h + gap * (piece.h - 1)).alpha(alpha)) {
        piece.cells.forEach { pos ->
            Box(
                Modifier
                    .offset(x = (cell + gap) * pos.c, y = (cell + gap) * pos.r)
                    .size(cell)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color),
            ) {
                Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.28f)))
            }
        }
    }
}

@Composable
private fun PiecePx(piece: Piece, cell: Float, gap: Float, color: Color, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Box(
        modifier.size(
            with(density) { (piece.w * cell + (piece.w - 1) * gap).toDp() },
            with(density) { (piece.h * cell + (piece.h - 1) * gap).toDp() },
        ),
    ) {
        piece.cells.forEach { pos ->
            Box(
                Modifier
                    .offset { IntOffset((pos.c * (cell + gap)).roundToInt(), (pos.r * (cell + gap)).roundToInt()) }
                    .size(with(density) { cell.toDp() })
                    .clip(RoundedCornerShape(8.dp))
                    .background(color),
            ) {
                Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.28f)))
            }
        }
    }
}

@Composable
private fun PlayIcon(color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val s = size.minDimension
        val path = Path().apply {
            moveTo(s * 0.32f, s * 0.18f)
            lineTo(s * 0.82f, s * 0.5f)
            lineTo(s * 0.32f, s * 0.82f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun AgainIcon(color: Color) {
    Canvas(Modifier.size(16.dp)) {
        val s = size.minDimension
        drawArc(color, 40f, 280f, false, Offset(s * 0.16f, s * 0.16f), Size(s * 0.68f, s * 0.68f), style = Stroke(s * 0.12f))
        val head = Path().apply {
            moveTo(s * 0.62f, s * 0.12f)
            lineTo(s * 0.88f, s * 0.28f)
            lineTo(s * 0.62f, s * 0.36f)
            close()
        }
        drawPath(head, color)
    }
}

@Composable
private fun Trophy(color: Color) {
    Canvas(Modifier.size(16.dp)) {
        val s = size.minDimension
        val path = Path().apply {
            moveTo(s * 0.28f, s * 0.16f)
            lineTo(s * 0.72f, s * 0.16f)
            lineTo(s * 0.66f, s * 0.48f)
            quadraticTo(s * 0.5f, s * 0.66f, s * 0.34f, s * 0.48f)
            close()
        }
        drawPath(path, color)
        drawArc(color, 200f, 140f, false, Offset(0f, s * 0.16f), Size(s * 0.28f, s * 0.34f), style = Stroke(s * 0.08f))
        drawArc(color, 200f, 140f, false, Offset(s * 0.72f, s * 0.16f), Size(s * 0.28f, s * 0.34f), style = Stroke(s * 0.08f))
        drawLine(color, Offset(s * 0.5f, s * 0.62f), Offset(s * 0.5f, s * 0.8f), s * 0.08f)
        drawLine(color, Offset(s * 0.32f, s * 0.86f), Offset(s * 0.68f, s * 0.86f), s * 0.08f)
    }
}

@Composable
private fun Speaker(color: Color, on: Boolean) {
    Canvas(Modifier.size(22.dp)) {
        val s = size.minDimension
        drawRoundRect(color, Offset(s * 0.16f, s * 0.38f), Size(s * 0.16f, s * 0.24f), CornerRadius(s * 0.04f))
        val cone = Path().apply {
            moveTo(s * 0.32f, s * 0.38f)
            lineTo(s * 0.52f, s * 0.22f)
            lineTo(s * 0.52f, s * 0.78f)
            lineTo(s * 0.32f, s * 0.62f)
            close()
        }
        drawPath(cone, color)
        if (on) {
            drawArc(color, -50f, 100f, false, Offset(s * 0.48f, s * 0.32f), Size(s * 0.28f, s * 0.36f), style = Stroke(s * 0.07f))
        } else {
            drawLine(color, Offset(s * 0.62f, s * 0.34f), Offset(s * 0.84f, s * 0.66f), s * 0.07f)
            drawLine(color, Offset(s * 0.84f, s * 0.34f), Offset(s * 0.62f, s * 0.66f), s * 0.07f)
        }
    }
}

@Composable
private fun Sliders(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val s = size.minDimension
        val ys = floatArrayOf(0.28f, 0.5f, 0.72f)
        val knobs = floatArrayOf(0.38f, 0.66f, 0.48f)
        for (i in 0..2) {
            val y = s * ys[i]
            drawLine(color, Offset(s * 0.18f, y), Offset(s * 0.82f, y), s * 0.07f)
            drawCircle(color, s * 0.09f, Offset(s * knobs[i], y))
        }
    }
}

@Composable
private fun Cross(color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val s = size.minDimension
        drawLine(color, Offset(s * 0.22f, s * 0.22f), Offset(s * 0.78f, s * 0.78f), s * 0.1f)
        drawLine(color, Offset(s * 0.78f, s * 0.22f), Offset(s * 0.22f, s * 0.78f), s * 0.1f)
    }
}

private fun fmt(n: Int, copy: Copy): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag(copy.numberLocale)).format(n)
