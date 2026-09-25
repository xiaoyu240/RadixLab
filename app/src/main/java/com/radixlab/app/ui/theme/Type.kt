package com.radixlab.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.radixlab.app.R

/* ==========================================================================
   进制工坊 / RadixLab — 字体规范（v5「大字网格」）
   数字与标识符使用 Azeret Mono（与网站同为 Fontshare / ITF 出品，
   免费商用、无需署名）；中文由系统字体兜底，行为与网站完全一致。
   ========================================================================== */

/**
 * 等宽字体族 = Azeret Mono（随包自带，不联网下载）。
 * 只覆盖拉丁字符与数字，中文与缺字形自动回落到系统字体。
 */
val MonoFontFamily: FontFamily = FontFamily(
    Font(R.font.azeret_mono_400, FontWeight.Normal),
    Font(R.font.azeret_mono_500, FontWeight.Medium),
    Font(R.font.azeret_mono_600, FontWeight.SemiBold),
    Font(R.font.azeret_mono_700, FontWeight.Bold)
)

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

/** 全局排版：在 Material 3 默认排版上做品牌化调整 */
val RadixLabTypography = Typography(
    // 标题统一收紧字距（网站 h1/h2/h3 为 -0.035em），靠字号对比撑版面
    displaySmall = Base.displaySmall.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-1.2).sp
    ),
    headlineMedium = Base.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.9).sp
    ),
    headlineSmall = Base.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.75).sp
    ),
    titleLarge = Base.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.6).sp
    ),
    titleMedium = Base.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.5).sp
    ),
    titleSmall = Base.titleSmall.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp
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
