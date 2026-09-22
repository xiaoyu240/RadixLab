package com.radixlab.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.radixlab.app.ui.screens.AboutScreen
import com.radixlab.app.ui.screens.HomeScreen
import com.radixlab.app.ui.screens.IpConverterScreen
import com.radixlab.app.ui.screens.ProfileScreen
import com.radixlab.app.viewmodel.ConverterViewModel

/**
 * 应用导航图。
 * 四个底部导航页面平级，使用 Navigation Compose 统一管理。
 *
 * [converterViewModel] 由外层提升到 Activity 作用域，首页与「我的」共用同一份状态。
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    converterViewModel: ConverterViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.START.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.HOME.route) {
            HomeScreen(viewModel = converterViewModel)
        }

        composable(BottomNavItem.IP_TOOL.route) {
            IpConverterScreen()
        }

        composable(BottomNavItem.PROFILE.route) {
            ProfileScreen(viewModel = converterViewModel)
        }

        composable(BottomNavItem.ABOUT.route) {
            AboutScreen()
        }
    }
}
