package com.radixlab.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/* ==========================================================================
   进化工坊 / RadixLab — 圆角 Token
   规范：小 8 / 中 12 / 大 20 / 按钮 12 / 卡片 20
   ========================================================================== */

/** 小圆角 8dp —— 标签、输入框、小图标容器 */
val RadiusSmall = 8.dp

/** 中圆角 12dp —— 按钮、次级容器 */
val RadiusMedium = 12.dp

/** 大圆角 20dp —— 卡片、弹层、图片 */
val RadiusLarge = 20.dp

/** 超大圆角 28dp —— 底部弹层（BottomSheet） */
val RadiusExtraLarge = 28.dp

/** 胶囊圆角 —— 状态点、Chip */
val RadiusPill = 999.dp

val RadixLabShapes = Shapes(
    extraSmall = RoundedCornerShape(RadiusSmall),
    small = RoundedCornerShape(RadiusSmall),
    medium = RoundedCornerShape(RadiusMedium),   // 按钮
    large = RoundedCornerShape(RadiusLarge),     // 卡片
    extraLarge = RoundedCornerShape(RadiusExtraLarge)
)

/** 按钮统一 12dp 圆角（规范：按钮 12） */
val ButtonShape = RoundedCornerShape(RadiusMedium)

/** 卡片统一 20dp 圆角（规范：卡片 20） */
val CardShape = RoundedCornerShape(RadiusLarge)

/** 输入框 / 结果块统一 12dp */
val FieldShape = RoundedCornerShape(RadiusMedium)

/** Chip 使用胶囊圆角 */
val ChipShape = RoundedCornerShape(RadiusPill)

/* ==========================================================================
   间距 / 尺寸 Token
   规范：4 / 8 / 12 / 16 / 24 / 32；按钮高 48
   与 web/css/style.css 的 --space-* 一一对应
   ========================================================================== */
object Dimens {

    /** 4dp */
    val Space1 = 4.dp

    /** 8dp */
    val Space2 = 8.dp

    /** 12dp */
    val Space3 = 12.dp

    /** 16dp */
    val Space4 = 16.dp

    /** 24dp */
    val Space6 = 24.dp

    /** 32dp */
    val Space8 = 32.dp

    /** 页面左右安全边距 */
    val PageGutter = Space4

    /** 按钮高度（规范：48） */
    val ButtonHeight = 48.dp

    /** 卡片内边距 */
    val CardPadding = Space4

    /** 小图标 18dp */
    val IconSmall = 18.dp

    /** 中图标 22dp */
    val IconMedium = 22.dp

    /** 大图标 / 图标容器 28dp */
    val IconLarge = 28.dp

    /** 图标容器（可点击）触控尺寸 */
    val TouchTarget = 44.dp

    /** 状态点 */
    val Dot = 8.dp

    /** Logo 尺寸 */
    val LogoLarge = 72.dp
    val LogoMedium = 48.dp
}
