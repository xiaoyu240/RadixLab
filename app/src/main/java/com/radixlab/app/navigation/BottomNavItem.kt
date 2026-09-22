package com.radixlab.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航项。
 * 首页 / IP工具 / 我的 / 关于 —— 与官网导航结构保持一致。
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
    IP_TOOL(
        route = "ip",
        label = "IP工具",
        icon = Icons.Outlined.Language
    ),
    PROFILE(
        route = "profile",
        label = "我的",
        icon = Icons.Outlined.Person
    ),
    ABOUT(
        route = "about",
        label = "关于",
        icon = Icons.Outlined.Info
    );

    companion object {
        /** 起始页 */
        val START = HOME

        fun fromRoute(route: String?): BottomNavItem =
            entries.firstOrNull { it.route == route } ?: START
    }
}
