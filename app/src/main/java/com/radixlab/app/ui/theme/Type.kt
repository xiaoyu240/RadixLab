package com.radixlab.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/* ==========================================================================
   进化工坊 / RadixLab — 字体规范
   中文使用系统默认字体；数字与进制结果使用 JetBrains Mono / 等宽字体
   ========================================================================== */

/**
 * 等宽字体族。
 * Android 无内置 JetBrains Mono，因此走系统等宽（Roboto Mono / Droid Sans Mono）；
 * 若要使用真正的 JetBrains Mono，把字体文件放入 res/font 并改为
 * FontFamily(Font(R.font.jetbrains_mono_regular)) 即可。
 */
val MonoFontFamily: FontFamily = FontFamily.Monospace

/** 进制结果 / 大数字展示 */
val MonoDisplay = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 26.sp,
    lineHeight = 32.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = "tnum"
)

/** 进制结果 / 中等 */
val MonoTitle = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = "tnum"
)

/** 进制结果 / 小号（历史列表、次要数值） */
val MonoSmall = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = "tnum"
)

/** 进制结果 / 正文 */
val MonoBody = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = "tnum"
)

/** 进制徽标（BIN / OCT / DEC / HEX） */
val MonoLabel = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.4.sp,
    fontFeatureSettings = "tnum"
)

private val Base = Typography()

/** 全局排版：在 Material 3 默认排版上做少量品牌化调整 */
val RadixLabTypography = Typography(
    displaySmall = Base.displaySmall.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = Base.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = Base.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = Base.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 26.sp
    ),
    titleMedium = Base.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = Base.titleSmall.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = Base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = Base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = Base.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = Base.labelLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelMedium = Base.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = Base.labelSmall.copy(fontSize = 11.sp, lineHeight = 15.sp)
)
