package com.radixlab.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.radixlab.app.ui.theme.Dimens
import com.radixlab.app.viewmodel.CalculatorViewModel
import com.radixlab.app.viewmodel.ConverterViewModel
import com.radixlab.app.viewmodel.IpViewModel

/**
 * 通用工具页 —— 顶部返回行 + 对应工具内容。
 *
 * 这是一个**独立路由**（`tool/{key}`），不属于底部导航的任何一个 Tab，
 * 因此外层 Scaffold 不会给它渲染底部导航栏 —— 和用户要的「像计算器那个页面」一致。
 *
 * 三个 ViewModel 都由外层提升到 Activity 作用域，所以：
 * 返回首页再进来，之前的输入还在。
 */
@Composable
internal fun ToolScreen(
    tool: HomeTool,
    converterViewModel: ConverterViewModel,
    calculatorViewModel: CalculatorViewModel,
    ipViewModel: IpViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {

        // 返回行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Dimens.Space1, end = Dimens.PageGutter)
                .padding(top = Dimens.Space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回首页",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = tool.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // v5：页头下一条 1px 细线，与网站导航下边线同构
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        when (tool) {
            HomeTool.CONVERTER -> ConverterContent(
                viewModel = converterViewModel,
                modifier = Modifier.weight(1f)
            )

            HomeTool.CALCULATOR -> CalculatorContent(
                viewModel = calculatorViewModel,
                modifier = Modifier.weight(1f)
            )

            HomeTool.IP_TOOL -> IpToolContent(
                viewModel = ipViewModel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
