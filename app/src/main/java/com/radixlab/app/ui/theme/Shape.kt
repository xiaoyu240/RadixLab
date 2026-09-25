package com.radixlab.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/* ==========================================================================
   进制工坊 / RadixLab — 圆角 Token（v5「大字网格」）
   规范：全站直角（0），只有状态点与底部导航选中态用胶囊。
   与 web/css/style.css 的 --radius-* 一致（--radius-btn / --radius-card = 0）。
   ========================================================================== */

/** 直角 0dp —— 输入框、结果块、标签 */
val RadiusSmall = 0.dp

/** 直角 0dp —— 按钮、次级容器 */
val RadiusMedium = 0.dp

/** 直角 0dp —— 卡片、弹层 */
val RadiusLarge = 0.dp

/** 直角 0dp —— 底部弹层（BottomSheet） */
val RadiusExtraLarge = 0.dp

/** 胶囊圆角 —— 状态点、导航选中态（网站上 nav 选中态也是胶囊） */
val RadiusPill = 999.dp

/** 品牌标识专用小圆角（图标本身是圆角方块，不属于界面结构） */
val RadiusLogo = 8.dp

val RadixLabShapes = Shapes(
    extraSmall = RoundedCornerShape(RadiusSmall),
    small = RoundedCornerShape(RadiusSmall),
    medium = RoundedCornerShape(RadiusMedium),   // 按钮
    large = RoundedCornerShape(RadiusLarge),     // 卡片
    extraLarge = RoundedCornerShape(RadiusExtraLarge)
)

/** 按钮直角（规范：--radius-btn: 0） */
val ButtonShape = RoundedCornerShape(RadiusMedium)

/** 卡片直角（规范：--radius-card: 0） */
val CardShape = RoundedCornerShape(RadiusLarge)

/** 输入框 / 结果块直角 */
val FieldShape = RoundedCornerShape(RadiusMedium)

/** Chip 直角（网站上 hdemo__chip 就是直角细线方块） */
val ChipShape = RoundedCornerShape(RadiusSmall)

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
