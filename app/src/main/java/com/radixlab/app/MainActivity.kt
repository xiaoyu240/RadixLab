package com.radixlab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radixlab.app.data.repository.HistoryRepository
import com.radixlab.app.ui.theme.RadixLabTheme
import com.radixlab.app.ui.theme.ThemeMode

/**
 * 唯一 Activity。
 *
 * 采用边到边（edge-to-edge）显示，主题模式由本地 DataStore 决定：
 * 跟随系统 / 强制浅色 / 强制深色。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val repository = remember(context) { HistoryRepository.get(context) }
            val themeMode by repository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }

            RadixLabTheme(darkTheme = darkTheme) {
                RadixLabApp()
            }
        }
    }
}
