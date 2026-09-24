package com.radixlab.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.radixlab.app.navigation.AppNavHost
import com.radixlab.app.navigation.BottomNavItem
import com.radixlab.app.viewmodel.CalculatorViewModel
import com.radixlab.app.viewmodel.ConverterViewModel
import com.radixlab.app.viewmodel.IpViewModel

/** 底部栏显隐动画时长（毫秒），与 NavHost 的推入转场大致同步 */
private const val BAR_MS = 220

/** 双击退出的判定窗口：两次返回间隔小于这个值才真的退出应用 */
private const val EXIT_WINDOW_MS = 2000L

/**
 * 应用根组件：底部导航 + 导航图。
 *
 * 视觉规范：底部栏背景使用 --surface，选中态使用 --brand-primary，
 * 未选中态使用 --text-secondary，与网站完全一致。
 *
 * 返回行为：
 * - 在工具页（子页面）：返回 = 回上一页（由 NavHost 出栈 + 转场动画完成）
 * - 在首页：第一次返回只提示「再按一次退出」，[EXIT_WINDOW_MS] 内再按一次才退出应用
 */
@Composable
fun RadixLabApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // 只有顶层页面才显示底部导航栏；进入工具页时它整体滑下去
    val showBottomBar = BottomNavItem.isTopLevel(currentRoute)

    // 提升到 Activity 作用域，保证：
    // ①「首页」与「我的」共用同一份转换状态（历史记录回填后首页立刻能看到结果）
    // ② 进工具页 → 返回 → 再进，输入内容不丢
    val converterViewModel: ConverterViewModel = viewModel()
    val calculatorViewModel: CalculatorViewModel = viewModel()
    val ipViewModel: IpViewModel = viewModel()

    val context = LocalContext.current
    // 按路由做 key：离开首页时这个时间戳自动清零，避免跨页面误判
    var lastBackAt by remember(currentRoute) { mutableStateOf(0L) }

    BackHandler(enabled = currentRoute == BottomNavItem.HOME.route) {
        val now = System.currentTimeMillis()
        if (now - lastBackAt < EXIT_WINDOW_MS) {
            context.findActivity()?.finish()
        } else {
            lastBackAt = now
            Toast.makeText(context, "再按一次退出", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(animationSpec = tween(BAR_MS)) { full -> full } +
                    fadeIn(animationSpec = tween(BAR_MS)),
                exit = slideOutVertically(animationSpec = tween(BAR_MS)) { full -> full } +
                    fadeOut(animationSpec = tween(BAR_MS))
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    // 关闭 Material 3 默认的染色提升，保证与网站底栏颜色完全一致
                    tonalElevation = 0.dp
                ) {
                    BottomNavItem.entries.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        // 单实例 + 状态保持，避免重复入栈
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(text = item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            converterViewModel = converterViewModel,
            calculatorViewModel = calculatorViewModel,
            ipViewModel = ipViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/** 从可能被包装过的 Context 里找出 Activity（Compose 里拿到的不一定是 Activity 本身） */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
