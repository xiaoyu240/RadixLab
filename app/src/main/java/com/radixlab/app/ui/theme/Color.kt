package com.radixlab.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/* ==========================================================================
   进化工坊 / RadixLab — 颜色 Token
   与 web/css/style.css 的 :root 变量一一对应，保证网站与 App 完全一致
   ========================================================================== */

// ---------------------------------------------------------------------------
// 品牌色（浅色 / 深色模式共用）
// ---------------------------------------------------------------------------
val BrandPrimary = Color(0xFF2563EB)        // --brand-primary
val BrandPrimaryDark = Color(0xFF1D4ED8)    // --brand-primary-dark
val BrandPrimaryLight = Color(0xFFDBEAFE)   // --brand-primary-light
val BrandAccent = Color(0xFF06B6D4)         // --brand-accent
val BrandSuccess = Color(0xFF10B981)        // --brand-success
val BrandError = Color(0xFFEF4444)          // --brand-error
val BrandWarning = Color(0xFFF59E0B)        // --brand-warning

// ---------------------------------------------------------------------------
// 浅色模式表面 Token
// ---------------------------------------------------------------------------
val LightBg = Color(0xFFF8FAFC)             // --bg
val LightSurface = Color(0xFFFFFFFF)        // --surface
val LightTextPrimary = Color(0xFF0F172A)    // --text-primary
val LightTextSecondary = Color(0xFF64748B)  // --text-secondary
val LightBorder = Color(0xFFE2E8F0)         // --border

// 派生色（保持同一色相体系）
val LightSurfaceContainer = Color(0xFFF1F5F9)
val LightSurfaceContainerHigh = Color(0xFFE9EEF5)
val LightOnPrimaryContainer = Color(0xFF1D4ED8)
val LightOutline = Color(0xFFCBD5E1)
val LightAccentContainer = Color(0xFFCFFAFE)
val LightOnAccentContainer = Color(0xFF0E7490)

// ---------------------------------------------------------------------------
// 深色模式表面 Token
// ---------------------------------------------------------------------------
val DarkBg = Color(0xFF0B1220)              // --bg
val DarkSurface = Color(0xFF111827)         // --surface
val DarkTextPrimary = Color(0xFFF1F5F9)     // --text-primary
val DarkTextSecondary = Color(0xFF94A3B8)   // --text-secondary
val DarkBorder = Color(0xFF1E293B)          // --border

// 派生色
val DarkSurfaceContainer = Color(0xFF151E2E)
val DarkSurfaceContainerHigh = Color(0xFF1A2438)
val DarkPrimaryContainer = Color(0xFF1E3A8A)
val DarkOnPrimaryContainer = Color(0xFFDBEAFE)
val DarkOutline = Color(0xFF334155)
val DarkAccentContainer = Color(0xFF083344)
val DarkOnAccentContainer = Color(0xFF67E8F9)
val DarkAccentText = Color(0xFF93C5FD)

/* ==========================================================================
   ColorScheme
   ========================================================================== */

val RadixLabLightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryLight,
    onPrimaryContainer = LightOnPrimaryContainer,
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
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),

    outline = LightOutline,
    outlineVariant = LightBorder,
    scrim = Color(0xFF0F172A),

    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFEEF2F7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFFE2E8F0)
)

val RadixLabDarkColors = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    inversePrimary = BrandPrimaryLight,

    secondary = BrandAccent,
    onSecondary = Color(0xFF042F3A),
    secondaryContainer = DarkAccentContainer,
    onSecondaryContainer = DarkOnAccentContainer,

    tertiary = BrandAccent,
    onTertiary = Color(0xFF042F3A),

    background = DarkBg,
    onBackground = DarkTextPrimary,

    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkBorder,
    onSurfaceVariant = DarkTextSecondary,
    surfaceTint = BrandAccent,

    inverseSurface = DarkTextPrimary,
    inverseOnSurface = DarkBg,

    error = BrandError,
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),

    outline = DarkOutline,
    outlineVariant = DarkBorder,
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFF1F2A3C),
    surfaceDim = Color(0xFF0B1220),
    surfaceContainerLowest = Color(0xFF070D17),
    surfaceContainerLow = Color(0xFF0F1626),
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFF232F45)
)
