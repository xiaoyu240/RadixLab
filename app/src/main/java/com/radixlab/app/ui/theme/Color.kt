package com.radixlab.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/* ==========================================================================
   进制工坊 / RadixLab — 颜色 Token（v5「大字网格」）
   与 web/css/style.css 的 :root / [data-theme="dark"] 变量一一对应，
   保证网站与 App 完全一致：暖白底 + 暖夜底 + 沉稳橙红 + 1px 细线。
   深色 token 与浅色 token 成对维护：改了一边记得改另一边。
   ========================================================================== */

// ---------------------------------------------------------------------------
// 品牌色（浅色 / 深色模式共用）
// ---------------------------------------------------------------------------
val BrandPrimary = Color(0xFFEA3417)        // --brand-primary（浅色）
val BrandPrimaryDark = Color(0xFFC22B12)    // --brand-primary-dark
val BrandPrimaryLight = Color(0xFFFDEFEC)   // --brand-primary-light（rgba(234,52,23,.08) 叠白）
val BrandOnTint = Color(0xFFC22B12)         // --brand-on-tint
val BrandAccent = Color(0xFF2F9E77)         // --brand-accent
val BrandSuccess = Color(0xFF2F9E77)        // --brand-success
val BrandError = Color(0xFFD93843)          // --brand-error
val BrandWarning = Color(0xFFE8930C)        // --brand-warning

/** 深色模式下的品牌色（更亮的橙红，保证暖夜底上的对比度） */
val BrandPrimaryDarkTheme = Color(0xFFFF5A33)
val BrandPrimaryDarkThemeDeep = Color(0xFFFF7A52)

/**
 * 品牌标识底色 —— 保持启动图标那支蓝（与 res/values/colors.xml 的
 * ic_launcher_background 一致）。图标里的白色「10」是按蓝底设计的，
 * 网站导航栏用的也是这张蓝底图标，所以品牌红只管界面强调色，不管图标。
 */
val LauncherBlue = Color(0xFF2563EB)

// ---------------------------------------------------------------------------
// 浅色模式表面 Token —— 暖白，结构靠 1px 细线不靠色块
// ---------------------------------------------------------------------------
val LightBg = Color(0xFFFAFAF8)             // --bg
val LightSurface = Color(0xFFFFFFFF)        // --surface
val LightTextPrimary = Color(0xFF101010)    // --text-primary
val LightTextSecondary = Color(0xFF565656)  // --text-secondary
val LightBorder = Color(0xFFD9D9D7)         // --border（rgba(16,16,16,.14) 叠底）
val LightHairline = Color(0xFFE2E2E0)       // --hairline（.10）
val LightInset = Color(0xFFF0F0EE)          // --inset（.04）

// 派生色（同一灰阶体系）
val LightSurfaceContainer = LightInset
val LightSurfaceContainerHigh = Color(0xFFE7E7E5)
val LightOutline = Color(0xFFC9C9C7)
val LightAccentContainer = Color(0xFFEAF5F1)   // rgba(47,158,119,.10) 叠白
val LightOnAccentContainer = Color(0xFF1F6B51)

// ---------------------------------------------------------------------------
// 深色模式表面 Token —— 暖夜
// ---------------------------------------------------------------------------
val DarkBg = Color(0xFF0D0C0B)              // --bg
val DarkSurface = Color(0xFF141210)         // --surface
val DarkTextPrimary = Color(0xFFF2F1EE)     // --text-primary
val DarkTextSecondary = Color(0xFFA8A29A)   // --text-secondary
val DarkBorder = Color(0xFF2F2E2D)          // --border（.15 叠底）
val DarkHairline = Color(0xFF262524)        // --hairline（.10）
val DarkInset = Color(0xFF1D1C1B)           // --inset（.05）

// 派生色
val DarkSurfaceContainer = DarkInset
val DarkSurfaceContainerHigh = DarkHairline
val DarkPrimaryContainer = Color(0xFF351C15)   // rgba(255,90,51,.14) 叠底
val DarkOnPrimaryContainer = BrandPrimaryDarkThemeDeep
val DarkOutline = Color(0xFF3A3837)
val DarkAccentContainer = Color(0xFF182820)    // rgba(47,158,119,.16) 叠底
val DarkOnAccentContainer = Color(0xFF6FD2AE)

/* ==========================================================================
   ColorScheme
   ========================================================================== */

val RadixLabLightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryLight,
    onPrimaryContainer = BrandOnTint,
    inversePrimary = BrandPrimaryLight,

    secondary = BrandAccent,
    onSecondary = Color.White,
    secondaryContainer = LightAccentContainer,
    onSecondaryContainer = LightOnAccentContainer,

    tertiary = BrandAccent,
    onTertiary = Color.White,

    background = LightBg,
    onBackground = LightTextPrimary,

    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = LightTextSecondary,
    surfaceTint = BrandPrimary,

    inverseSurface = LightTextPrimary,
    inverseOnSurface = LightSurface,

    error = BrandError,
    onError = Color.White,
    errorContainer = Color(0xFFFBE3E5),
    onErrorContainer = Color(0xFF8C2430),

    outline = LightOutline,
    outlineVariant = LightBorder,
    scrim = Color(0xFF101010),

    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFF2F2F0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAF8),
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFFDEDEDC)
)

val RadixLabDarkColors = darkColorScheme(
    primary = BrandPrimaryDarkTheme,
    onPrimary = Color(0xFF1A0F0B),
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    inversePrimary = BrandPrimaryDarkTheme,

    secondary = BrandAccent,
    onSecondary = Color(0xFF06251C),
    secondaryContainer = DarkAccentContainer,
    onSecondaryContainer = DarkOnAccentContainer,

    tertiary = BrandAccent,
    onTertiary = Color(0xFF06251C),

    background = DarkBg,
    onBackground = DarkTextPrimary,

    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkInset,
    onSurfaceVariant = DarkTextSecondary,
    surfaceTint = BrandPrimaryDarkTheme,

    inverseSurface = DarkTextPrimary,
    inverseOnSurface = DarkBg,

    error = Color(0xFFF0959C),
    onError = Color(0xFF3A0F14),
    errorContainer = Color(0xFF4A1419),
    onErrorContainer = Color(0xFFFBE3E5),

    outline = DarkOutline,
    outlineVariant = DarkBorder,
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFF262524),
    surfaceDim = Color(0xFF0D0C0B),
    surfaceContainerLowest = Color(0xFF090807),
    surfaceContainerLow = Color(0xFF111010),
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFF312F2E)
)
