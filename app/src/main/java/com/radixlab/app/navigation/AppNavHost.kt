package com.radixlab.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.radixlab.app.ui.screens.HomeScreen
import com.radixlab.app.ui.screens.HomeTool
import com.radixlab.app.ui.screens.ProfileScreen
import com.radixlab.app.ui.screens.ToolScreen
import com.radixlab.app.ui.terminal.TerminalScreen
import com.radixlab.app.viewmodel.CalculatorViewModel
import com.radixlab.app.viewmodel.ConverterViewModel
import com.radixlab.app.viewmodel.IpViewModel

/** 工具页路由（子页面，不属于底部导航） */
object ToolDestination {
    const val ARG_KEY = "toolKey"

    /** 带参数的匹配模式 */
    const val PATTERN = "tool/{$ARG_KEY}"

    /** 生成实际跳转路径 */
    fun route(key: String): String = "tool/$key"
}

/** 推入 / 弹出动画时长（毫秒） */
private const val SLIDE_MS = 320

/** 淡入淡出时长。比滑动短一点，视觉上更「跟手」。 */
private const val FADE_MS = 220

/** 退场页面的位移比例：只滑出 1/3 屏，形成「上一层被压在下面」的层次感。 */
private const val OUTGOING_SHIFT = 3

private const val TERMINAL_ROUTE = "terminal"

/**
 * 应用导航图。
 *
 * 路由结构：
 * - `home` / `profile`  —— 两个底部导航页面（顶层），显示底部导航栏
 * - `tool/{key}`        —— 进制转换 / 大数计算器 / IP 工具（子页面），**不显示**底部导航栏
 *
 * 转场规则（统一在这里配，各处不用重复写）：
 * - **Tab 之间**（home ↔ profile）：纯淡入淡出，不要左右滑动（那是「同级切换」的语义）
 * - **推入工具页**：新页面从右侧滑入，旧页面向左滑出 1/3 并淡出
 * - **返回**：正好相反 —— 工具页向右滑出，首页从左侧（1/3 处）滑回
 *
 * 三个 ViewModel 都由外层提升到 Activity 作用域，
 * 所以「首页输入 → 进工具页 → 返回 → 再进」输入内容都还在。
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    converterViewModel: ConverterViewModel,
    calculatorViewModel: CalculatorViewModel,
    ipViewModel: IpViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.START.route,
        modifier = modifier,
        enterTransition = { pushEnter() },
        exitTransition = { pushExit() },
        popEnterTransition = { popEnter() },
        popExitTransition = { popExit() }
    ) {
        composable(BottomNavItem.HOME.route) {
            HomeScreen(
                onToolClick = { tool ->
                    navController.navigate(ToolDestination.route(tool.key)) {
                        // 连点两次卡片不会入栈两次
                        launchSingleTop = true
                    }
                },
                onLogoTaps = {
                    navController.navigate(TERMINAL_ROUTE) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = TERMINAL_ROUTE,
            enterTransition = { fadeIn(animationSpec = tween(FADE_MS)) },
            exitTransition = { fadeOut(animationSpec = tween(FADE_MS)) },
            popEnterTransition = { fadeIn(animationSpec = tween(FADE_MS)) },
            popExitTransition = { fadeOut(animationSpec = tween(FADE_MS)) }
        ) {
            TerminalScreen(onExit = { navController.popBackStack() })
        }

        composable(BottomNavItem.PROFILE.route) {
            ProfileScreen(viewModel = converterViewModel)
        }

        composable(
            route = ToolDestination.PATTERN,
            arguments = listOf(navArgument(ToolDestination.ARG_KEY) { type = NavType.StringType })
        ) { entry ->
            val tool = HomeTool.fromKey(entry.arguments?.getString(ToolDestination.ARG_KEY))

            if (tool == null) {
                // 理论上不会发生（key 都是代码里写死的）；真遇到就直接弹回，别留白屏
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                ToolScreen(
                    tool = tool,
                    converterViewModel = converterViewModel,
                    calculatorViewModel = calculatorViewModel,
                    ipViewModel = ipViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// 转场动画
// ---------------------------------------------------------------------

/** 当前这次切换是否发生在两个顶层页面之间（= 底部 Tab 切换） */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    BottomNavItem.isTopLevel(initialState.destination.route) &&
        BottomNavItem.isTopLevel(targetState.destination.route)

/** 推入：新页面从右滑入 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.pushEnter(): EnterTransition =
    if (isTabSwitch()) {
        fadeIn(animationSpec = tween(FADE_MS))
    } else {
        slideInHorizontally(animationSpec = tween(SLIDE_MS)) { full -> full } +
            fadeIn(animationSpec = tween(FADE_MS))
    }

/** 推入：旧页面向左滑出 1/3 并淡出 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.pushExit(): ExitTransition =
    if (isTabSwitch()) {
        fadeOut(animationSpec = tween(FADE_MS))
    } else {
        slideOutHorizontally(animationSpec = tween(SLIDE_MS)) { full -> -full / OUTGOING_SHIFT } +
            fadeOut(animationSpec = tween(FADE_MS))
    }

/** 返回：上一页从左侧 1/3 处滑回 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition =
    if (isTabSwitch()) {
        fadeIn(animationSpec = tween(FADE_MS))
    } else {
        slideInHorizontally(animationSpec = tween(SLIDE_MS)) { full -> -full / OUTGOING_SHIFT } +
            fadeIn(animationSpec = tween(FADE_MS))
    }

/** 返回：被弹出的页面向右滑出 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition =
    if (isTabSwitch()) {
        fadeOut(animationSpec = tween(FADE_MS))
    } else {
        slideOutHorizontally(animationSpec = tween(SLIDE_MS)) { full -> full } +
            fadeOut(animationSpec = tween(FADE_MS))
    }
