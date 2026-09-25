package com.radixlab.app.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radixlab.app.ui.theme.MonoFontFamily

sealed interface TerminalKey {
    data class CharKey(val value: Char) : TerminalKey
    data object Backspace : TerminalKey
    data object ClearLine : TerminalKey
    data object Enter : TerminalKey
    data object Escape : TerminalKey
    data object Tab : TerminalKey
    data object Ctrl : TerminalKey
    data object Alt : TerminalKey
    data object ArrowUp : TerminalKey
    data object ArrowDown : TerminalKey
    data object ArrowLeft : TerminalKey
    data object ArrowRight : TerminalKey
    data object Interrupt : TerminalKey
    data object Eof : TerminalKey
}

private val INK = Color(0xFFECECEC)
private val PANEL_BG = Color(0xFF040604)
private val EDGE = INK.copy(alpha = 0.28f)

private const val GAP = 2f

private val NUMBER_ROW = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
private val LETTER_TOP = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
private val LETTER_MID = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
private val LETTER_BOTTOM = listOf("z", "x", "c", "v", "b", "n", "m")
private val SYMBOL_TOP = listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")")
private val SYMBOL_MID = listOf("-", "_", "=", "+", "[", "]", "{", "}", "/", "\\")
private val SYMBOL_BOTTOM = listOf(";", ":", "'", "\"", ",", ".", "<", ">", "~", "π")

private val FN_KEYS: List<Pair<String, TerminalKey>> = listOf(
    "ESC" to TerminalKey.Escape,
    "TAB" to TerminalKey.Tab,
    "CTRL" to TerminalKey.Ctrl,
    "ALT" to TerminalKey.Alt,
    "↑" to TerminalKey.ArrowUp,
    "↓" to TerminalKey.ArrowDown,
    "←" to TerminalKey.ArrowLeft,
    "→" to TerminalKey.ArrowRight,
    "^C" to TerminalKey.Interrupt,
    "^D" to TerminalKey.Eof,
    "CLR" to TerminalKey.ClearLine
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerminalKeyboard(
    modifier: Modifier = Modifier,
    ctrlArmed: Boolean,
    altArmed: Boolean,
    onKey: (TerminalKey) -> Unit
) {
    var symbolPage by remember { mutableStateOf(false) }
    var shifted by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PANEL_BG)
            .drawBehind {
                drawRect(
                    color = EDGE,
                    topLeft = Offset.Zero,
                    size = Size(size.width, 1.dp.toPx())
                )
            }
            .padding(horizontal = 2.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FN_KEYS.forEach { (label, key) ->
                FnKey(
                    label = label,
                    active = when (key) {
                        TerminalKey.Ctrl -> ctrlArmed
                        TerminalKey.Alt -> altArmed
                        else -> false
                    }
                ) { onKey(key) }
            }
        }

        LetterRow(NUMBER_ROW) { onKey(it) }

        if (symbolPage) {
            LetterRow(SYMBOL_TOP) { onKey(it) }
            LetterRow(SYMBOL_MID) { onKey(it) }
            LetterRow(SYMBOL_BOTTOM) { onKey(it) }
        } else {
            LetterRow(LETTER_TOP, shifted = shifted) { onKey(it) }

            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(0.5f))
                LETTER_MID.forEach { label ->
                    val typed = if (shifted) label.uppercase() else label
                    Key(label = typed) { onKey(TerminalKey.CharKey(typed[0])) }
                }
                Spacer(modifier = Modifier.weight(0.5f))
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Key(label = "⇧", weight = 1.5f, active = shifted) { shifted = !shifted }
                LETTER_BOTTOM.forEach { label ->
                    val typed = if (shifted) label.uppercase() else label
                    Key(label = typed) { onKey(TerminalKey.CharKey(typed[0])) }
                }
                Key(
                    label = "⌫",
                    weight = 1.5f,
                    onLongPress = { onKey(TerminalKey.ClearLine) }
                ) { onKey(TerminalKey.Backspace) }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Key(
                label = if (symbolPage) "ABC" else "SYM",
                weight = 1.5f,
                active = symbolPage
            ) { symbolPage = !symbolPage }
            if (symbolPage) {
                Key(label = ",", weight = 1f) { onKey(TerminalKey.CharKey(',')) }
            } else {
                Key(label = "-", weight = 1f) { onKey(TerminalKey.CharKey('-')) }
            }
            Key(label = "空格", weight = 4f) { onKey(TerminalKey.CharKey(' ')) }
            Key(label = ".", weight = 1f) { onKey(TerminalKey.CharKey('.')) }
            Key(label = "ENTER", weight = 2.5f, strong = true) { onKey(TerminalKey.Enter) }
        }
    }
}

@Composable
private fun LetterRow(
    labels: List<String>,
    shifted: Boolean = false,
    onKey: (TerminalKey) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { label ->
            val typed = if (shifted && label.length == 1 && label[0] in 'a'..'z') {
                label.uppercase()
            } else {
                label
            }
            Key(label = typed) { onKey(TerminalKey.CharKey(typed[0])) }
        }
    }
}

@Composable
private fun RowScope.Key(
    label: String,
    weight: Float = 1f,
    active: Boolean = false,
    strong: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val borderColor = when {
        active || pressed -> INK
        strong -> INK.copy(alpha = 0.75f)
        else -> INK.copy(alpha = 0.42f)
    }
    val bg = if (pressed || active) INK else Color.Transparent
    val fg = if (pressed || active) PANEL_BG else INK

    Box(
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = GAP.dp)
            .height(36.dp)
            .background(bg)
            .border(1.dp, borderColor)
            .pointerInput(label, onLongPress) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { if (onLongPress != null) onLongPress() else onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = MonoFontFamily,
                fontSize = if (label.length > 1) 11.sp else 14.sp,
                fontWeight = FontWeight.Medium,
                color = fg
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun FnKey(
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val fg = if (pressed || active) PANEL_BG else INK.copy(alpha = 0.88f)
    val bg = if (pressed || active) INK else Color.Transparent

    Box(
        modifier = Modifier
            .height(30.dp)
            .defaultMinSize(minWidth = 44.dp)
            .padding(horizontal = GAP.dp)
            .background(bg)
            .border(1.dp, if (pressed || active) INK else INK.copy(alpha = 0.32f))
            .pointerInput(label) {
                detectTapGestures(
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = MonoFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = fg
            ),
            maxLines = 1
        )
    }
}
