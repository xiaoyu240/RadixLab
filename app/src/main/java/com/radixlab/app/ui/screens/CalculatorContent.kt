package com.radixlab.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radixlab.app.data.CalculatorEngine
import com.radixlab.app.data.PiEngine
import com.radixlab.app.ui.components.InfoCard
import com.radixlab.app.ui.theme.CardShape
import com.radixlab.app.ui.theme.Dimens
import com.radixlab.app.ui.theme.FieldShape
import com.radixlab.app.ui.theme.MonoBody
import com.radixlab.app.ui.theme.MonoLabel
import com.radixlab.app.ui.theme.MonoSmall
import com.radixlab.app.ui.theme.MonoTitle
import com.radixlab.app.ui.theme.RadixResultCardBorder
import com.radixlab.app.utils.rememberCopyAction
import com.radixlab.app.viewmodel.CalculatorViewModel

/** 结果太长时界面最多渲染多少字符（完整结果始终可以复制） */
private const val MAX_RENDER_CHARS = 4000

/** 键盘按钮类型 */
private enum class KeyKind { DIGIT, OP, FUNC }

/** 键盘按钮动作 */
private sealed interface KeyAction {
    /** 往算式末尾插入一段文本 */
    data class Insert(val text: String) : KeyAction

    /** 退格 */
    data object Backspace : KeyAction
}

private data class CalcKey(
    val label: String,
    val kind: KeyKind,
    val action: KeyAction
)

/**
 * 四列键盘，共 5 行 —— 与官网在线计算器（`web/calculator.html`）**布局完全一致**。
 * 清空用输入框右侧的 ✕，退格用 ⌫。
 */
private val KEYPAD_ROWS: List<List<CalcKey>> = listOf(
    listOf(
        CalcKey("(", KeyKind.FUNC, KeyAction.Insert("(")),
        CalcKey(")", KeyKind.FUNC, KeyAction.Insert(")")),
        CalcKey("^", KeyKind.OP, KeyAction.Insert("^")),
        CalcKey("%", KeyKind.OP, KeyAction.Insert("%"))
    ),
    listOf(
        CalcKey("7", KeyKind.DIGIT, KeyAction.Insert("7")),
        CalcKey("8", KeyKind.DIGIT, KeyAction.Insert("8")),
        CalcKey("9", KeyKind.DIGIT, KeyAction.Insert("9")),
        CalcKey("÷", KeyKind.OP, KeyAction.Insert("/"))
    ),
    listOf(
        CalcKey("4", KeyKind.DIGIT, KeyAction.Insert("4")),
        CalcKey("5", KeyKind.DIGIT, KeyAction.Insert("5")),
        CalcKey("6", KeyKind.DIGIT, KeyAction.Insert("6")),
        CalcKey("×", KeyKind.OP, KeyAction.Insert("*"))
    ),
    listOf(
        CalcKey("1", KeyKind.DIGIT, KeyAction.Insert("1")),
        CalcKey("2", KeyKind.DIGIT, KeyAction.Insert("2")),
        CalcKey("3", KeyKind.DIGIT, KeyAction.Insert("3")),
        CalcKey("−", KeyKind.OP, KeyAction.Insert("-"))
    ),
    listOf(
        CalcKey("0", KeyKind.DIGIT, KeyAction.Insert("0")),
        CalcKey(".", KeyKind.DIGIT, KeyAction.Insert(".")),
        CalcKey("⌫", KeyKind.FUNC, KeyAction.Backspace),
        CalcKey("+", KeyKind.OP, KeyAction.Insert("+"))
    )
)

/**
 * 大数计算器内容区（嵌入首页工具卡片使用）。
 *
 * 与官网在线计算器共用同一套算法（[CalculatorEngine] ↔ web/js/calculator.js）：
 * 四则运算 + 括号 + 乘方 + 取余，有理数精确计算；
 * 除不尽时只显示前 [CalculatorEngine.DECIMAL_PLACES] 位小数，并给出提示。
 *
 * 输入即算，无需按等号；结果**不写入历史记录**。
 */
@Composable
fun CalculatorContent(
    modifier: Modifier = Modifier,
    viewModel: CalculatorViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val copy = rememberCopyAction("进制工坊")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.PageGutter)
            .padding(top = Dimens.Space4, bottom = Dimens.Space8),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space4)
    ) {

        // ---------------------------------------------------------------
        // 算式输入
        // ---------------------------------------------------------------
        OutlinedTextField(
            value = state.expression,
            onValueChange = viewModel::onExpressionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = "算式", style = MonoLabel) },
            placeholder = {
                Text(
                    text = "例如 2^1000、1/3、(50+2)*7",
                    style = MonoBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            textStyle = MonoTitle,
            singleLine = false,
            maxLines = 3,
            isError = state.error != null,
            shape = FieldShape,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            trailingIcon = {
                if (state.expression.isNotEmpty()) {
                    IconButton(onClick = viewModel::clear) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "清空算式",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            supportingText = {
                Text(
                    text = "支持 + − × ÷、括号 ( )、乘方 ^、取余 %；全角字符与空格会自动纠正",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        // ---------------------------------------------------------------
        // 错误提示 / 未输完提示
        // ---------------------------------------------------------------
        val error = state.error
        if (error != null) {
            ErrorBanner(message = error)
        }

        // ---------------------------------------------------------------
        // 结果
        // ---------------------------------------------------------------
        ResultBlock(
            value = state.result,
            exact = state.exact,
            approx = state.approx,
            digits = state.digits,
            hints = buildList {
                if (state.showInfiniteHint) add(CalculatorEngine.INFINITE_HINT)
                if (state.error == null && state.result.isEmpty() && state.expression.isNotBlank()) {
                    add(if (state.incomplete) "算式还没输完，继续输入就会出结果" else "算式暂时无法计算")
                }
            },
            emptyPlaceholder = "等待输入算式",
            piRunning = state.piRunning,
            onPiClick = viewModel::startPi,
            onCopy = { copy(state.result) }
        )

        // ---------------------------------------------------------------
        // π 无限计算：进度面板
        // ---------------------------------------------------------------
        if (state.piPanelVisible) {
            PiPanel(
                meter = state.piMeter,
                digits = state.piDigits,
                running = state.piRunning,
                elapsedSeconds = state.piElapsedSeconds,
                onStop = viewModel::stopPi,
                onHide = viewModel::hidePiPanel,
                onCopy = { copy(viewModel.currentPiText()) }
            )
        }

        // ---------------------------------------------------------------
        // 键盘
        // ---------------------------------------------------------------
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
            KEYPAD_ROWS.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
                ) {
                    row.forEach { key ->
                        KeypadKey(
                            key = key,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                when (val action = key.action) {
                                    is KeyAction.Insert -> viewModel.append(action.text)
                                    KeyAction.Backspace -> viewModel.backspace()
                                }
                            }
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------------
        // 说明
        // ---------------------------------------------------------------
        InfoCard(title = "运算规则", borderColor = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                text = "· 优先级：乘方 ^ ＞ 乘除取余 ＞ 加减；同级从左到右，括号可改变顺序",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "· 乘方右结合：2^3^2 = 2^(3^2) = 512；指数可以为负，如 2^-1 = 0.5，但必须是整数",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "· 取余按向下取整：-7 % 3 = 2，7 % -3 = -2（与 Python 一致）",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "· 全程用有理数精确计算，50 位以上的大数也不丢精度；除不尽的小数只显示前 " +
                    "${CalculatorEngine.DECIMAL_PLACES} 位",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "· 输入 π 或 pi 得到圆周率 3.1415926535（近似值）。π 是无限不循环小数，" +
                    "点结果区的「π 一直算下去」可以一档一档真算下去 —— 每涨 1 MB 提醒一次，到 10 MB 强制停下",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "· 计算器的算式与结果不会写入历史记录，关掉即消失",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ---------------------------------------------------------------
        // π 的分级提醒弹窗（文案与官网逐字一致）
        // ---------------------------------------------------------------
        state.piDialog?.let { spec ->
            PiDialog(spec = spec, onAnswer = viewModel::answerPiDialog)
        }
    }
}

/**
 * 结果区：等宽大字号 + 一键复制；结果超长时限制高度可滚动，
 * 超过 [MAX_RENDER_CHARS] 字符则只渲染前一段（完整结果仍可复制）。
 */
@Composable
private fun ResultBlock(
    value: String,
    exact: Boolean,
    approx: Boolean,
    digits: Int,
    hints: List<String>,
    emptyPlaceholder: String,
    piRunning: Boolean,
    onPiClick: () -> Unit,
    onCopy: () -> Unit
) {
    val hasValue = value.isNotEmpty()
    val truncatedForRender = value.length > MAX_RENDER_CHARS
    val rendered = remember(value) {
        if (truncatedForRender) value.take(MAX_RENDER_CHARS) + "…" else value
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (hasValue) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        // v5：结果卡描边用品牌红，空态用灰细线；一律无阴影
        border = if (hasValue) {
            RadixResultCardBorder()
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "结果",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (hasValue) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = onPiClick, enabled = !piRunning) {
                    Text(
                        text = if (piRunning) "π 正在算…" else "π 一直算下去",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (hasValue) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(Dimens.TouchTarget)) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "复制结果",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(Dimens.IconMedium)
                        )
                    }
                }
            }

            if (hasValue) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = rendered,
                        style = MonoTitle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else {
                Text(
                    text = emptyPlaceholder,
                    style = MonoTitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (hasValue) {
                Text(
                    text = buildString {
                        when {
                            exact -> append("精确值")
                            approx -> append("算式里用到了 π，按 ${CalculatorEngine.DECIMAL_PLACES} 位小数参与运算")
                            else -> append("除不尽，已截断到 ${CalculatorEngine.DECIMAL_PLACES} 位小数")
                        }
                        append(" · 共 $digits 位数字")
                        if (truncatedForRender) append(" · 界面仅显示前 $MAX_RENDER_CHARS 字符，完整结果请点右上角复制")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            hints.forEach { hint ->
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * π 无限计算面板：当前档位说明 + 已算出的位数（截断显示）+ 停止 / 复制 / 收起。
 *
 * 「占用」= 已算出的 π 文本（1 位 1 字节）+ 参与运算的大数实际字节数，都是引擎回报的实测值。
 */
@Composable
private fun PiPanel(
    meter: String,
    digits: String,
    running: Boolean,
    elapsedSeconds: Int,
    onStop: () -> Unit,
    onHide: () -> Unit,
    onCopy: () -> Unit
) {
    val truncated = digits.length > MAX_RENDER_CHARS
    val shown = remember(digits) {
        if (truncated) {
            digits.take(MAX_RENDER_CHARS) + "…（界面只显示前 $MAX_RENDER_CHARS 位，完整结果可以复制）"
        } else {
            digits
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "π 无限计算",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.weight(1f))

                if (digits.isNotEmpty()) {
                    TextButton(onClick = onCopy) { Text("复制") }
                }
                if (running) {
                    TextButton(onClick = onStop) { Text("停止") }
                } else {
                    TextButton(onClick = onHide) { Text("收起") }
                }
            }

            Text(
                text = if (running) "$meter（已用时 $elapsedSeconds 秒）" else meter,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (digits.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = shown,
                        style = MonoSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** π 的分级提醒弹窗（文案来自 [PiEngine]，与官网逐字一致） */
@Composable
private fun PiDialog(spec: PiEngine.Dialog, onAnswer: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAnswer(false) },
        title = { Text(text = spec.title) },
        text = { Text(text = spec.body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = { onAnswer(true) }) { Text(spec.confirm) }
        },
        dismissButton = {
            val label = spec.cancel
            if (label != null) {
                TextButton(onClick = { onAnswer(false) }) { Text(label) }
            }
        }
    )
}

/** 键盘按键：数字 / 运算符 / 功能键三种配色 */
@Composable
private fun KeypadKey(
    key: CalcKey,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background: Color
    val foreground: Color
    when (key.kind) {
        KeyKind.DIGIT -> {
            background = MaterialTheme.colorScheme.surface
            foreground = MaterialTheme.colorScheme.onSurface
        }
        KeyKind.OP -> {
            background = MaterialTheme.colorScheme.secondaryContainer
            foreground = MaterialTheme.colorScheme.onSecondaryContainer
        }
        KeyKind.FUNC -> {
            background = MaterialTheme.colorScheme.surfaceVariant
            foreground = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = FieldShape,
        color = background,
        contentColor = foreground,
        border = if (key.kind == KeyKind.DIGIT) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        } else {
            null
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (key.action == KeyAction.Backspace) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = "退格",
                    modifier = Modifier.size(Dimens.IconMedium)
                )
            } else {
                Text(
                    text = key.label,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
