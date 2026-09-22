package com.radixlab.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/* ==========================================================================
   进化工坊 / RadixLab — 主题入口
   网站与 App 共用同一套设计 Token；这里把 Token 注入 Material 3
   ========================================================================== */

/**
 * 主题模式：
 * - THEME_SYSTEM：跟随系统（默认）
 * - THEME_LIGHT ：强制浅色
 * - THEME_DARK  ：强制深色
 */
object ThemeMode {
    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2

    /** 用于「我的」页面展示的选项顺序 */
    val entries = listOf(SYSTEM, LIGHT, DARK)

    fun label(mode: Int): String = when (mode) {
        LIGHT -> "浅色"
        DARK -> "深色"
        else -> "跟随系统"
    }
}

/**
 * 应用主题。
 *
 * 说明：不启用 Material You 动态取色 —— 品牌色由设计规范固定为 #2563EB，
 * 必须保证在网站与 App 上完全一致。
 */
@Composable
fun RadixLabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) RadixLabDarkColors else RadixLabLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 状态栏 / 导航栏图标随主题反色，保证对比度
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RadixLabTypography,
        shapes = RadixLabShapes,
        content = content
    )
}
