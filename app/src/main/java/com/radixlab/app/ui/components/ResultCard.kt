package com.radixlab.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.radixlab.app.ui.theme.CardShape
import com.radixlab.app.ui.theme.Dimens
import com.radixlab.app.ui.theme.MonoDisplay
import com.radixlab.app.ui.theme.MonoLabel
import com.radixlab.app.utils.rememberCopyAction

/**
 * 结果卡。
 *
 * 规范对应：卡片 20dp 圆角、阴影 0 4px 16px rgba(15,23,42,.06)（映射为 2dp 高度），
 * 数值使用等宽字体，右侧提供一键复制。
 */
@Composable
fun ResultCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: String? = null,
    highlighted: Boolean = false,
    copyLabel: String = "RadixLab",
    emptyPlaceholder: String = "—",
    onCopy: ((String) -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null
) {
    val copy = rememberCopyAction(copyLabel)
    val hasValue = value.isNotEmpty()
    val displayValue = if (hasValue) value else emptyPlaceholder

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (badge != null) {
                    Text(
                        text = badge,
                        style = MonoLabel,
                        color = if (highlighted) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(end = Dimens.Space2)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (highlighted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                if (hasValue) {
                    IconButton(
                        onClick = { onCopy?.invoke(value) ?: copy(value) },
                        modifier = Modifier.size(Dimens.TouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "复制$title",
                            tint = if (highlighted) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(Dimens.IconMedium)
                        )
                    }
                }
            }

            Text(
                text = displayValue,
                style = MonoDisplay,
                color = when {
                    !hasValue -> MaterialTheme.colorScheme.onSurfaceVariant
                    highlighted -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                overflow = TextOverflow.Visible
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (highlighted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            footer?.invoke(this)
        }
    }
}

/**
 * 说明卡：用于承载转换步骤、辅助说明等次要信息。
 */
@Composable
fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (borderColor == Color.Unspecified) {
                MaterialTheme.colorScheme.outlineVariant
            } else {
                borderColor
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}
