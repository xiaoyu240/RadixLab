package com.radixlab.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/* ==========================================================================
   进制工坊 / RadixLab — 按钮样式（对齐网站 v5）
   网站按钮不用品牌红，用「反色」：
   .btn--primary   → 文字色底 + 背景色字（浅色下是近黑底白字）
   .btn--secondary → 透明底 + 文字色 1px 描边
   品牌红只留给链接、徽标与选中态，避免整屏都是红色。
   ========================================================================== */

/** 主按钮 = 网站 .btn--primary（反色，直角，无阴影） */
@Composable
fun RadixPrimaryButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.onSurface,
    contentColor = MaterialTheme.colorScheme.background,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
)

/** 次按钮文字色 = 网站 .btn--secondary（文字色，不是品牌红） */
@Composable
fun RadixOutlinedButtonColors(): ButtonColors = ButtonDefaults.outlinedButtonColors(
    contentColor = MaterialTheme.colorScheme.onSurface,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
)

/** 次按钮描边 = 文字色 1px */
@Composable
fun RadixOutlinedButtonBorder(): BorderStroke =
    BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)

/** 卡片描边 = 1px 细线（v5 结构靠细线，不靠阴影） */
@Composable
fun RadixCardBorder(): BorderStroke =
    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

/** 结果卡描边 = 品牌红 1px（全屏唯一一处红色结构线） */
@Composable
fun RadixResultCardBorder(): BorderStroke =
    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
