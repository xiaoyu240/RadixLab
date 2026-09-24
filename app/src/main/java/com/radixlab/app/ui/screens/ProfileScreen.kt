package com.radixlab.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radixlab.app.BuildConfig
import com.radixlab.app.R
import com.radixlab.app.data.model.RecordType
import com.radixlab.app.ui.components.BrandLogo
import com.radixlab.app.ui.components.HistoryItem
import com.radixlab.app.ui.theme.CardShape
import com.radixlab.app.ui.theme.ChipShape
import com.radixlab.app.ui.theme.Dimens
import com.radixlab.app.ui.theme.MonoSmall
import com.radixlab.app.ui.theme.ThemeMode
import com.radixlab.app.viewmodel.ConverterViewModel

/**
 * 「我的」页面。
 *
 * 三块内容，从上到下：外观设置 / 历史记录 / 关于。
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
                Text(
                    text = "我的",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
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
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
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
                }
            }
        }

        // ---------------------------------------------------------------
        // 历史记录
        // ---------------------------------------------------------------
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "历史记录",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
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
                        Spacer(modifier = Modifier.width(Dimens.Space1))
                        Text(text = "清空")
                    }
                }
            }
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
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
            }
        }

        if (visibleRecords.isEmpty()) {
            item {
                Text(
                    text = if (showFavoritesOnly) {
                        "还没有收藏的记录，在记录里点星标即可收藏"
                    } else {
                        "还没有历史记录，去首页完成一次转换即可"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        // 关于
        // ---------------------------------------------------------------
        item {
            AboutCard()
        }

        item {
            Text(
                text = "© 2024-2026 ${stringResource(R.string.app_name)} · " +
                    stringResource(R.string.app_studio) + " 出品\n" +
                    "数据仅保存在本机，卸载即彻底删除",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
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

/**
 * 「关于」卡片：版本 + 常用链接，紧凑列表样式。
 */
@Composable
private fun AboutCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val githubUrl = stringResource(R.string.url_github)
    val websiteUrl = stringResource(R.string.url_website)
    val privacyUrl = stringResource(R.string.url_privacy)
    val issuesUrl = stringResource(R.string.url_issues)

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.browser_missing),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            Text(
                text = "关于",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            AboutRow(label = "版本", value = "v${BuildConfig.VERSION_NAME}")

            AboutLink(label = "源码仓库", icon = Icons.Outlined.Code) { openUrl(githubUrl) }
            AboutLink(label = "官方网站", icon = Icons.Outlined.Language) { openUrl(websiteUrl) }
            AboutLink(label = "隐私政策", icon = Icons.Outlined.Lock) { openUrl(privacyUrl) }
            AboutLink(label = "问题反馈", icon = Icons.Outlined.MailOutline) { openUrl(issuesUrl) }
        }
    }
}

/** 非链接信息行：左侧标签、右侧等宽值 */
@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.Space1),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MonoSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** 可点击链接行：左侧图标 + 标签，右侧箭头 */
@Composable
private fun AboutLink(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimens.IconSmall)
        )
        Spacer(modifier = Modifier.width(Dimens.Space3))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimens.IconMedium)
        )
    }
}
