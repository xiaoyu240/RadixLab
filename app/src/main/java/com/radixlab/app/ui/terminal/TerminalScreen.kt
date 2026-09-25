package com.radixlab.app.ui.terminal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radixlab.app.ui.theme.MonoFontFamily
import kotlinx.coroutines.delay

private const val OPEN_MS = 460
private const val SCAN_START_MS = 380
private const val SCAN_MS = 780
private const val CONTENT_IN_MS = 520

private val SCREEN_BG = Color(0xFF040604)
private val INK_DIM = Color(0xFF6E8F79)
private val INK_OUT = Color(0xFFB8E6C4)
private val INK_ERR = Color(0xFFFF6B6B)
private val INK_OK = Color(0xFF3DFF8F)
private val INK_BRIGHT = Color(0xFFF4F4F4)

@Composable
fun TerminalScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var shell by remember { mutableStateOf(TerminalEngine.Shell()) }
    var lines by remember { mutableStateOf<List<TerminalEngine.Line>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var cursor by remember { mutableIntStateOf(0) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    var ctrlArmed by remember { mutableStateOf(false) }
    var altArmed by remember { mutableStateOf(false) }
    var matrix by remember { mutableStateOf<TerminalEngine.MatrixOptions?>(null) }
    var ready by remember { mutableStateOf(false) }
    var keyboardVisible by remember { mutableStateOf(true) }

    var blinkOn by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(520)
            blinkOn = !blinkOn
        }
    }

    val openP = remember { Animatable(0f) }
    val scanP = remember { Animatable(0f) }
    val contentP = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        openP.animateTo(1f, tween(OPEN_MS, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(SCAN_START_MS.toLong())
        scanP.animateTo(1f, tween(SCAN_MS, easing = LinearEasing))
    }
    LaunchedEffect(Unit) {
        delay(CONTENT_IN_MS.toLong())
        contentP.animateTo(1f, tween(420))
    }
    LaunchedEffect(Unit) {
        delay(620)
        TerminalEngine.bootLog().forEach { line ->
            lines = lines + line
            delay(95)
        }
        delay(170)
        lines = lines + TerminalEngine.welcome()
        ready = true
    }

    val scroll = rememberScrollState()
    LaunchedEffect(lines.size, matrix, keyboardVisible) {
        if (matrix == null && lines.isNotEmpty()) scroll.animateScrollTo(scroll.maxValue)
    }

    fun appendLines(newLines: List<TerminalEngine.Line>) {
        if (newLines.isNotEmpty()) lines = lines + newLines
    }

    fun handleKey(key: TerminalKey) {
        val running = matrix
        if (running != null) {
            val quit = when {
                running.screensaver -> true
                key is TerminalKey.CharKey && (key.value == 'q' || key.value == 'Q') -> true
                key == TerminalKey.Escape || key == TerminalKey.Interrupt || key == TerminalKey.Enter -> true
                else -> false
            }
            if (quit) {
                matrix = null
                appendLines(
                    listOf(
                        TerminalEngine.Line(
                            "cmatrix: 已退出 —— 虚拟矩阵雨，真机 CPU 全程没被碰过",
                            TerminalEngine.Kind.DIM
                        )
                    )
                )
            }
            return
        }

        if (ctrlArmed) {
            if (key is TerminalKey.CharKey) {
                when (key.value.lowercaseChar()) {
                    'c' -> {
                        appendLines(listOf(TerminalEngine.Line(TerminalEngine.prompt(shell.cwd) + input + "^C", TerminalEngine.Kind.IN)))
                        input = ""
                        cursor = 0
                        ctrlArmed = false
                        return
                    }
                    'l' -> {
                        lines = emptyList()
                        ctrlArmed = false
                        return
                    }
                    'd' -> {
                        ctrlArmed = false
                        onExit()
                        return
                    }
                }
            }
            ctrlArmed = false
        }

        when (key) {
            is TerminalKey.CharKey -> {
                val ch = key.value
                if (altArmed) altArmed = false
                if (input.length < 240) {
                    input = input.substring(0, cursor) + ch + input.substring(cursor)
                    cursor += 1
                }
            }

            TerminalKey.Backspace -> {
                if (cursor > 0) {
                    input = input.substring(0, cursor - 1) + input.substring(cursor)
                    cursor -= 1
                }
            }

            TerminalKey.ClearLine -> {
                input = ""
                cursor = 0
            }

            TerminalKey.Ctrl -> ctrlArmed = !ctrlArmed
            TerminalKey.Alt -> altArmed = !altArmed

            TerminalKey.ArrowLeft -> if (cursor > 0) cursor -= 1
            TerminalKey.ArrowRight -> if (cursor < input.length) cursor += 1

            TerminalKey.ArrowUp -> {
                val h = shell.history
                if (h.isNotEmpty()) {
                    val next = if (historyIndex < h.size - 1) historyIndex + 1 else historyIndex
                    historyIndex = next
                    input = h[h.size - 1 - next.coerceAtLeast(0)]
                    cursor = input.length
                }
            }

            TerminalKey.ArrowDown -> {
                val h = shell.history
                if (historyIndex > 0) {
                    historyIndex -= 1
                    input = h[h.size - 1 - historyIndex]
                    cursor = input.length
                } else {
                    historyIndex = -1
                    input = ""
                    cursor = 0
                }
            }

            TerminalKey.Tab -> {
                val completed = TerminalEngine.complete(input, shell)
                if (completed != null) {
                    input = completed
                    cursor = completed.length
                }
            }

            TerminalKey.Escape -> {
                input = ""
                cursor = 0
                ctrlArmed = false
                altArmed = false
            }

            TerminalKey.Interrupt -> {
                if (input.isNotEmpty()) {
                    appendLines(listOf(TerminalEngine.Line(TerminalEngine.prompt(shell.cwd) + input + "^C", TerminalEngine.Kind.IN)))
                }
                input = ""
                cursor = 0
            }

            TerminalKey.Eof -> onExit()

            TerminalKey.Enter -> {
                val outcome = TerminalEngine.execute(input, shell)
                shell = outcome.shell
                when (outcome.action) {
                    TerminalEngine.Action.Clear -> lines = emptyList()
                    is TerminalEngine.Action.Matrix -> {
                        appendLines(outcome.lines)
                        matrix = outcome.action.options
                    }
                    TerminalEngine.Action.Exit -> {
                        appendLines(outcome.lines)
                        onExit()
                    }
                    null -> appendLines(outcome.lines)
                }
                input = ""
                cursor = 0
                historyIndex = -1
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SCREEN_BG)
    ) {
        val density = LocalDensity.current
        val fullHeightPx = with(density) { maxHeight.toPx() }
        val fullWidthPx = with(density) { maxWidth.toPx() }

        Column(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, INK_OK.copy(alpha = 0.30f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RADIX VT",
                    style = mono(12.sp, FontWeight.Medium, INK_OK)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "虚拟环境 · 未连接真机",
                    style = mono(11.sp, FontWeight.Normal, INK_DIM),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "v1.0",
                    style = mono(11.sp, FontWeight.Normal, INK_DIM)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .border(
                            1.dp,
                            if (keyboardVisible) INK_OK.copy(alpha = 0.55f)
                            else INK_BRIGHT.copy(alpha = 0.30f)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { keyboardVisible = !keyboardVisible })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    KeyGlyph(tint = if (keyboardVisible) INK_OK else INK_DIM)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .border(1.dp, INK_BRIGHT.copy(alpha = 0.45f))
                        .pointerInput(Unit) { detectTapGestures(onTap = { onExit() }) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "×", style = mono(14.sp, FontWeight.Medium, INK_BRIGHT))
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (matrix != null) {
                    MatrixRain(
                        options = matrix!!,
                        modifier = Modifier.fillMaxSize(),
                        hint = if (matrix!!.screensaver) "屏保模式 · 任意键退出" else "q / Ctrl+C 退出"
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        lines.forEach { line ->
                            Text(
                                text = line.text.ifEmpty { " " },
                                style = mono(13.sp, FontWeight.Normal, colorOf(line.kind)),
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            if (matrix == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(INK_OK.copy(alpha = 0.18f))
                )
                val promptText = TerminalEngine.prompt(shell.cwd)
                val caret = if (blinkOn) "▌" else " "
                val before = input.take(cursor.coerceIn(0, input.length))
                val after = input.drop(cursor.coerceIn(0, input.length))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = promptText, style = mono(13.sp, FontWeight.Medium, INK_OK))
                    Text(
                        text = before + caret + after,
                        style = mono(13.sp, FontWeight.Medium, INK_BRIGHT),
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    if (!keyboardVisible) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .border(1.dp, INK_OK.copy(alpha = 0.45f))
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = { keyboardVisible = true })
                                }
                                .padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KeyGlyph(tint = INK_OK)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(text = "键盘", style = mono(11.sp, FontWeight.Medium, INK_OK))
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = keyboardVisible,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(120))
            ) {
                TerminalKeyboard(
                    ctrlArmed = ctrlArmed,
                    altArmed = altArmed,
                    onKey = { key -> if (ready) handleKey(key) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SCREEN_BG.copy(alpha = (1f - contentP.value).coerceIn(0f, 1f)))
        )

        Canvas(
            modifier = Modifier.fillMaxSize(),
            onDraw = {
                var y = 0f
                while (y < size.height) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.18f),
                        topLeft = Offset(0f, y),
                        size = androidx.compose.ui.geometry.Size(size.width, 1f)
                    )
                    y += 3f
                }
            }
        )

        val open = openP.value
        val scan = scanP.value

        if (open < 1f || scan < 1f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (open < 1f) {
                    val h = 2f + open * (fullHeightPx - 2f)
                    val top = (fullHeightPx - h) / 2f
                    drawRect(
                        color = Color(0xFFE9FFEF),
                        topLeft = Offset(0f, top),
                        size = androidx.compose.ui.geometry.Size(fullWidthPx, h)
                    )
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(0f, fullHeightPx / 2f - 1f),
                        size = androidx.compose.ui.geometry.Size(fullWidthPx, 2f)
                    )
                }
                if (scan > 0f && scan < 1f) {
                    val bandH = 46f
                    val y = scan * fullHeightPx
                    drawRect(
                        color = INK_OK.copy(alpha = 0.10f),
                        topLeft = Offset(0f, y - bandH),
                        size = androidx.compose.ui.geometry.Size(fullWidthPx, bandH)
                    )
                    drawRect(
                        color = Color.White.copy(alpha = 0.22f),
                        topLeft = Offset(0f, y - 1.5f),
                        size = androidx.compose.ui.geometry.Size(fullWidthPx, 1.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val stroke = 1.3f
        val w = size.width
        val h = size.height
        drawRect(
            color = tint,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = androidx.compose.ui.geometry.Size(w - stroke, h - stroke),
            style = Stroke(width = stroke)
        )
        val topRow = h * 0.42f
        val bottomRow = h * 0.68f
        drawLine(
            color = tint,
            start = Offset(w * 0.24f, topRow),
            end = Offset(w * 0.76f, topRow),
            strokeWidth = stroke
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.30f, bottomRow),
            end = Offset(w * 0.70f, bottomRow),
            strokeWidth = stroke
        )
    }
}

private fun mono(size: androidx.compose.ui.unit.TextUnit, weight: FontWeight, color: Color) =
    TextStyle(
        fontFamily = MonoFontFamily,
        fontSize = size,
        fontWeight = weight,
        color = color
    )

private fun colorOf(kind: TerminalEngine.Kind): Color = when (kind) {
    TerminalEngine.Kind.IN -> INK_BRIGHT
    TerminalEngine.Kind.ERR -> INK_ERR
    TerminalEngine.Kind.OK -> INK_OK
    TerminalEngine.Kind.DIM -> INK_DIM
    TerminalEngine.Kind.OUT -> INK_OUT
}
