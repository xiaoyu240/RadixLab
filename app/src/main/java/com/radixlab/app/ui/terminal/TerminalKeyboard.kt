package com.radixlab.app.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
private const val GRID_COLS = 10

private val LETTER_ROWS = listOf(
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "_"),
    listOf("_", "z", "x", "c", "v", "b", "n", "m", "_")
)

private val SYMBOL_ROWS = listOf(
    listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")"),
    listOf("-", "_", "=", "+", "[", "]", "{", "}", "/", "\\"),
    listOf(";", ":", "'", "\"", ",", ".", "<", ">", "~", "`"),
    listOf("|", "?", "π", "×", "÷", "√", "∞", "°", "±", "§")
)

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
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FnKey("ESC") { onKey(TerminalKey.Escape) }
            FnKey("TAB") { onKey(TerminalKey.Tab) }
            FnKey("CTRL", active = ctrlArmed) { onKey(TerminalKey.Ctrl) }
            FnKey("ALT", active = altArmed) { onKey(TerminalKey.Alt) }
            FnKey("↑") { onKey(TerminalKey.ArrowUp) }
            FnKey("↓") { onKey(TerminalKey.ArrowDown) }
            FnKey("←") { onKey(TerminalKey.ArrowLeft) }
            FnKey("→") { onKey(TerminalKey.ArrowRight) }
            FnKey("^C") { onKey(TerminalKey.Interrupt) }
            FnKey("^D") { onKey(TerminalKey.Eof) }
            FnKey("CLR") { onKey(TerminalKey.ClearLine) }
        }

        val rows = if (symbolPage) SYMBOL_ROWS else LETTER_ROWS
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { label ->
                    if (label == "_") {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val typed = if (shifted && label.length == 1 && label[0] in 'a'..'z') {
                            label.uppercase()
                        } else {
                            label
                        }
                        Key(label = typed) { onKey(TerminalKey.CharKey(typed[0])) }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (symbolPage) {
                Key(label = "ABC", weight = 1f) { symbolPage = false }
                Key(label = "⇧", weight = 1f, active = shifted) { shifted = !shifted }
                Spacer(modifier = Modifier.weight(2f))
            } else {
                Key(label = "SYM", weight = 1f) { symbolPage = true }
                Key(label = "⇧", weight = 1f, active = shifted) { shifted = !shifted }
                Key(label = "-", weight = 2f) { onKey(TerminalKey.CharKey('-')) }
            }
            Key(
                label = "⌫",
                weight = 1.4f,
                onLongPress = { onKey(TerminalKey.ClearLine) }
            ) { onKey(TerminalKey.Backspace) }
            Key(label = "空格", weight = 1.6f) { onKey(TerminalKey.CharKey(' ')) }
            Key(label = "ENTER", weight = 2f, strong = true) { onKey(TerminalKey.Enter) }
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
            .height(38.dp)
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
            .height(32.dp)
            .background(bg)
            .border(1.dp, if (pressed || active) INK else INK.copy(alpha = 0.32f))
            .padding(horizontal = 12.dp)
            .pointerInput(label) {
                detectTapGestures(
                    onTap = { onClick() }
                )
            },
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
