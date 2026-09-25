package com.radixlab.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.radixlab.app.ui.components.BrandLogo
import com.radixlab.app.ui.theme.CardShape
import com.radixlab.app.ui.theme.Dimens

/** 首页工具卡片定义 */
internal enum class HomeTool(
    val key: String,
    val label: String,
    val description: String,
    val icon: ImageVector
) {
    CONVERTER(
        key = "converter",
        label = "进制转换",
        description = "2–36 任意进制互转",
        icon = Icons.Outlined.SwapHoriz
    ),
    CALCULATOR(
        key = "calculator",
        label = "大数计算器",
        description = "几十位大数精确四则运算",
        icon = Icons.Outlined.Calculate
    ),
    IP_TOOL(
        key = "ip",
        label = "IP 工具",
        description = "IPv4 / IPv6 转换解析",
        icon = Icons.Outlined.Language
    );

    companion object {
        fun fromKey(key: String?): HomeTool? = entries.firstOrNull { it.key == key }
    }
}

/**
 * 首页 —— 品牌头部 + 正方形工具卡片宫格。
 *
 * 只负责「选工具」这一件事：点击卡片时把 [HomeTool] 交给外层做导航跳转
 * （工具页是**独立路由**，所以进入后不会再显示底部导航栏，并且带转场动画）。
 * 输入状态由各工具自己的 ViewModel 保存在 Activity 作用域，返回再进不丢。
 */
@Composable
internal fun HomeScreen(
    onToolClick: (HomeTool) -> Unit,
    onLogoTaps: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapAt by remember { mutableLongStateOf(0L) }

    val logoTapModifier = Modifier.pointerInput(onLogoTaps) {
        detectTapGestures(
            onTap = {
                val now = System.currentTimeMillis()
                val next = if (now - lastTapAt > 700L) 1 else tapCount + 1
                lastTapAt = now
                if (next >= 5) {
                    tapCount = 0
                    lastTapAt = 0L
                    onLogoTaps()
                } else {
                    tapCount = next
                }
            }
        )
    }

    // 刻意不加 verticalScroll：
    // 可滚动列的高度约束是无限的，Spacer(weight(1f)) 会被当成 0，宫格就贴不到垂直居中。
    // 竖屏锁定后 3 张卡片（2 行正方形）在任何手机上都放得下，不需要滚动。
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.PageGutter)
            .padding(top = Dimens.Space6, bottom = Dimens.Space8)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = logoTapModifier
        ) {
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

        Spacer(modifier = Modifier.weight(1f))

        // 正方形工具卡片宫格：一行两张，不足的补空位（保持正方形）
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space3)) {
            HomeTool.entries.chunked(2).forEach { rowTools ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
                ) {
                    rowTools.forEach { tool ->
                        ToolCard(
                            tool = tool,
                            modifier = Modifier.weight(1f),
                            onClick = { onToolClick(tool) }
                        )
                    }
                    repeat(2 - rowTools.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "点击卡片开始使用 · 所有计算均在本机完成",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/**
 * 正方形工具卡片：图标 + 名称 + 一句话说明，点击进入工具。
 */
@Composable
private fun ToolCard(
    tool: HomeTool,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.Space4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 图标容器：圆角方块底 + 居中图标
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(Dimens.Space3)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(Dimens.IconLarge + 6.dp)
                )
            }

            Spacer(modifier = Modifier.size(Dimens.Space3))

            Text(
                text = tool.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.size(Dimens.Space1))

            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
