package com.radixlab.app.ui.terminal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radixlab.app.ui.theme.MonoFontFamily
import kotlin.math.abs
import kotlin.math.floor

private val GLYPHS: List<Char> =
    "日ﾊﾐﾋｰｳｼﾅﾓﾆｻﾜﾂｵﾘｱﾎﾃﾏｹﾒｴｶｷﾑﾕﾗｾﾈｽﾀﾇﾍ0123456789".toList()

private const val MAX_COLS = 72
private const val MAX_TRAIL = 22
private const val MIN_TRAIL = 6

@Composable
fun MatrixRain(
    options: TerminalEngine.MatrixOptions,
    modifier: Modifier = Modifier,
    hint: String
) {
    val density = LocalDensity.current
    val style = remember(options.bold) {
        TextStyle(
            fontFamily = MonoFontFamily,
            fontSize = 15.sp,
            lineHeight = 16.sp,
            fontWeight = if (options.bold) FontWeight.Bold else FontWeight.Normal
        )
    }
    val measurer = rememberTextMeasurer()

    val layouts: Map<Char, TextLayoutResult> = remember(measurer, style) {
        GLYPHS.associateWith { ch -> measurer.measure(AnnotatedString(ch.toString()), style) }
    }
    val cell = remember(layouts) {
        val sample = layouts.values.first()
        val w = sample.size.width.coerceAtLeast(1)
        val h = sample.size.height.coerceAtLeast(1)
        w to h
    }

    BoxWithConstraints(modifier.background(Color(0xFF040604))) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val cols = ((widthPx / cell.first).toInt()).coerceIn(1, MAX_COLS)
        val rows = ((heightPx / cell.second).toInt()).coerceAtLeast(1)

        val heads = remember(cols) {
            FloatArray(cols) { -(1f + (abs(it * 37 % (rows + MAX_TRAIL))).toFloat()) }
        }
        val speeds = remember(cols, options.async) {
            FloatArray(cols) { if (options.async) 0.6f + ((it * 53) % 13) / 10f else 1f }
        }
        val trails = remember(cols) {
            IntArray(cols) { MIN_TRAIL + (abs(it * 29) % (MAX_TRAIL - MIN_TRAIL + 1)) }
        }

        var tick by remember { mutableIntStateOf(0) }

        LaunchedEffect(cols, rows, options.delayMs, options.async) {
            val delayNanos = options.delayMs.toLong() * 1_000_000L
            var acc = 0L
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    if (last == 0L) last = now
                    acc += now - last
                    last = now
                    var steps = 0
                    while (acc >= delayNanos && steps < 3) {
                        acc -= delayNanos
                        steps++
                        for (i in 0 until cols) {
                            heads[i] += speeds[i]
                            if (heads[i] - trails[i] > rows) {
                                heads[i] = -(1f + ((i * 7 + steps) % 18).toFloat())
                                if (options.async) speeds[i] = 0.6f + ((i * 53 + steps * 17) % 13) / 10f
                            }
                        }
                    }
                    acc = acc.coerceAtMost(delayNanos * 3)
                    tick++
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (tick < 0) return@Canvas
            val cellW = cell.first.toFloat()
            val cellH = cell.second.toFloat()

            for (col in 0 until cols) {
                val head = heads[col]
                val trail = trails[col]

                for (k in 0 until trail) {
                    val rowF = head - k
                    val row = floor(rowF).toInt()
                    if (row < 0 || row >= rows) continue

                    val seed = col * 73856093 xor (row * 19349663) xor ((tick / 4) * 83492791)
                    val glyph = GLYPHS[(seed and 0x7FFFFFFF) % GLYPHS.size]
                    val layout = layouts[glyph] ?: continue

                    val base = if (options.rainbow) {
                        Color.hsv(((col * 360f / cols) + tick * 0.6f) % 360f, 0.80f, 1f)
                    } else {
                        baseColor(options.color)
                    }

                    val color = if (k == 0) {
                        lerp(base, Color.White, 0.72f)
                    } else {
                        val fade = 1f - k.toFloat() / trail
                        base.copy(alpha = (fade * 0.92f).coerceIn(0.10f, 0.92f))
                    }

                    drawText(
                        textLayoutResult = layout,
                        color = color,
                        topLeft = Offset(col * cellW, row * cellH)
                    )
                }
            }
        }

        Text(
            text = hint,
            style = TextStyle(
                fontFamily = MonoFontFamily,
                fontSize = 11.sp,
                color = Color(0xFF9BE8B0).copy(alpha = 0.75f)
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .wrapContentHeight()
                .padding(bottom = 6.dp)
        )
    }
}

private fun baseColor(name: String): Color = when (name) {
    "red" -> Color(0xFFFF5A5A)
    "blue" -> Color(0xFF5A9BFF)
    "cyan" -> Color(0xFF5AFFF0)
    "magenta" -> Color(0xFFFF6AE0)
    "yellow" -> Color(0xFFFFE05A)
    "white" -> Color(0xFFE8E8E8)
    else -> Color(0xFF33FF66)
}
