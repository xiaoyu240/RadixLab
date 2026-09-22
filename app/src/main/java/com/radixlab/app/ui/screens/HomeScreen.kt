package com.radixlab.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radixlab.app.data.ConversionEngine
import com.radixlab.app.ui.components.BaseSelector
import com.radixlab.app.ui.components.BrandLogo
import com.radixlab.app.ui.components.InfoCard
import com.radixlab.app.ui.components.ResultCard
import com.radixlab.app.ui.theme.Dimens
import com.radixlab.app.ui.theme.FieldShape
import com.radixlab.app.ui.theme.MonoBody
import com.radixlab.app.ui.theme.MonoLabel
import com.radixlab.app.ui.theme.MonoSmall
import com.radixlab.app.ui.theme.MonoTitle
import com.radixlab.app.ui.theme.RadiusSmall
import com.radixlab.app.utils.rememberCopyAction
import com.radixlab.app.viewmodel.ConverterViewModel
import com.radixlab.app.viewmodel.PRESET_BASES

/**
 * 首页 —— 通用进制转换。
 *
 * 2–36 任意进制互转，二进制/八进制/十进制/十六进制快捷选择，
 * BigInteger 大数运算，非法字符校验，一键复制，可展开查看转换步骤。
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: ConverterViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val copy = rememberCopyAction("进制工坊")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.PageGutter)
            .padding(top = Dimens.Space6, bottom = Dimens.Space8),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space4)
    ) {

        // ---------------------------------------------------------------
        // 品牌头部
        // ---------------------------------------------------------------
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandLogo(size = Dimens.LogoMedium)
            Spacer(modifier = Modifier.width(Dimens.Space3))
            Column {
                Text(
                    text = "进制工坊",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "进制转换，一个就够",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ---------------------------------------------------------------
        // 输入
        // ---------------------------------------------------------------
        OutlinedTextField(
            value = state.input,
            onValueChange = viewModel::onInputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = "待转换的数值", style = MonoLabel) },
            placeholder = {
                Text(
                    text = if (state.fromBase == 2) "例如 1010 1100" else "例如 255、FF、777",
                    style = MonoBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            textStyle = MonoTitle,
            singleLine = false,
            maxLines = 3,
            isError = state.error != null,
            shape = FieldShape,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.Characters
            ),
            trailingIcon = {
                if (state.input.isNotEmpty()) {
                    IconButton(onClick = viewModel::clearInput) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "清空输入",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            supportingText = {
                Text(
                    text = "自动忽略空格与 0x / 0b / 0o 前缀；支持任意长度大数",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        // ---------------------------------------------------------------
        // 源进制 / 目标进制
        // ---------------------------------------------------------------
        BaseSelector(
            title = "源进制",
            selectedBase = state.fromBase,
            isCustom = state.fromCustom,
            presets = PRESET_BASES,
            onPresetSelected = viewModel::onFromPresetSelected,
            onCustomSelected = viewModel::onFromCustomSelected,
            onBaseChanged = viewModel::onFromBaseChanged
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                onClick = viewModel::swapBases,
                shape = FieldShape
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapVert,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconSmall)
                )
                Spacer(modifier = Modifier.width(Dimens.Space2))
                Text(text = "交换进制")
            }
        }

        BaseSelector(
            title = "目标进制",
            selectedBase = state.toBase,
            isCustom = state.toCustom,
            presets = PRESET_BASES,
            onPresetSelected = viewModel::onToPresetSelected,
            onCustomSelected = viewModel::onToCustomSelected,
            onBaseChanged = viewModel::onToBaseChanged
        )

        // ---------------------------------------------------------------
        // 错误提示
        // ---------------------------------------------------------------
        val error = state.error
        if (error != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = FieldShape
            ) {
                Row(
                    modifier = Modifier.padding(Dimens.Space3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(Dimens.IconSmall)
                    )
                    Spacer(modifier = Modifier.width(Dimens.Space2))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // ---------------------------------------------------------------
        // 结果
        // ---------------------------------------------------------------
        ResultCard(
            title = "转换结果",
            badge = ConversionEngine.baseShortName(state.toBase),
            value = state.output,
            highlighted = true,
            subtitle = if (state.hasResult) {
                "${ConversionEngine.baseLabel(state.fromBase)} → ${ConversionEngine.baseLabel(state.toBase)}" +
                    if (state.isLongResult) " · 结果较长，可点击右上角复制" else ""
            } else {
                "在顶部输入数值，结果会实时出现在这里"
            },
            emptyPlaceholder = "等待输入"
        )

        if (state.hasResult) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
            ) {
                Button(
                    onClick = { copy(state.output) },
                    modifier = Modifier.weight(1f),
                    shape = FieldShape
                ) {
                    Text(text = "复制结果")
                }
                OutlinedButton(
                    onClick = viewModel::toggleSteps,
                    modifier = Modifier.weight(1f),
                    shape = FieldShape
                ) {
                    Icon(
                        imageVector = if (state.showSteps) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconSmall)
                    )
                    Spacer(modifier = Modifier.width(Dimens.Space2))
                    Text(text = if (state.showSteps) "收起步骤" else "转换步骤")
                }
            }

            Text(
                text = "十进制中间值：${state.decimal}",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ---------------------------------------------------------------
        // 转换步骤
        // ---------------------------------------------------------------
        if (state.showSteps && state.steps.isNotEmpty()) {
            InfoCard(title = "计算过程") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                state.steps.forEach { step ->
                    Text(
                        text = step,
                        style = MonoSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // ---------------------------------------------------------------
        // 说明
        // ---------------------------------------------------------------
        InfoCard(title = "关于结果", borderColor = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                text = "· 采用 BigInteger 精确运算，不经过浮点，任意位数都不会丢精度",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "· 非法字符会在输入框中就地提示，例如 2 进制输入「2」会直接报错",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "· 每次成功的转换会自动写入「我的 → 历史记录」，可收藏、可清空",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
