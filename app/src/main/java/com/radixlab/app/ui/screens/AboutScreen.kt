package com.radixlab.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.radixlab.app.BuildConfig
import com.radixlab.app.R
import com.radixlab.app.ui.components.BrandLogo
import com.radixlab.app.ui.components.InfoCard
import com.radixlab.app.ui.theme.CardShape
import com.radixlab.app.ui.theme.ChipShape
import com.radixlab.app.ui.theme.Dimens
import com.radixlab.app.ui.theme.FieldShape
import com.radixlab.app.ui.theme.MonoBody
import com.radixlab.app.ui.theme.MonoSmall

/**
 * 「关于」页面。
 *
 * 提供两个主要入口：打开 GitHub 开源项目、打开官方网站，
 * 均通过系统标准 Intent.ACTION_VIEW 交给浏览器打开。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val githubUrl = stringResource(R.string.url_github)
    val websiteUrl = stringResource(R.string.url_website)
    val privacyUrl = stringResource(R.string.url_privacy)
    val websiteRepoUrl = stringResource(R.string.url_website_repo)
    val issuesUrl = stringResource(R.string.url_issues)

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.browser_missing),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.PageGutter)
            .padding(top = Dimens.Space8, bottom = Dimens.Space8),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space4),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ---------------------------------------------------------------
        // 品牌
        // ---------------------------------------------------------------
        BrandLogo(size = Dimens.LogoLarge)

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = stringResource(R.string.app_slogan),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            style = MonoBody,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Dimens.Space1))

        Text(
            text = stringResource(R.string.app_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Dimens.Space4)
        )

        Spacer(modifier = Modifier.height(Dimens.Space2))

        // ---------------------------------------------------------------
        // 两个主要入口
        // ---------------------------------------------------------------
        Button(
            onClick = { openUrl(githubUrl) },
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.ButtonHeight),
            shape = FieldShape
        ) {
            Icon(
                imageVector = Icons.Outlined.Code,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconSmall)
            )
            Spacer(modifier = Modifier.width(Dimens.Space2))
            Text(
                text = stringResource(R.string.about_open_github),
                fontWeight = FontWeight.SemiBold
            )
        }

        OutlinedButton(
            onClick = { openUrl(websiteUrl) },
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.ButtonHeight),
            shape = FieldShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconSmall)
            )
            Spacer(modifier = Modifier.width(Dimens.Space2))
            Text(
                text = stringResource(R.string.about_open_website),
                fontWeight = FontWeight.SemiBold
            )
        }

        // ---------------------------------------------------------------
        // 更多链接
        // ---------------------------------------------------------------
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            TextButton(onClick = { openUrl(privacyUrl) }, shape = ChipShape) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconSmall)
                )
                Spacer(modifier = Modifier.width(Dimens.Space1))
                Text(text = "隐私政策")
            }
            TextButton(onClick = { openUrl(websiteRepoUrl) }, shape = ChipShape) {
                Text(text = "源码仓库")
            }
            TextButton(onClick = { openUrl(issuesUrl) }, shape = ChipShape) {
                Icon(
                    imageVector = Icons.Outlined.MailOutline,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconSmall)
                )
                Spacer(modifier = Modifier.width(Dimens.Space1))
                Text(text = "问题反馈")
            }
        }

        // ---------------------------------------------------------------
        // 应用信息
        // ---------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.CardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
            ) {
                Text(
                    text = "应用信息",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                InfoRow(label = "软件名称", value = stringResource(R.string.app_name))
                InfoRow(label = "开发工作室", value = stringResource(R.string.app_studio))
                InfoRow(label = "版本", value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                InfoRow(label = "包名", value = BuildConfig.APPLICATION_ID)
                InfoRow(label = "系统要求", value = "Android 8.0 及以上（API 26+）")
                InfoRow(label = "编译目标", value = "compileSdk 35 · targetSdk 35 · minSdk 26")
                InfoRow(label = "安装包大小", value = "约 5 MB")
                InfoRow(label = "构建类型", value = if (BuildConfig.DEBUG) "Debug（调试版）" else "Release（正式版）")
            }
        }

        // ---------------------------------------------------------------
        // 技术栈
        // ---------------------------------------------------------------
        InfoCard(title = "技术栈", borderColor = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                text = "Kotlin + Jetpack Compose + Material 3",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Navigation Compose · ViewModel · DataStore",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "BigInteger 大数运算（2-36 任意进制）",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Gradle Kotlin DSL · Version Catalog",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // ---------------------------------------------------------------
        // 隐私承诺
        // ---------------------------------------------------------------
        InfoCard(title = "隐私承诺", borderColor = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                text = "· 不申请任何权限，包括网络权限，应用无法联网",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "· 不收集任何数据，不集成任何统计或广告 SDK",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "· 历史记录与收藏仅保存在本机，卸载即彻底删除",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "· 完整源码开源，可自行审阅验证",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ---------------------------------------------------------------
        // 版权
        // ---------------------------------------------------------------
        Text(
            text = "© 2024-2026 ${stringResource(R.string.app_name)} RadixLab\n" +
                stringResource(R.string.app_studio) + " 出品\n" +
                stringResource(R.string.app_slogan),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** 信息行：左侧标签、右侧等宽值 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MonoSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
