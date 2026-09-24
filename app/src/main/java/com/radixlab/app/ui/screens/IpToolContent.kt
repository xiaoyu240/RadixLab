package com.radixlab.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radixlab.app.ui.components.InfoCard
import com.radixlab.app.ui.components.ResultCard
import com.radixlab.app.ui.theme.ChipShape
import com.radixlab.app.ui.theme.Dimens
import com.radixlab.app.ui.theme.FieldShape
import com.radixlab.app.ui.theme.MonoBody
import com.radixlab.app.ui.theme.MonoLabel
import com.radixlab.app.ui.theme.MonoSmall
import com.radixlab.app.ui.theme.MonoTitle
import com.radixlab.app.viewmodel.IpInputType
import com.radixlab.app.viewmodel.IpViewModel

/**
 * IP 工具内容区（嵌入首页选项卡使用，不再有独立页面和品牌头部）。
 *
 * IPv4：点分十进制 / 十进制整数 / 二进制 / 十六进制 四向互转，每段 0-255 严格校验。
 * IPv6：压缩形式与完整展开互转，支持内嵌 IPv4。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IpToolContent(
    modifier: Modifier = Modifier,
    viewModel: IpViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.PageGutter)
            .padding(top = Dimens.Space4, bottom = Dimens.Space8),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space4)
    ) {

        // ===============================================================
        // IPv4
        // ===============================================================
        Text(
            text = "IPv4 转换",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            IpInputType.entries.toList().forEach { type ->
                val selected = state.ipv4InputType == type
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.onIpv4TypeChange(type) },
                    shape = ChipShape,
                    label = { Text(text = type.label, style = MonoLabel) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        OutlinedTextField(
            value = state.ipv4Input,
            onValueChange = viewModel::onIpv4InputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = state.ipv4InputType.label, style = MonoLabel) },
            placeholder = {
                Text(
                    text = state.ipv4InputType.hint,
                    style = MonoBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            textStyle = MonoTitle,
            singleLine = false,
            maxLines = 3,
            isError = state.ipv4Error != null,
            shape = FieldShape,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            trailingIcon = {
                if (state.ipv4Input.isNotEmpty()) {
                    IconButton(onClick = viewModel::clearIpv4) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "清空输入",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )

        // 示例地址
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            Text(
                text = "示例：",
                style = MonoLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.Space3)
            )
            IpViewModel.IPV4_SAMPLES.forEach { sample ->
                TextButton(
                    onClick = { viewModel.fillIpv4Sample(sample) },
                    shape = ChipShape
                ) {
                    Text(text = sample, style = MonoSmall)
                }
            }
        }

        val ipv4Error = state.ipv4Error
        if (ipv4Error != null) {
            ErrorBanner(message = ipv4Error)
        }

        ResultCard(
            title = "点分十进制",
            badge = "IPv4",
            value = state.dotted,
            highlighted = true,
            subtitle = if (state.ipv4HasResult) "每段取值 0 - 255" else "输入任意一种表示形式，四种结果同时给出",
            emptyPlaceholder = "等待输入"
        )

        ResultCard(
            title = "二进制",
            badge = "BIN",
            value = state.binary,
            subtitle = "每段 8 位，共 32 位"
        )

        ResultCard(
            title = "十六进制",
            badge = "HEX",
            value = state.hex,
            subtitle = "每段 2 位，如 C0.A8.01.01"
        )

        ResultCard(
            title = "十进制整数",
            badge = "DEC",
            value = state.decimal,
            subtitle = "32 位无符号整数（0 - 4294967295）"
        )

        if (state.ipv4HasResult) {
            OutlinedButton(
                onClick = viewModel::toggleIpv4Steps,
                modifier = Modifier.fillMaxWidth(),
                shape = FieldShape
            ) {
                Icon(
                    imageVector = if (state.showSteps) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconSmall)
                )
                Spacer(modifier = Modifier.width(Dimens.Space2))
                Text(text = if (state.showSteps) "收起换算过程" else "查看换算过程")
            }

            if (state.showSteps && state.ipv4Steps.isNotEmpty()) {
                InfoCard(title = "换算过程") {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    state.ipv4Steps.forEach { step ->
                        Text(
                            text = step,
                            style = MonoSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // ===============================================================
        // IPv6
        // ===============================================================
        Text(
            text = "IPv6 压缩 / 展开",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        OutlinedTextField(
            value = state.ipv6Input,
            onValueChange = viewModel::onIpv6InputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = "IPv6 地址", style = MonoLabel) },
            placeholder = {
                Text(
                    text = "2001:0db8:0000:0000:0000:ff00:0042:8329",
                    style = MonoBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            textStyle = MonoSmall,
            singleLine = false,
            maxLines = 3,
            isError = state.ipv6Error != null,
            shape = FieldShape,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            trailingIcon = {
                if (state.ipv6Input.isNotEmpty()) {
                    IconButton(onClick = viewModel::clearIpv6) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "清空输入",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            Text(
                text = "示例：",
                style = MonoLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.Space3)
            )
            IpViewModel.IPV6_SAMPLES.forEach { sample ->
                TextButton(
                    onClick = { viewModel.fillIpv6Sample(sample) },
                    shape = ChipShape
                ) {
                    Text(text = sample, style = MonoSmall)
                }
            }
        }

        val ipv6Error = state.ipv6Error
        if (ipv6Error != null) {
            ErrorBanner(message = ipv6Error)
        }

        ResultCard(
            title = "压缩形式",
            badge = "RFC 5952",
            value = state.ipv6Compressed,
            highlighted = true,
            subtitle = "最长连续 0 段压缩为 ::（至少 2 段才会压缩）",
            emptyPlaceholder = "等待输入"
        )

        ResultCard(
            title = "完整展开",
            badge = "EXPAND",
            value = state.ipv6Expanded,
            subtitle = "8 组 × 4 位十六进制"
        )

        ResultCard(
            title = "逐组分段",
            badge = "GROUPS",
            value = state.ipv6Grouped,
            subtitle = "空格分隔，便于逐段核对"
        )

        if (state.ipv6HasResult) {
            OutlinedButton(
                onClick = viewModel::useCompressedAsInput,
                modifier = Modifier.fillMaxWidth(),
                shape = FieldShape
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconSmall)
                )
                Spacer(modifier = Modifier.width(Dimens.Space2))
                Text(text = "用压缩形式反向验证")
            }
        }

        // ---------------------------------------------------------------
        // 说明
        // ---------------------------------------------------------------
        InfoCard(title = "校验规则", borderColor = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                text = "· IPv4 必须 4 段，每段 0-255，不允许空段与非数字字符",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "· 二进制路径要求恰好 32 位；十六进制路径要求 1-8 位",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "· IPv6 支持「::」压缩、内嵌 IPv4（::ffff:192.168.1.1）与 zone id",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 统一的错误提示条 */
@Composable
internal fun ErrorBanner(message: String) {
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
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
