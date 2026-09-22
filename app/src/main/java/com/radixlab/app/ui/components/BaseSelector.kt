package com.radixlab.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.radixlab.app.data.ConversionEngine
import com.radixlab.app.ui.theme.ChipShape
import com.radixlab.app.ui.theme.Dimens
import com.radixlab.app.ui.theme.MonoLabel
import com.radixlab.app.ui.theme.MonoTitle
import kotlin.math.roundToInt

/**
 * 进制选择器。
 *
 * 一行快捷 Chip（二进制 / 八进制 / 十进制 / 十六进制）+ 一个「自定义」入口，
 * 选择自定义时展开 2–36 的滑杆。网站与 App 使用同一套 Chip 视觉。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BaseSelector(
    title: String,
    selectedBase: Int,
    isCustom: Boolean,
    onPresetSelected: (Int) -> Unit,
    onCustomSelected: () -> Unit,
    onBaseChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    presets: List<Int> = listOf(2, 8, 10, 16)
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            presets.forEach { base ->
                val selected = !isCustom && selectedBase == base
                FilterChip(
                    selected = selected,
                    onClick = { onPresetSelected(base) },
                    shape = ChipShape,
                    label = { Text(text = ConversionEngine.baseLabel(base), style = MonoLabel) },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.IconSmall)
                            )
                        }
                    } else {
                        null
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            FilterChip(
                selected = isCustom,
                onClick = onCustomSelected,
                shape = ChipShape,
                label = {
                    Text(
                        text = if (isCustom) "自定义 $selectedBase" else "自定义 2-36",
                        style = MonoLabel
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        if (isCustom) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "当前选择 $selectedBase 进制" }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${ConversionEngine.baseName(selectedBase)}（范围 2 - 36）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "$selectedBase",
                        style = MonoTitle,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = selectedBase.toFloat(),
                    onValueChange = { onBaseChanged(it.roundToInt()) },
                    valueRange = ConversionEngine.MIN_BASE.toFloat()..ConversionEngine.MAX_BASE.toFloat(),
                    steps = ConversionEngine.MAX_BASE - ConversionEngine.MIN_BASE - 1
                )
            }
        } else {
            Spacer(modifier = Modifier.height(Dimens.Space1))
        }
    }
}
