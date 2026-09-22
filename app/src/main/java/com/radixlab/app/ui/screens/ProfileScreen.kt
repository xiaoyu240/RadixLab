package com.radixlab.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radixlab.app.data.model.RecordType
import com.radixlab.app.ui.components.BrandLogo
import com.radixlab.app.ui.components.HistoryItem
import com.radixlab.app.ui.components.InfoCard
import com.radixlab.app.ui.theme.CardShape
import com.radixlab.app.ui.theme.ChipShape
import com.radixlab.app.ui.theme.Dimens
import com.radixlab.app.ui.theme.FieldShape
import com.radixlab.app.ui.theme.MonoSmall
import com.radixlab.app.ui.theme.ThemeMode
import com.radixlab.app.viewmodel.ConverterViewModel

/**
 * 「我的」页面。
 *
 * 外观设置（跟随系统 / 浅色 / 深色）+ 历史记录（筛选、收藏、删除、清空）。
 * 所有数据仅保存在本机 DataStore 中。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ConverterViewModel = viewModel()
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    var showFavoritesOnly by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val visibleRecords = remember(history, showFavoritesOnly) {
        if (showFavoritesOnly) history.filter { it.favorite } else history
    }
    val favoriteCount = remember(history) { history.count { it.favorite } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.PageGutter,
            end = Dimens.PageGutter,
            top = Dimens.Space6,
            bottom = Dimens.Space8
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {

        // ---------------------------------------------------------------
        // 头部
        // ---------------------------------------------------------------
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandLogo(size = Dimens.LogoMedium)
                Spacer(modifier = Modifier.width(Dimens.Space3))
                Column {
                    Text(
                        text = "我的",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "历史记录与外观设置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ---------------------------------------------------------------
        // 外观
        // ---------------------------------------------------------------
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
                ) {
                    Text(
                        text = "外观",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                        verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = ChipShape,
                                label = { Text(text = ThemeMode.label(mode)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                    Text(
                        text = "跟随系统时会随手机的深色模式自动切换；网站与 App 使用同一套颜色规范。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---------------------------------------------------------------
        // 历史记录标题 + 统计 + 清空
        // ---------------------------------------------------------------
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimens.IconMedium)
                )
                Spacer(modifier = Modifier.width(Dimens.Space2))
                Text(
                    text = "历史记录",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "共 ${history.size} 条 · 收藏 $favoriteCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
            ) {
                FilterChip(
                    selected = !showFavoritesOnly,
                    onClick = { showFavoritesOnly = false },
                    shape = ChipShape,
                    label = { Text(text = "全部 ${history.size}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = showFavoritesOnly,
                    onClick = { showFavoritesOnly = true },
                    shape = ChipShape,
                    label = { Text(text = "收藏 $favoriteCount") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.IconSmall)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                if (history.isNotEmpty()) {
                    TextButton(
                        onClick = { showClearDialog = true },
                        shape = ChipShape
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.IconSmall)
                        )
                        Spacer(modifier = Modifier.width(Dimens.Space2))
                        Text(text = "清空")
                    }
                }
            }
        }

        // ---------------------------------------------------------------
        // 列表
        // ---------------------------------------------------------------
        if (visibleRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.Space8),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                    ) {
                        Text(
                            text = if (showFavoritesOnly) "还没有收藏的记录" else "还没有历史记录",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (showFavoritesOnly) {
                                "在历史记录里点星标即可收藏常用数值"
                            } else {
                                "去首页完成一次进制转换，记录会自动出现在这里"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(items = visibleRecords, key = { it.id }) { record ->
                HistoryItem(
                    record = record,
                    onToggleFavorite = { viewModel.toggleFavorite(record.id) },
                    onDelete = { viewModel.removeRecord(record.id) },
                    onClick = {
                        if (record.type == RecordType.RADIX) {
                            viewModel.applyRecord(record)
                        }
                    }
                )
            }
        }

        // ---------------------------------------------------------------
        // 说明
        // ---------------------------------------------------------------
        item {
            InfoCard(title = "数据说明", borderColor = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    text = "· 全部记录仅保存在本机应用私有目录（DataStore），不会上传到任何服务器",
                    style = MonoSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "· 最多保留 300 条，收藏的记录不会被自动清理",
                    style = MonoSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "· 卸载应用即彻底删除全部数据",
                    style = MonoSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // -------------------------------------------------------------------
    // 清空确认对话框
    // -------------------------------------------------------------------
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape = CardShape,
            title = { Text(text = "清空全部历史记录？") },
            text = {
                Text(
                    text = "将删除全部 ${history.size} 条转换记录（含 $favoriteCount 条收藏），" +
                        "该操作不可撤销，且数据只存在于本机、无法找回。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearDialog = false
                    }
                ) {
                    Text(
                        text = "确认清空",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = "取消")
                }
            }
        )
    }
}
