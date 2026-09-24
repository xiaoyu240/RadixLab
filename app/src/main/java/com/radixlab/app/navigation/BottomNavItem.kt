package com.radixlab.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航项。
 *
 * 仅保留：首页 / 我的。
 * 进制转换与 IP 工具收进首页选项卡，「关于」并入「我的」页面。
 */
enum class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME(
        route = "home",
        label = "首页",
        icon = Icons.Outlined.Home
    ),
    PROFILE(
        route = "profile",
        label = "我的",
        icon = Icons.Outlined.Person
    );

    companion object {
        /** 起始页 */
        val START = HOME

        /**
         * 顶层路由集合。
         *
         * 只有这些路由才显示底部导航栏；工具页（`tool/{key}`）是子页面，
         * 不在集合里，所以进入工具后底部栏会自动消失。
         */
        val routes: Set<String> = entries.map { it.route }.toSet()

        /** 当前路由是否是顶层页面（决定底部栏显隐） */
        fun isTopLevel(route: String?): Boolean = route in routes

        fun fromRoute(route: String?): BottomNavItem =
            entries.firstOrNull { it.route == route } ?: START
    }
}
