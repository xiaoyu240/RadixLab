package com.radixlab.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.radixlab.app.R
import com.radixlab.app.ui.theme.BrandPrimary
import com.radixlab.app.ui.theme.BrandPrimaryDark
import com.radixlab.app.ui.theme.Dimens
import com.radixlab.app.ui.theme.RadiusLarge

/**
 * 品牌图标。
 *
 * 与网站 web/assets/icon.png、应用启动图标使用同一套图形语言：
 * 品牌蓝底 + 白色「10」（二进制）+ 青色强调点，圆角 20。
 */
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = Dimens.LogoMedium,
    shape: Shape = RoundedCornerShape(RadiusLarge)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(listOf(BrandPrimary, BrandPrimaryDark))),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "进制工坊图标",
            modifier = Modifier.fillMaxSize()
        )
    }
}
